package cn.keking.service.impl;

import cn.keking.config.ConfigConstants;
import cn.keking.model.FileAttribute;
import cn.keking.model.FileType;
import cn.keking.model.ReturnResponse;
import cn.keking.service.FilePreview;
import cn.keking.utils.ConfigUtils;
import cn.keking.utils.ConvertMediaUtils;
import cn.keking.utils.DownloadUtils;
import cn.keking.service.FileHandlerService;
import cn.keking.web.filter.BaseUrlFilter;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * @author : kl
 * @authorboke : kailing.pub
 * @create : 2018-03-25 上午11:58
 * @description:
 **/
@Service
public class MediaFilePreviewImpl implements FilePreview {
    static Logger logger = LoggerFactory.getLogger(MediaFilePreviewImpl.class);
    private final FileHandlerService fileHandlerService;
    private final OtherFilePreviewImpl otherFilePreview;

    private static final Object LOCK = new Object();

    private static final List<String> VIDEO_LIST = Arrays.asList("mp3", "wav", "mp4", "flv");

    public MediaFilePreviewImpl(FileHandlerService fileHandlerService, OtherFilePreviewImpl otherFilePreview) {
        this.fileHandlerService = fileHandlerService;
        this.otherFilePreview = otherFilePreview;
    }
    @Override
    public String filePreviewHandle(String url, Model model, FileAttribute fileAttribute) {
        logger.debug("filePreviewHandle, url:[{}]", url);
        fileAttribute.setSuffix(fileAttribute.getSuffix().toLowerCase());
        fileAttribute.setName(
                fileAttribute.getName().substring(0 ,
                        fileAttribute.getName().lastIndexOf('.') + 1) + fileAttribute.getSuffix());
        if (url != null && url.toLowerCase().startsWith("http")) {
            ReturnResponse<String> response = DownloadUtils.downLoad(fileAttribute, fileAttribute.getName());
            if (response.isFailure()) {
                return otherFilePreview.notSupportedFile(model, fileAttribute, response.getMsg());
            } else {
                url=BaseUrlFilter.getBaseUrl() + fileHandlerService.getRelativePath(response.getContent());
                fileAttribute.setUrl(url);
            }
        }

        if (checkNeedConvert(fileAttribute.getSuffix()) || ConvertMediaUtils.checkAvcodec(fileAttribute)) {
            url = convertUrl(fileAttribute);
        } else {
            //正常media类型
            String[] medias = ConfigConstants.getMedia();
            for (String media : medias) {
                if (media.equals(fileAttribute.getSuffix())) {
                    model.addAttribute("mediaUrl", url);
                    return MEDIA_FILE_PREVIEW_PAGE;
                }
            }
            return otherFilePreview.notSupportedFile(model, fileAttribute, "暂不支持");
        }
        model.addAttribute("mediaUrl", url);
        return MEDIA_FILE_PREVIEW_PAGE;
    }

    /**
     * 检查视频文件处理逻辑
     * 返回处理过后的url
     * @return url
     */
    private String convertUrl(FileAttribute fileAttribute) {
        String url = fileAttribute.getUrl();
        if (fileHandlerService.listConvertedMedias().containsKey(url)) {
            url = fileHandlerService.getConvertedMedias(url);
        } else {
            if (!fileHandlerService.listConvertedMedias().containsKey(url)) {
                synchronized (LOCK) {
                    if (!fileHandlerService.listConvertedMedias().containsKey(url)) {
                        String convertedUrl = convertToMp4(fileAttribute);
                        //加入缓存
                        fileHandlerService.addConvertedMedias(url, convertedUrl);
                        url = convertedUrl;
                    }
                }
            }
        }
        logger.debug("convertUrl, url:[{}]", url);
        return url;
    }

    /**
     * 检查视频文件转换是否已开启，以及当前文件是否需要转换
     *
     * @return true-需要转换 false-不需要转换
     */
    private boolean checkNeedConvert(String suffix) {
        //1.检查开关是否开启
        if ("false".equals(ConfigConstants.getMediaConvertDisable())) {
            return false;
        }
        //2.检查当前文件是否需要转换
        String[] mediaTypesConvert = FileType.MEDIA_TYPES_CONVERT;
        for (String temp : mediaTypesConvert) {
            if (suffix.equals(temp)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 将浏览器不兼容视频格式转换成MP4
     *
     * @param fileAttribute 文件属性
     * @return videoUrl
     */
    private static String convertToMp4(FileAttribute fileAttribute) {

        // 说明：这里做临时处理，取上传文件的目录
        String homePath = ConfigUtils.getHomePath();

        String filePath = homePath + File.separator + "file" + File.separator + fileAttribute.getName();
        String convertFileName = null;

        File file = new File(filePath);
        FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(file);
        String fileName;
        Frame capturedFrame;
        FFmpegFrameRecorder recorder;
        try {
            if (VIDEO_LIST.contains(fileAttribute.getSuffix())) {
                String convert = file.getParentFile() + File.separator
                        + "SameTypeConvert";
                File convertFile =  new File(convert);
                if (!convertFile.exists()) {
                    convertFile.mkdir();
                }
                fileName = convert + File.separator + file.getName().substring(0, file.getName().indexOf(".")) + ".mp4";
                convertFileName = fileAttribute.getUrl()
                        .replace(file.getName(),
                                   "SameTypeConvert" + "/" + file.getName())
                        .replace(fileAttribute.getSuffix(), "mp4");
            } else {
                fileName = file.getAbsolutePath().replace(fileAttribute.getSuffix(), "mp4");
                convertFileName = fileAttribute.getUrl().replace(fileAttribute.getSuffix(), "mp4");
            }
            File desFile = new File(fileName);
            // 判断一下防止穿透缓存
            if (desFile.exists()) {
                logger.debug("desFile is exist, return");
                return convertFileName;
            }
            frameGrabber.start();
            recorder = new FFmpegFrameRecorder(fileName, frameGrabber.getImageWidth(), frameGrabber.getImageHeight(), frameGrabber.getAudioChannels());
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
            recorder.setFormat("mp4");
            recorder.setFrameRate(frameGrabber.getFrameRate());
            recorder.setSampleRate(frameGrabber.getSampleRate());

            recorder.setAudioChannels(frameGrabber.getAudioChannels());
            recorder.setFrameRate(frameGrabber.getFrameRate());
            recorder.start();
            while ((capturedFrame = frameGrabber.grabFrame()) != null) {
                try {
                    recorder.setTimestamp(frameGrabber.getTimestamp());
                    recorder.record(capturedFrame);
                } catch (Exception e) {
                }
            }
            recorder.stop();
            recorder.release();
            frameGrabber.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return convertFileName;
    }
}
