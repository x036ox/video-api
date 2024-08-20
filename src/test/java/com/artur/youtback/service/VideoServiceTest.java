package com.artur.youtback.service;

import com.artur.common.entity.Like;
import com.artur.common.entity.VideoEntity;
import com.artur.common.entity.user.UserEntity;
import com.artur.common.exception.NotFoundException;
import com.artur.common.repository.UserRepository;
import com.artur.common.repository.VideoRepository;
import com.artur.youtback.YoutBackApplicationTests;
import com.artur.youtback.model.user.User;
import com.artur.youtback.model.user.UserCreateRequest;
import com.artur.youtback.model.video.Video;
import com.artur.youtback.model.video.VideoUpdateRequest;
import com.artur.youtback.utils.AppAuthorities;
import com.artur.youtback.utils.AppConstants;
import jakarta.persistence.EntityManager;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;



class VideoServiceTest extends YoutBackApplicationTests {

    @Autowired
    UserRepository userRepository;
    @Autowired
    VideoRepository videoRepository;
    @Autowired
    VideoService videoService;
    @Autowired
    EntityManager entityManager;
    @Autowired
    UserService userService;

    @Test
    void createUpdateDeleteTest() throws Exception {
        File videoFile = new File(TEST_VIDEO_FILE);
        File imageFile = new File(TEST_IMAGE_FILE);
        User user = userService.registerUser(new UserCreateRequest(
                UUID.randomUUID().toString(),
                "admin",
                "example@gmail.com",
                AppAuthorities.ROLE_USER.name(),
                null
        ));
        clearInvocations(objectStorageService, replyingKafkaTemplate);
        VideoEntity videoEntity = videoService.create(
                "Test video" ,
                "Description",
                "Music",
                imageFile,
                videoFile,
                user.getId());
        long id = videoEntity.getId();
        verify(objectStorageService, times(2)).putObject(any(InputStream.class), anyString());       //uploaded thumbnail and video
        verify(replyingKafkaTemplate, times(2)).sendAndReceive(any(ProducerRecord.class));          // send thumbnail and video processing message
        assertNotNull(videoEntity.getVideoMetadata());

        clearInvocations(objectStorageService,replyingKafkaTemplate);
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
        verify(replyingKafkaTemplate, times(2)).sendAndReceive(any(ProducerRecord.class));          // send thumbnail and video processing message
        verify(objectStorageService, times(2)).putObject(any(InputStream.class), anyString());         //uploaded videos

        assertTrue(videoRepository.existsById(id));
        assertNotEquals("Test video", videoEntity.getTitle());
        assertNotEquals("Description", videoEntity.getDescription());

        clearInvocations(objectStorageService);
        videoService.deleteById(videoEntity.getId());
        assertTrue(videoRepository.findById(id).isEmpty());
        verify(objectStorageService, times(1)).removeFolder(AppConstants.VIDEO_PATH + videoEntity.getId());
    }

    @Test
    public void watchByIdTest() throws Exception {
        UserEntity userEntity = createTestUser();
        VideoEntity videoEntity = createTestVideo(userEntity.getId());
        entityManager.flush();

        String videoCategory = videoEntity.getVideoMetadata().getCategory();
        String videoLanguage = videoEntity.getVideoMetadata().getLanguage();
        int categoryBefore = userEntity.getUserMetadata().getCategories().get(videoCategory) != null ?
                userEntity.getUserMetadata().getCategories().get(videoCategory) : 0;
        int languageBefore = userEntity.getUserMetadata().getLanguages().get(videoLanguage) != null ?
                userEntity.getUserMetadata().getLanguages().get(videoLanguage) : 0;
        int viewsBefore = videoEntity.getViews();

        videoService.watchById(videoEntity.getId(), userEntity.getId());
        entityManager.flush();
        entityManager.refresh(videoEntity);
        entityManager.refresh(userEntity);

        assertEquals(categoryBefore + 1, userEntity.getUserMetadata().getCategories().get(videoCategory));
        assertEquals(languageBefore + 1, userEntity.getUserMetadata().getLanguages().get(videoLanguage));
        assertTrue(userEntity.getWatchHistory().stream().anyMatch(el -> Objects.equals(el.getVideoId(), videoEntity.getId()) && el.getDate().isAfter(Instant.now().minus(1,ChronoUnit.DAYS))));
        assertEquals(viewsBefore + 1, videoEntity.getViews());
    }


    @Test
    void findByOption() throws Exception {
        UserEntity userEntity = createTestUser();
        VideoEntity videoEntity = createTestVideo(userEntity.getId());

        videoEntity.setViews(100);
        videoEntity.setTitle("Language 'English'");
        videoEntity.setTitle("Language 'English'");
        videoEntity.setLikes(Set.of(Like.create(userEntity, videoEntity, Instant.now().minus(2, ChronoUnit.DAYS))));

        List<Video> result = videoService.findByOption(
                List.of(
                        "BY_LIKES",
                        "BY_VIEWS",
                        "BY_TITLE"
                ),
                List.of(
                        "1/40",
                        "22/700",
                        "Language"
                ));
        assertFalse(result.isEmpty());
        Video video = result.getFirst();
        assertTrue(video.getTitle().contains("Language"));
        assertTrue(video.getLikes() >= 1 && video.getLikes() < 40);
    }


    private VideoEntity createTestVideo(String userId) throws Exception {
        var entity = videoService.create(
                "some video",
                "description",
                "Sport",
                new File(TEST_IMAGE_FILE),
                new File(TEST_VIDEO_FILE),
                userId
        );
        videoRepository.flush();
        return entity;
    }

    private UserEntity createTestUser() throws Exception {
        String id = UUID.randomUUID().toString();
        userService.registerUser(new UserCreateRequest(
                id,
                "admin",
                "example@gmail.com",
                AppAuthorities.ROLE_USER.name(),
                null
        ));
        userRepository.flush();
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("User was to found"));
    }
}