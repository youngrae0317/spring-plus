# 🚀 Spring 플러스 주차 개인 과제

---


### Level 1

#### 1. `@Transactional` 문제 해결
* **문제:** `TodoService` 클래스에 `@Transactional(readOnly = true)`가 설정되어 있어, 할 일을 저장하는 `saveTodo` 메소드에서 데이터 변경이 불가능한 오류가 발생
* **해결:** `saveTodo` 메소드에 `@Transactional` 어노테이션을 추가 하여, 쓰기 트랜잭션이 가능하도록 수정

#### 2. JWT 수정
* **요구사항:** JWT에 사용자의 닉네임 정보 포함
* **해결:**
    1.  `User` 엔티티에 `nickname` 필드를 추가했습니다.
    2.  `JwtUtil`의 `createToken` 메소드에서 `nickname`을 claim으로 추가하여 토큰을 생성하도록 수정
    3.  `JwtAuthenticationFilter`에서 토큰을 파싱할 때 `nickname` 추출하여 `AuthUser` 객체를 생성하도록 변경

#### 3. JPA의 이해
* **요구:** 할 일 목록 조회 시 `weather` 및 `modifiedAt` 을 이용하여 동적 검색 기능 추가
* **해결 방안:** `TodoRepository`에 `findByConditions`를 추가하여 `weather`, `startDate`, `endDate` 파라미터가 `null`이 아닐 경우 `WHERE` 절에 해당 조건을 동적으로 추가하여 검색

#### 4. 컨트롤러 테스트 코드 오류 수정
* **문제:** `todo_단건_조회_시_todo가_존재하지_않아_예외가_발생한다()` 테스트에서 `InvalidRequestException` HTTP 상태 코드 `400 Bad Request`로 예상
* **해결:** `InvalidRequestException`은 `HttpStatus.BAD_REQUEST` 으로 처리 되고 있었으므로 테스트 코드의 `when` 블록에서 `todoService.getTodo(todoId)`가 예외를 던지도록 설정하고, `andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.name()))` 와 같이 다른 것들도 수정

#### 5. AOP의 이해
* **문제:** `UserAdminController`의 `changeUserRole` 메소드 실행 전 로그를 남기는 AOP 동작이 잘못됨
* **해결:** `AdminAccessLoggingAspect`의 Pointcut 설정이 잘못 되어 있었다. `execution(* org.example.expert.domain.user.controller.UserAdminController.changeUserRole(..))`로 정확하게 수정

---

### Level 2

#### 6. JPA Cascade
* **요구:** 할 일을 생성하면, 할 일을 생성한 유저는 자동으로 담당자(`Manager`)로 등록되어야 했습니다.
* **해결:** `Todo` 엔티티와 `Manager` 엔티티에서 `@OneToMany(mappedBy = "todo", cascade = {CascadeType.PERSIST, CascadeType.REMOVE})` 로 수정하여 `cascade` 옵션을 추가

#### 7. N+1
* **문제:** 댓글 목록을 조회하는 `getComments()` API 호출 시, 추가적인 쿼리가 발생하는 N+1 문제 발생
* **해결 방안:** `CommentRepository`의 조회 메소드에 fetch join을 사용. `@Query("SELECT c FROM Comment c JOIN FETCH c.user WHERE c.todo.id = :todoId")`를 N+1 문제 해결

#### 8. QueryDSL
* **요구:** JPQL 메소드 `findByIdWithUser`를 QueryDSL로 변경
* **해결:** `TodoRepositoryQuery` 인터페이스를 정의하고 `TodoRepositoryQueryImpl` 에 QueryDSL을 사용한 `findByIdWithUser` 메소드를 구현. `JPAQueryFactory`를 사용하여 `select(todo).from(todo).leftJoin(todo.user, user).fetchJoin()` 적용.

#### 9. Spring Security
* **요구:** 기존 `Filter`와 `Argument Resolver` 를 Spring Security로 변경
* **해결:** `SecurityConfig` 클래스를 생성

---

### Level 3: 도전 기능

#### 10. QueryDSL 을 사용하여 검색 기능 만들기
* **요구:** 제목, 생성일, 담당자 닉네임 등 여러 조건으로 할 일을 동적으로 검색하고, 필요한 데이터(제목, 담당자 수, 총 댓글 개수)만 페이징 처리하여 반환
* **해결:**
    1.  `/todos/search` 엔드포인트를 `TodoController`에 추가했습니다.
    2.  `TodoRepositoryQueryImpl`에서 QueryDSL의 `BooleanExpression`을 사용하여 각 검색 조건이 존재할 때만 쿼리에 포함되도록 동적 쿼리를 구현
    3.  `select` 에서 `new QTodoearchResponse(...)`를 사용하여 `TodoSearchResponse` DTO로 직접 결과를 매핑.

#### 11. `@Transaction` 심화
* **요구:** 담당자 등록(`saveManager`)과는 별개로, 로그 테이블에 항상 요청 로그가 남아야 됨.
* **해결 방안:**
    1.  로그를 기록하는 `LogService`와 `Log` 엔티티를 생성
    2.  `LogService`의 `saveLog` 메소드에 `@Transactional(propagation = Propagation.REQUIRES_NEW)` 옵션을 적용
    3.  `ManagerService`에 `saveManager` 메서드 내부에서 이 `saveLog` 메소드를 호출합니다. `REQUIRES_NEW` 을 적용하였으므로 `saveManager`의 트랜잭션이 롤백되더라도, `saveLog`는 새로운 트랜잭션에서 실행된다.