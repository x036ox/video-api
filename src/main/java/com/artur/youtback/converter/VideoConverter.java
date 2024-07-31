package com.artur.youtback.converter;

import com.artur.common.entity.VideoEntity;
import com.artur.common.entity.user.UserEntity;
import com.artur.youtback.model.video.Video;
import com.artur.objectstorage.service.ObjectStorageService;
import com.artur.youtback.utils.AppConstants;
import com.artur.youtback.utils.ImageUtils;
import com.artur.common.utils.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

@Component
public class VideoConverter {
    private static final Logger logger = LoggerFactory.getLogger(VideoConverter.class);

    @Autowired
    ObjectStorageService objectStorageService;

    public Video convertToModel(VideoEntity videoEntity) {
        Integer duration = videoEntity.getVideoMetadata().getDuration();
        String encodedImage = null;
        try {
            encodedImage = ImageUtils.encodeImageBase64(objectStorageService.getObject(AppConstants.VIDEO_PATH + videoEntity.getId() + "/" + AppConstants.THUMBNAIL_FILENAME));
        } catch (Exception e) {
            logger.error("Cant get thumbnail (path: "
                    + AppConstants.VIDEO_PATH + videoEntity.getId() + "/" + AppConstants.THUMBNAIL_FILENAME +
                    ") from " + objectStorageService.getClass() + "!! User has empty thumbnail displayed");
        }
        String encodedPicture = null;
        try {
            encodedPicture = ImageUtils.encodeImageBase64(objectStorageService.getObject(videoEntity.getUser().getPicture()));
        } catch (Exception e) {
            logger.error("Cant get user picture (path: "
                    + videoEntity.getUser().getPicture() +
                    ") from " + objectStorageService.getClass() + "!! User has empty thumbnail displayed");
        }
        return Video.newBuilder()
                .id(videoEntity.getId())
                .title(videoEntity.getTitle())
                .duration(TimeUtils.seccondsToString(duration,  duration >= 3600 ? "HH:mm:ss" : "mm:ss"))
                .thumbnail(encodedImage)
                .views(handleViews(videoEntity.getViews()))
                .likes(videoEntity.getLikes().size())
                .uploadDate(handleDate(videoEntity.getUploadDate()))
                .description(videoEntity.getDescription())
                .channelId(videoEntity.getUser().getId())
                .creatorPicture(encodedPicture)
                .category(videoEntity.getVideoMetadata().getCategory())
                .creatorName(videoEntity.getUser().getUsername())
                .build();
    }

    public VideoEntity convertToEntity(String title, String description, UserEntity channel){
        return new VideoEntity(
                null,
                title,
                0,
                LocalDateTime.now(),
                description,
                channel
        );
    }

    private String handleViews(Integer views){
        if(views == 1) return "1 view";
        else return Integer.toString(views).concat(" views");
    }

    private String handleDate(LocalDateTime uploadDate){
        Period period = Period.between(uploadDate.toLocalDate(), LocalDate.now());
        Duration duration = Duration.between(uploadDate, LocalDateTime.now());
        int years = period.getYears();
        if(years >= 1){
            return Integer.toString(years).concat(years == 1 ? " year" : " years").concat(" ago");
        }
        int months = period.getMonths();
        if(months >= 1){
            return Integer.toString(months).concat(months == 1 ? " month" : " months").concat(" ago");
        }
        int days = period.getDays();
        if(days >= 1){
            return Integer.toString(days).concat(days == 1 ? " day" : " days").concat(" ago");
        }
        int hours = (int)Math.floor(duration.toHours() % 24);
        if(hours >= 1){
            return Integer.toString(hours).concat(hours == 1 ? " hour" : " hours").concat(" ago");
        }
        int minutes = (int)Math.floor(duration.toMinutes() % 60);
        if(minutes >= 1){
            return Integer.toString(minutes).concat(minutes == 1 ? " minute" : " minutes").concat(" ago");
        }

        int seconds = (int)Math.ceil(duration.toSeconds() % 60);
        return Integer.toString(seconds).concat(" seconds ago");
    }
}
