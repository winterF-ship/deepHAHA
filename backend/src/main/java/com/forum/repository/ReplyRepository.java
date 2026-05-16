package com.forum.repository;

import com.forum.entity.Post;
import com.forum.entity.Reply;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReplyRepository extends JpaRepository<Reply, Long> {
    List<Reply> findByPostIdOrderByCreatedAtAsc(Long postId);

    @Query("SELECT COUNT(r) FROM Reply r WHERE r.author.id = :authorId AND r.createdAt > :after AND (r.deleted = 0 OR r.deleted IS NULL)")
    long countByAuthorIdAndCreatedAtAfter(@Param("authorId") Long authorId, @Param("after") LocalDateTime after);

    @Query("SELECT COUNT(r) FROM Reply r WHERE r.post.id = :postId")
    long countAllByPostId(@Param("postId") Long postId);

    @Query("SELECT COUNT(r) FROM Reply r WHERE r.post.id = :postId AND (r.deleted = 0 OR r.deleted IS NULL)")
    long countActiveByPostId(@Param("postId") Long postId);

    @Query("SELECT COUNT(r) FROM Reply r WHERE r.post.id = :postId AND r.replyType = 'FLOOR' AND (r.deleted = 0 OR r.deleted IS NULL)")
    long countActiveFloorByPostId(@Param("postId") Long postId);

    @Query("SELECT COUNT(r) FROM Reply r WHERE r.post.id = :postId AND r.replyType = 'SUB_REPLY' AND (r.deleted = 0 OR r.deleted IS NULL)")
    long countActiveSubReplyByPostId(@Param("postId") Long postId);

    @Query("SELECT COUNT(r) FROM Reply r WHERE r.post.id = :postId AND r.replyType = 'SUB_REPLY' AND (r.deleted = 0 OR r.deleted IS NULL) AND (r.rootReplyId IS NULL OR r.parentReply IS NULL)")
    long countActiveOrphanSubReplyByPostId(@Param("postId") Long postId);

    @Query("SELECT COUNT(r) FROM Reply r WHERE r.author.id = :authorId AND r.replyType = 'SUB_REPLY' AND r.parentReply.author.isBot = 1 AND r.createdAt > :after AND (r.deleted = 0 OR r.deleted IS NULL)")
    long countSubRepliesToBotsByAuthorId(@Param("authorId") Long authorId, @Param("after") LocalDateTime after);

    @Query("SELECT COUNT(r) FROM Reply r WHERE r.author.id = :authorId AND r.replyType = 'SUB_REPLY' AND (r.parentReply.author.isBot = 0 OR r.parentReply.author.isBot IS NULL) AND r.createdAt > :after AND (r.deleted = 0 OR r.deleted IS NULL)")
    long countSubRepliesToHumansByAuthorId(@Param("authorId") Long authorId, @Param("after") LocalDateTime after);

    @Query("SELECT COUNT(r) FROM Reply r WHERE r.author.id = :authorId AND r.replyType = 'FLOOR' AND r.createdAt > :after AND (r.deleted = 0 OR r.deleted IS NULL)")
    long countFloorRepliesByAuthorId(@Param("authorId") Long authorId, @Param("after") LocalDateTime after);
    boolean existsByPostAndAuthorIsBot(Post post, Integer isBot);

    @Query("SELECT COALESCE(MAX(r.floorNo), 0) FROM Reply r WHERE r.post.id = :postId")
    Integer findMaxFloorNoByPostId(@Param("postId") Long postId);

    @Query("SELECT COUNT(r) FROM Reply r WHERE r.post.id = :postId AND r.author.isBot = 1 AND (r.deleted = 0 OR r.deleted IS NULL)")
    int countAiRepliesByPostId(@Param("postId") Long postId);

    @Query("SELECT COUNT(r) > 0 FROM Reply r WHERE r.parentReply.id = :parentReplyId AND r.author.id = :authorId AND (r.deleted = 0 OR r.deleted IS NULL)")
    boolean existsByParentReplyIdAndAuthorId(@Param("parentReplyId") Long parentReplyId, @Param("authorId") Long authorId);

    @Query("SELECT r FROM Reply r WHERE r.post.id = :postId AND (r.deleted = 0 OR r.deleted IS NULL) ORDER BY r.createdAt ASC")
    List<Reply> findActiveRepliesByPostId(@Param("postId") Long postId);

    @Query("SELECT COUNT(r) > 0 FROM Reply r WHERE r.post.id = :postId AND r.author.id = :authorId AND r.parentReply IS NULL AND (r.deleted = 0 OR r.deleted IS NULL)")
    boolean existsFloorByPostIdAndAuthorId(@Param("postId") Long postId, @Param("authorId") Long authorId);

    @Query("SELECT r FROM Reply r WHERE r.replyToUser IS NOT NULL AND r.replyToUser.isBot = 1 AND (r.author.isBot = 0 OR r.author.isBot IS NULL) AND r.createdAt > :since AND r.replyType = 'SUB_REPLY' AND (r.deleted = 0 OR r.deleted IS NULL) ORDER BY r.createdAt ASC")
    List<Reply> findRecentHumanRepliesToBots(@Param("since") LocalDateTime since);

    @Query("SELECT r FROM Reply r WHERE r.post.author.isBot = 1 AND r.replyType = 'FLOOR' AND (r.author.isBot = 0 OR r.author.isBot IS NULL) AND r.createdAt > :since AND (r.deleted = 0 OR r.deleted IS NULL) ORDER BY r.createdAt ASC")
    List<Reply> findRecentHumanFloorsOnAiPosts(@Param("since") LocalDateTime since);

    @Query("SELECT r.content FROM Reply r WHERE r.author.id = :botId AND (r.deleted = 0 OR r.deleted IS NULL) ORDER BY r.createdAt DESC")
    List<String> findRecentContentsByBotId(@Param("botId") Long botId, Pageable pageable);

    @Query("SELECT r FROM Reply r WHERE r.author.isBot = 1 AND r.replyType = 'FLOOR' AND r.createdAt > :since AND (r.deleted = 0 OR r.deleted IS NULL) ORDER BY r.createdAt DESC")
    List<Reply> findRecentBotReplies(@Param("since") LocalDateTime since);

    long countByAuthorId(Long authorId);

    @Modifying
    @Query("UPDATE Reply r SET r.replyToUser = NULL WHERE r.replyToUser.id = :userId")
    void clearReplyToUserByUserId(@Param("userId") Long userId);

    @Query("SELECT r FROM Reply r JOIN r.post p WHERE (r.deleted = 0 OR r.deleted IS NULL) AND r.author.id <> :userId AND ((r.replyType = 'FLOOR' AND p.author.id = :userId) OR (r.replyType = 'SUB_REPLY' AND r.replyToUser.id = :userId)) ORDER BY r.createdAt DESC")
    List<Reply> findRepliesToMe(@Param("userId") Long userId);
}
