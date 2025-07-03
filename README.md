# SPRING PLUS

# 프로젝트: Todo Expert

## 목차

1. 프로젝트 개요
2. 기능 구현 목록
3. 레벨별 구현 상세
4. 트러블슈팅
5. API 요약
6. 기술 스택

---

## 1. 프로젝트 개요

Spring Boot 기반의 할 일 관리 시스템.
JWT 기반 인증 및 권한 처리, QueryDSL을 활용한 검색 최적화, AOP 기반 로깅 기능, N+1 문제 해결 등 다양한 실무 상황을 반영한 기능들을 구현했다.

---

## 2. 기능 구현 목록

| 레벨      | 내용                                                    |
|---------|-------------------------------------------------------|
| Level 1 | 트랜잭션 처리, JWT에서 유저 정보 추출, JPQL 동적 검색, 테스트 코드 개선, AOP   |
| Level 2 | Cascade 저장, N+1 해결, QueryDSL 리팩토링, Spring Security 전환 |
| Level 3 | 복합 검색 기능(QueryDSL), 트랜잭션 분리 로깅                        |

---

## 3. 레벨별 구현 상세

### Level 1

* `@Transactional(readOnly = true)`에서 insert 발생 시 예외 해결
  → 서비스 계층에서 `@Transactional(readOnly = false)` 명시

* User 엔티티에 nickname 컬럼 추가
  → JWT 생성 시 nickname 포함
  → 토큰 검증 후 nickname 조회 가능하도록 구현

* JPQL 기반 동적 검색 기능 구현
  → `weather`, `modifiedAt` 기간 조건을 동적으로 처리
  → `@Query` 문 내부에서 `:조건 IS NULL OR 필드 = :조건` 방식 사용

* Controller 테스트 예외 메시지 불일치 해결
  → 실제 예외 메시지를 확인하고 테스트 코드 수정

* AOP 로그 처리 시점 수정
  → `@Before` 어노테이션 사용하여 메서드 실행 전 로그 수행

### Level 2

* Todo 생성 시 유저가 자동으로 매니저로 등록되도록 cascade 적용
  → `@OneToMany(mappedBy = "...", cascade = CascadeType.PERSIST)` 적용

* 댓글 목록 조회 시 발생한 N+1 문제 해결
  → `@EntityGraph(attributePaths = {"user"})` 또는 JPQL `fetch join` 사용

* `findByIdWithUser()`를 QueryDSL로 전환
  → 연관 관계를 join 처리하며 N+1 문제 방지

* Spring Security 적용
  → 기존 필터 기반 인증 구조 제거
  → `SecurityFilterChain`, `UserDetailsService`, `@AuthenticationPrincipal` 적용
  → JWT는 동일하게 사용하며 권한은 Security에서 처리

### Level 3

* 일정 검색 기능 구현 (QueryDSL 기반)

    * 제목, 생성일 범위, 담당자 닉네임 조건으로 검색 가능
    * `Projections.fields()`로 필요한 필드만 조회 (제목, 담당자 수, 댓글 수)
    * `Pageable`을 통한 페이징 처리
    * 제목 및 닉네임은 부분 일치 지원

* 트랜잭션 분리 처리 (로그 기록)

    * 매니저 등록 시 로그 테이블에 요청 정보 저장
    * 로그는 등록 실패와 상관없이 항상 저장되어야 함
    * 로그 저장 메서드는 `@Transactional(propagation = REQUIRES_NEW)` 설정

---

## 4. 트러블슈팅

### 트랜잭션 오류

* 문제: `readOnly = true`인 상태에서 insert 시도
* 해결: `readOnly = false`로 명시하거나 insert 메서드를 분리

### N+1 문제

* 문제: 댓글 조회 시 user 객체를 Lazy 로딩하면서 다수의 쿼리 발생
* 해결: `@EntityGraph` 또는 `fetch join` 적용하여 해결

### AOP 미작동

* 문제: 로그가 메서드 실행 후 출력됨
* 해결: `@Before` 사용하여 실행 전 로그 처리

---

## 5. 기술 스택

* Java 17
* Spring Boot 3.x
* Spring Data JPA
* QueryDSL
* Spring Security
* JWT
* JUnit 5 / AssertJ / Mockito
* H2 / MySQL
* Gradle