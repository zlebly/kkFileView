package cn.keking.service.impl;

import cn.keking.config.ConfigConstants;
import cn.keking.model.FileAttribute;
import cn.keking.model.FileType;
import cn.keking.model.ReturnResponse;
import cn.keking.service.FilePreview;
import cn.keking.utils.ConvertMediaUtils;
import cn.keking.utils.DownloadUtils;
import cn.keking.service.FileHandlerService;
import cn.keking.web.filter.BaseUrlFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.io.FileNotFoundException;
import java.io.InputStream;

import static cn.keking.utils.ConvertMediaUtils.convertToMp4;

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
//            ReturnResponse<String> response = DownloadUtils.downLoadInputStream(fileAttribute, fileAttribute.getName());
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
}
