package com.artur.youtback.service;

import com.artur.youtback.YoutBackApplicationTests;
import com.artur.youtback.converter.UserConverter;
import com.artur.youtback.entity.VideoEntity;
import com.artur.youtback.entity.user.UserEntity;
import com.artur.youtback.listener.ProcessingEventHandler;
import com.artur.youtback.mediator.ProcessingEventMediator;
import com.artur.youtback.model.user.User;
import com.artur.youtback.model.user.UserCreateRequest;
import com.artur.youtback.model.user.UserUpdateRequest;
import com.artur.youtback.model.video.VideoCreateRequest;
import com.artur.youtback.repository.UserRepository;
import com.artur.youtback.repository.VideoRepository;
import com.artur.youtback.service.minio.ObjectStorageService;
import com.artur.youtback.utils.AppAuthorities;
import com.artur.youtback.utils.AppConstants;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest extends YoutBackApplicationTests {

    @Autowired
    UserService userService;
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
    @Autowired
    UserConverter userConverter;

    @Test
    @Transactional
    void createUpdateDeleteTest() throws Exception {
        when(processingEventMediator.userPictureProcessingWait(anyString())).thenReturn(true);
        Path picturePath = Path.of(TEST_IMAGE_FILE);
        MockMultipartFile picture = new MockMultipartFile("user-picture", Files.readAllBytes(picturePath));
        User user = assertDoesNotThrow(() -> userService.registerUser(new UserCreateRequest(
                "example@gmail.com",
                "test-user",
                "password",
                picture
        )));
        String id = user.getId();
        assertTrue(userRepository.existsById(id));
        verify(objectStorageService).putObject(any(InputStream.class), eq(AppConstants.USER_PATH + id + AppConstants.PROFILE_PIC_FILENAME_EXTENSION));
        verify(processingServiceTemplate, times(1)).send(eq(AppConstants.USER_PICTURE_INPUT_TOPIC), anyString(), anyString());
        verify(processingEventMediator, times(1)).userPictureProcessingWait(eq(user.getId()));

        clearInvocations(objectStorageService);
        clearInvocations(processingServiceTemplate);
        clearInvocations(processingEventMediator);
        userService.update(new UserUpdateRequest(
                "newusername",
                "new password",
                new MockMultipartFile("new-user-picture", "picture.png", "image/png", new byte[]{2, 3, 4})
        ), id);
        UserEntity userEntity = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Cannot find user"));
        verify(objectStorageService).putObject(any(), eq(AppConstants.USER_PATH + id + AppConstants.PROFILE_PIC_FILENAME_EXTENSION));
        verify(processingServiceTemplate, times(1)).send(eq(AppConstants.USER_PICTURE_INPUT_TOPIC),anyString(), anyString());
        verify(processingEventMediator, times(1)).userPictureProcessingWait(eq(user.getId().toString()));
        assertEquals("new test-user", userEntity.getUsername());

        userService.deleteById(id);
        assertTrue(userRepository.findById(id).isEmpty());
        verify(objectStorageService).removeObject( AppConstants.USER_PATH + id + AppConstants.PROFILE_PIC_FILENAME_EXTENSION);
    }

    @Test
    public void shTest(){
        System.out.println("souting");
        userRepository.findById("8ba55bc1-6b77-465f-97a0-8d72484ba63f").get().getSearchHistory().forEach(System.out::println);
    }

    @Test
    public void notInterestedTest() throws Exception {
        String id = "1";
        UserEntity userEntity = userConverter.convertToEntity(
                userService.registerUser(new UserCreateRequest(
                        "1",
                        "user",
                        AppAuthorities.ROLE_USER.name(),
                        null
                        )
                ));
        UserEntity videoOwner = userConverter.convertToEntity(
                userService.registerUser(new UserCreateRequest(
                                "2",
                                "user",
                                AppAuthorities.ROLE_USER.name(),
                                null
                        )
                ));
        VideoEntity videoEntity = videoService.create(
                new VideoCreateRequest("video",
                        "description",
                        "Sport",
                        new MockMultipartFile("thumbnail", new byte[]{2, 2}),
                        new MockMultipartFile("video", new byte[]{2, 2})),
                videoOwner.getId()
        ).orElseThrow(() -> new RuntimeException("Could not create video"));
        userEntity.getUserMetadata().getCategories().put("Sport", 4);
        userRepository.save(userEntity);            //artificially assign categories to the user
        userService.notInterested(videoEntity.getId(), userEntity.getId());
        int categoriesBefore = userEntity.getUserMetadata().getCategories().get(videoEntity.getVideoMetadata().getCategory());

        entityManager.refresh(userEntity);
        assertNotEquals(categoriesBefore, userEntity.getUserMetadata().getCategories().get(videoEntity.getVideoMetadata().getCategory()));
    }
}