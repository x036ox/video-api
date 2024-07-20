package com.artur.youtback.config;

import com.artur.objectstorage.config.MinioConfig;
import com.artur.objectstorage.service.MinioObjectStorageService;
import com.artur.objectstorage.service.ObjectStorageService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "object-storage")
public class ObjectStorageConfig {

    private String url;
    private String accessKey;
    private String secretKey;
    private String rootFolder;

    @Bean
    public ObjectStorageService objectStorageService(){
        var config = MinioConfig.builder()
                .accessKey(accessKey)
                .secretKey(secretKey)
                .url(url)
                .storeBucket(rootFolder)
                .build();
        return new MinioObjectStorageService(config);
    }
}
