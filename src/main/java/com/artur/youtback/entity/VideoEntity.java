package com.artur.youtback.entity;

import com.artur.youtback.entity.user.UserEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;


@Entity
public class VideoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @NotBlank
    private String title;
    @NotNull
    private Integer views;
    private LocalDateTime uploadDate;
    private String description;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity user;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "videoEntity")
    private Set<Like> likes = new HashSet<>();
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "videoEntity")
    @PrimaryKeyJoinColumn
    private VideoMetadata videoMetadata;

    public VideoEntity(Long id, String title, Integer views, LocalDateTime uploadDate, String description, UserEntity user) {
        this.id = id;
        this.title = title;
        this.views = views;
        this.uploadDate = uploadDate;
        this.description = description;
        this.user = user;
    }

    public VideoEntity() {
    }

    @Override
    public String toString() {
        return "id: " + id + "title: " + title + "language " + videoMetadata.getLanguage() + "likes " + likes.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VideoEntity that = (VideoEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public VideoMetadata getVideoMetadata() {
        return videoMetadata;
    }

    public void setVideoMetadata(VideoMetadata videoMetadata) {
        this.videoMetadata = videoMetadata;
    }

    public Set<Like> getLikes() {
        return likes;
    }

    public void setLikes(Set<Like> likes) {
        this.likes = likes;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getViews() {
        return views;
    }

    public void setViews(Integer views) {
        this.views = views;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
