FROM openjdk:21-jdk-slim
WORKDIR /video-api
COPY ./target/video-api-1.0.jar /video-api
COPY ./videos-to-create /yout-back/videos-to-create
COPY ./user-pictures-to-create /yout-back/user-pictures-to-create
COPY ./video-thumbnails-to-create /yout-back/video-thumbnails-to-create
EXPOSE 8080
CMD ["java", "-jar", "video-api-1.0.jar"]