package org.example.expert.domain.comment.dto.response;

import lombok.Getter;
import org.example.expert.domain.user.dto.response.UserResponse;

@Getter
public class CommentResponse {

    private final Long id;
    private final String contents;
    private final UserResponse user;
    private final long likeCount;
    private final boolean liked;

    public CommentResponse(Long id, String contents, UserResponse user, long likeCount, boolean liked) {
        this.id = id;
        this.contents = contents;
        this.user = user;
        this.likeCount = likeCount;
        this.liked = liked;
    }
}
