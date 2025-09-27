package org.example.expert.domain.comment.service;

import lombok.RequiredArgsConstructor;
import org.example.expert.domain.comment.dto.request.CommentSaveRequest;
import org.example.expert.domain.comment.dto.response.CommentLikeResponse;
import org.example.expert.domain.comment.dto.response.CommentResponse;
import org.example.expert.domain.comment.dto.response.CommentSaveResponse;
import org.example.expert.domain.comment.entity.Comment;
import org.example.expert.domain.comment.entity.CommentLike;
import org.example.expert.domain.comment.repository.CommentLikeRepository;
import org.example.expert.domain.comment.repository.CommentRepository;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.example.expert.domain.user.dto.response.UserResponse;
import org.example.expert.domain.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final TodoRepository todoRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;

    @Transactional
    public CommentSaveResponse saveComment(AuthUser authUser, long todoId, CommentSaveRequest commentSaveRequest) {
        User user = User.fromAuthUser(authUser);
        Todo todo = todoRepository.findById(todoId).orElseThrow(() ->
                new InvalidRequestException("Todo not found"));

        Comment newComment = new Comment(
                commentSaveRequest.getContents(),
                user,
                todo
        );

        Comment savedComment = commentRepository.save(newComment);

        return new CommentSaveResponse(
                savedComment.getId(),
                savedComment.getContents(),
                new UserResponse(user.getId(), user.getEmail())
        );
    }

    @Transactional
    public CommentLikeResponse toggleLike(AuthUser authUser, long todoId, long commentId) {
        if (authUser == null) {
            throw new InvalidRequestException("Authentication required");
        }

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new InvalidRequestException("Comment not found"));

        if (!Objects.equals(comment.getTodo().getId(), todoId)) {
            throw new InvalidRequestException("Comment does not belong to todo");
        }

        User user = User.fromAuthUser(authUser);

        Optional<CommentLike> existing = commentLikeRepository.findByCommentIdAndUserId(commentId, user.getId());
        boolean liked;
        if (existing.isPresent()) {
            commentLikeRepository.delete(existing.get());
            liked = false;
        } else {
            CommentLike commentLike = new CommentLike(comment, user);
            commentLikeRepository.save(commentLike);
            liked = true;
        }

        long likeCount = commentLikeRepository.countByCommentId(commentId);
        return new CommentLikeResponse(commentId, likeCount, liked);
    }

    public List<CommentResponse> getComments(long todoId, AuthUser authUser) {
        List<Comment> commentList = commentRepository.findByTodoIdWithUser(todoId);
        if (commentList.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> commentIds = commentList.stream()
                .map(Comment::getId)
                .collect(Collectors.toList());

        Map<Long, Long> likeCounts = commentLikeRepository.countByCommentIdIn(commentIds).stream()
                .collect(Collectors.toMap(
                        CommentLikeRepository.CommentLikeCount::getCommentId,
                        CommentLikeRepository.CommentLikeCount::getLikeCount
                ));

        Set<Long> likedCommentIds = authUser == null
                ? Collections.emptySet()
                : commentLikeRepository.findByUser_IdAndComment_IdIn(authUser.getId(), commentIds).stream()
                        .map(commentLike -> commentLike.getComment().getId())
                        .collect(Collectors.toSet());

        List<CommentResponse> dtoList = new ArrayList<>();
        for (Comment comment : commentList) {
            User user = comment.getUser();
            long likeCount = likeCounts.getOrDefault(comment.getId(), 0L);
            boolean liked = likedCommentIds.contains(comment.getId());
            CommentResponse dto = new CommentResponse(
                    comment.getId(),
                    comment.getContents(),
                    new UserResponse(user.getId(), user.getEmail()),
                    likeCount,
                    liked
            );
            dtoList.add(dto);
        }
        return dtoList;
    }
}
