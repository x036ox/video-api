package com.artur.youtback.service;

import com.artur.objectstorage.service.ObjectStorageService;
import com.artur.youtback.config.KafkaConfig;
import com.artur.youtback.exception.ProcessingException;
import com.artur.youtback.model.ImageUploadRequest;
import com.artur.youtback.utils.AppConstants;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.requestreply.RequestReplyFuture;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Service
public class ImageService {
    @Autowired
    private ObjectStorageService objectStorageService;
    @Autowired
    private ReplyingKafkaTemplate<String, String, Boolean> replyingKafkaTemplate;
    @Value("${application.path.default-user-picture}")
    private Resource defaultUserPicture;

    public String uploadUserPicture(ImageUploadRequest uploadRequest) throws Exception {
        return uploadImage(uploadRequest.id(),
                AppConstants.USER_PATH,
                uploadRequest.image().getOriginalFilename(),
                KafkaConfig.USER_PICTURE_INPUT_TOPIC,
                uploadRequest.image().getInputStream());
    }

    public String uploadImage(@Nullable String id,
                                    String prefix,
                                    String filename,
                                    String kafkaTopic,
                                    InputStream inputStream) throws Exception {
        if(!prefix.endsWith("/")){
            prefix += '/';
        }
        String folder;
        if(id == null){
            folder = genFolderName(prefix);
        } else {
            folder = prefix + id + '/';
        }
        String path = folder + filename;
        return saveImage(inputStream, path, kafkaTopic);
    }

    public String uploadThumbnail(ImageUploadRequest uploadRequest) throws Exception {
       return uploadImage(uploadRequest.id(),
               AppConstants.VIDEO_PATH,
               uploadRequest.image().getOriginalFilename(),
               KafkaConfig.THUMBNAIL_INPUT_TOPIC,
               uploadRequest.image().getInputStream());
    }

    private String genFolderName(@Nullable String prefix) throws Exception {
        if(prefix == null){
            prefix = "";
        } else if(!prefix.endsWith("/")){
            prefix += '/';
        }
        String name;
        do {
            name = RandomStringUtils.random(12) + '/';
        } while (!objectStorageService.listFiles(prefix + name).isEmpty());
        return name;
    }

    /**Uploads image to {@link ObjectStorageService}, sends message to Kafka for processing this image and waits until processing done.
     * @param inputStream input stream of the picture
     * @param pictureName name of the image by which it will be saved
     * @throws Exception - if can not compress this image or if {@link ObjectStorageService} can not upload this image.
     */
    private String saveImage(InputStream inputStream, String pictureName, String kafkaTopic) throws Exception {
        Assert.notNull(inputStream, "Input stream can not be null");
        Assert.notNull(pictureName, "Picture name can not be null");

        try (inputStream){
            objectStorageService.putObject(inputStream, pictureName);
            RequestReplyFuture<String, String, Boolean> response = replyingKafkaTemplate.sendAndReceive(
                    new ProducerRecord<>(kafkaTopic ,pictureName)
            );
            if(!response.get(5, TimeUnit.MINUTES).value()){
                throw new ProcessingException("User picture processing failed");
            }
            return pictureName;
        }
    }

    public InputStream getImage(String filename) throws Exception {
        return objectStorageService.getObject(filename);
    }

    public InputStream getDefaultPicture() throws IOException {
        return defaultUserPicture.getInputStream();
    }

    public void deleteImage(String filename) throws Exception {
        objectStorageService.removeObject(filename);
        String prefix = filename.substring(0, filename.lastIndexOf("/"));
        if(!objectStorageService.listFiles(prefix).isEmpty()){
            objectStorageService.removeFolder(prefix);
        }
    }
}
