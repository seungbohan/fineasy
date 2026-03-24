# 종목 토론방 기능 구현 계획서

작성일: 2026-03-24
버전: 1.0
연관 문서: community-requirements.md

---

## Phase 1: 백엔드 — DB 스키마 + 게시글 CRUD

### 목표
게시글 작성/조회/삭제 API를 완성하고, 프론트엔드가 연동 가능한 상태로 만든다.

### 작업 목록

1. **DB 마이그레이션 (DDL)**
   - 대상 파일: 신규 SQL 마이그레이션 파일 또는 schema.sql
   - `stock_posts`, `stock_post_reactions`, `stock_post_comments` 테이블 생성
   - 인덱스 포함 (요구사항 정의서 데이터 모델 참조)

2. **엔티티 작성**
   - `StockPostEntity.java` — 불변 생성자 패턴, @PrePersist
   - `StockPostReactionEntity.java`
   - `StockPostCommentEntity.java`
   - 모두 `com.fineasy.entity` 패키지에 위치

3. **리포지토리 작성**
   - `StockPostRepository.java` — 커서 페이지네이션용 쿼리 포함
   - `StockPostReactionRepository.java` — findByPostIdAndUserId 포함
   - `StockPostCommentRepository.java`

4. **금칙어 필터 작성**
   - `ProfanityFilter.java` — `com.fineasy.util` 패키지
   - 초기 금칙어 목록 하드코딩, contains 방식으로 검사
   - `boolean contains(String text)` 단일 메서드

5. **DTO 작성**
   - Request: `CreatePostRequest.java` (record), `CreateCommentRequest.java` (record)
   - Response: `PostResponse.java` (record), `PostListResponse.java` (record), `CommentResponse.java` (record), `CommentListResponse.java` (record)

6. **서비스 작성**
   - `StockPostService.java`
     - `getPostsByStockCode(stockCode, cursor, size, userId)` — 커서 페이지네이션
     - `createPost(stockCode, userId, content)` — 금칙어 필터 후 저장
     - `deletePost(postId, userId)` — 소프트 삭제, 본인 검증
     - `getPostCount(stockCode)` — 배지용

7. **컨트롤러 작성**
   - `StockPostController.java` — `/api/v1/stocks/{stockCode}/posts` 경로
   - SecurityConfig에 인증 필요 엔드포인트 추가

### 완료 조건
- POST /api/v1/stocks/005930/posts 호출 시 게시글 저장됨
- GET /api/v1/stocks/005930/posts 커서 페이지네이션으로 목록 반환됨
- DELETE 시 본인 외 403 반환, 소프트 삭제 처리됨
- 금칙어 포함 시 400 반환됨

### 예상 리스크
- RISK-1: stock_posts에 stock_code를 FK 없이 VARCHAR로 저장했으므로 잘못된 stockCode로도 게시글 작성이 가능하다. 서비스 레이어에서 StockRepository.existsByStockCode() 호출로 검증이 필요하다.
- RISK-2: like_count, comment_count 컬럼의 정합성 — 동시 요청 시 Race Condition 발생 가능. @Transactional + 원자적 UPDATE (UPDATE stock_posts SET like_count = like_count + 1 WHERE id = ?)로 처리 필요.

---

## Phase 2: 백엔드 — 반응(좋아요/싫어요) + 댓글 API

### 목표
게시글에 감정 반응을 남기고 댓글을 달 수 있는 API를 완성한다.

### 작업 목록

1. **반응 서비스/컨트롤러 작성**
   - `StockPostReactionService.java`
     - `toggleReaction(postId, userId, reactionType)` — 토글 로직 구현
     - 기존 반응 조회 → 동일 타입이면 삭제(취소), 다른 타입이면 교체
     - like_count / dislike_count 원자적 업데이트
   - `StockPostReactionController.java` — `/api/v1/posts/{postId}/reactions`

2. **댓글 서비스/컨트롤러 작성**
   - `StockPostCommentService.java`
     - `getCommentsByPostId(postId, cursor, size)` — 커서 페이지네이션
     - `createComment(postId, userId, content)` — 금칙어 필터 + comment_count +1
     - `deleteComment(commentId, userId)` — 소프트 삭제, comment_count -1
   - `StockPostCommentController.java` — `/api/v1/posts/{postId}/comments`

3. **게시글 목록에 myReaction 포함**
   - 인증된 요청이면 해당 userId의 reaction을 batch 조회하여 PostResponse에 포함
   - 비인증 요청이면 myReaction: null 반환

### 완료 조건
- 좋아요 토글 동작 (누르기 → LIKE, 다시 누르기 → null, 싫어요로 전환)
- 댓글 작성/삭제 동작
- myReaction 필드가 목록에 정확히 반환됨

### 예상 리스크
- RISK-3: 반응 토글에서 기존 반응 삭제 + 새 반응 추가가 단일 트랜잭션 내에서 이루어져야 한다. 별도 트랜잭션으로 분리 시 데이터 불일치 발생 가능.
- RISK-4: 게시글 목록 N개에 대해 각각 반응 조회 시 N+1 쿼리 발생. `findByPostIdInAndUserId(postIds, userId)` 단일 쿼리로 IN 절 처리 필요.

---

## Phase 3: 프론트엔드 — 토론 탭 UI 구현

### 목표
종목 상세 페이지에 토론 탭을 추가하고, 게시글 목록/작성/삭제/반응/댓글 UI를 구현한다.

### 작업 목록

1. **타입 정의 추가**
   - `frontend/src/types/index.ts`에 `Post`, `Comment`, `ReactionType`, `PostListResponse`, `CommentListResponse` 타입 추가

2. **API 훅 작성**
   - `frontend/src/hooks/use-community.ts`
     - `useStockPosts(stockCode, cursor)` — useInfiniteQuery
     - `usePostCount(stockCode)` — useQuery (staleTime: 5분)
     - `useCreatePost()` — useMutation
     - `useDeletePost()` — useMutation
     - `useToggleReaction()` — useMutation (낙관적 업데이트 적용)
     - `usePostComments(postId)` — useInfiniteQuery
     - `useCreateComment()` — useMutation
     - `useDeleteComment()` — useMutation

3. **컴포넌트 작성**
   - `frontend/src/components/community/post-write-form.tsx` — 게시글 작성 폼
   - `frontend/src/components/community/post-card.tsx` — 게시글 카드
   - `frontend/src/components/community/post-list.tsx` — 게시글 목록 + 더 보기
   - `frontend/src/components/community/comment-sheet.tsx` — 댓글 Bottom Sheet (기존 Sheet 컴포넌트 활용)
   - `frontend/src/components/community/comment-item.tsx` — 댓글 아이템

4. **탭 추가**
   - `stock-detail-client.tsx`
     - activeTab 타입에 `'community'` 추가
     - 탭 네비게이션에 '토론' 버튼 추가 (MessageSquare 아이콘)
     - `<CommunityTab stockCode={stockCode} />` 렌더링 조건 추가

5. **CommunityTab 구성**
   - 투자 권유 금지 안내 배너 (상단 고정)
   - PostWriteForm (로그인 상태이면 활성화, 비로그인이면 로그인 유도 링크)
   - PostList (PostCard 목록 + 더 보기 버튼)
   - CommentSheet (게시글 카드 클릭 시 열림)

### 완료 조건
- 종목 상세 페이지에서 '토론' 탭 클릭 시 게시글 목록 표시됨
- 로그인 상태에서 게시글 작성/삭제 동작
- 좋아요/싫어요 클릭 시 낙관적 업데이트로 즉시 반영됨
- 댓글 Sheet가 열리고 댓글 작성/삭제 동작

### 예상 리스크
- RISK-5: 탭이 4개로 늘어나면 모바일에서 가로 공간이 부족해질 수 있다. 각 탭 텍스트를 짧게 유지하거나 아이콘 병용으로 대응한다.
- RISK-6: 낙관적 업데이트(좋아요) 후 서버 실패 시 롤백 처리를 누락하면 UI-서버 불일치가 발생한다. useMutation의 onError에서 queryClient.invalidateQueries 호출로 강제 동기화한다.

---

## Phase 4: 배포 및 검증

### 작업 목록

1. **DB 마이그레이션 적용**
   - 운영 PostgreSQL에 DDL 실행 (deploy 디렉토리 스크립트 또는 수동 실행)

2. **SecurityConfig 검토**
   - GET 엔드포인트는 permitAll, POST/DELETE는 authenticated 설정 확인

3. **통합 테스트**
   - 게시글 작성 → 목록 조회 → 좋아요 → 댓글 작성 → 삭제 전 과정 수동 테스트
   - 비로그인 조회, 타인 게시글 삭제 시도(403), 금칙어 필터(400) 검증

4. **캐싱 적용**
   - 게시글 수(count) Redis 캐시 5분 TTL 적용

### 완료 조건
- 운영 환경에서 전 과정 동작 확인
- 비정상 케이스 (401, 403, 400) 정상 응답 확인

---

## 작업 의존 관계

```
Phase 1 (게시글 CRUD 백엔드)
    ↓
Phase 2 (반응/댓글 백엔드)    ←── Phase 1과 병렬 불가 (엔티티 의존)
    ↓
Phase 3 (프론트엔드 UI)       ←── Phase 1+2 API 완성 후 시작 가능
    ↓
Phase 4 (배포/검증)
```

Phase 2와 Phase 3는 백엔드 API 스펙이 확정되면 일부 병렬 작업 가능
(타입 정의, 컴포넌트 UI 껍데기는 API 없이도 작성 가능)

---

## 검증 계획

| 단계 | 검증 방법 |
|------|-----------|
| Phase 1 완료 | Swagger UI에서 게시글 CRUD 직접 호출 테스트 |
| Phase 2 완료 | 좋아요 토글 시 DB stock_posts.like_count 직접 확인 |
| Phase 3 완료 | 로컬 환경에서 브라우저 수동 테스트 (로그인/비로그인 전환) |
| Phase 4 완료 | 운영 환경 fineasy.co.kr에서 전 과정 수동 테스트 |
