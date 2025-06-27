package org.example.expert.domain.todo.repository;


import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.expert.domain.todo.dto.request.TodoSearchRequest;
import org.example.expert.domain.todo.dto.response.TodoSearchResponse;
import org.example.expert.domain.todo.entity.Todo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.example.expert.domain.comment.entity.QComment.comment;
import static org.example.expert.domain.manager.entity.QManager.manager;
import static org.example.expert.domain.todo.entity.QTodo.todo;
import static org.example.expert.domain.user.entity.QUser.user;

@Repository
@RequiredArgsConstructor
public class QTodoRepositoryImpl implements QTodoRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<Todo> findByIdWithUser(Long id) {
        Todo result = queryFactory
                .selectFrom(todo)
                .leftJoin(todo.user, user).fetchJoin()
                .where(todo.id.eq(id))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public Page<TodoSearchResponse> searchTodos(TodoSearchRequest request, Pageable pageable) {

        /**
         * 데이터 조회 쿼리
         */
        List<TodoSearchResponse> results = queryFactory
                .select(Projections.fields(
                        TodoSearchResponse.class,
                        todo.title.as("title"),
                        manager.countDistinct().coalesce(0L).as("managerCount"),
                        comment.countDistinct().coalesce(0L).as("commentCount")
                ))
                .from(todo)
                .leftJoin(todo.managers, manager)
                .leftJoin(manager.user, user)
                .leftJoin(todo.comments, comment)
                .where(
                        containsTitle(request.getTitle()),
                        containsNickname(request.getNickname()),
                        betweenCreatedAt(request.getStartDate(), request.getEndDate())
                )
                .groupBy(todo.id, todo.title, todo.createdAt) // createdAt 추가
                .orderBy(todo.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = getTotalCount(request);

        return new PageImpl<>(results, pageable, total != null ? total : 0L);
    }

    /**
     * COUNT 쿼리 - 닉네임 검색이 있을 때만 JOIN 수행
     */
    private Long getTotalCount(TodoSearchRequest request) {
        JPAQuery<Long> countQuery = queryFactory
                .select(todo.countDistinct())
                .from(todo);

        boolean hasNicknameCondition = request.getNickname() != null && !request.getNickname().isBlank();

        if (hasNicknameCondition) {
            countQuery.leftJoin(todo.managers, manager)
                    .leftJoin(manager.user, user);
        }

        return countQuery.where(
                containsTitle(request.getTitle()),
                hasNicknameCondition ? containsNickname(request.getNickname()) : null,
                betweenCreatedAt(request.getStartDate(), request.getEndDate())
        ).fetchOne();
    }

    private BooleanExpression containsTitle(String title) {
        return (title == null || title.isBlank()) ? null : todo.title.containsIgnoreCase(title);
    }

    private BooleanExpression containsNickname(String nickname) {
        return (nickname == null || nickname.isBlank()) ? null : user.nickname.containsIgnoreCase(nickname);
    }

    private BooleanExpression betweenCreatedAt(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null && endDate == null) {
            return null;
        }
        if (startDate == null) {
            return todo.createdAt.loe(endDate);
        }
        if (endDate == null) {
            return todo.createdAt.goe(startDate);
        }
        return todo.createdAt.between(startDate, endDate);
    }
}
