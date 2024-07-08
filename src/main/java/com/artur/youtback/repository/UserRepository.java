package com.artur.youtback.repository;

import com.artur.youtback.entity.Like;
import com.artur.youtback.entity.user.UserEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, String> {

    List<UserEntity> findByUsername(String username, Pageable pageable);

    @Query("SELECT user FROM UserEntity user INNER JOIN user.subscribers s GROUP BY user.id HAVING COUNT(*) BETWEEN :from AND :to")
    List<UserEntity> findBySubscribers(@Param("from") String from, @Param("to") String to, Pageable pageable);

    @Query("SELECT user FROM UserEntity user INNER JOIN userVideos GROUP BY user.id HAVING COUNT(*) BETWEEN :from AND :to")
    List<UserEntity> findByVideos(@Param("from") String from, @Param("to") String to, Pageable pageable);

    @Query("SELECT user FROM UserEntity user INNER JOIN subscribers s GROUP BY user.id ORDER BY COUNT(s.id) DESC")
    List<UserEntity> findMostSubscribes(Pageable pageable);

    @Query("SELECT user FROM UserEntity user WHERE user.authorities= :authority")
    List<UserEntity> findByAuthority(@Param("authority")String authority, Pageable pageable);

    @Modifying
    @Query("INSERT INTO Like  (userEntity ,videoEntity, timestamp) VALUES (:id, :videoId, :instant)")
    void addLike(@Param("id") String userId,@Param("videoId") Long videoId,@Param("instant") Instant time);

    @Modifying
    @Query("INSERT INTO Like (userEntity ,videoEntity, timestamp) VALUES (:#{#like.userEntity.id}, :#{#like.videoEntity.id}, :#{#like.timestamp})")
    void addLike(@Param("like") Like like);

    @Modifying
    @Query("DELETE FROM Like l WHERE l.userEntity = :#{#like.userEntity} AND l.videoEntity = :#{#like.videoEntity}")
    void deleteLike(@Param("like") Like like);
}
