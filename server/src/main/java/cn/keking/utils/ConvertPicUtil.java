package cn.keking.utils;


import cn.keking.config.ConfigConstants;

import com.sun.media.jai.codec.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.io.FileChannelRandomAccessSource;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.RandomAccessFileOrArray;
import com.itextpdf.text.pdf.codec.TiffImage;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ConvertPicUtil {

    private static final int FIT_WIDTH = 500;
    private static final int FIT_HEIGHT = 900;

    private final static Logger logger = LoggerFactory.getLogger(ConvertPicUtil.class);
    private final static String fileDir = ConfigConstants.getFileDir();
    /**
     * Tif 转  JPG。
     *
     * @param strInputFile  输入文件的路径和文件名
     * @param strOutputFile 输出文件的路径和文件名
     * @return boolean 是否转换成功
     */
    public static List<String> convertTif2Jpg(String strInputFile, String strOutputFile) {
        List<String> listImageFiles = new ArrayList<>();

        if (strInputFile == null || "".equals(strInputFile.trim())) {
            return null;
        }
        if (!new File(strInputFile).exists()) {
            logger.info("找不到文件【" + strInputFile + "】");
            return null;
        }
        strInputFile = strInputFile.replaceAll("\\\\", "/");
        FileSeekableStream fileSeekStream = null;
        try {
            JPEGEncodeParam jpegEncodeParam = new JPEGEncodeParam();
            TIFFEncodeParam tiffEncodeParam = new TIFFEncodeParam();
            tiffEncodeParam.setCompression(TIFFEncodeParam.COMPRESSION_GROUP4);
            tiffEncodeParam.setLittleEndian(false);
            fileSeekStream = new FileSeekableStream(strInputFile);
            ImageDecoder imageDecoder = ImageCodec.createImageDecoder("TIFF", fileSeekStream, null);
            int intTifCount = imageDecoder.getNumPages();
            logger.info("该tif文件共有【" + intTifCount + "】页");
            String  strJpgPath = fileDir+strOutputFile;
            // 处理目标文件夹，如果不存在则自动创建
            File fileJpgPath = new File(strJpgPath);
            if (!fileJpgPath.exists() && !fileJpgPath.mkdirs()) {
                logger.error("{} 创建失败", strJpgPath);
            }
            // 循环，处理每页tif文件，转换为jpg
            for (int i = 0; i < intTifCount; i++) {
                String strJpg= strJpgPath + "/" + i + ".jpg";
                String strJpgUrl = strOutputFile + "/" + i + ".jpg";
                File fileJpg = new File(strJpg);
                // 如果文件不存在，则生成
                if (!fileJpg.exists()) {
                    RenderedImage renderedImage = imageDecoder.decodeAsRenderedImage(i);
                    ParameterBlock pb = new ParameterBlock();
                    pb.addSource(renderedImage);
                    pb.add(fileJpg.toString());
                    pb.add("JPEG");
                    pb.add(jpegEncodeParam);
                    RenderedOp renderedOp = JAI.create("filestore", pb);
                    renderedOp.dispose();
                    logger.info("每页分别保存至： " + fileJpg.getCanonicalPath());
                }
                listImageFiles.add(strJpgUrl);
            }

            return listImageFiles;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (fileSeekStream != null) {
                try {
                    fileSeekStream.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 将Jpg图片转换为Pdf文件
     *
     * @param strJpgFile 输入的jpg的路径和文件名
     * @param strPdfFile 输出的pdf的路径和文件名
     */
    public static boolean convertJpg2Pdf(String strJpgFile, String strPdfFile) {
        Document document = null;
        RandomAccessFileOrArray rafa = null;
        try {
            document = new Document();
            PdfWriter.getInstance(document, Files.newOutputStream(Paths.get(strPdfFile)));
            document.open();
            rafa = new RandomAccessFileOrArray(new FileChannelRandomAccessSource(new RandomAccessFile(strJpgFile, "r").getChannel()));
            int pages = TiffImage.getNumberOfPages(rafa);
            Image image;
            for (int i = 1; i <= pages; i++) {
                try {
                    image = TiffImage.getTiffImage(rafa, i);
                    image.scaleToFit(FIT_WIDTH, FIT_HEIGHT);
                    document.add(image);
                } catch (Exception e) {
                    document.close();
                    rafa.close();
                    e.printStackTrace();
                }
            }
            document.close();
            rafa.close();
            return true;
        } catch (Exception e) {
            logger.error("图片转PDF异常，图片文件路径：" + strJpgFile, e);
        } finally {
            try {
                if (document != null && document.isOpen()) {
                    document.close();
                }
                if (rafa != null) {
                    rafa.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}