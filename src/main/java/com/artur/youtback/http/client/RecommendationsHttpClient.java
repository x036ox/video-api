package com.artur.youtback.http.client;

import com.artur.common.exception.NotFoundException;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Component
public class RecommendationsHttpClient {
    private static final Logger logger = LoggerFactory.getLogger(RecommendationsHttpClient.class);

    @Value("${application.recommendations-service.url}")
    private String url;

    @Autowired
    RestTemplate restTemplate;

    public List<Long> getRecommendations(
            @Nullable String userId,
            @NotNull Integer page,
            @NotNull String languages,
            @NotNull Integer size) throws NotFoundException {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(url)
                .queryParam("page", page)
                .queryParam("languages", languages)
                .queryParam("size", size);
        if(userId != null){
            uriBuilder.queryParam("userId", userId);
        }
        ResponseEntity<List<Long>> response = restTemplate.exchange(
                uriBuilder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                });
        List<Long> videos = response.getBody();
        if(videos == null || videos.isEmpty()){
            throw new NotFoundException("Recommendation server returned an empty list or null");
        }
        return videos;
    }
}
