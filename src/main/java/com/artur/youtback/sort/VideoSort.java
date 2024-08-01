package com.artur.youtback.sort;

import com.artur.common.entity.VideoEntity;
import jakarta.validation.constraints.NotNull;

import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;

public enum VideoSort {
    BY_VIEWS_FROM_DESC,
    BY_VIEWS_FROM_ASC,
    BY_UPLOAD_DATE_FROM_OLDEST,
    BY_UPLOAD_DATE_FROM_NEWEST,
    BY_DURATION_FROM_DESC,
    BY_DURATION_FROM_ACS;

    public static VideoSort convert(int sortInteger){
        switch (sortInteger) {
            case 1 -> {
                return VideoSort.BY_VIEWS_FROM_DESC;
            }
            case 2 -> {
                return VideoSort.BY_VIEWS_FROM_ASC;
            }
            case 3 -> {
                return VideoSort.BY_UPLOAD_DATE_FROM_OLDEST;
            }
            case 4 -> {
                return VideoSort.BY_UPLOAD_DATE_FROM_NEWEST;
            }
            case 5 -> {
                return VideoSort.BY_DURATION_FROM_DESC;
            }
            case 6 -> {
                return VideoSort.BY_DURATION_FROM_ACS;
            }
            default -> {
                return null;
            }
        }
    }

    public static Comparator<VideoEntity> getComparator(@NotNull VideoSort videoSort){
        Objects.requireNonNull(videoSort, "VideoSort can not be null");

        switch (videoSort){
            case BY_VIEWS_FROM_DESC -> {
                return Comparator.comparingInt(VideoEntity::getViews).reversed();
            }
            case BY_VIEWS_FROM_ASC -> {
                return Comparator.comparingInt(VideoEntity::getViews);
            }
            case BY_DURATION_FROM_DESC -> {
                return Collections.reverseOrder(Comparator.comparingInt(v -> v.getVideoMetadata().getDuration()));
            }
            case BY_DURATION_FROM_ACS -> {
                return Comparator.comparingInt(o -> o.getVideoMetadata().getDuration());
            }
            case BY_UPLOAD_DATE_FROM_NEWEST -> {
                return Comparator.comparing(VideoEntity::getUploadDate).reversed();
            }
            case BY_UPLOAD_DATE_FROM_OLDEST -> {
                return Comparator.comparing(VideoEntity::getUploadDate);
            }
            default -> {
                return Comparator.comparingInt(o -> o.getVideoMetadata().getDuration());
            }
        }
    }
}
