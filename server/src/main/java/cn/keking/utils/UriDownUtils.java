package cn.keking.utils;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
/**
 * @author wukun
 * @version 1.0
 * @date 2023/6/13 20:45
 */
public class UriDownUtils {

    public static void downloadFile(String fileURL, String savePath) throws IOException {
        URL url = new URL(fileURL);
        BufferedInputStream inputStream = null;
        FileOutputStream outputStream = null;

        try {
            inputStream = new BufferedInputStream(url.openStream());
            outputStream = new FileOutputStream(savePath);

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer, 0, buffer.length)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        String url="http://66.6.66.234:8800/grs/file/get/gkGu^o5fwwQ8riey6xOQcYBiLSEiiU?fullfilename=gkGu^o5fwwQ8riey6xOQcYBiLSEiiU.PDF";
        UriDownUtils.downloadFile(url, "D:\\Gitee\\file-online-preview\\server\\src\\main\\file\\gkGu^o5fwwQ8riey6xOQcYBiLSEiiU.pdf");
    }
}
