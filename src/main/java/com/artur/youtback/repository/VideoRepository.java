package com.artur.youtback.repository;

import com.artur.youtback.entity.VideoEntity;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public interface VideoRepository extends JpaRepository<VideoEntity, Long> {

    @Modifying
    @Query("UPDATE VideoEntity SET views = views + 1 WHERE id = :videoId")
    void incrementViewsById(@Param("videoId") Long videoId);

    @Query("SELECT video FROM VideoEntity video JOIN VideoMetadata metadata GROUP BY video.id ORDER BY metadata.duration DESC")
    List<VideoEntity> findMostDuration(Pageable pageable);

    @Query("SELECT video FROM VideoEntity video WHERE video.views BETWEEN :from AND :to")
    List<VideoEntity> findByViews(@Param("from") String from, @Param("to") String to, Pageable pageable);

    @Query("SELECT video FROM VideoEntity video INNER JOIN likes GROUP BY video.id HAVING COUNT(*) BETWEEN :from AND :to")
    List<VideoEntity> findByLikes(@Param("from") String from, @Param("to") String to, Pageable pageable);

    @Query("SELECT video FROM VideoEntity video INNER JOIN likes GROUP BY video.id ORDER BY COUNT(video.id) DESC")
    List<VideoEntity> findMostLikes(Pageable pageable);

    @Query("SELECT video FROM VideoEntity video GROUP BY video.id ORDER BY video.views DESC")
    List<VideoEntity> findMostViews(Pageable pageable);

    @Query("SELECT video FROM VideoEntity video WHERE video.title LIKE %:title%")
    List<VideoEntity> findByTitle(@Param("title")String title, Pageable pageable);


    @Query("SELECT v FROM VideoEntity v JOIN likes l JOIN videoMetadata m WHERE (v.id NOT IN :exceptions) AND l.timestamp >= :timestamp AND m.language = :language  GROUP BY v.id ORDER BY COUNT(v.id) DESC")
    List<VideoEntity> findMostPopularVideos(
            @Param("timestamp")Instant timestamp,
            @Param("language") String language,
            @Param("exceptions") Set<Long> exceptions,
            Pageable pageable);

    @Query("SELECT v FROM VideoEntity v JOIN likes l JOIN videoMetadata m WHERE (v.id NOT IN :exceptions) AND l.timestamp >= :timestamp GROUP BY v.id ORDER BY COUNT(v.id) DESC")
    List<VideoEntity> findMostPopularVideos(
            @Param("timestamp")Instant timestamp,
            @Param("exceptions") Set<Long> exceptions,
            Pageable pageable);



    /**Gets recommendations for user just in one SQL request. Videos will be selected by user`s most common languages
     * and categories. The result videos will be ordered firstly by languages then by categories in reverse and
     * by amount of likes. This guarantees to get most popular videos with languages that user likes and categories
     * that user are interested in.
     * @param userId user id for which should be recommendations found. Can not be null.
     * @param timestamp the range from {@code Instant.now()} in which video is considered popular. Can not be null
     * @param size the size of recommendations
     * @return List of founded recommendations
     */
    @Query("SELECT v FROM VideoEntity v " +
            "JOIN videoMetadata vm " +
            "JOIN likes l " +
            "LEFT OUTER JOIN UserMetadata um ON um.id = :userId " +
//            "WHERE (v.id NOT IN :exceptions) " +
            "WHERE l.timestamp > :timestamp " +
            "AND vm.category = KEY(um.categories) AND vm.language = KEY(um.languages) " +
            "GROUP BY v.id " +
            "ORDER BY VALUE(um.languages) DESC, VALUE(um.categories) DESC, COUNT(*) DESC"
    )
    List<VideoEntity> findRecommendationsForUser(
            @Param("userId") String userId,
            @Param("timestamp") Instant timestamp,
            Pageable size);


    /** The same as {@code findRecommendationsForUser()}, but uses native query and returns List of ids.
     * @param userId user id for which should be recommendations found. Can not be null.
     * @param timestamp the range from {@code Instant.now()} in which video is considered popular. Can not be null
     * @param exceptions video ids that should be excluded. Can not be null or empty.
     * @param size the size of recommendations
     * @return List of founded recommendations
     */
    @Query(nativeQuery = true, value = "select v.id from video_entity v " +
            "join video_metadata vm on v.id = vm.id " +
            "join video_like l on l.video_id = v.id " +
            "join user_category uc on uc.metadata_id = :userId and vm.category = uc.category " +
            "join user_language ul on ul.metadata_id = :userId and vm.language = ul.language " +
            "where v.id not in :exceptions and l.timestamp > :timestamp " +
            "group by v.id " +
            "order by ul.repeats desc, uc.repeats desc, COUNT(*) DESC;"
    )
    List<Long> getFindIdsForUser(@Param("userId") String userId,
                                 @Param("timestamp") Instant timestamp,
                                 @Param("exceptions") @NotEmpty Set<Long> exceptions,
                                 Pageable size);

    /**The same as overloaded method, just without exceptions. We need this method because if we pass an empty set
     * with exceptions, the result List will be empty.
     * @param userId user id for which should be recommendations found. Can not be null.
     * @param timestamp the range from {@code Instant.now()} in which video is considered popular. Can not be null
     * @param size the size of recommendations
     * @return List of founded recommendations
     */
    @Query(nativeQuery = true, value = "select v.id from video_entity v " +
            "join video_metadata vm on v.id = vm.id " +
            "join video_like l on l.video_id = v.id " +
            "join user_category uc on uc.metadata_id = :userId and vm.category = uc.category " +
            "join user_language ul on ul.metadata_id = :userId and vm.language = ul.language " +
            "where l.timestamp > :timestamp " +
            "group by v.id " +
            "order by ul.repeats desc, uc.repeats desc, COUNT(*) DESC;"
    )
    List<Long> getFindIdsForUser(@Param("userId") String userId,
                                 @Param("timestamp") Instant timestamp,
                                 Pageable size);

    /** Checks if specified set of exceptions is empty or null and calls the corresponding method to
     * find recommendations.
     * @param userId user id for which should be recommendations found. Can not be null.
     * @param timestamp the range from {@code Instant.now()} in which video is considered popular. Can not be null
     * @param exceptions video ids that should be excluded. Can be null or empty.
     * @param size the size of recommendations
     * @return List of founded recommendations
     */
    default List<Long> findRecommendationsTestIds(String userId, Instant timestamp, Set<Long> exceptions, Pageable size){
        if(exceptions.isEmpty()){
            return this.getFindIdsForUser(userId, timestamp, size);
        } else{
            return this.getFindIdsForUser(userId, timestamp, exceptions, size);
        }
    }

    List<VideoEntity> findByIdNotIn(Set<Long> idsToExclude, Pageable pageable);

}
