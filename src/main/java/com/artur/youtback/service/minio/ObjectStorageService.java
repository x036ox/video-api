package com.artur.youtback.service.minio;

import io.minio.GetObjectResponse;

import java.io.File;
import java.io.InputStream;
import java.util.List;

public interface ObjectStorageService {

    String putObject(InputStream objectInputStream, String objectName) throws Exception;

    void uploadObject(File object, String pathname) throws Exception;

    void putFolder(String folderName) throws Exception;

    List<String> listFiles(String prefix) throws Exception;

    InputStream getObject(String objectName) throws Exception;

    void removeObject(String objectName) throws Exception;

    void removeFolder(String prefix) throws Exception;

    String getObjectUrl(String objectName) throws Exception;
}
