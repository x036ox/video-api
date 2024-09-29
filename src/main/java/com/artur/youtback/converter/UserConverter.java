package com.artur.youtback.converter;

import com.artur.common.entity.SearchHistory;
import com.artur.common.entity.user.UserEntity;
import com.artur.youtback.model.user.User;
import com.artur.objectstorage.service.ObjectStorageService;
import com.artur.youtback.utils.AppAuthorities;
import com.artur.youtback.utils.ImageUtils;
import com.artur.youtback.utils.comparators.SearchHistoryComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserConverter {
    private static final Logger logger = LoggerFactory.getLogger(UserConverter.class);

    @Autowired
    ObjectStorageService objectStorageService;
    @Autowired
    VideoConverter videoConverter;

    public User convertToModel(UserEntity userEntity){
        Set<UserEntity> subscribers = userEntity.getSubscribers();
        /*sorting search history by date added (from present to past)*/
        List<String> searchOptionList = userEntity.getSearchHistory().stream()
                .sorted(new SearchHistoryComparator()).map(SearchHistory::getSearchOption).toList();
        return User.builder()
                .id(userEntity.getId())
                .username(userEntity.getUsername())
                .picture(userEntity.getPicture())
                .subscribers(Integer.toString(subscribers.size()).concat(subscribers.size() == 1 ? " subscriber" : " subscribers"))
                .userVideos(userEntity.getUserVideos().stream().map(videoConverter::convertToModel).collect(Collectors.toList()))
                .searchHistory(userEntity.getSearchHistory().stream().map(SearchHistory::getSearchOption).toList())
                .authorities(userEntity.getAuthorities())
                .build();
    }

    public UserEntity convertToEntity(User user) {
        return new UserEntity(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPicture(),
                AppAuthorities.ROLE_USER.toString()
        );
    }
}
