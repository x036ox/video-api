package com.artur.youtback.entity.user;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
public class WatchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity userEntity;

    private Long videoId;

     private LocalDateTime date;

    public WatchHistory(Long id, UserEntity userEntity, Long videoId) {
        this.id = id;
        this.userEntity = userEntity;
        this.videoId = videoId;
        this.date = LocalDateTime.now();
    }

    public WatchHistory() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WatchHistory that = (WatchHistory) o;
        return Objects.equals(userEntity.getId(), that.userEntity.getId()) && Objects.equals(videoId, that.videoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userEntity.getId(), videoId);
    }

    public Long getVideoId() {
        return videoId;
    }

    public void setVideoId(Long videoId) {
        this.videoId = videoId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "WatchHistory{" +
                "userEntity=" + userEntity +
                ", videoId=" + videoId +
                ", date=" + date +
                '}';
    }

    public UserEntity getUserEntity() {
        return userEntity;
    }

    public void setUserEntity(UserEntity userEntity) {
        this.userEntity = userEntity;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }
}
