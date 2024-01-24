package cn.keking.utils;

import cn.keking.model.FileAttribute;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Arrays;
import java.util.List;

/**
 * @author douwenjie
 */
public class ConvertMediaUtils {
    private static final Logger logger = LoggerFactory.getLogger(ConfigUtils.class);
    private static final List<Integer> MP4_CODEC_LIST =
            Arrays.asList(avcodec.AV_CODEC_ID_H264, avcodec.AV_CODEC_ID_MPEG4, avcodec.AV_CODEC_ID_H265);

    private static final List<Integer> FLV_CODEC_LIST = Arrays.asList(avcodec.AV_CODEC_ID_H264);

    private static final List<String> VIDEO_LIST = Arrays.asList("mp3", "wav", "mp4", "flv");

    private static final List<String> CONVERT_SPECIAL = Arrays.asList("wmv");

    private static final List<String> CONVERT_SAME_TYPE = Arrays.asList("mp4", "flv");

    private static final String CONVERT_FAILED = "FAILED";
    /**
     * 获取本地ffmpeg执行器
     */
    private static final String FFMPEG_PATH = Loader.load(org.bytedeco.ffmpeg.ffmpeg.class);

    public static Boolean checkAvcodec(FileAttribute fileAttribute) {
        logger.info("start checkAvcodec");
        if (!CONVERT_SAME_TYPE.contains(fileAttribute.getSuffix())) {
            return false;
        }
        String homePath = ConfigUtils.getHomePath();
        String filePath = homePath + File.separator + "file" + File.separator + fileAttribute.getName();
        File file = new File(filePath);
        try {
            FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(file);
            frameGrabber.start();
            int videoCodec = frameGrabber.getVideoCodec();
            System.out.println("fileName:" + fileAttribute + ", videoCodec:" + videoCodec);
            if (!MP4_CODEC_LIST.contains(videoCodec)) {
                return true;
            }
            if (!FLV_CODEC_LIST.contains(frameGrabber.getVideoCodec())) {
                return true;
            }
            frameGrabber.stop();
        } catch (FrameGrabber.Exception e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    /**
     * 将浏览器不兼容视频格式转换成MP4
     *
     * @param fileAttribute 文件属性
     * @return videoUrl
     */
    public static String convertToMp4(FileAttribute fileAttribute) {

        // 这里做临时处理，取上传文件的目录
        String homePath = ConfigUtils.getHomePath();

        String filePath = homePath + File.separator + "file" + File.separator + fileAttribute.getName();
        String convertedUrl = null;
        File convertedFile = null;
        File file = new File(filePath);
        String convertedFileName;
        try {
            if (VIDEO_LIST.contains(fileAttribute.getSuffix())) {
                String convert = file.getParentFile() + File.separator
                        + "SameTypeConvert";
                File convertFile =  new File(convert);
                if (!convertFile.exists()) {
                    convertFile.mkdir();
                }
                convertedFileName = convert + File.separator + file.getName().substring(0, file.getName().indexOf(".")) + ".mp4";
                convertedUrl = fileAttribute.getUrl()
                        .replace(file.getName(),
                                "SameTypeConvert" + "/" + file.getName())
                        .replace(fileAttribute.getSuffix(), "mp4");
            } else {
                convertedFileName = file.getAbsolutePath().replace(fileAttribute.getSuffix(), "mp4");
                convertedUrl = fileAttribute.getUrl().replace(fileAttribute.getSuffix(), "mp4");
            }
            convertedFile = new File(convertedFileName);
            // 判断一下防止穿透缓存
            if (convertedFile.exists()) {
                logger.debug("convertedFile is exist, return");
                return convertedUrl;
            }

            convertToMp4ByFfmpeg(file, convertedFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return convertedUrl;
    }

    /**
     * 如果视频格式是avi/mkv/wmv，需要转码成mp4格式
     * @param file            待转文件
     * @param convertFileName 输出文件
     */
    public static void convertToMp4ByFfmpegFrame (File file, String convertFileName) {
        Frame capturedFrame;
        FFmpegFrameRecorder recorder;
        try {
            FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(file);
            frameGrabber.start();
            recorder = new FFmpegFrameRecorder(convertFileName, frameGrabber.getImageWidth(),
                    frameGrabber.getImageHeight(), frameGrabber.getAudioChannels());
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
                    logger.error(e.getMessage());
                }
            }
            recorder.stop();
            recorder.release();
            frameGrabber.stop();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(CONVERT_FAILED);
        }
    }

    /**
     * 如果视频格式是avi/mkv/wmv，需要转码成mp4格式
     * @param inputDir  待转文件
     * @param outputDir 输出路径
     */
    public static void convertToMp4ByFfmpeg (File inputDir, File outputDir) {
        if (!new File(FFMPEG_PATH).exists()) {
            logger.error(FFMPEG_PATH + " is not exists");
        }
        ProcessBuilder pb = new ProcessBuilder(FFMPEG_PATH, "-y", "-i", inputDir.getAbsolutePath(),
                "-vcodec", "h264", outputDir.getAbsolutePath());
        try {
            pb.inheritIO().start().waitFor();
        } catch (IOException | InterruptedException e) {
            logger.error("ffmpeg转换异常", e);
        }
    }
}
