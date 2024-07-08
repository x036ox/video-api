package com.artur.youtback.utils;

import jakarta.validation.constraints.NotNull;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

public class ImageUtils {
    private static final Logger logger = LoggerFactory.getLogger(ImageUtils.class);

    public static byte[] compress(@NotNull InputStream inputStream) throws IOException {
        try (
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ){
            Thumbnails.of(inputStream)
                    .size(240, 320)
                    .outputQuality(0.6)
                    .allowOverwrite(true)
                    .toOutputStream(byteArrayOutputStream);
            return byteArrayOutputStream.toByteArray();
        }
    }

    public static String convertToString(String userId){
        //TODO: implement this method
        //TODO: make this class non-static
        return "";
    }


    public static String encodeImageBase64(InputStream inputStream){
        try(inputStream){
            return encodeImageBase64(inputStream.readAllBytes());
        } catch (IOException e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    public static String encodeImageBase64(byte[] bytes){
        StringBuilder sb = new StringBuilder();
        sb.append("data:image/");
        sb.append(AppConstants.IMAGE_FORMAT);
        sb.append(";base64, ");
        sb.append(Base64.getEncoder().encodeToString(bytes));
        return sb.toString();
    }
}
