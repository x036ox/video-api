package com.artur.youtback.http.client;

import com.artur.youtback.utils.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.File;

@Component
public class ImageUploadHttpClient {

    @Value("${application.url.user-picture-upload}")
    private String userPictureUploadUrl;

    @Autowired
    private RestTemplate restTemplate;

    public String uploadUserPicture(String id, File image){
        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        formData.add("id", id);
        formData.add("image", new FileSystemResource(image));

        HttpHeaders headers = getRequestHeaders();

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(formData, headers);
        var response = this.restTemplate.postForEntity(userPictureUploadUrl, requestEntity, String.class);
        if(response.getStatusCode().is2xxSuccessful()){
            return response.getBody();
        } else {
            throw new IllegalArgumentException(String.format("Something went wrong while uploading image via http, status code: %s, message: %s", response.getStatusCode(), response.getBody()));
        }
    }

    @NotNull
    private static HttpHeaders getRequestHeaders() {
        HttpHeaders headers = new HttpHeaders();
        HttpServletRequest request = RequestUtils.getCurrentHttpRequest();
        headers.set("X-Forwarded-Host", request.getHeader("Host"));
        headers.set("X-Real-Ip", request.getHeader("X-Real-Ip"));
        headers.set("X-Forwarded-Proto", request.getHeader("X-Forwarded-Proto"));
        headers.set("X-Forwarded-For", request.getRemoteAddr());
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return headers;
    }

    public void deleteUserPicture(String url){
        restTemplate.delete(url);
    }
}
