package org.example.expert.domain.comment.repository;

import org.example.expert.domain.comment.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    @Query("SELECT cl FROM CommentLike cl WHERE cl.comment.id = :commentId AND cl.user.id = :userId")
    Optional<CommentLike> findByCommentIdAndUserId(@Param("commentId") Long commentId, @Param("userId") Long userId);

    @Query("SELECT COUNT(cl.id) FROM CommentLike cl WHERE cl.comment.id = :commentId")
    long countByCommentId(@Param("commentId") Long commentId);

    @Query("SELECT cl.comment.id AS commentId, COUNT(cl.id) AS likeCount " +
            "FROM CommentLike cl " +
            "WHERE cl.comment.id IN :commentIds " +
            "GROUP BY cl.comment.id")
    List<CommentLikeCount> countByCommentIdIn(@Param("commentIds") Collection<Long> commentIds);

    List<CommentLike> findByUser_IdAndComment_IdIn(Long userId, Collection<Long> commentIds);

    interface CommentLikeCount {
        Long getCommentId();
        long getLikeCount();
    }
}
