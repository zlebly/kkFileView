package cn.keking.utils;

import cn.keking.model.FileAttribute;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FrameGrabber;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * @author douwenjie
 * @create 2023-03-22
 */
public class ConvertMediaUtils {

    private static final List<Integer> MP4_CODEC_LIST =
            Arrays.asList(avcodec.AV_CODEC_ID_H264, avcodec.AV_CODEC_ID_MPEG4);

    private static final List<Integer> FLV_CODEC_LIST = Arrays.asList(avcodec.AV_CODEC_ID_H264);
    public static Boolean checkAvcodec(FileAttribute fileAttribute) {
        String homePath = ConfigUtils.getHomePath();
        String filePath = homePath + File.separator + "file" + File.separator + fileAttribute.getName();
        File file = new File(filePath);
        FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(file);
        try {
            frameGrabber.start();
            int videoCodec = frameGrabber.getVideoCodec();
            if ("mp4".equalsIgnoreCase(fileAttribute.getSuffix())
                    && !MP4_CODEC_LIST.contains(videoCodec)) {
                return true;
            }
            if ("flv".equalsIgnoreCase(fileAttribute.getSuffix())
                    && !FLV_CODEC_LIST.contains(frameGrabber.getVideoCodec())) {
                return true;
            }
            frameGrabber.stop();
        } catch (FrameGrabber.Exception e) {
            throw new RuntimeException(e);
        }
        return false;
    }
}
