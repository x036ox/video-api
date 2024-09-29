package com.artur.youtback.service;

import com.artur.youtback.config.KafkaConfig;
import com.artur.youtback.converter.UserConverter;
import com.artur.youtback.converter.VideoConverter;
import com.artur.common.entity.Like;
import com.artur.common.entity.SearchHistory;
import com.artur.common.entity.VideoEntity;
import com.artur.common.entity.user.UserEntity;
import com.artur.common.entity.user.WatchHistory;
import com.artur.common.exception.NotFoundException;
import com.artur.youtback.exception.AlreadyExistException;
import com.artur.youtback.http.client.ImageUploadHttpClient;
import com.artur.youtback.model.user.User;
import com.artur.youtback.model.user.UserCreateRequest;
import com.artur.youtback.model.user.UserUpdateRequest;
import com.artur.youtback.model.video.Video;
import com.artur.common.repository.*;
import com.artur.objectstorage.service.ObjectStorageService;
import com.artur.youtback.sort.VideoSort;
import com.artur.youtback.utils.*;
import com.artur.youtback.utils.comparators.SearchHistoryComparator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.lang.Nullable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.io.File;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Value("${application.url.default-user-picture}")
    String defaultUserPicture;

    @Autowired
    UserRepository userRepository;
    @Autowired
    SearchHistoryRepository searchHistoryRepository;
    @Autowired
    VideoRepository videoRepository;
    @Autowired
    LikeRepository likeRepository;
    @Autowired
    UserMetadataRepository userMetadataRepository;
    @Autowired
    EntityManager entityManager;
    @Autowired
    UserConverter userConverter;
    @Autowired
    VideoConverter videoConverter;
    @Autowired
    ObjectStorageService objectStorageService;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    ReplyingKafkaTemplate<String, String, Boolean> replyingKafkaTemplate;
    @Autowired
    ImageService imageService;
    @Autowired
    ImageUploadHttpClient imageUploadHttpClient;


    public List<User> findAll() throws NotFoundException {
        List<User> userList = userRepository.findAll().stream().map(
                userConverter::convertToModel
        ).toList();
        if(userList.isEmpty()) throw new NotFoundException("No users was found");
        return userList;
    }

    public User findById(String id) throws NotFoundException {
        Optional<UserEntity> optionalUserEntity = userRepository.findById(id);
        if(optionalUserEntity.isEmpty()) throw new NotFoundException("User not Found");

        return userConverter.convertToModel(optionalUserEntity.get());
    }

    /**
     * Find all user videos. Can be sorted by specified sort option, that can be null.
     * If null, result would be sorted by upload date.
     * @param userId user id
     * @param videoSort sort option, can be null.
     * @return List of founded videos.
     * @throws NotFoundException if user with specified id was not found.
     */
    public List<Video> getAllUserVideos(String userId, @Nullable VideoSort videoSort) throws NotFoundException {
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        if(videoSort != null){
            return userEntity.getUserVideos().stream().sorted(VideoSort.getComparator(videoSort)).map(videoConverter::convertToModel).toList();
        }

        return userEntity.getUserVideos().stream().map(videoConverter::convertToModel).toList();
    }


    /**Find users by specified options in {@link Tools} class. Converts result into List of DTO.
     * @param options options to search by. Options specified in {@link com.artur.youtback.utils.FindOptions.UserOptions}
     * @param values values for the options, can be null. Range should be indicated like "1/100" for range from 1 to 100.
     * @return List of users founded by specified options
     * @throws IllegalArgumentException if range is specified incorrectly
     */
    public List<User> findByOption(List<String> options, List<String> values)throws IllegalArgumentException{
        return Tools.findByOption(options, values, entityManager).stream().map(userConverter::convertToModel).toList();
    }

    /** Indicates that the specified video is not interesting for user. Takes category of this video and
     * decreases user`s number of watched videos with this category by 0.25 times. If after this user has 0
     * repetitions in this category, this category will be deleted.
     * @param videoId video id
     * @param userId user id
     * @throws NotFoundException if user or video is not found
     */
    public void notInterested(Long videoId, String userId) throws NotFoundException {
        VideoEntity videoEntity = videoRepository.findById(videoId).orElseThrow(() -> new NotFoundException("Video not found"));
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));

        String category = videoEntity.getVideoMetadata().getCategory();
        if(Objects.equals(userEntity.getUserMetadata().getCategories().computeIfPresent(category, (key, value) -> (int) (value * 0.25f)), 0)){
            userEntity.getUserMetadata().getCategories().remove(category);
        }
        userMetadataRepository.save(userEntity.getUserMetadata());
    }

    /**Delete user from database and all user data from {@link ObjectStorageService}.
     * @param id user id
     * @throws Exception if user not found or {@link ObjectStorageService} can not remove user data
     */
    @Transactional
    public void deleteById(String id) throws Exception {
        UserEntity userEntity = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not Found"));
        userRepository.delete(userEntity);
        objectStorageService.removeFolder(AppConstants.USER_PATH + userEntity.getId());
        logger.info("User with id {} successfully deleted" , id);
    }

    /**Update {@link UserEntity}. The fields that could be updated:
     * <ul>
     *     <li>Username - can be null
     *     <li>Picture - can be null
     *     <li>Authorities - user`s authorities</li>
     * </ul>
     * If anything of this is null, it wouldn't be changed.
     * @param user instance of {@link UserUpdateRequest}, that contains user data to update
     * @throws Exception if user was not found or if exceptions occurred in {@link ObjectStorageService}
     */
    public void update(UserUpdateRequest user, String userId) throws Exception {
        Optional<UserEntity> optionalUserEntity = userRepository.findById(userId);
        if(optionalUserEntity.isEmpty()) throw new NotFoundException("User not Found");

        UserEntity userEntity = optionalUserEntity.get();
        if(user.username() != null){
            userEntity.setUsername(user.username());
        }
        if(user.picture() != null){
           userEntity.setPicture(user.picture());
        }
        userRepository.save(userEntity);
    }


    private String formPictureName(String pictureId, @Nullable String originalFilename){
        Assert.notNull(pictureId, "Picture id can not be null");
        return AppConstants.USER_PATH + pictureId + "." + StringUtils.getFilenameExtension(originalFilename);
    }

    public User registerUser(UserCreateRequest user) throws AlreadyExistException {
        return registerUser(user.id(),
                user.username(),
                user.email(),
                user.authorities() == null ? AppAuthorities.ROLE_USER.name() : user.authorities(),
                user.picture() == null ? defaultUserPicture : user.picture());
    }

    /**Saves {@link UserEntity} to database, uploads picture to {@link ObjectStorageService} and sends message to processor service.
     * @param  id user id, can not be null.
     * @param  username username, can not be null.
     * @param  authorities authorities, can not be null.
     * @param picture user`s picture input stream, can not be null
     * @return created user, converted to DTO {@link User}.
     * @throws Exception if user with this id already exists or if {@link ObjectStorageService} can not save picture.
     */
    @Transactional
    private User registerUser(String id,
                              String username,
                              String email,
                              String authorities,
                              String picture) throws AlreadyExistException {
        if(userRepository.existsById(id)){
            throw new AlreadyExistException("User with this id [" + id + "] already exists");
        }
        UserEntity userEntity = new UserEntity(
                id,
                username,
                email,
                picture,
                authorities
        );
        userRepository.save(userEntity);
        return userConverter.convertToModel(userEntity);
    }

    /**Adds search option in search history. Removes extra options if there are more than specified
     *  in {@code MAX_SEARCH_HISTORY_OPTIONS}. If contains the same search option, updates date of this option
     * @param id user id
     * @param searchOption search option. Anything that user searched.
     * @throws NotFoundException if user with this id was not found.
     */
    public void addSearchOption(String id, String searchOption) throws NotFoundException {
        UserEntity userEntity = userRepository.findById(id).orElseThrow(() ->  new NotFoundException("User not found"));
        if(userEntity.getSearchHistory() == null) userEntity.setSearchHistory(new ArrayList<>());                               //if adding at the first time

        List<SearchHistory> searchHistoryList = userEntity.getSearchHistory();
        /*if we have the same value, we have to update date added in database*/
        for (SearchHistory searchHistory: searchHistoryList) {
            if(searchHistory.getSearchOption().equals(searchOption)){
                searchHistory.setDateAdded();
                searchHistoryRepository.save(searchHistory);
            }
        }
        /*remove extra option*/

        if(searchHistoryList.size() > AppConstants.MAX_SEARCH_HISTORY_OPTIONS - 1){
            /*sorting list by date added and deleting the latest by his id*/
            searchHistoryRepository.deleteById(searchHistoryList.stream().sorted(new SearchHistoryComparator()).toList().get(AppConstants.MAX_SEARCH_HISTORY_OPTIONS - 1).getId());
        }
        searchHistoryRepository.save(new SearchHistory(null, searchOption, userEntity));
    }


    /**Likes a video. If like already exists, remove this like, otherwise add.
     * @param userId user that likes video
     * @param videoId video that liked user
     * @throws NotFoundException if user or video not found
     */
    public void likeVideo(String userId, Long videoId) throws NotFoundException {
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        VideoEntity videoEntity = videoRepository.findById(videoId).orElseThrow(() -> new NotFoundException("Video not found"));
        Set<Like> likedVideos = userEntity.getLikes();
        //we cant use contains() because we don't have like's id
        Optional<Like> optionalLike = likedVideos.stream().filter(like -> like.getVideoEntity().getId().equals(videoId)).findFirst();
        if(optionalLike.isEmpty()){
            likeRepository.save(Like.create(userEntity, videoEntity));
        }
        else {
            Like like = optionalLike.get();
            like.getUserEntity().getLikes().remove(like);
            like.getVideoEntity().getLikes().remove(like);
            likeRepository.delete(like);
        }
    }

    /**Dislike this video. If liked, removes it.
     * @param userId user id that dislike video
     * @param videoId video id that disliked by user.
     * @throws NotFoundException - if user or video not found
     */
    public void dislikeVideo(String userId, Long videoId) throws NotFoundException {
        if(!userRepository.existsById(userId)) throw new NotFoundException("User not found");
        if(!videoRepository.existsById(videoId)) throw new NotFoundException("Video not found");
        UserEntity userEntity = userRepository.getReferenceById(userId);
        userEntity.getLikes().stream().filter(like -> like.getVideoEntity().getId().equals(videoId)).findFirst().ifPresent(like -> {
            try{
                like.getUserEntity().getLikes().remove(like);
                like.getVideoEntity().getLikes().remove(like);
                likeRepository.delete(like);
            } catch (Exception e){
                logger.error(e.getMessage());
            }
        });
    }

    /** Gets user search history.
     * @param userId user id
     * @return videos that user have watched.
     * @throws NotFoundException if user not found
     */
    public List<Video> getWatchHistory(String userId) throws NotFoundException {
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(()-> new NotFoundException("User not found, id: " + userId));
        List<Video> result = new ArrayList<>();
        for (WatchHistory watchHistory :userEntity.getWatchHistory()) {
            videoRepository.findById(watchHistory.getVideoId()).ifPresent(v -> result.add(videoConverter.convertToModel(v)));
        }
        return result;
    }

    /**Gets users that user subscribed.
     * @param userId user id. Can not be null
     * @return List of users founded
     * @throws NotFoundException if user was not found
     */
    public List<User> getUserSubscribes(String userId) throws NotFoundException {
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User with id " + userId + " was not found"));
        return user.getSubscribes().stream().map(userConverter::convertToModel).toList();
    }

    /**Gets videos that user liked.
     * @param userId user id. Can not be null
     * @return List of videos founded
     * @throws NotFoundException if user was not found
     */
    public List<Video> getUserLikes(String userId) throws NotFoundException {
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User with id " + userId + " was not found"));
        return user.getLikes().stream().map(Like::getVideoEntity).map(videoConverter::convertToModel).toList();
    }

    /**Deletes specified search option
     * @param userId user id
     * @param searchOption search option to delete
     * @throws NotFoundException if user or search option not found
     */
    public void deleteSearchOption(String userId, String searchOption) throws NotFoundException {
        Optional<UserEntity> optionalUserEntity = userRepository.findById(userId);
        if(optionalUserEntity.isEmpty()) throw new NotFoundException("User not found");

        UserEntity userEntity = optionalUserEntity.get();
        if(userEntity.getSearchHistory() == null) throw new NotFoundException("Search option not found");

        for (SearchHistory searchHistory:
             userEntity.getSearchHistory()) {
            if(searchHistory.getSearchOption().equals(searchOption)){
                searchHistoryRepository.delete(searchHistory);
                return;
            }
        }
        throw new NotFoundException("Search option not found");
    }

    /**Checks if user liked video.
     * @param userId user id
     * @param videoId video id
     * @return true if user liked video, otherwise false
     * @throws NotFoundException - if user or video not found
     */
    public boolean hasUserLikedVideo(String userId, Long videoId) throws NotFoundException {
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        if(!videoRepository.existsById(videoId)) throw new NotFoundException("Video not found");
        return userEntity.getLikes().stream().anyMatch(like -> like.getVideoEntity().getId().equals(videoId));
    }

    /**Add user in subscribes to another user.
     * @param userId user id that subscribe another
     * @param subscribedChannelId user id that being subscribed
     * @throws NotFoundException if one of users not found
     */
    public void subscribeById(String userId, String subscribedChannelId) throws NotFoundException {
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        UserEntity subscribedChannel = userRepository.findById(subscribedChannelId).orElseThrow(() ->  new NotFoundException("Subscribed channel not found"));

        userEntity.getSubscribes().add(subscribedChannel);

        userRepository.save(userEntity);
    }

    /**Unsubscribe from this user
     * @param userId user id that will unsubscribe
     * @param subscribedChannelId user id that will be unsubscribed
     * @throws NotFoundException if one of users not found
     */
    public void unsubscribeById(String userId, String subscribedChannelId) throws NotFoundException {
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        if(!userRepository.existsById(userId)) throw new NotFoundException("User not found");

        userEntity.setSubscribes(userEntity.getSubscribes().stream().filter((subbedChannel) ->
                !subbedChannel.getId().equals(subscribedChannelId)
                ).collect(Collectors.toSet()));

        userRepository.save(userEntity);
    }

    /**Checks if user subscribes to another user.
     * @param userId user id that will be checked
     * @param subbedChannelId another user id
     * @return true if user subscribed on another, otherwise false
     * @throws NotFoundException - if user not found
     */
    public boolean hasUserSubscribedChannel(String userId, String subbedChannelId) throws NotFoundException {
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));

        return userEntity.getSubscribes().stream().anyMatch((subbedChannel) -> subbedChannel.getId().equals(subbedChannelId));
    }

    /**Creates specified amount of users. Users data will be picked randomly of
     * already specified lists of usernames, pictures, etc. Pictures stored in file system
     * in path {@code userPictureFolderPath}.
     * @param amount num of users to create
     * @return how many users was created
     * @throws Exception if {@link ObjectStorageService} can not upload picture or if IOException happened
     */
    public int addUsers(int amount) throws Exception {
        AtomicInteger createdUsers = new AtomicInteger(amount);
        String userPictureFolderPath = "user-pictures-to-create";

        String[] names = "Liam Noah Oliver James Elijah William Henry Lucas Benjamin Theodore Mateo Levi Sebastian Daniel Jack Michael Alexander Owen Asher Samuel Ethan Leo Jackson Mason Ezra John Hudson Luca Aiden Joseph David Jacob Logan Luke Julian Gabriel Grayson Wyatt Matthew Maverick Dylan Isaac Elias Anthony Thomas Jayden Carter Santiago Ezekiel Charles Josiah Caleb Cooper Lincoln Miles Christopher Nathan Isaiah Kai Joshua Andrew Angel Adrian Cameron Nolan Waylon Jaxon Roman Eli Wesley Aaron Ian Christian Ryan Leonardo Brooks Axel Walker Jonathan Easton Everett Weston Bennett Robert Jameson Landon Silas Jose Beau Micah Colton Jordan Jeremiah Parker Greyson Rowan Adam Nicholas Theo Xavier".split(" ");
        File[] profilePics = new File(userPictureFolderPath).listFiles();
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        ThreadPoolExecutor executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(12);
        Runnable task = () -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);

            int index = (int)Math.floor(Math.random() * names.length);
            String uuid = genUserId();
            String username = names[index];
            File profilePic = profilePics[(int)Math.floor(Math.random() * profilePics.length)];
            String email = uuid + "@gmail.com";
            try {
                registerUser(uuid,
                        username,
                        email,
                        AppAuthorities.ROLE_USER.name(),
                        imageUploadHttpClient.uploadUserPicture(uuid, profilePic)
                );
            } catch (AlreadyExistException | IllegalArgumentException e) {
                createdUsers.decrementAndGet();
                logger.error(e.getMessage(), e);
            }
        };
        for(int i = 0; i< amount; i++){
            executorService.execute(task);
        }
        executorService.shutdown();
        executorService.awaitTermination(200, TimeUnit.HOURS);
        return createdUsers.get();
    }

    private String genUserId(){
        String id;
        do{
            id = UUID.randomUUID().toString();
        } while(userRepository.existsById(id));
        return id;
    }


    private static class Tools{

        /**Finds user by specified criteria(options). Options are accepted as a List of string
         *  and converted to {@link com.artur.youtback.utils.FindOptions.UserOptions}. All options will be taken into
         *  account. So the result list will contain all users that satisfy the specified criteria.
         * @param options options to search by. Can not be null Options specified
         *               in {@link com.artur.youtback.utils.FindOptions.UserOptions}. If option isn`t contains in
         *                List it will be skipped.
         * @param values value for the options. Can not be null. Range should be indicated like "1/100" for range from 1 to 100.
         *             For example for option ADMINS does not need a value and there should be null. Every single
         *               string in List should match the element with the same index.
         * @return List of users founded by specified options
         * @throws IllegalArgumentException if range is specified incorrectly
         */
        static List<UserEntity> findByOption(List<String> options, List<String> values, EntityManager entityManager) throws IllegalArgumentException{
            CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
            CriteriaQuery<UserEntity> criteriaQuery = criteriaBuilder.createQuery(UserEntity.class);
            Predicate predicate = criteriaBuilder.conjunction();
            Root<UserEntity> root = criteriaQuery.from(UserEntity.class);

            for (int i = 0; i < options.size() ; i++) {
                String option = options.get(i);
                String value = values.get(i);
                if(option.equalsIgnoreCase(FindOptions.UserOptions.BY_EMAIL.name())){
                    predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("email"), value));
                } else if (option.equalsIgnoreCase(FindOptions.UserOptions.BY_ID.name())) {
                    predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("id"), value));
                } else if (option.equalsIgnoreCase(FindOptions.UserOptions.BY_SUBSCRIBERS.name())) {
                    String[] fromTo = value.split("/");
                    if(fromTo.length != 2){
                        throw new IllegalArgumentException("Illegal arguments option: [" + option + "]" + " value [" + value + "]");
                    }
                    predicate = criteriaBuilder.and(predicate, criteriaBuilder.between(criteriaBuilder.size(root.get("subscribers")), Integer.parseInt(fromTo[0]), Integer.parseInt(fromTo[1])));
                } else if (option.equalsIgnoreCase(FindOptions.UserOptions.BY_VIDEO.name())) {
                    String[] fromTo = value.split("/");
                    if(fromTo.length != 2){
                        throw new IllegalArgumentException("Illegal arguments option: [" + option + "]" + " value [" + value + "]");
                    }
                    predicate = criteriaBuilder.and(predicate, criteriaBuilder.between(criteriaBuilder.size(root.get("userVideos")), Integer.parseInt(fromTo[0]), Integer.parseInt(fromTo[1])));
                } else if (option.equalsIgnoreCase(FindOptions.UserOptions.BY_USERNAME.name())) {
                    predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("username"), value));
                }
            }
            criteriaQuery.where(predicate);
            return entityManager.createQuery(criteriaQuery).getResultList();
        }
    }
}
