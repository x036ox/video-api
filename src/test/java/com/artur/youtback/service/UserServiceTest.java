package com.artur.youtback.service;

import com.artur.youtback.YoutBackApplicationTests;
import com.artur.youtback.converter.UserConverter;
import com.artur.common.entity.VideoEntity;
import com.artur.common.entity.user.UserEntity;
import com.artur.youtback.model.user.User;
import com.artur.youtback.model.user.UserCreateRequest;
import com.artur.youtback.model.user.UserUpdateRequest;
import com.artur.youtback.model.video.VideoCreateRequest;
import com.artur.common.repository.UserRepository;
import com.artur.common.repository.VideoRepository;
import com.artur.youtback.utils.AppAuthorities;
import com.artur.youtback.utils.AppConstants;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest extends YoutBackApplicationTests {

    @Autowired
    UserService userService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    VideoRepository videoRepository;
    @Autowired
    VideoService videoService;
    @Autowired
    EntityManager entityManager;
    @Autowired
    UserConverter userConverter;

    @Test
    @Transactional
    void createUpdateDeleteTest() throws Exception {
        User user = assertDoesNotThrow(() -> userService.registerUser(new UserCreateRequest(
                "example@gmail.com",
                "test-user",
                "example@gmail.com",
                "password",
                TEST_IMAGE_FILE
        )));
        String id = user.getId();
        assertTrue(userRepository.existsById(id));
        assertNotNull(userRepository.findById(user.getId()).get().getUserMetadata());

        clearInvocations(objectStorageService, replyingKafkaTemplate);
        userService.update(new UserUpdateRequest(
                "new test-user",
                "new password",
                new MockMultipartFile("new-user-picture", "picture.png", "image/png", new byte[]{2, 3, 4})
        ), id);
        UserEntity userEntity = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Cannot find user"));
        verify(objectStorageService).putObject(any(), anyString());
        verify(replyingKafkaTemplate, times(1)).sendAndReceive(any(ProducerRecord.class));
        assertEquals("new test-user", userEntity.getUsername());

        userService.deleteById(id);
        assertTrue(userRepository.findById(id).isEmpty());
        verify(objectStorageService).removeObject( anyString());
    }


    @Test
    public void notInterestedTest() throws Exception {
        String id = "1";
        userConverter.convertToEntity(
                userService.registerUser(new UserCreateRequest(
                        "1",
                        "user",
                        "example@gmail.com",
                        AppAuthorities.ROLE_USER.name(),
                        null
                        )
                ));
        UserEntity userEntity = userRepository.findById("1").orElseThrow();

        userConverter.convertToEntity(
        userService.registerUser(new UserCreateRequest(
                        "2",
                        "user",
                "example@gmail.com",
                        AppAuthorities.ROLE_USER.name(),
                        null
                )
        ));
        UserEntity videoOwner = userRepository.findById("2").orElseThrow();
        VideoEntity videoEntity = videoService.create(
                new VideoCreateRequest("video",
                        "description",
                        "Sport",
                        new MockMultipartFile("thumbnail", new byte[]{2, 2}),
                        new MockMultipartFile("video", Files.readAllBytes(Path.of(TEST_VIDEO_FILE)))),
                videoOwner.getId()
        );
        userEntity.getUserMetadata().getCategories().put("Sport", 4);
        userRepository.saveAndFlush(userEntity);            //artificially assign categories to the user
        userService.notInterested(videoEntity.getId(), userEntity.getId());
        int categoriesBefore = userEntity.getUserMetadata().getCategories().get(videoEntity.getVideoMetadata().getCategory());

        entityManager.refresh(userEntity);
        assertNotEquals(categoriesBefore, userEntity.getUserMetadata().getCategories().get(videoEntity.getVideoMetadata().getCategory()));
    }
}