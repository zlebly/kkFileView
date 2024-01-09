package cn.keking.utils;

import io.minio.*;
import io.minio.messages.Bucket;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.FastByteArrayOutputStream;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author douwenjie
 * @create 2024-01-09
 */
@Component
public class MinioUtil {
    private static final Logger log = LoggerFactory.getLogger(MinioUtil.class);

    @Autowired
    private MinioClient minioClient;

    /**
     * 查看存储bucket是否存在
     *
     * @return boolean
     */
    public boolean bucketExists(String bucketName) {
        boolean found = false;
        try {
            BucketExistsArgs args = BucketExistsArgs.builder().bucket(bucketName)
                    .build();
            found = minioClient.bucketExists(args);
        } catch (Exception e) {
            log.error("bucketExists  ", e);
        }
        return found;
    }

    /**
     * 创建存储bucket
     *
     * @return Boolean
     */
    public boolean makeBucket(String bucketName) {
        try {
            MakeBucketArgs makeArgs = MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build();
            minioClient.makeBucket(makeArgs);
        } catch (Exception e) {
            log.error("makeBucket  ", e);
            return false;
        }
        return true;
    }

    /**
     * 删除存储bucket
     *
     * @return Boolean
     */
    public boolean removeBucket(String bucketName) {
        try {
            RemoveBucketArgs removeArgs = RemoveBucketArgs.builder()
                    .bucket(bucketName)
                    .build();
            minioClient.removeBucket(removeArgs);
        } catch (Exception e) {
            log.error("removeBucket  ", e);
            return false;
        }
        return true;
    }

    /**
     * 获取全部bucket
     */
    public List<Bucket> getAllBuckets() {
        try {
            return minioClient.listBuckets();
        } catch (Exception e) {
            log.error("getAllBuckets  ", e);
        }
        return null;
    }

    /**
     * 文件上传
     *
     * @param bucketName
     * @param fileName   上传文件的路径和名字
     * @param file
     * @return
     */
    public boolean upload(String bucketName, String fileName, MultipartFile file) {
        try {
            PutObjectArgs objectArgs = PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build();
            //文件名称相同会覆盖
            minioClient.putObject(objectArgs);
        } catch (Exception e) {
            log.error("upload  ", e);
            return false;
        }
        return true;
    }


    /**
     * 文件下载
     *
     * @param fileName 文件的路径和名字
     * @return Boolean
     */
    public void download(String fileName, String saveName, String bucketName) {
        DownloadObjectArgs build = DownloadObjectArgs.builder()
                .bucket(bucketName)
                .filename(saveName)
                .object(fileName)
                .build();
        try {
            minioClient.downloadObject(build);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * web下载文件
     *
     * @param fileName   文件的路径和名字
     * @param bucketName
     * @param resp
     */
    public void webDownload(String fileName, String bucketName, HttpServletResponse resp) {
        GetObjectArgs objectArgs = GetObjectArgs.builder()
                .bucket(bucketName)
                .object(fileName)
                .build();
        try (GetObjectResponse response = minioClient.getObject(objectArgs)) {
            byte[] buf = new byte[1024];
            int len;
            try (FastByteArrayOutputStream os = new FastByteArrayOutputStream()) {
                while ((len = response.read(buf)) != -1) {
                    os.write(buf, 0, len);
                }
                os.flush();
                byte[] bytes = os.toByteArray();
                resp.setCharacterEncoding("utf-8");
                //设置强制下载不打开
                //res.setContentType("application/force-download");
                resp.addHeader("Content-Disposition", "attachment;fileName=" + fileName);
                try (ServletOutputStream stream = resp.getOutputStream()) {
                    stream.write(bytes);
                    stream.flush();
                }
            }
        } catch (Exception e) {
            log.error("webDownload  ", e);
        }
    }

    /**
     * 查看文件对象
     *
     * @return 存储bucket内文件对象信息
     */
    public List<Item> listObjects(String bucketName) {
        ListObjectsArgs listObjectsArgs = ListObjectsArgs.builder()
                .bucket(bucketName)
                .build();
        Iterable<Result<Item>> results = minioClient.listObjects(listObjectsArgs);
        List<Item> itemList = new ArrayList<>();
        try {
            for (Result<Item> result : results) {
                itemList.add(result.get());
            }
        } catch (Exception e) {
            log.error("listObjects  ", e);
            return null;
        }
        return itemList;
    }

    /**
     * 删除
     *
     * @param fileName
     * @return
     * @throws Exception
     */
    public boolean removeOne(String fileName, String bucketName) {
        try {
            RemoveObjectArgs removeArgs = RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .build();
            minioClient.removeObject(removeArgs);
        } catch (Exception e) {
            log.error("remove  ", e);
            return false;
        }
        return true;
    }

    /**
     * 批量删除文件对象
     *
     * @param objects 对象名称集合
     */
    public Iterable<Result<DeleteError>> removeObjects(List<String> objects, String bucketName) {
        List<DeleteObject> dos = objects.stream().map(DeleteObject::new).collect(Collectors.toList());
        RemoveObjectsArgs build = RemoveObjectsArgs.builder()
                .bucket(bucketName)
                .objects(dos)
                .build();
        Iterable<Result<DeleteError>> results = minioClient.removeObjects(build);
        return results;
    }
}

