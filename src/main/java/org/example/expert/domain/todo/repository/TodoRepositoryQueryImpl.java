package org.example.expert.domain.todo.repository;

import static org.example.expert.domain.comment.entity.QComment.comment;
import static org.example.expert.domain.manager.entity.QManager.manager;
import static org.example.expert.domain.todo.entity.QTodo.todo;
import static org.example.expert.domain.user.entity.QUser.user;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.example.expert.domain.todo.dto.response.QTodoSearchResponse;
import org.example.expert.domain.todo.dto.response.TodoSearchResponse;
import org.example.expert.domain.todo.entity.Todo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
public class TodoRepositoryQueryImpl implements TodoRepositoryQuery {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<Todo> findByIdWithUser(Long todoId) {
        Todo result = queryFactory
                .select(todo)
                .from(todo)
                .leftJoin(todo.user, user)
                .fetchJoin()
                .where(todo.id.eq(todoId))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public Page<TodoSearchResponse> searchTodos(String title, LocalDateTime startDate, LocalDateTime endDate,
                                                String nickname, Pageable pageable) {
        // 조건에 맞는 목록 조회
        List<TodoSearchResponse> resulsts = queryFactory
                .select(new QTodoSearchResponse(
                        todo.title,
                        manager.id.countDistinct(),
                        comment.id.countDistinct()
                ))
                .from(todo)
                .leftJoin(todo.managers, manager)
                .leftJoin(todo.comments, comment)
                .where(
                        titleContains(title),
                        createdDateBetween(startDate, endDate),
                        managerNicknameContains(nickname)
                )
                .groupBy(todo.id, todo.title, todo.createdAt) // 기본 키와 select 절의 non-aggregate 필드로 그룹화
                .orderBy(todo.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 카운트 조회 쿼리
        Long count = queryFactory
                .select(todo.id.countDistinct())
                .from(todo)
                .leftJoin(todo.managers, manager)
                .where(
                        titleContains(title),
                        createdDateBetween(startDate, endDate),
                        managerNicknameContains(nickname)
                )
                .fetchOne();

        return new PageImpl<>(resulsts, pageable, count == null ? 0L : count);
    }

    /**
     * 동적 쿼리 BooleanExpression
     **/

    private BooleanExpression titleContains(String title) {

        return StringUtils.hasText(title) ? todo.title.containsIgnoreCase(title) : null;
    }

    private BooleanExpression createdDateBetween(LocalDateTime startDate, LocalDateTime endDate) {

        // startDate 와 endDate 두 파라미터 입력이 있을 경우
        if (startDate != null & endDate != null) {
            return todo.createdAt.between(startDate, endDate);
        } else if (startDate != null) { // 시작날만 있을 경우
            return todo.createdAt.goe(startDate);
        } else if (endDate != null) { // 종료날만 있을 경우
            return todo.createdAt.loe(endDate);
        }

        return null;
    }

    private BooleanExpression managerNicknameContains(String nickname) {

        return StringUtils.hasText(nickname) ? todo.managers.any().user.nickname.containsIgnoreCase(nickname) : null;
    }
}
