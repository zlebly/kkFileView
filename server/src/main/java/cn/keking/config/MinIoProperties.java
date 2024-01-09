package cn.keking.config;

import io.minio.MinioClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * @author douwenjie
 * @create 2024-01-09
 */
@Component
@ConfigurationProperties(prefix = "min.io")
public class MinIoProperties {

    /**
     * Minio 服务端ip
     */
    private String endpoint;

    private String accessKey;

    private String secretKey;

    private String bucketName;

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    @Bean
    public MinioClient getMinioClient() {
        return MinioClient
                .builder()
                .endpoint(endpoint).credentials(accessKey, secretKey).build();
    }
}

