package cn.keking.service;

import cn.keking.config.ConfigConstants;
import cn.keking.model.FileAttribute;
import cn.keking.model.FileType;
import cn.keking.service.cache.CacheService;
import cn.keking.service.cache.NotResourceCache;
import cn.keking.utils.EncodingDetects;
import cn.keking.utils.KkFileUtils;
import cn.keking.utils.WebUtils;
import cn.keking.web.filter.BaseUrlFilter;
import com.aspose.cad.CodePages;
import com.aspose.cad.Color;
import com.aspose.cad.Image;
import com.aspose.cad.LoadOptions;
import com.aspose.cad.imageoptions.CadRasterizationOptions;
import com.aspose.cad.imageoptions.PdfOptions;
import com.itextpdf.text.pdf.PdfReader;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.apache.poi.EncryptedDocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * @author yudian-it
 * @date 2017/11/13
 */
@Component
public class FileHandlerService {

    private static final String PDF2JPG_IMAGE_FORMAT = ".jpg";
    private static final String PDF_PASSWORD_MSG = "password";
    private final Logger logger = LoggerFactory.getLogger(FileHandlerService.class);
    private final String fileDir = ConfigConstants.getFileDir();
    private final CacheService cacheService;

    @Value("${server.tomcat.uri-encoding:UTF-8}")
    private String uriEncoding;

    public FileHandlerService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    /**
     * @return 已转换过的文件集合(缓存)
     */
    public Map<String, String> listConvertedFiles() {
        return cacheService.getPDFCache();
    }

    /**
     * @return 已转换过的文件，根据文件名获取
     */
    public String getConvertedFile(String key) {
        return cacheService.getPDFCache(key);
    }

    /**
     * @param key pdf本地路径
     * @return 已将pdf转换成图片的图片本地相对路径
     */
    public Integer getPdf2jpgCache(String key) {
        return cacheService.getPdfImageCache(key);
    }


    /**
     * 从路径中获取文件负
     *
     * @param path 类似这种：C:\Users\yudian-it\Downloads
     * @return 文件名
     */
    public String getFileNameFromPath(String path) {
        return path.substring(path.lastIndexOf(File.separator) + 1);
    }

    /**
     * 获取相对路径
     *
     * @param absolutePath 绝对路径
     * @return 相对路径
     */
    public String getRelativePath(String absolutePath) {
        return absolutePath.substring(fileDir.length());
    }

    /**
     * 添加转换后PDF缓存
     *
     * @param fileName pdf文件名
     * @param value    缓存相对路径
     */
    public void addConvertedFile(String fileName, String value) {
        cacheService.putPDFCache(fileName, value);
    }

    /**
     * 添加转换后图片组缓存
     *
     * @param pdfFilePath pdf文件绝对路径
     * @param num         图片张数
     */
    public void addPdf2jpgCache(String pdfFilePath, int num) {
        cacheService.putPdfImageCache(pdfFilePath, num);
    }

    /**
     * 获取redis中压缩包内图片文件
     *
     * @param fileKey fileKey
     * @return 图片文件访问url列表
     */
    public List<String> getImgCache(String fileKey) {
        return cacheService.getImgCache(fileKey);
    }

    /**
     * 设置redis中压缩包内图片文件
     *
     * @param fileKey fileKey
     * @param imgs    图片文件访问url列表
     */
    public void putImgCache(String fileKey, List<String> imgs) {
        cacheService.putImgCache(fileKey, imgs);
    }

    /**
     * 对转换后的文件进行操作(改变编码方式)
     *
     * @param outFilePath 文件绝对路径
     */
    public void doActionConvertedFile(String outFilePath) {
        String charset = EncodingDetects.getJavaEncode(outFilePath);
        StringBuilder sb = new StringBuilder();
        try (InputStream inputStream = new FileInputStream(outFilePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charset))) {
            String line;
            while (null != (line = reader.readLine())) {
                if (line.contains("charset=gb2312")) {
                    line = line.replace("charset=gb2312", "charset=utf-8");
                }
                sb.append(line);
            }
            // 添加sheet控制头
            sb.append("<script src=\"js/jquery-3.6.1.min.js\" type=\"text/javascript\"></script>");
            sb.append("<script src=\"js/excel.header.js\" type=\"text/javascript\"></script>");
            sb.append("<link rel=\"stylesheet\" href=\"bootstrap/css/xlsx.css\">");
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 重新写入文件
        try (FileOutputStream fos = new FileOutputStream(outFilePath);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8))) {
            writer.write(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取本地 pdf 转 image 后的 web 访问地址
     * @param pdfName pdf文件名
     * @param index 图片索引
     * @return 图片访问地址
     */
    private String getPdf2jpgUrl(String pdfName, int index) {
        String baseUrl = BaseUrlFilter.getBaseUrl();
        String pdfFolder = pdfName.substring(0, pdfName.length() - 4);
        String urlPrefix;

        try {
            urlPrefix = baseUrl + URLEncoder.encode(pdfFolder, uriEncoding).replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            logger.error("UnsupportedEncodingException", e);
            urlPrefix = baseUrl + pdfFolder;
        }
        return urlPrefix + "/" + index + PDF2JPG_IMAGE_FORMAT;
    }

    /**
     * 获取缓存中的 pdf 转换成 jpg 图片集
     * @param pdfFilePath pdf文件路径
     * @param pdfName pdf文件名称
     * @return 图片访问集合
     */
    private List<String> loadPdf2jpgCache(String pdfFilePath, String pdfName) {
        List<String> imageUrls = new ArrayList<>();
        Integer imageCount = this.getPdf2jpgCache(pdfFilePath);
        if (Objects.isNull(imageCount)) {
            return imageUrls;
        }
        IntStream.range(0, imageCount).forEach(i -> {
            String imageUrl = this.getPdf2jpgUrl(pdfName, i);
            imageUrls.add(imageUrl);
        });
        return imageUrls;
    }

    /**
     * pdf文件转换成jpg图片集
     *
     * @param pdfFilePath pdf文件路径
     * @param pdfName     pdf文件名称
     * @return 图片访问集合
     */
    public List<String> pdf2jpg(String pdfFilePath, String pdfName, FileAttribute fileAttribute) throws Exception {
        boolean forceUpdatedCache = fileAttribute.forceUpdatedCache();
        String filePassword = fileAttribute.getFilePassword();
        String pdfPassword = null;
        PDDocument doc = null;
        PdfReader pdfReader = null;
        if (!forceUpdatedCache) {
            List<String> cacheResult = this.loadPdf2jpgCache(pdfFilePath, pdfName);
            if (!CollectionUtils.isEmpty(cacheResult)) {
                return cacheResult;
            }
        }
        List<String> imageUrls = new ArrayList<>();
        try {
            File pdfFile = new File(pdfFilePath);
            if (!pdfFile.exists()) {
                return null;
            }
            doc = PDDocument.load(pdfFile,filePassword);
            doc.setResourceCache(new NotResourceCache());
            int pageCount = doc.getNumberOfPages();
            PDFRenderer pdfRenderer = new PDFRenderer(doc);
            int index = pdfFilePath.lastIndexOf(".");
            String folder = pdfFilePath.substring(0, index);
            File path = new File(folder);
            if (!path.exists() && !path.mkdirs()) {
                logger.error("创建转换文件【{}】目录失败，请检查目录权限！", folder);
            }
            String imageFilePath;
            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                imageFilePath = folder + File.separator + pageIndex + PDF2JPG_IMAGE_FORMAT;
                BufferedImage image = pdfRenderer.renderImageWithDPI(pageIndex, ConfigConstants.getPdf2JpgDpi(), ImageType.RGB);
                ImageIOUtil.writeImage(image, imageFilePath, ConfigConstants.getPdf2JpgDpi());
                String imageUrl = this.getPdf2jpgUrl(pdfName, pageIndex);
                imageUrls.add(imageUrl);
            }
            try {
                pdfReader =  new PdfReader(pdfFilePath);   //读取PDF文件
            } catch (Exception e) {  //获取异常方法 判断是否有加密字符串
                Throwable[] throwableArray = ExceptionUtils.getThrowables(e);
                for (Throwable throwable : throwableArray) {
                    if (throwable instanceof IOException || throwable instanceof EncryptedDocumentException) {
                        if (e.getMessage().toLowerCase().contains(PDF_PASSWORD_MSG)) {
                            pdfPassword = PDF_PASSWORD_MSG;
                        }
                    }
                }
                logger.error("Convert pdf exception, pdfFilePath：{}", pdfFilePath, e);
            } finally {
                if (pdfReader != null) {   //关闭
                    pdfReader.close();
                }
            }
            //判断是否加密文件 加密文件不缓存
            if (!PDF_PASSWORD_MSG.equals(pdfPassword)) {
                this.addPdf2jpgCache(pdfFilePath, pageCount);
            }
        } catch (IOException e) {
            logger.error("Convert pdf to jpg exception, pdfFilePath：{}", pdfFilePath, e);
            throw new Exception(e);
        } finally {
            if (doc != null) {   //关闭
                doc.close();
            }
        }
        return imageUrls;
    }

    /**
     * cad文件转pdf
     *
     * @param inputFilePath  cad文件路径
     * @param outputFilePath pdf输出文件路径
     * @return 转换是否成功
     */
    public String cadToPdf(String inputFilePath, String outputFilePath)  throws Exception  {
        File outputFile = new File(outputFilePath);
        LoadOptions opts = new LoadOptions();
        opts.setSpecifiedEncoding(CodePages.SimpChinese);
        com.aspose.cad.Image cadImage = Image.load(inputFilePath, opts);
        CadRasterizationOptions cadRasterizationOptions = new CadRasterizationOptions();
        cadRasterizationOptions.setBackgroundColor(Color.getWhite());
        cadRasterizationOptions.setPageWidth(1400);
        cadRasterizationOptions.setPageHeight(650);
        cadRasterizationOptions.setAutomaticLayoutsScaling(true);
        cadRasterizationOptions.setNoScaling(false);
        cadRasterizationOptions.setDrawType(1);
        PdfOptions pdfOptions = new PdfOptions();
        pdfOptions.setVectorRasterizationOptions(cadRasterizationOptions);
        OutputStream stream = null;
        try {
            stream = new FileOutputStream(outputFile);
            cadImage.save(stream, pdfOptions);
        } catch (IOException e) {
            logger.error("PDFFileNotFoundException，inputFilePath：{}", inputFilePath, e);
            return "null";
        } finally {
            if (stream != null) {   //关闭
                stream.close();
            }
            if (cadImage != null) {   //关闭
                cadImage.close();
            }
        }
        return "true";
    }
    /**
     *
     * @param str 原字符串（待截取原串）
     * @param posStr 指定字符串
     * @return 截取截取指定字符串之后的数据
     */
    public static String getSubString(String str, String posStr){
        return str.substring(str.indexOf(posStr) + posStr.length());
    }


    /**
     * 获取文件属性
     *
     * @param url url
     * @return 文件属性
     */
    public FileAttribute getFileAttribute(String url, HttpServletRequest req) {
        FileAttribute attribute = new FileAttribute();
        String suffix;
        FileType type;
        String fileName;
        String fullFileName = WebUtils.getUrlParameterReg(url, "fullfilename");
        if (StringUtils.hasText(fullFileName)) {
            fileName = fullFileName;
            type = FileType.typeFromFileName(fullFileName);
            suffix = KkFileUtils.suffixFromFileName(fullFileName);
        } else {
            fileName = WebUtils.getFileNameFromURL(url);
            type = FileType.typeFromUrl(url);
            suffix = WebUtils.suffixFromUrl(url);
        }
        if (url.contains("?fileKey=")) {
            String[] strs = url.split("=");  //处理解压后有反代情况下 文件的路径
            String  urlStrr = getSubString(url, strs[1]);
            urlStrr =  urlStrr.substring(0,urlStrr.lastIndexOf("?"));
            fileName = strs[1] + urlStrr.trim();
            attribute.setSkipDownLoad(true);
        }
        url = WebUtils.encodeUrlFileName(url);
        fileName = KkFileUtils.htmlEscape(fileName);  //文件名处理
        attribute.setType(type);
        attribute.setName(fileName);
        attribute.setSuffix(suffix);
        attribute.setUrl(url);
        if (req != null) {
            String officePreviewType = req.getParameter("officePreviewType");
            String forceUpdatedCache = req.getParameter("forceUpdatedCache");
            String fileKey = WebUtils.getUrlParameterReg(url, "fileKey");
            if (StringUtils.hasText(officePreviewType)) {
                attribute.setOfficePreviewType(officePreviewType);
            }
            if (StringUtils.hasText(fileKey)) {
                attribute.setFileKey(fileKey);
            }
            if ("true".equalsIgnoreCase(forceUpdatedCache)) {
                attribute.setForceUpdatedCache(true);
            }

            String tifPreviewType = req.getParameter("tifPreviewType");
            if (StringUtils.hasText(tifPreviewType)) {
                attribute.setTifPreviewType(tifPreviewType);
            }

            String filePassword = req.getParameter("filePassword");
            if (StringUtils.hasText(filePassword)) {
                attribute.setFilePassword(filePassword);
            }

            String userToken = req.getParameter("userToken");
            if (StringUtils.hasText(userToken)) {
                attribute.setUserToken(userToken);
            }
        }

        return attribute;
    }

    /**
     * @return 已转换过的视频文件集合(缓存)
     */
    public Map<String, String> listConvertedMedias() {
        return cacheService.getMediaConvertCache();
    }

    /**
     * 添加转换后的视频文件缓存
     *
     * @param fileName
     * @param value
     */
    public void addConvertedMedias(String fileName, String value) {
        cacheService.putMediaConvertCache(fileName, value);
    }

    /**
     * @return 已转换视频文件缓存，根据文件名获取
     */
    public String getConvertedMedias(String key) {
        return cacheService.getMediaConvertCache(key);
    }
}
