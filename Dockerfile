FROM openjdk:21-jdk-slim
WORKDIR /video-api
COPY ./target/video-api-1.0.jar /video-api
COPY ./videos-to-create /video-api/videos-to-create
COPY ./user-pictures-to-create /video-api/user-pictures-to-create
COPY ./video-thumbnails-to-create /video-api/video-thumbnails-to-create
EXPOSE 8080
CMD ["java", "-jar", "video-api-1.0.jar"]