package com.artur.youtback.model.video;

import java.io.Serializable;


public class Video implements Serializable {
        public static final String  DEFAULT_THUMBNAIL = "Prewievs/thumbnail-1.webp";


        private Long id;
        private String title;
        private String duration;
        private String thumbnail;
        private String views;
        private Integer likes;
        private String uploadDate;
        private String category;
        private String description;
        private String channelId;
        private String creatorPicture;
        private String creatorName;

        public Video(Long id, String title, String duration,String thumbnail, String views, Integer likes, String uploadDate, String description, String channelId, String creatorPicture, String creatorName, String category) {
                this.id = id;
                this.title = title;
                this.duration = duration;
                this.thumbnail = thumbnail;
                this.views = views;
                this.likes = likes;
                this.uploadDate = uploadDate;
                this.description = description;
                this.channelId = channelId;
                this.creatorPicture = creatorPicture;
                this.creatorName = creatorName;
                this.category = category;
        }

        public Video(){

        }

        public static VideoBuilder newBuilder(){
                return new DefaultVideoBuilder();
        }

        public Long getId() {
                return id;
        }

        public String getTitle() {
                return title;
        }

        public String getDuration() {
                return duration;
        }

        public String getThumbnail() {
                return thumbnail;
        }

        public String getViews() {
                return views;
        }

        public Integer getLikes() {
                return likes;
        }

        public String getUploadDate() {
                return uploadDate;
        }

        public String getDescription() {
                return description;
        }

        public String getChannelId() {
                return channelId;
        }

        public String getCreatorPicture() {
                return creatorPicture;
        }

        public String getCreatorName() {
                return creatorName;
        }

        public String getCategory() {
                return category;
        }

        public void setCategory(String category) {
                this.category = category;
        }

        public interface VideoBuilder{

                VideoBuilder id(Long id);
                VideoBuilder title(String title);
                VideoBuilder description(String description);
                VideoBuilder duration(String duration);
                VideoBuilder thumbnail(String thumbnail);
                VideoBuilder views(String views);
                VideoBuilder likes(Integer likes);
                VideoBuilder uploadDate(String uploadDate);
                VideoBuilder channelId(String channelId);
                VideoBuilder creatorName(String creatorName);
                VideoBuilder creatorPicture(String creatorPicture);

                VideoBuilder category(String category);
                Video build();
        }

        private static class DefaultVideoBuilder implements VideoBuilder{
                private final Video video;

                public DefaultVideoBuilder(){
                        this.video = new Video();
                }

                @Override
                public VideoBuilder id(Long id) {
                        this.video.id = id;
                        return this;
                }

                @Override
                public VideoBuilder title(String title) {
                        this.video.title = title;
                        return this;
                }

                @Override
                public VideoBuilder description(String description) {
                        this.video.description = description;
                        return this;
                }

                @Override
                public VideoBuilder duration(String duration) {
                        this.video.duration = duration;
                        return this;
                }

                @Override
                public VideoBuilder thumbnail(String thumbnail) {
                        this.video.thumbnail = thumbnail;
                        return this;
                }

                @Override
                public VideoBuilder views(String views) {
                        this.video.views = views;
                        return this;
                }

                @Override
                public VideoBuilder likes(Integer likes) {
                        this.video.likes = likes;
                        return this;
                }

                @Override
                public VideoBuilder uploadDate(String uploadDate) {
                        this.video.uploadDate = uploadDate;
                        return this;
                }

                @Override
                public VideoBuilder channelId(String channelId) {
                        this.video.channelId = channelId;
                        return this;
                }

                @Override
                public VideoBuilder creatorName(String creatorName) {
                        this.video.creatorName = creatorName;
                        return this;
                }

                @Override
                public VideoBuilder creatorPicture(String creatorPicture) {
                        this.video.creatorPicture = creatorPicture;
                        return this;
                }

                @Override
                public VideoBuilder category(String category) {
                        this.video.category = category;
                        return this;
                }

                @Override
                public Video build() {
                        return video;
                }


        }
}


