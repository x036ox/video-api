package com.artur.youtback.service;

import com.artur.youtback.entity.VideoEntity;
import com.artur.youtback.exception.NotFoundException;
import com.artur.youtback.repository.LikeRepository;
import com.artur.youtback.repository.UserRepository;
import com.artur.youtback.repository.VideoRepository;
import com.artur.youtback.utils.AppConstants;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RecommendationService {
    private static final Logger logger = LoggerFactory.getLogger(RecommendationService.class);

    @Autowired
    UserRepository userRepository;
    @Autowired
    VideoRepository videoRepository;


    /** Gets recommendations. Videos will not be repeated and will not be contained in {@code excludes}. <p>
     * The main idea is to get {@code  AppConstants.RECS_SIZE} amount of videos in <strong>one SQL request</strong>.
     * This SQL request explained in {@link LikeRepository}.findRecommendations() method.<p>
     *  There two ways to get recommendations:
     *  <ul>
     *      <li>
     *          If user id is not null it gets his categories points and returns the most popular videos
     *          with them and with the users most common user languages. If after one SQL request amount of videos is less
     *          than {@code RECS_SIZE} (it can be if user don`t have enough metadata or number of existed videos is to small),
     *          it will find the most popular videos with the specified browser languages for any topic. If still have not enough
     *          videos, it will find just some popular videos without any language, that's should not be happened if we have enough
     *          videos in database.
     *      </li>
     *      <li>
     *          If user id is null, it will return recommendations with specified languages. This languages should be taken from
     *          browser or found by user`s country language which can be obtained by ip address.
     *      </li>
     *  </ul>
     *  The most popular videos are those that have the most likes during {@code Instant.now().minus(MAX_POPULARITY_DAYS)}.
     *
     * @param userId user id for which should be recommendations found. Can be null.
     * @param page page.
     * @param browserLanguages browser languages from which the request was made. Can not be null and empty
     * @param size the size of recommendations
     * @return List of founded recommendations
     * @throws NotFoundException if user id is not null, but user with this id is not found
     */
    public List<VideoEntity> getRecommendationsFor(@Nullable Long userId,@NotNull Integer page, @NotEmpty String[] browserLanguages, int size) throws NotFoundException {
        final int RECS_SIZE = Math.min(size, AppConstants.MAX_VIDEOS_PER_REQUEST);
        List <VideoEntity> videos = new ArrayList<>();
        if(userId != null){
            if(!userRepository.existsById(userId))
                throw new NotFoundException("User with specified id [" + userId + "] was not found");
            videos.addAll(getByCategoriesAndLanguages(userId, page, RECS_SIZE));
        }

        if(videos.size() < RECS_SIZE){
            //finding with browser language
            videos.addAll(getByLanguages(
                    videos.isEmpty() ? Set.of(-1L) : videos.stream().map(VideoEntity::getId).collect(Collectors.toSet()),
                    page,
                    browserLanguages, RECS_SIZE - videos.size()));
        }
        //finding random popular videos
        if(videos.size() < RECS_SIZE){
            logger.warn("Recommendation not found with user and browser languages for user: " + userId);
            videos.addAll(getSomePopularVideos(
                    videos.isEmpty() ? Set.of(-1L) : videos.stream().map(VideoEntity::getId).collect(Collectors.toSet()),
                    page,
                    RECS_SIZE - videos.size()));
        }
        if(videos.size() < RECS_SIZE){
            logger.warn("Finding just random videos: " + userId);
            videos.addAll(videoRepository.findByIdNotIn(
                    videos.isEmpty() ? Set.of(-1L) : videos.stream().map(VideoEntity::getId).collect(Collectors.toSet()),
                    PageRequest.of( page,RECS_SIZE - videos.size())));
        }
        Collections.shuffle(videos);
        return videos.stream().limit(RECS_SIZE).toList();
    }


    /**Gets videos by his categories and languages points. Calls corresponding method from {@code likeRepository}.
     * The most popular videos will be selected by likes which date is in range from {@code Instant.now().minus(AppConstants.POPULARITY_DAYS)}
     * @param userId user id for which recommendations should be found
     * @param size size of recommendations result
     * @return List of found recommendations
     */
    private List<VideoEntity> getByCategoriesAndLanguages(Long userId, int page, int size){
        return videoRepository.findRecommendationsForUser(userId,
                Instant.now().minus(AppConstants.POPULARITY_DAYS, ChronoUnit.DAYS),
                PageRequest.of(page, size));
    }


    /**Gets recommendations just by languages. For example if user id is unknown, we can find recommendations
     * by user`s browser languages. This languages should be ordered by priority. Firstly it will found
     * videos by first language and if still not enough videos it will continue to next language and so on.
     * @param exceptions videos ids that should be excluded. Can be null or empty.
     * @param languages languages, for example {"ru", "en"}. Can not be null and empty
     * @param size size of recommendations result
     * @return List of found recommendations
     */
    private List<VideoEntity> getByLanguages(Set<Long> exceptions, int page, String[] languages, int size){
        List<VideoEntity> result = new ArrayList<>(size);
        for (String language:languages) {
              result.addAll(getSome(
                      language,
                      exceptions,
                      page,
                      size - result.size()));
              if(result.size() == size) break;
        }
        return result;
    }

    /**Gets recommendations by one language. The most popular videos will be selected by likes which date
     * is in range from {@code Instant.now().minus(AppConstants.POPULARITY_DAYS)}.
     * @param language language
     * @param exceptions videos ids that should be excluded. Can be null or empty.
     * @param size size of recommendations result
     * @return List of found recommendations
     */
    private List<VideoEntity> getSome(String language, Set<Long> exceptions, int page, int size){
        return videoRepository.findMostPopularVideos(
                Instant.now().minus(AppConstants.POPULARITY_DAYS, ChronoUnit.DAYS),
                language,
                exceptions,
                PageRequest.of(page, size));
    }

    /**Gets just some popular videos without categories nor languages. The most popular videos will be selected by
     * likes which date is in range from {@code Instant.now().minus(AppConstants.POPULARITY_DAYS)}.
     * @param exceptions videos ids that should be excluded. Can be null or empty.
     * @param size size of recommendations result
     * @return List of found recommendations
     */
    private List<VideoEntity> getSomePopularVideos(Set<Long> exceptions, int page, int size){
        return videoRepository.findMostPopularVideos(
                Instant.now().minus(AppConstants.POPULARITY_DAYS, ChronoUnit.DAYS),
                exceptions,
                PageRequest.of(page, size)
        );

    }
}
