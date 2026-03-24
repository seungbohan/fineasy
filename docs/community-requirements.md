# 종목 토론방(커뮤니티) 기능 요구사항 정의서

작성일: 2026-03-24
버전: 1.0

---

## 배경 및 목적

FinEasy는 현재 AI 분석, 뉴스, 재무제표 등 정보 소비 중심의 플랫폼이다. 사용자 간 투자 의견
교환 공간이 없어 체류 시간이 짧고 재방문 유인이 부족하다. 각 종목별 토론방을 추가하여
커뮤니티 기반의 사용자 참여를 유도하고, 실제 투자자들의 의견이 모이는 공간을 만든다.

---

## 기능 요구사항

### FR-1: 게시글 작성
- FR-1-1: 로그인한 사용자는 stockCode에 귀속된 토론방에 게시글을 작성할 수 있다
- FR-1-2: 게시글은 텍스트 본문만 지원한다 (이미지 첨부 미지원 — 1차 범위 제외)
- FR-1-3: 본문 최소 길이: 10자, 최대 길이: 500자
- FR-1-4: 작성 시 작성자의 닉네임(UserEntity.nickname)이 함께 저장된다
- FR-1-5: 금칙어 필터링을 통과해야 게시글이 저장된다 (FR-9 참조)

### FR-2: 게시글 조회
- FR-2-1: 비로그인 사용자도 게시글 목록과 상세 내용을 조회할 수 있다
- FR-2-2: 목록은 최신 글 우선(createdAt DESC) 정렬이 기본이다
- FR-2-3: 커서 기반 페이지네이션으로 다음 페이지를 로드한다 (size=20)
- FR-2-4: 각 게시글에 작성자 닉네임, 작성 상대시간, 좋아요 수, 싫어요 수, 댓글 수를 함께 반환한다
- FR-2-5: 삭제된 게시글은 "삭제된 게시글입니다"로 표시하되 목록에서 제거하지 않는다 (소프트 삭제)

### FR-3: 게시글 삭제
- FR-3-1: 본인이 작성한 게시글만 삭제할 수 있다
- FR-3-2: 삭제는 소프트 삭제(isDeleted=true)로 처리한다
- FR-3-3: 삭제된 게시글의 본문은 "삭제된 게시글입니다"로 치환하여 반환한다

### FR-4: 좋아요/싫어요
- FR-4-1: 로그인한 사용자만 좋아요/싫어요를 누를 수 있다
- FR-4-2: 사용자당 게시글 1개에 좋아요 또는 싫어요 중 하나만 가능하다 (중복 불가)
- FR-4-3: 이미 누른 반응을 다시 누르면 취소(토글)된다
- FR-4-4: 좋아요 상태에서 싫어요를 누르면 기존 좋아요가 취소되고 싫어요로 전환된다 (반대도 동일)
- FR-4-5: 현재 사용자의 반응 상태(liked/disliked/none)를 목록 조회 시 함께 반환한다

### FR-5: 댓글 작성
- FR-5-1: 로그인한 사용자는 게시글에 댓글을 작성할 수 있다
- FR-5-2: 댓글 최소 길이: 2자, 최대 길이: 200자
- FR-5-3: 댓글도 금칙어 필터링을 통과해야 저장된다
- FR-5-4: 1차 범위에서 대댓글(depth > 1)은 지원하지 않는다

### FR-6: 댓글 삭제
- FR-6-1: 본인이 작성한 댓글만 삭제할 수 있다
- FR-6-2: 소프트 삭제 처리, "삭제된 댓글입니다"로 치환하여 반환한다

### FR-7: 댓글 조회
- FR-7-1: 게시글 ID로 댓글 목록을 조회한다
- FR-7-2: 등록 시간 오름차순(createdAt ASC) 정렬이 기본이다
- FR-7-3: 커서 기반 페이지네이션 적용 (size=30)
- FR-7-4: 각 댓글에 작성자 닉네임, 작성 상대시간, 소프트 삭제 여부를 반환한다

### FR-8: 게시글 수 배지
- FR-8-1: 종목 상세 페이지의 '토론' 탭에 해당 종목의 게시글 수를 배지로 표시한다
- FR-8-2: 삭제된 게시글은 수에서 제외한다

### FR-9: 금칙어 필터링
- FR-9-1: 욕설/비속어 금칙어 목록을 서버에서 관리한다 (하드코딩 초기 목록, 향후 DB 이관 고려)
- FR-9-2: 금칙어 포함 시 HTTP 400 에러와 함께 "부적절한 내용이 포함되어 있습니다" 메시지를 반환한다
- FR-9-3: 완성형 단어 매칭 방식 (정규식 포함 검색, 자모 분리 공격 방어는 1차 범위 제외)

---

## 비기능 요구사항

### NFR-1: 성능
- 게시글 목록 조회 응답시간 < 200ms (Redis 캐시 미적용, DB 인덱스로 처리)
- 게시글 작성/삭제/반응은 캐시 무효화 없이 직접 DB 처리 (실시간성 보장)
- 인기 게시글(좋아요 상위) 집계는 1차에서 제외 — 단순 최신순만 지원

### NFR-2: 보안
- 게시글 작성/삭제/반응 API는 JWT 인증 필수
- 타인 게시글 삭제 시도 → HTTP 403 Forbidden
- 게시글 ID는 Long(순차) 대신 유지 (내부 서비스 — 외부 노출 위험 낮음)
- XSS 방어: 백엔드에서 HTML 이스케이프 처리 (Jsoup 또는 StringEscapeUtils)

### NFR-3: 확장성
- 향후 신고(Report) 기능 추가를 고려하여 post_id, user_id를 FK로 참조하는 구조로 설계
- 관리자 기능(게시글 강제 삭제, 사용자 정지)은 1차 범위 제외, 테이블 구조에서만 고려

### NFR-4: 캐싱 전략
- 게시글 목록은 Redis 캐시 미적용 (커서 기반 실시간 조회 — 캐시 효과 낮음)
- 종목별 게시글 수(배지용)는 Redis 5분 TTL 캐시 적용

---

## 제약 조건

- TECH-1: 기존 UserEntity, StockEntity를 그대로 참조한다 (스키마 변경 없음)
- TECH-2: 인증 방식은 기존 JWT Bearer 토큰 방식을 그대로 사용한다
- TECH-3: ApiResponse<T> record 래퍼를 모든 응답에 적용한다
- TECH-4: 엔티티 불변 생성자 패턴과 @PrePersist 패턴을 준수한다
- TECH-5: DTO는 Java record로 작성한다
- TECH-6: 프론트엔드 탭 구조에 '토론' 탭을 추가한다 (기존 '종목정보', '공시', '타임라인' 탭과 동일한 세그먼트 컨트롤 UI)
- LEGAL-1: 커뮤니티 기능에 "투자 권유 금지" 안내 문구를 상단에 고정 노출한다

---

## 가정사항

- ASSUME-1: 실시간 웹소켓 스트리밍은 지원하지 않는다. 새 게시글은 수동 새로고침(TanStack Query refetch) 으로 확인한다
- ASSUME-2: 게시글 수정 기능은 1차 범위에서 제외한다 (내용이 잘못된 경우 삭제 후 재작성)
- ASSUME-3: 이미지/파일 첨부는 1차 범위에서 제외한다
- ASSUME-4: 사용자 차단(Block) 기능은 1차 범위에서 제외한다
- ASSUME-5: 관리자 게시글 관리 도구는 1차 범위에서 제외한다

---

## 미결 사항

- OPEN-1: 게시글 정렬 옵션 제공 여부 (최신순만 vs 인기순도 제공?) — 현재 최신순만으로 결정
- OPEN-2: 비속어 목록 초기 데이터 범위 (한국어 욕설만 vs 영어 포함?) — 한국어만 시작
- OPEN-3: 같은 사용자가 여러 계정으로 도배할 경우 IP 기반 제한 적용 여부 — 1차 제외, Rate Limit으로 대응

---

## 데이터 모델 설계

### 테이블: stock_posts (종목 게시글)

```
stock_posts
  id              BIGSERIAL PRIMARY KEY
  stock_code      VARCHAR(20)   NOT NULL  -- stocks.stock_code FK 대신 값만 저장 (조회 유연성)
  user_id         BIGINT        NOT NULL  REFERENCES users(id)
  content         TEXT          NOT NULL  -- 10~500자
  like_count      INT           NOT NULL  DEFAULT 0
  dislike_count   INT           NOT NULL  DEFAULT 0
  comment_count   INT           NOT NULL  DEFAULT 0
  is_deleted      BOOLEAN       NOT NULL  DEFAULT false
  created_at      TIMESTAMP     NOT NULL  DEFAULT now()
  updated_at      TIMESTAMP     NOT NULL  DEFAULT now()

인덱스:
  idx_stock_posts_stock_code_created  (stock_code, created_at DESC)  -- 목록 조회
  idx_stock_posts_user_id             (user_id)                       -- 내 글 조회 (향후)
  idx_stock_posts_cursor              (stock_code, id DESC)           -- 커서 페이지네이션
```

설계 결정 이유:
- stock_code를 FK 대신 VARCHAR로 저장: 종목 삭제 시에도 게시글 보존, 조회 시 JOIN 불필요
- like_count/comment_count 컬럼 별도 관리: 집계 쿼리 없이 O(1) 조회 가능 (정합성은 트랜잭션으로 보장)

### 테이블: stock_post_reactions (게시글 반응)

```
stock_post_reactions
  id              BIGSERIAL PRIMARY KEY
  post_id         BIGINT        NOT NULL  REFERENCES stock_posts(id) ON DELETE CASCADE
  user_id         BIGINT        NOT NULL  REFERENCES users(id)
  reaction_type   VARCHAR(10)   NOT NULL  -- 'LIKE' | 'DISLIKE'
  created_at      TIMESTAMP     NOT NULL  DEFAULT now()

인덱스:
  UNIQUE (post_id, user_id)               -- 사용자당 게시글 1개 반응 제약
  idx_reactions_post_id  (post_id)
```

### 테이블: stock_post_comments (댓글)

```
stock_post_comments
  id              BIGSERIAL PRIMARY KEY
  post_id         BIGINT        NOT NULL  REFERENCES stock_posts(id) ON DELETE CASCADE
  user_id         BIGINT        NOT NULL  REFERENCES users(id)
  content         TEXT          NOT NULL  -- 2~200자
  is_deleted      BOOLEAN       NOT NULL  DEFAULT false
  created_at      TIMESTAMP     NOT NULL  DEFAULT now()
  updated_at      TIMESTAMP     NOT NULL  DEFAULT now()

인덱스:
  idx_comments_post_id_created  (post_id, created_at ASC)  -- 댓글 목록 조회
```

---

## API 엔드포인트 설계

### 게시글 API

| Method | Path | 인증 | 설명 |
|--------|------|------|------|
| GET    | /api/v1/stocks/{stockCode}/posts | 불필요 | 게시글 목록 (커서 페이지네이션) |
| POST   | /api/v1/stocks/{stockCode}/posts | 필요 | 게시글 작성 |
| DELETE | /api/v1/stocks/{stockCode}/posts/{postId} | 필요 (본인) | 게시글 삭제 |
| GET    | /api/v1/stocks/{stockCode}/posts/count | 불필요 | 게시글 수 (배지용) |

### 반응 API

| Method | Path | 인증 | 설명 |
|--------|------|------|------|
| POST   | /api/v1/posts/{postId}/reactions | 필요 | 좋아요/싫어요 토글 |

### 댓글 API

| Method | Path | 인증 | 설명 |
|--------|------|------|------|
| GET    | /api/v1/posts/{postId}/comments | 불필요 | 댓글 목록 (커서 페이지네이션) |
| POST   | /api/v1/posts/{postId}/comments | 필요 | 댓글 작성 |
| DELETE | /api/v1/posts/{postId}/comments/{commentId} | 필요 (본인) | 댓글 삭제 |

---

## 요청/응답 형식

### 게시글 목록 조회

요청:
```
GET /api/v1/stocks/005930/posts?cursor=123&size=20
```
- cursor: 마지막으로 받은 postId (없으면 첫 페이지)
- size: 기본값 20, 최대 50

응답:
```json
{
  "success": true,
  "data": {
    "posts": [
      {
        "id": 100,
        "content": "실적 발표가 기대됩니다",
        "authorNickname": "투자왕",
        "likeCount": 5,
        "dislikeCount": 1,
        "commentCount": 3,
        "myReaction": "LIKE",
        "isDeleted": false,
        "createdAt": "2026-03-24T10:00:00Z"
      }
    ],
    "nextCursor": 80,
    "hasNext": true
  }
}
```
- myReaction: "LIKE" | "DISLIKE" | null (비로그인 시 항상 null)

### 게시글 작성

요청:
```json
POST /api/v1/stocks/005930/posts
Authorization: Bearer {token}
{
  "content": "삼성전자 실적 발표 기대됩니다. 내년 반도체 시장 전망이 밝네요."
}
```

응답:
```json
{
  "success": true,
  "data": {
    "id": 101,
    "content": "삼성전자 실적 발표 기대됩니다...",
    "authorNickname": "투자왕",
    "likeCount": 0,
    "dislikeCount": 0,
    "commentCount": 0,
    "myReaction": null,
    "isDeleted": false,
    "createdAt": "2026-03-24T10:05:00Z"
  }
}
```

### 반응(좋아요/싫어요) 토글

요청:
```json
POST /api/v1/posts/100/reactions
Authorization: Bearer {token}
{
  "reactionType": "LIKE"
}
```

응답:
```json
{
  "success": true,
  "data": {
    "postId": 100,
    "likeCount": 6,
    "dislikeCount": 1,
    "myReaction": "LIKE"
  }
}
```
- 이미 LIKE 상태에서 LIKE 요청 → myReaction: null (취소)
- LIKE 상태에서 DISLIKE 요청 → myReaction: "DISLIKE" (전환)

---

## UI/UX 설계 방향

### 탭 통합 방식
기존 세그먼트 컨트롤(종목정보 / 공시 / 타임라인)에 **토론** 탭을 추가한다.
탭 순서: 종목정보 | 공시 | 타임라인 | 토론

이유: 별도 페이지로 분리하면 종목 컨텍스트(가격, 차트)를 잃게 된다.
탭 방식이 사용자가 종목 정보를 보며 토론 내용을 함께 확인하는 데 적합하다.

### 모바일 우선 레이아웃
- 게시글 카드: 상단에 닉네임 + 상대시간, 하단에 좋아요/싫어요/댓글 액션 바
- 댓글 영역: 게시글 카드 하단 드롭다운 방식 (Sheet 컴포넌트 사용, 기존 뉴스 분석 Sheet와 동일 패턴)
- 글 작성: 탭 상단 고정 입력 폼 (항상 노출, 비로그인 시 클릭 시 로그인 유도)

### 페이지네이션 방식
무한 스크롤 대신 **"더 보기" 버튼** 방식을 선택한다.

이유:
- 모바일에서 무한 스크롤은 하단 네비게이션 바와 충돌 가능성이 있음
- TanStack Query의 useInfiniteQuery로 "더 보기" 버튼도 동일하게 구현 가능
- 사용자가 원하는 시점에 추가 로드 제어 가능

### 투자 권유 금지 안내
토론 탭 최상단에 고정 배너로 표시:
"이 공간은 투자자 의견 공유 목적으로 운영됩니다. 모든 내용은 투자 권유가 아니며, 실제 투자는 본인 판단 하에 이루어져야 합니다."
