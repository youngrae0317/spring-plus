package org.example.expert.domain.comment.service;

import org.example.expert.domain.comment.dto.response.CommentLikeResponse;
import org.example.expert.domain.comment.dto.response.CommentResponse;
import org.example.expert.domain.comment.entity.Comment;
import org.example.expert.domain.comment.entity.CommentLike;
import org.example.expert.domain.comment.repository.CommentLikeRepository;
import org.example.expert.domain.comment.repository.CommentRepository;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class CommentServiceTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentLikeRepository commentLikeRepository;

    private User author;
    private Todo todo;
    private Comment comment;
    private AuthUser authUser;

    @BeforeEach
    void setUp() {
        author = userRepository.save(new User("author@example.com", "password", "author", UserRole.ROLE_USER));
        todo = todoRepository.save(new Todo("title", "contents", "sunny", author));
        comment = commentRepository.save(new Comment("comment", author, todo));
        authUser = new AuthUser(author.getId(), author.getEmail(), author.getNickname(), author.getUserRole());
    }

    @Test
    void toggleLike_createsLikeAndUpdatesCount() {
        CommentLikeResponse response = commentService.toggleLike(authUser, todo.getId(), comment.getId());

        assertThat(response.isLiked()).isTrue();
        assertThat(response.getLikeCount()).isEqualTo(1L);
        assertThat(commentLikeRepository.countByCommentId(comment.getId())).isEqualTo(1L);
    }

    @Test
    void toggleLike_removesExistingLike() {
        commentService.toggleLike(authUser, todo.getId(), comment.getId());

        CommentLikeResponse response = commentService.toggleLike(authUser, todo.getId(), comment.getId());

        assertThat(response.isLiked()).isFalse();
        assertThat(response.getLikeCount()).isEqualTo(0L);
        assertThat(commentLikeRepository.countByCommentId(comment.getId())).isEqualTo(0L);
    }

    @Test
    void duplicateLike_preventedByUniqueConstraint() {
        commentLikeRepository.saveAndFlush(new CommentLike(comment, author));

        assertThatThrownBy(() -> commentLikeRepository.saveAndFlush(new CommentLike(comment, author)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void getComments_returnsAggregatedLikeData() {
        User otherUser = userRepository.save(new User("other@example.com", "password", "other", UserRole.ROLE_USER));
        Comment secondComment = commentRepository.save(new Comment("another comment", otherUser, todo));

        commentLikeRepository.save(new CommentLike(comment, author));
        commentLikeRepository.save(new CommentLike(comment, otherUser));
        commentLikeRepository.save(new CommentLike(secondComment, otherUser));

        List<CommentResponse> responses = commentService.getComments(todo.getId(), authUser);
        Map<Long, CommentResponse> responseMap = responses.stream()
                .collect(Collectors.toMap(CommentResponse::getId, Function.identity()));

        CommentResponse firstResponse = responseMap.get(comment.getId());
        CommentResponse secondResponse = responseMap.get(secondComment.getId());

        assertThat(firstResponse.getLikeCount()).isEqualTo(2L);
        assertThat(firstResponse.isLiked()).isTrue();
        assertThat(secondResponse.getLikeCount()).isEqualTo(1L);
        assertThat(secondResponse.isLiked()).isFalse();
    }
}
