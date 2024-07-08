package com.artur.youtback.service;

import com.artur.youtback.YoutBackApplicationTests;
import com.artur.youtback.config.MinioConfig;
import com.artur.youtback.entity.VideoEntity;
import com.artur.youtback.entity.user.UserEntity;
import com.artur.youtback.exception.NotFoundException;
import com.artur.youtback.listener.ProcessingEventHandler;
import com.artur.youtback.mediator.ProcessingEventMediator;
import com.artur.youtback.model.video.Video;
import com.artur.youtback.model.video.VideoUpdateRequest;
import com.artur.youtback.repository.UserRepository;
import com.artur.youtback.repository.VideoRepository;
import com.artur.youtback.service.minio.ObjectStorageService;
import com.artur.youtback.utils.AppAuthorities;
import com.artur.youtback.utils.AppConstants;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;



class VideoServiceTest extends YoutBackApplicationTests {

    @MockBean
    MinioConfig minioConfig;
    @MockBean
    ObjectStorageService objectStorageService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    VideoRepository videoRepository;
    @Autowired
    VideoService videoService;
    @Autowired
    EntityManager entityManager;
    @MockBean
    ProcessingEventMediator processingEventMediator;
    @MockBean
    ProcessingEventHandler processingEventHandler;


    @Test
    void createUpdateDeleteTest() throws Exception {
        when(processingEventMediator.thumbnailProcessingWait(anyString())).thenReturn(true);
        when(processingEventMediator.videoProcessingWait(anyString())).thenReturn(true);
        File videoFile = new File(TEST_VIDEO_FILE);
        File imageFile = new File(TEST_IMAGE_FILE);
        VideoEntity videoEntity = assertDoesNotThrow(() -> videoService.create(
                "Test video" ,
                "Description",
                "Music",
                imageFile,
                videoFile,
                UUID.randomUUID().toString()).orElseThrow(() -> new RuntimeException("User did not created")));
        long id = videoEntity.getId();
        verify(objectStorageService, times(2)).putObject(any(InputStream.class), anyString());       //uploaded thumbnail and video
        verify(processingServiceTemplate, times(1)).send(eq(AppConstants.THUMBNAIL_INPUT_TOPIC), anyString(), anyString());  //send message to process
        verify(processingServiceTemplate, times(1)).send(eq(AppConstants.VIDEO_INPUT_TOPIC), anyString(), anyString());    //send message to process
        verify(processingEventMediator, times(1)).thumbnailProcessingWait(videoEntity.getId().toString());    //wait until thumbnail processed
        verify(processingEventMediator, times(1)).videoProcessingWait(videoEntity.getId().toString());    //wait until video processed

        clearInvocations(objectStorageService);
        clearInvocations(processingServiceTemplate);
        clearInvocations(processingEventMediator);
        MockMultipartFile newVideo = new MockMultipartFile("New video", Files.readAllBytes(videoFile.toPath()));
        MockMultipartFile newThumbnail = new MockMultipartFile("New thumbnail", Files.readAllBytes(imageFile.toPath()));
        videoService.update(new VideoUpdateRequest(
                id,
                "Updated",
                "Desctiption updated",
                null,
                newVideo,
                newThumbnail
        ));
        verify(objectStorageService, times(2)).putObject(any(InputStream.class), anyString());       //uploaded picture
        verify(processingServiceTemplate, times(1)).send(eq(AppConstants.THUMBNAIL_INPUT_TOPIC), anyString(), anyString()); // send thumbnail processing message
        verify(objectStorageService, times(2)).putObject(any(InputStream.class), anyString());         //uploaded videos
        verify(processingServiceTemplate, times(1)).send(eq(AppConstants.VIDEO_INPUT_TOPIC), anyString(), anyString()); // send video processing message
        verify(processingEventMediator, times(1)).thumbnailProcessingWait(videoEntity.getId().toString());    //wait until thumbnail processed
        verify(processingEventMediator, times(1)).videoProcessingWait(videoEntity.getId().toString());    //wait until video processed

        assertTrue(videoRepository.existsById(id));
        assertNotEquals("Test video", videoEntity.getTitle());
        assertNotEquals("Description", videoEntity.getDescription());

        clearInvocations(objectStorageService);
        videoService.deleteById(videoEntity.getId());
        assertTrue(videoRepository.findById(id).isEmpty());
        verify(objectStorageService, times(1)).removeFolder(AppConstants.VIDEO_PATH + videoEntity.getId());
    }

    @Test
    public void watchByIdTest() throws NotFoundException {
        long testVideoId = 139L;
        UserEntity userEntity = userRepository.findByAuthority(AppAuthorities.ROLE_ADMIN.name(), Pageable.ofSize(1)).getFirst();
        VideoEntity videoEntity = videoRepository.findById(testVideoId).orElseThrow(() -> new RuntimeException("Video not found"));
        String videoCategory = videoEntity.getVideoMetadata().getCategory();
        String videoLanguage = videoEntity.getVideoMetadata().getLanguage();
        int categoryBefore = userEntity.getUserMetadata().getCategories().get(videoCategory) != null ?
                userEntity.getUserMetadata().getCategories().get(videoCategory) : 0;
        int languageBefore = userEntity.getUserMetadata().getLanguages().get(videoLanguage);
        int viewsBefore = videoEntity.getViews();

        entityManager.refresh(videoEntity);
        entityManager.refresh(userEntity);
        videoService.watchById(testVideoId, userEntity.getId());

        assertEquals(categoryBefore + 1, userEntity.getUserMetadata().getCategories().get(videoCategory));
        assertEquals(languageBefore + 1, userEntity.getUserMetadata().getLanguages().get(videoLanguage));
        assertTrue(userEntity.getWatchHistory().stream().anyMatch(el -> el.getVideoId() == testVideoId && el.getDate().isAfter(LocalDateTime.now().minusDays(1))));
        assertEquals(viewsBefore + 1, videoEntity.getViews());
    }


    @Test
    void findByOption() {
        List<String> options = new ArrayList<>();
        List<String> values = new ArrayList<>();
        options.add("BY_TITLE");
        values.add("Language");
        options.add("BY_ID");
        values.add("22");
        options.add("BY_VIEWS");
        values.add("22/700");
        options.add("BY_LIKES");
        values.add("22/40");
        List<Video> result = videoService.findByOption(options, values);
        assertFalse(result.isEmpty());
        Video video = result.getFirst();
        assertTrue(video.getTitle().contains("Language"));
        assertEquals(video.getId(),22L);
        assertTrue(video.getLikes() >= 22 && video.getLikes() < 40);
    }
}