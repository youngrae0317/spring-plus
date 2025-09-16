package org.example.expert.domain.todo.repository;

import static org.example.expert.domain.todo.entity.QTodo.todo;
import static org.example.expert.domain.user.entity.QUser.user;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
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
    public Page<Todo> searchTodos(String title, LocalDateTime startDate, LocalDateTime endDate, String nickname, Pageable pageable) {
        // BooleanBuilder를 활용하여 동적 쿼리 조건 구성
        BooleanBuilder builder = new BooleanBuilder();

        /**  StringUtils를 활용하여 여러가지 메서드 활용 **/
        // 제목이 Text 형태인지 확인 후 true이면 request의 title이 포함되어있는지 조건 결합
        if (StringUtils.hasText(title))
            builder.and(todo.title.contains(title));

        // startDate가 null 이 아닐 경우 생성일이 startDate 보다 크거나 같은 조건 결합
        if (startDate != null)
            builder.and(todo.createdAt.goe(startDate));

        // EndDate가 null 이 아닐 경우 생성일이 endDate 보다 작거나 같은 조건 결합
        if (endDate != null)
            builder.and(todo.createdAt.loe(endDate));

        // 담당자 닉네임이 Text 형태인지 확인 후 true이면 request의 nickname이 포함되어있는지 조건 결합
        if (StringUtils.hasText(nickname))
            builder.and(todo.managers.any().user.nickname.contains(nickname));

        // 조건에 맞는 목록 조회
        List<Todo> resulsts = queryFactory
                .selectFrom(todo)
                .where(builder)
                .orderBy(todo.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long count = queryFactory
                .select(todo.count())
                .from(todo)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(resulsts, pageable, count == null ? 0L : count);

    }
}
