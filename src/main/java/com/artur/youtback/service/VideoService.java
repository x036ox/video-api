package com.artur.youtback.service;

import com.artur.common.entity.Like;
import com.artur.common.entity.VideoEntity;
import com.artur.common.entity.VideoMetadata;
import com.artur.common.entity.user.UserEntity;
import com.artur.common.entity.user.UserMetadata;
import com.artur.common.entity.user.WatchHistory;
import com.artur.common.exception.NotFoundException;
import com.artur.common.repository.*;
import com.artur.objectstorage.service.ObjectStorageService;
import com.artur.youtback.config.KafkaConfig;
import com.artur.youtback.converter.VideoConverter;
import com.artur.youtback.exception.ProcessingException;
import com.artur.youtback.http.client.RecommendationsHttpClient;
import com.artur.youtback.model.video.Video;
import com.artur.youtback.model.video.VideoCreateRequest;
import com.artur.youtback.model.video.VideoUpdateRequest;
import com.artur.youtback.sort.VideoSort;
import com.artur.youtback.utils.AppConstants;
import com.artur.youtback.utils.FindOptions;
import com.artur.youtback.utils.MediaUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.tika.language.detect.LanguageDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.requestreply.RequestReplyFuture;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class VideoService {
    private static final Logger logger = LoggerFactory.getLogger(VideoService.class);

    @Autowired
    private VideoRepository videoRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private VideoMetadataRepository videoMetadataRepository;
    @Autowired
    private LikeRepository likeRepository;
    @Autowired
    RecommendationsHttpClient recommendationsClient;
    @Autowired
    TransactionTemplate transactionTemplate;
    @Autowired
    WatchHistoryRepository watchHistoryRepository;
    @Autowired
    EntityManager entityManager;
    @Autowired
    ObjectStorageService objectStorageService;
    @Autowired
    VideoConverter videoConverter;
    @Autowired
    LanguageDetector languageDetector;
    @Autowired
    UserMetadataRepository userMetadataRepository;
    @Autowired
    ReplyingKafkaTemplate<String, String, Boolean> replyingKafkaTemplate;


    @Cacheable(value = "video", key = "#id")
    public Video findById(Long id) throws NotFoundException{
        Optional<VideoEntity> optionalVideoEntity = videoRepository.findById(id);
        if(optionalVideoEntity.isEmpty()) throw new NotFoundException("Video not Found");

        return videoConverter.convertToModel(optionalVideoEntity.get());
    }

    @Cacheable(value = "videos")
    public List<Video> findByOption(List<String> options, List<String> values) throws NullPointerException, IllegalArgumentException{
        return Objects.requireNonNull(Tools.findByOption(options, values, entityManager).stream().map(videoConverter::convertToModel).toList());
    }

    public List<Video> recommendations(
            String userId,
            Integer page,
            @NotNull String languages,
            Integer size,
            VideoSort videoSort
    ) throws IllegalArgumentException{
        if(languages.isEmpty()) throw new IllegalArgumentException("Should be at least one language");
        try {
            List<Long> ids = recommendationsClient.getRecommendations(
                    userId,
                    page,
                    languages,
                    size
            );
            List<VideoEntity> videos = ids.stream()
                    .map(id -> videoRepository.findById(id).orElseThrow())
                    .collect(Collectors.toCollection(ArrayList::new));
            if(videoSort != null){
                return videos.stream()
                        .sorted(VideoSort.getComparator(videoSort))
                        .map(videoConverter::convertToModel).toList();
            } else {
                return videos.stream().map(videoConverter::convertToModel).toList();
            }
        } catch (NotFoundException e) {
            logger.error(e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**Increments requested video`s views. If specified userId in not null, gets this user and increments his category
     * and language "points" that match to the video. Adds this video in user`s watch history.
     * @param videoId video id
     * @param userId user id, can be null
     * @return video, converted to DTO
     * @throws NotFoundException if video id not found
     */
    @Cacheable(value = "video", key = "#videoId")
    @Transactional
    public Video watchById(Long videoId, String userId) throws NotFoundException{
        VideoEntity videoEntity = videoRepository.findById(videoId).orElseThrow(() -> new NotFoundException("Video not found"));
        videoEntity.setViews(videoEntity.getViews() + 1);
        if(userId != null){
            userRepository.findById(userId).ifPresent(userEntity -> {
                UserMetadata userMetadata;
                if(userEntity.getUserMetadata() == null){
                    userMetadata = new UserMetadata(userEntity);
                } else {
                    userMetadata = userEntity.getUserMetadata();
                }
                userMetadata.incrementLanguage(videoEntity.getVideoMetadata().getLanguage());
                userMetadata.incrementCategory(videoEntity.getVideoMetadata().getCategory());
                //TODO: change logic of watch history
                userEntity.getWatchHistory().removeIf(el ->{
                    if(el.getDate().isAfter(LocalDateTime.now().minusDays(1))&& Objects.equals(el.getVideoId(), videoId)){
                        watchHistoryRepository.deleteById(el.getId());
                        return true;
                    }
                    return false;
                });
                userEntity.getWatchHistory().add(new WatchHistory(null, userEntity, videoId));
                userRepository.save(userEntity);
                userMetadataRepository.save(userMetadata);
            });
        }
        videoRepository.save(videoEntity);
        return videoConverter.convertToModel(videoEntity);
    }


    public InputStream m3u8Index(Long videoId) throws NotFoundException {
        // TODO: 29.01.2024 make m3u8Index and ts methods to return StreamingResponseBody
        try{
//            BufferedReader reader = new BufferedReader(new InputStreamReader(
//                    objectStorageService.getObject(AppConstants.VIDEO_PATH + videoId + "/index.m3u8")
//            ));
//            return outputStream -> {
//                try{
//                    String line;
//                    while((line = reader.readLine()) != null){
//                        outputStream.write(line.getBytes());
//                        outputStream.flush();
//                    }
//                } catch(Exception e){
//                    logger.error(e.getMessage());
//                    e.printStackTrace();
//                } finally {
//                    outputStream.close();
//                    reader.close();
//                }
//            };
            return objectStorageService.getObject(AppConstants.VIDEO_PATH + videoId + "/index.m3u8");
        } catch(Exception e){
            throw new NotFoundException("cannot retrieve target m3u8 file: " + e);
        }
    }

    public InputStream ts(Long id,String filename) throws NotFoundException {
        try{
//            BufferedReader reader = new BufferedReader(new InputStreamReader(
//                    objectStorageService.getObject(AppConstants.VIDEO_PATH + id + "/" + filename)
//            ));
//            return outputStream -> {
//                try{
//                    String line;
//                    while((line = reader.readLine()) != null){
//                        outputStream.write(line.getBytes());
//                        outputStream.flush();
//                    }
//                } catch(Exception e){
//                    logger.error(e.getMessage());
//                    e.printStackTrace();
//                } finally {
//                    outputStream.close();
//                    reader.close();
//                }
//            };
            return objectStorageService.getObject(AppConstants.VIDEO_PATH + id + "/" + filename);
        } catch(Exception e){
            logger.error(e.getMessage());
            throw new NotFoundException("cannot retrieve target [" + filename + " ] file");
        }
    }

    private void videoCreatedPublish(Long videoId){
        replyingKafkaTemplate.send(
                KafkaConfig.VIDEO_CREATED_NOTIFICATION_TOPIC,
                videoId.toString());
    }

    public VideoEntity create(VideoCreateRequest video, String userId)  throws Exception{
        try(
                InputStream thumbnailInputStream = video.thumbnail().getInputStream();
                ByteArrayInputStream videoInputStream = new ByteArrayInputStream(video.video().getBytes());
        ) {
            VideoEntity videoEntity = create(video.title(), video.description(), video.category(), thumbnailInputStream, videoInputStream, userId);
            videoCreatedPublish(videoEntity.getId());
            return videoEntity;
        }
    }

    public VideoEntity create(String title, String description, String category, File thumbnail, File video, String userId)  throws Exception{
        try(
                InputStream thumbnailInputStream = new FileInputStream(thumbnail);
                ByteArrayInputStream videoInputStream = new ByteArrayInputStream(Files.readAllBytes(video.toPath()));
        ) {
            return create(title, description, category,thumbnailInputStream , videoInputStream, userId);
        }
    }

    /**Creates a new video. Specified video uploads to {@link ObjectStorageService} and a message is sent for processing by Kafka.
     * Detects video language by title and duration by Apache Tika`s {@link LanguageDetector}.
     * Input stream does not close. Uses byte array input stream due to stream being read multiple times.
     * @param title video title
     * @param description video description
     * @param category video category
     * @param thumbnail video thumbnail input stream
     * @param video video byte array input stream
     * @param userId user id
     * @return optional of VideoEntity
     * @throws Exception if user not found or failed uploading to {@link ObjectStorageService} or failed to parse duration.
     */
    @Transactional
    private VideoEntity create(String title, String description, String category, InputStream thumbnail, ByteArrayInputStream video, String userId) throws Exception{
        //TODO: avoid to use ByteArrayInputStream, in order not to store whole video in memory
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        String folder = null;
        try {
            Integer duration = (int)Float.parseFloat(MediaUtils.getDuration(video));
            String language = languageDetector.detect(title).getLanguage();
            VideoEntity videoEntity = videoConverter.convertToEntity(title, description, userEntity);
            videoEntity.setVideoMetadata(new VideoMetadata(videoEntity, language, duration, category));
            videoRepository.save(videoEntity);

            folder = AppConstants.VIDEO_PATH + videoEntity.getId();
            String thumbnailFilename = folder + "/" + AppConstants.THUMBNAIL_FILENAME;
            objectStorageService.putObject(thumbnail, thumbnailFilename);
            RequestReplyFuture<String, String, Boolean> thumbnailResponseFuture = replyingKafkaTemplate.sendAndReceive(
                    new ProducerRecord<>(KafkaConfig.THUMBNAIL_INPUT_TOPIC, thumbnailFilename)
            );

            video.reset();
            String videoFilename = AppConstants.VIDEO_PATH + videoEntity.getId() + "/" + "index.mp4";
            objectStorageService.putObject(video, videoFilename);
            RequestReplyFuture<String, String, Boolean> videoResponseFuture = replyingKafkaTemplate.sendAndReceive(
                    new ProducerRecord<>(KafkaConfig.VIDEO_INPUT_TOPIC, videoFilename)
            );

            if(!thumbnailResponseFuture.get(5, TimeUnit.MINUTES).value() || !videoResponseFuture.get(5, TimeUnit.MINUTES).value()){
                throw new ProcessingException("Received false from processing microservice");
            }

            logger.info("Video {} successfully created", videoEntity.getId());
            return videoEntity;
        } catch (Exception e) {
            logger.error("Could not create video uploaded from client", e);
            if(folder != null){
                objectStorageService.removeFolder(folder);
            }
            throw e;
        }
    }


    /**Deletes video from database and {@link ObjectStorageService}.
     * @param id video id
     * @throws Exception if deleting from minio service failed.
     */
    @CacheEvict(value = "video", key = "#id")
    @Transactional
    public void deleteById(Long id) throws Exception {
        if(!videoRepository.existsById(id)) throw new NotFoundException("Video not found");
        VideoEntity videoEntity = videoRepository.getReferenceById(id);
        likeRepository.deleteAllById(videoEntity.getLikes().stream().map(Like::getId).toList());
        watchHistoryRepository.deleteAllByVideoId(id);
        videoRepository.deleteById(id);
        objectStorageService.removeFolder(AppConstants.VIDEO_PATH + id);
        logger.trace("Video with id {} was successfully deleted", id);
    }

    /**Update {@link VideoEntity}. The fields that could be updated:
     * <ul>
     *     <li>Description - video`s description. Can be null
     *     <li>Title - video`s title. Can be null
     *     <li>Thumbnail - video`s thumbnail. Can be null
     *     <li>Video - video itself. Can be null
     *     <li>Category - video`s category. Can be null
     * </ul>
     * If anything of this is null, it wouldn't be changed.
     * @param updateRequest instance of {@link VideoUpdateRequest}
     * @throws Exception - if video not found or error occurred while uploading to {@link ObjectStorageService}
     */
    @CachePut(value = "video", key = "#updateRequest.videoId")
    public Video update(VideoUpdateRequest updateRequest) throws Exception {
        Optional<VideoEntity> optionalVideoEntity = videoRepository.findById(updateRequest.videoId());
        if(optionalVideoEntity.isEmpty()) throw new NotFoundException("Video not Found");

        //data allowed to update
        VideoEntity videoEntity = optionalVideoEntity.get();
        if(updateRequest.description() != null){
            videoEntity.setDescription(updateRequest.description());
        }
        if(updateRequest.title() != null){
            videoEntity.setTitle(updateRequest.title());
        }
        if(updateRequest.thumbnail() != null){
            try (InputStream thumbnailInputStream = updateRequest.thumbnail().getInputStream()){
                objectStorageService.putObject(thumbnailInputStream, AppConstants.VIDEO_PATH + videoEntity.getId() + "/" + AppConstants.THUMBNAIL_FILENAME);
                RequestReplyFuture<String, String, Boolean> response = replyingKafkaTemplate.sendAndReceive(
                        new ProducerRecord<>(
                                KafkaConfig.THUMBNAIL_INPUT_TOPIC,
                                AppConstants.VIDEO_PATH + videoEntity.getId() + "/" + AppConstants.THUMBNAIL_FILENAME)
                );
                if(!response.get().value()){
                    throw new ProcessingException("Could not process thumbnail");
                }
            }
        }
        if(updateRequest.video() != null){
            for(var el : objectStorageService.listFiles(AppConstants.VIDEO_PATH + videoEntity.getId() + "/")){
                if(!el.contains(AppConstants.THUMBNAIL_FILENAME)){
                    objectStorageService.removeObject(el);
                }
            }
            try (InputStream videoInputStream = updateRequest.video().getInputStream()) {
                String videoFilename = AppConstants.VIDEO_PATH + videoEntity.getId() + "/" + "index.mp4";
                objectStorageService.putObject(videoInputStream, videoFilename);
                RequestReplyFuture<String, String, Boolean> response = replyingKafkaTemplate.sendAndReceive(
                        new ProducerRecord<>(
                                KafkaConfig.VIDEO_INPUT_TOPIC,
                                videoFilename)
                );
                if(!response.get(5, TimeUnit.MINUTES).value()){
                    throw new ProcessingException("Could not process video");
                }
            }
        }
        if(updateRequest.category() != null){
            videoEntity.getVideoMetadata().setCategory(updateRequest.category());
        }
        return videoConverter.convertToModel(videoRepository.save(videoEntity));
    }

    /**Creates specified amount of videos. Video data will be picked randomly of
     * already specified lists of titles, categories, etc. Thumbnails and videos to create stored in file system.
     * For every video randomly picks amount of likes and different users like this video. Date of liking this video
     * picks randomly so that could help with recommendations. Every video creates in parallel. Used one thread per
     * video. This method cannot be tested cause of new transactions in every single thread which causes the rollback
     * to fail. Waits until all threads are terminated.
     * @param amount num of videos to create
     * @return amount of created videos
     */
    @Transactional
    public int addVideos(int amount) {
        AtomicInteger createdVideos = new AtomicInteger(amount);
        String[] categories = {"Sport", "Music", "Education", "Movies", "Games", "Other"};
        String[][] titles = {{"Football", "Basketball", "Hockey", "Golf"}, {"Eminem", "XXXTentacion", "Drake", "Три дня дождя", "Playboi Carti","Yeat"}, {"Java", "Php", "English", "French", "C#", "C++"}, {"Oppenheimer", "American psycho", "Good fellas","Fight club","Breaking bad", "The boys"}, {"GTA V", "GTA San Andreas", "GTA IV", "Fortnite", "Minecraft", "Need For Speed Most Wanted"}, {"Monkeys", "Cars", "Dogs", "Cats", "Nature"}};
        String videoThumbnailsToCreateDirectory = "video-thumbnails-to-create";
        String videosToCreateDirectory = "videos-to-create";
        File[][] thumbnails = {new File(videoThumbnailsToCreateDirectory + "/sport").listFiles(),
                new File(videoThumbnailsToCreateDirectory + "/music").listFiles(),
                new File(videoThumbnailsToCreateDirectory + "/education").listFiles(),
                new File(videoThumbnailsToCreateDirectory + "/movies").listFiles(),
                new File(videoThumbnailsToCreateDirectory + "/games").listFiles(),
                new File(videoThumbnailsToCreateDirectory + "/other").listFiles()
        };
        File[][] videos = {new File(videosToCreateDirectory + "/sport").listFiles(),
                new File(videosToCreateDirectory + "/music").listFiles(),
                new File(videosToCreateDirectory + "/education").listFiles(),
                new File(videosToCreateDirectory + "/movies").listFiles(),
                new File(videosToCreateDirectory + "/games").listFiles(),
                new File(videosToCreateDirectory + "/other").listFiles()
        };
        String description = "Nothing here...";
        List<UserEntity> users = userRepository.findAll();
        Runnable task = () -> {
            transactionTemplate.execute(status -> {
                UserEntity user = users.get((int) Math.floor(Math.random() * users.size()));
                int categoryIndex = (int)Math.floor(Math.random() * categories.length);
                String category = categories[categoryIndex];
                String title = titles[categoryIndex][(int)Math.floor(Math.random() * titles[categoryIndex].length)] + " by " + user.getUsername();
                try {
                    VideoEntity createdVideo = create(title, description, category, thumbnails[categoryIndex][(int)Math.floor(Math.random() * thumbnails[categoryIndex].length)],
                            videos[categoryIndex][(int)Math.floor(Math.random() * videos[categoryIndex].length)], user.getId());
                    int likesToAdd = (int)Math.floor(Math.random() * users.size());
                    Set<String> exceptions = new HashSet<>();
                    for (int i = 0;i < likesToAdd;i++){
                        Instant timestamp = Instant.now().minus((int)Math.floor(Math.random() * 2592000), ChronoUnit.SECONDS);
                        UserEntity userEntity = users.get((int)Math.floor(Math.random() * users.size()));
                        if(!exceptions.contains(userEntity.getId())){
                            addLike(userEntity, createdVideo, timestamp);
                            exceptions.add(userEntity.getId());
                        }else{
                            i--;
                        }
                    }
                } catch (Exception e) {
                    status.setRollbackOnly();
                    createdVideos.decrementAndGet();
                    logger.error(e.getMessage());
                }
                return null;
            });
        };
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(amount);
        for(int i = 0; i< amount; i++){
            executor.execute(task);
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        executor.shutdown();
        try {
            executor.awaitTermination(200, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return createdVideos.get();
    }

    /**This method created to let user like video, but it allows to set custom date of like.
     * Should be only used in artificial video creation. In other cases use {@link UserService}`s method
     * @param user user entity that like video
     * @param video video that like by user
     * @param timestamp date of like
     */
    private void addLike(UserEntity user, VideoEntity video, Instant timestamp){
        Like like = new Like();
        like.setVideoEntity(video);
        like.setUserEntity(user);
        like.setTimestamp(timestamp);
        likeRepository.save(like);
    }


     protected static class Tools {

         /**Finds videos by specified criteria(options). Options are accepted as a List of string
          *  and converted to {@link com.artur.youtback.utils.FindOptions.VideoOptions}. All options will be taken into
          *  account. So the result list will contain all users that satisfy the specified criteria.
          * @param options option to search by. Acceptable options specified
          *              in {@link com.artur.youtback.utils.FindOptions.VideoOptions}
          * @param values value for the options, can not be null. Range should be indicated like "1/100" for range from 1 to 100.
          * @return List of users founded by specified options
          * @throws IllegalArgumentException if range is specified incorrectly
          */
         static List<VideoEntity> findByOption(List<String> options, List<String> values, EntityManager entityManager) throws IllegalArgumentException {
             CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
             CriteriaQuery<VideoEntity> criteriaQuery = criteriaBuilder.createQuery(VideoEntity.class);
             Predicate predicate = criteriaBuilder.conjunction();
             Root<VideoEntity> root = criteriaQuery.from(VideoEntity.class);

             for (int i = 0; i < options.size() ; i++) {
                 String option = options.get(i);
                 String value = values.get(i);
                 if(option.equalsIgnoreCase(FindOptions.VideoOptions.BY_TITLE.name())){
                     predicate = criteriaBuilder.and(predicate, criteriaBuilder.like(root.get("title"), "%" + value + "%"));
                 } else if (option.equalsIgnoreCase(FindOptions.VideoOptions.BY_ID.name())) {
                     predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("id"), value));
                 } else if (option.equalsIgnoreCase(FindOptions.VideoOptions.BY_VIEWS.name())) {
                     String[] fromTo = value.split("/");
                     if(fromTo.length != 2){
                         throw new IllegalArgumentException("Illegal arguments option: [" + option + "]" + " value [" + value + "]");
                     }
                     predicate = criteriaBuilder.and(predicate, criteriaBuilder.between(root.get("views"),fromTo[0], fromTo[1]));
                 } else if (option.equalsIgnoreCase(FindOptions.VideoOptions.BY_LIKES.name())) {
                     String[] fromTo = value.split("/");
                     if(fromTo.length != 2){
                         throw new IllegalArgumentException("Illegal arguments option: [" + option + "]" + " value [" + value + "]");
                     }
                     predicate = criteriaBuilder.and(predicate, criteriaBuilder.between(criteriaBuilder.size(root.get("likes")), Integer.parseInt(fromTo[0]), Integer.parseInt(fromTo[1])));
                 }
             }
             criteriaQuery.where(predicate);
             return entityManager.createQuery(criteriaQuery).getResultList();
         }

     }

}
