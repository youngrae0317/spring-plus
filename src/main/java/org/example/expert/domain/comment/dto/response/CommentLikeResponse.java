package org.example.expert.domain.comment.dto.response;

import lombok.Getter;

@Getter
public class CommentLikeResponse {

    private final Long commentId;
    private final long likeCount;
    private final boolean liked;

    public CommentLikeResponse(Long commentId, long likeCount, boolean liked) {
        this.commentId = commentId;
        this.likeCount = likeCount;
        this.liked = liked;
    }
}
