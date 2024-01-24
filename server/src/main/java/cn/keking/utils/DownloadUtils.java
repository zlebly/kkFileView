package cn.keking.utils;

import cn.keking.config.ConfigConstants;
import cn.keking.model.FileAttribute;
import cn.keking.model.ResponseResult;
import cn.keking.model.ReturnResponse;
import com.geor.grs.client.GrsClient;
import com.geor.grs.client.GrsClientDefault;
import io.mola.galimatias.GalimatiasParseException;
import jdk.internal.util.xml.impl.Input;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.*;
import java.net.URL;
import java.util.UUID;

import com.geor.gcf.sfp.SfpException;

import static cn.keking.utils.KkFileUtils.isFtpUrl;
import static cn.keking.utils.KkFileUtils.isHttpUrl;

/**
 * @author yudian-it
 */
public class DownloadUtils {

    private final static Logger logger = LoggerFactory.getLogger(DownloadUtils.class);
    private static final String fileDir = ConfigConstants.getFileDir();
    private static final String URL_PARAM_FTP_USERNAME = "ftp.username";
    private static final String URL_PARAM_FTP_PASSWORD = "ftp.password";
    private static final String URL_PARAM_FTP_CONTROL_ENCODING = "ftp.control.encoding";

    /**
     * @param fileAttribute fileAttribute
     * @param fileName      文件名
     * @return 本地文件绝对路径
     */
    public static ReturnResponse<String> downLoad(FileAttribute fileAttribute, String fileName) {
        // 忽略ssl证书
        String urlStr = null;
        try {
            SslUtils.ignoreSsl();
            urlStr = fileAttribute.getUrl().replaceAll("\\+", "%20");
        } catch (Exception e) {
            logger.error("忽略SSL证书异常:", e);
        }
        ReturnResponse<String> response = new ReturnResponse<>(0, "下载成功!!!", "");
        String realPath = getRelFilePath(fileName, fileAttribute);
        if (!KkFileUtils.isAllowedUpload(realPath)) {
            response.setCode(1);
            response.setContent(null);
            response.setMsg("下载失败:不支持的类型!" + urlStr);
            return response;
        }
        if(!StringUtils.hasText(realPath)){
            response.setCode(1);
            response.setContent(null);
            response.setMsg("下载失败:文件名不合法!" + urlStr);
            return response;
        }
        if(realPath.equals("cunzai")){
            response.setContent(fileDir + fileName);
            response.setMsg(fileName);
            return response;
        }
        try {
            URL url = WebUtils.normalizedURL(urlStr);
            if (!fileAttribute.getSkipDownLoad()) {
//                if (isHttpUrl(url)) {
//                    File realFile = new File(realPath);
//                    FileUtils.copyURLToFile(url, realFile);
//                } else if (isFtpUrl(url)) {
//                    String ftpUsername = WebUtils.getUrlParameterReg(fileAttribute.getUrl(), URL_PARAM_FTP_USERNAME);
//                    String ftpPassword = WebUtils.getUrlParameterReg(fileAttribute.getUrl(), URL_PARAM_FTP_PASSWORD);
//                    String ftpControlEncoding = WebUtils.getUrlParameterReg(fileAttribute.getUrl(), URL_PARAM_FTP_CONTROL_ENCODING);
//                    FtpUtils.download(fileAttribute.getUrl(), realPath, ftpUsername, ftpPassword, ftpControlEncoding);
//                } else {
//                    response.setCode(1);
//                    response.setMsg("url不能识别url" + urlStr);
//                }
                String resourceId = getResourceId(url.toString());
//                long start = System.currentTimeMillis();
                GrsClient client = new GrsClientDefault();
//                client.setHost(url.getHost(), url.getPort(),5000);
                client.setHost(url.getHost(), 9001,5000);
                client.getFile(resourceId, realPath);
//                long end = System.currentTimeMillis();
//                String socketDown = end - start + " ms";
//                logger.info("socketDown:{}", socketDown);
            }
            response.setContent(realPath);
            response.setMsg(fileName);
            return response;
        } catch (IOException | GalimatiasParseException | SfpException e) {
            logger.error("文件下载失败，url：{}", urlStr);
            response.setCode(1);
            response.setContent(null);
            if (e instanceof FileNotFoundException) {
                response.setMsg("文件不存在!!!");
            } else {
                response.setMsg(e.getMessage());
            }
            return response;
        }
    }

//    /**
//     * @param fileAttribute fileAttribute
//     * @param fileName      文件名
//     * @return 本地文件绝对路径
//     */
//    public static ReturnResponse<String> downLoadInputStream(FileAttribute fileAttribute, String fileName) {
//        // 忽略ssl证书
//        String urlStr = null;
//        try {
//            SslUtils.ignoreSsl();
//            urlStr = fileAttribute.getUrl().replaceAll("\\+", "%20");
//        } catch (Exception e) {
//            logger.error("忽略SSL证书异常:", e);
//        }
//        ReturnResponse<String> response = new ReturnResponse<>(0, "下载成功!!!", "");
//        String realPath = getRelFilePath(fileName, fileAttribute);
//        if (!KkFileUtils.isAllowedUpload(realPath)) {
//            response.setCode(1);
//            response.setContent(null);
//            response.setMsg("下载失败:不支持的类型!" + urlStr);
//            return response;
//        }
//        if(!StringUtils.hasText(realPath)){
//            response.setCode(1);
//            response.setContent(null);
//            response.setMsg("下载失败:文件名不合法!" + urlStr);
//            return response;
//        }
//        if(realPath.equals("cunzai")){
//            response.setContent(fileDir + fileName);
//            response.setMsg(fileName);
//            return response;
//        }
//        try {
//            URL url = WebUtils.normalizedURL(urlStr);
//            if (!fileAttribute.getSkipDownLoad()) {
//                if (isHttpUrl(url)) {
////                    File realFile = new File(realPath);
////                    FileUtils.copyURLToFile(url, realFile);
//                    File realFile = new File(realPath);
//                    realFile = new File(url.openStream());
//                } else {
//                    response.setCode(1);
//                    response.setMsg("url不能识别url" + urlStr);
//                }
////                String resourceId = getResourceId(url.toString());
////                GrsClient client = new GrsClientDefault();
////                client.setHost(url.getHost(), 9001,5000);
////                client.getFile(resourceId, realPath);
//            }
//            response.setContent(inputStream);
//            response.setMsg(fileName);
//            return response;
//        } catch (IOException | GalimatiasParseException e) {
//            logger.error("文件下载失败，url：{}", urlStr);
//            response.setCode(1);
//            response.setContent(null);
//            if (e instanceof FileNotFoundException) {
//                response.setMsg("文件不存在!!!");
//            } else {
//                response.setMsg(e.getMessage());
//            }
//            return response;
//        }
//    }

    /**
     * 获取真实文件绝对路径
     *
     * @param fileName 文件名
     * @return 文件路径
     */
    private static String getRelFilePath(String fileName, FileAttribute fileAttribute) {
        String type = fileAttribute.getSuffix();
        if (null == fileName) {
            UUID uuid = UUID.randomUUID();
            fileName = uuid + "." + type;
        } else { // 文件后缀不一致时，以type为准(针对simText【将类txt文件转为txt】)
            fileName = fileName.replace(fileName.substring(fileName.lastIndexOf(".") + 1), type);
        }
        // 判断是否非法地址
        if (KkFileUtils.isIllegalFileName(fileName)) {
            return null;
        }
        String realPath = fileDir + fileName;
        File dirFile = new File(fileDir);
        if (!dirFile.exists() && !dirFile.mkdirs()) {
            logger.error("创建目录【{}】失败,可能是权限不够，请检查", fileDir);
        }
        // 文件已在本地存在，跳过文件下载
        File realFile = new File(realPath);
        if (realFile.exists()) {
            fileAttribute.setSkipDownLoad(true);
            return "cunzai";
        }
        return realPath;
    }

    /**
     * 通过socket下载grs中的文件
     * @param grsii url
     * @return ResponseResult
     */
    public static ResponseResult downloadByResourceId(@RequestParam(name = "grsii", required = false) String grsii) {
        try {
            String resourceId = getResourceId(grsii);
            long start=System.currentTimeMillis();
            GrsClient client = new GrsClientDefault();
            client.setHost(grsii.split(":")[0], Integer.valueOf(grsii.split(":")[1]),5000);
            String outputDir = "./" + resourceId + ".mp4";
            client.getFile(resourceId,"./" + resourceId + ".mp4");
            long end = System.currentTimeMillis();
            String socketDown = end - start + " ms";
            return ResponseResult.success(outputDir,"文件下载耗时:" + socketDown);
        }catch (Exception e){
            return ResponseResult.failed("文件下载失败 :" + e.getMessage());
        }
    }

    private static String getResourceId(String url) {
        return url.split("resourceId=")[1].split("&filename")[0];
    }
}
