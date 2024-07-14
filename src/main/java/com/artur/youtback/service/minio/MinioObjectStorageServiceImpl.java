package com.artur.youtback.service.minio;

import com.artur.youtback.config.MinioConfig;
import io.minio.*;
import io.minio.messages.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class MinioObjectStorageServiceImpl implements ObjectStorageService {

    @Autowired
    MinioClient minioClient;
    @Autowired
    MinioConfig minioConfig;

    @Override
    public String putObject(InputStream objectInputStream, String objectName) throws Exception {
        return minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(minioConfig.getStoreBucket())
                        .object(objectName)
                        .stream(objectInputStream, -1, 5242880)
                        .build()
        ).object();
    }

    @Override
    public void uploadObject(File object, String pathname) throws Exception {
        minioClient.uploadObject(UploadObjectArgs.builder()
                        .object(pathname)
                        .filename(object.getAbsolutePath())
                        .bucket(minioConfig.getStoreBucket())
                .build());
    }

    @Override
    public void putFolder(String folderName) throws Exception {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(minioConfig.getStoreBucket())
                        .object(folderName)
                        .stream(new ByteArrayInputStream(new byte[] {}), 0, -1)
                        .build());
    }

    @Override
    public List<String> listFiles(String prefix) throws Exception {
        if(!prefix.endsWith("/")){
            prefix += "/";
        }
        List<String> results = new ArrayList<>();
        for (Result<Item> itemResult :
                minioClient.listObjects(
                        ListObjectsArgs.builder().bucket(minioConfig.getStoreBucket()).prefix(prefix).recursive(false).build())) {
            Item i = itemResult.get();
            if (i.isDir()) continue;
            results.add(i.objectName());
        }
        return results;
    }

    @Override
    public GetObjectResponse getObject(String objectName) throws Exception {
        return minioClient.getObject(GetObjectArgs.builder().bucket(minioConfig.getStoreBucket()).object(objectName).build());
    }

    @Override
    public void removeObject(String objectName) throws Exception {
        minioClient.removeObject(RemoveObjectArgs.builder().bucket(minioConfig.getStoreBucket()).object(objectName).build());
    }

    @Override
    public void removeFolder(String prefix) throws Exception {
        if(!prefix.endsWith("/")){
            prefix += "/";
        }
        for(var item : minioClient.listObjects(ListObjectsArgs.builder().prefix(prefix).bucket(minioConfig.getStoreBucket()).build())){
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioConfig.getStoreBucket())
                    .object(item.get().objectName())
                    .build());
        }
    }

    @Override
    public String getObjectUrl(String objectName) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .bucket(minioConfig.getStoreBucket())
                        .object(objectName)
                        .build()
        );
    }
}
