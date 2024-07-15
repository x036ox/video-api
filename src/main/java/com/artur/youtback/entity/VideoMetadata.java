package com.artur.youtback.entity;

import jakarta.persistence.*;

@Entity
public class VideoMetadata {

    @Id
    private Long videoId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    private VideoEntity videoEntity;

    private String language;
    private Integer duration;
    private String category = "";


    public VideoMetadata(VideoEntity videoEntity, String language, Integer duration, String category) {
        this.videoEntity = videoEntity;
        this.language = language;
        this.duration = duration;
        this.category = category;
    }

    public VideoMetadata(String language, Integer duration, String category) {
        this.language = language;
        this.duration = duration;
        this.category = category;
    }

    @Override
    public String toString() {
        return "Id: " + this.videoId + " video entity " + videoEntity.getId() + " language " + this.language + " duration " + duration;
    }

    public VideoMetadata() {

    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Long getVideoId() {
        return videoId;
    }

    public void setVideoId(Long videoId){
        this.videoId = videoId;
    }
    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public VideoEntity getVideoEntity() {
        return videoEntity;
    }

    public void setVideoEntity(VideoEntity videoEntity) {
        this.videoEntity = videoEntity;
    }
}
