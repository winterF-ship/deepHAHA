package com.forum.repository;

import com.forum.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findByCategoryId(Long categoryId, Pageable pageable);
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.createdAt >= :since AND p.author.isBot <> 1 ORDER BY p.createdAt ASC")
    List<Post> findRecentByNonBotAuthor(@Param("since") LocalDateTime since);

    @Query("SELECT p FROM Post p WHERE p.createdAt >= :since ORDER BY p.createdAt ASC")
    List<Post> findRecentPosts(@Param("since") LocalDateTime since);

    long countByAuthorId(Long authorId);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.author.id = :authorId AND p.createdAt > :after")
    long countByAuthorIdAndCreatedAtAfter(@Param("authorId") Long authorId, @Param("after") LocalDateTime after);

    @Modifying
    @Query(value = "DELETE FROM post_likes WHERE user_id = :userId", nativeQuery = true)
    void deleteLikesByUserId(@Param("userId") Long userId);

    @Modifying
    @Query(value = "DELETE FROM post_favorites WHERE user_id = :userId", nativeQuery = true)
    void deleteFavoritesByUserId(@Param("userId") Long userId);
}
