package com.artur.youtback.utils.comparators;

import com.artur.common.entity.VideoEntity;
import com.artur.youtback.utils.SortOption;
import org.springframework.lang.Nullable;

import java.util.Comparator;

public class SortOptionsComparators {

    public static Comparator<VideoEntity> get(@Nullable SortOption sortOption){
        if(sortOption == null){
            return null;
        }

        switch (sortOption){
            case BY_VIEWS_FROM_MOST -> {
                return new Comparator<VideoEntity>() {
                    @Override
                    public int compare(VideoEntity o1, VideoEntity o2) {
                        return o2.getViews() - o1.getViews();
                    }
                };
            }
            case BY_VIEWS_FROM_LEAST -> {
                return new Comparator<VideoEntity>() {
                    @Override
                    public int compare(VideoEntity o1, VideoEntity o2) {
                        return o1.getViews() - o2.getViews();
                    }
                };
            }
            case BY_DURATION_FROM_MOST -> {
                return new Comparator<VideoEntity>() {
                    @Override
                    public int compare(VideoEntity o1, VideoEntity o2) {
                        return o2.getVideoMetadata().getDuration() - o1.getVideoMetadata().getDuration();
                    }
                };
            }
            case BY_DURATION_FROM_LEAST -> {
                return new Comparator<VideoEntity>() {
                    @Override
                    public int compare(VideoEntity o1, VideoEntity o2) {
                        return o1.getVideoMetadata().getDuration() - o2.getVideoMetadata().getDuration();
                    }
                };
            }
            case BY_UPLOAD_DATE_FROM_NEWEST -> {
                return new Comparator<VideoEntity>() {
                    @Override
                    public int compare(VideoEntity o1, VideoEntity o2) {
                        return o2.getUploadDate().compareTo(o1.getUploadDate());
                    }
                };
            }
            case BY_UPLOAD_DATE_FROM_OLDEST -> {
                return new Comparator<VideoEntity>() {
                    @Override
                    public int compare(VideoEntity o1, VideoEntity o2) {
                        return o1.getUploadDate().compareTo(o2.getUploadDate());
                    }
                };
            }
            default -> {
                return new Comparator<VideoEntity>() {
                    @Override
                    public int compare(VideoEntity o1, VideoEntity o2) {
                        return o1.getVideoMetadata().getDuration() - o2.getVideoMetadata().getDuration();
                    }
                };
            }
        }
    }
}
