# Phase 4 요구사항 정의서: 뉴스 강화 + 공시 기능

**작성일**: 2026-03-15
**작성자**: Senior Requirements Planner
**대상 시스템**: FinEasy (Spring Boot 3.2.3 / Next.js App Router)

---

## 배경 및 목적

현재 FinEasy는 RSS 기반의 범용 뉴스 수집과 DART 재무제표 연동만 제공한다.
Phase 4에서는 세 가지 영역을 확장하여 종목 상세 페이지의 정보 밀도를 토스증권 수준으로 끌어올린다.

1. **Finnhub 뉴스**: 해외 종목별 실시간 뉴스를 종목 상세 페이지에 표시
2. **DART 공시**: 국내 종목의 최신 공시 목록을 종목 상세 페이지 "공시" 탭으로 노출
3. **SEC EDGAR 공시**: 해외 종목의 주요 Filing을 종목 상세 페이지 "공시" 탭으로 노출

---

## 기능 요구사항

### 기능 1: Finnhub 뉴스 수집 (해외 종목)

#### FR-1-1: Finnhub API 클라이언트 구현
- `GET /v1/company-news?symbol={ticker}&from={from}&to={to}&token={apiKey}` 엔드포인트 호출
- 무료 티어 Rate Limit: 분당 60회 → **스케줄러에서 종목별 호출 사이에 1초 대기**
- API 응답 필드 매핑: `id(long)`, `headline`, `url`, `source`, `datetime(unix timestamp)`, `summary`, `sentiment(score: -1~1)`
  - 주의: Finnhub `sentiment` 필드는 프리미엄 플랜 전용. 무료 티어에서는 `null` 반환 → 기존 `KeywordSentimentAnalyzer`로 대체 분석

#### FR-1-2: 해외 종목 뉴스 수집 스케줄러
- 대상: `StockEntity.market` = `NASDAQ`, `NYSE`, `AMEX` 인 종목
- 전체 해외 종목이 아니라 **관심 종목(Watchlist)에 등록된 해외 종목 + 인기 상위 50종목**만 수집 (Rate Limit 대응)
- 수집 주기: 1시간마다 (기존 RSS 스케줄러와 별도 실행)
- 중복 방지: 기존 `uk_news_original_url` UNIQUE 제약 활용 → `NewsArticleEntity.originalUrl` 충돌 시 skip
- 종목 태깅: 수집된 뉴스에 해당 `StockEntity`를 `taggedStocks`로 자동 태깅
- 수집 기간: 최근 7일치 뉴스 (from = 7일전, to = 오늘)

#### FR-1-3: 종목별 뉴스 API 응답에 Finnhub 뉴스 통합
- 기존 `GET /api/v1/stocks/{stockCode}/news` API가 이미 `news_stock_tags` 기준으로 조회함
- Finnhub에서 수집된 뉴스도 동일 테이블(`news_articles`)에 저장되고 `news_stock_tags`에 태깅되므로 **별도 API 불필요**
- 단, 응답에 `source` 구분을 위해 `NewsArticleResponse`에 `sourceType` 필드 추가 (`RSS` / `FINNHUB`)

#### FR-1-4: Finnhub 감성 점수 처리
- Finnhub 무료 티어는 `sentiment` 필드를 반환하지 않음 → 기존 `NewsSentimentService.analyzeSentiment()` 흐름 그대로 활용
- `NewsArticleEntity`에 `sourceType` 컬럼 추가 (`VARCHAR(20)`, 기본값 `'RSS'`)

---

### 기능 2: 국내 종목 공시 (DART API)

#### FR-2-1: DART 공시 목록 조회 API 구현
- DART API: `GET /list.json` (공시 검색)
  - 파라미터: `corp_code`, `bgn_de`(시작일), `end_de`(종료일), `pblntf_ty`(공시유형), `page_no`, `page_count`
  - 주요 공시 유형 (`pblntf_ty`):
    - `A`: 사업보고서 (연간)
    - `B`: 기타공시
    - `C`: 주요사항보고서
    - `D`: 외부감사 관련
    - `E`: 펀드공시
    - `F`: 자산유동화
    - `G`: 거래소공시
    - `H`: 공정공시
    - `I`: 증권신고서
  - FinEasy 대상 유형: `A`(사업보고서), `C`(주요사항보고서), `H`(공정공시), `I`(증권신고서)
- 응답 필드: `rcept_no`(접수번호), `corp_name`, `report_nm`(보고서명), `rcept_dt`(접수일), `flr_nm`(제출인)
- DART 원문 링크: `https://dart.fss.or.kr/dsaf001/main.do?rcpNo={rcept_no}`

#### FR-2-2: 공시 데이터 캐싱 전략
- **DB 저장 없음**: 공시 목록은 실시간성이 중요하고 데이터 변동이 잦아 DB 캐시 부적합
- **Redis 캐싱만 적용**: TTL 30분, 키 패턴 `dart:disclosure:{stockCode}:{pageNo}`
- Redis 비활성화 시: `DartApiClient`에서 직접 API 호출 (Best Effort)

#### FR-2-3: API 엔드포인트
```
GET /api/v1/stocks/{stockCode}/disclosures
  - QueryParam: page(default=0), size(default=10), type(optional, 공시유형코드)
  - Response: PageResponse<DartDisclosureResponse>

GET /api/v1/stocks/{stockCode}/disclosures/{rcpNo}
  - Response: DartDisclosureDetailResponse (원문 URL 포함)
```

#### FR-2-4: 대상 종목 제한
- `DartCorpCodeEntity`에 `corp_code`가 매핑된 국내 종목만 공시 제공
- 해외 종목(`market = NASDAQ/NYSE/AMEX`)에서 공시 탭 접근 시: SEC EDGAR 공시로 연결

#### FR-2-5: 중요 공시 하이라이트 표시
- 보고서명에 다음 키워드 포함 시 `isImportant=true` 표시:
  - "주요사항보고서", "사업보고서", "분기보고서", "반기보고서", "합병", "분할", "유상증자", "전환사채"

---

### 기능 3: 해외 종목 공시 (SEC EDGAR API)

#### FR-3-1: SEC EDGAR CIK 번호 매핑
- SEC EDGAR API: `https://efts.sec.gov/LATEST/search-index?q={ticker}&dateRange=custom&startdt={from}&enddt={to}&forms={formType}`
  - 또는 CIK 조회: `https://data.sec.gov/submissions/CIK{cik:010}.json`
  - 티커 → CIK 변환: `https://www.sec.gov/cgi-bin/browse-edgar?action=getcompany&CIK={ticker}&type=&dateb=&owner=include&count=10&search_text=&output=atom`
  - 권장 방식: `https://efts.sec.gov/LATEST/search-index?q=%22{ticker}%22&dateRange=custom&forms=10-K,10-Q,8-K` (인증 불필요)
- `StockEntity`에 `secCik` 컬럼 추가 (`VARCHAR(20)`, nullable)
- 초기 CIK 매핑: `SecEdgarCikSyncService`에서 해외 종목 전체 대상으로 CIK를 배치 조회하여 `StockEntity.secCik` 업데이트
  - EDGAR Company Facts API: `GET https://data.sec.gov/submissions/CIK{cik:010}.json`
  - Ticker → CIK 매핑 파일: `https://www.sec.gov/files/company_tickers.json` (전체 매핑 JSON, 단건 다운로드로 해결)

#### FR-3-2: SEC Filing 목록 조회
- EDGAR Submissions API: `GET https://data.sec.gov/submissions/CIK{cik:010}.json`
  - 응답의 `filings.recent` 배열에서 최신 Filing 목록 추출
  - 필드: `accessionNumber`, `filingDate`, `form`, `primaryDocument`, `primaryDocDescription`
- 대상 Form 유형:
  - `10-K`: 연간보고서
  - `10-Q`: 분기보고서
  - `8-K`: 중요사항
  - `Form 4`: 내부자 거래
  - `20-F`: 외국 민간 발행자 연간보고서 (ADR 종목 대응)
- SEC 원문 링크: `https://www.sec.gov/Archives/edgar/data/{cik}/{accessionNumber}/{primaryDocument}`
  - 또는 Filing index: `https://www.sec.gov/cgi-bin/browse-edgar?action=getcompany&CIK={cik}&type={form}&dateb=&owner=include&count=10`

#### FR-3-3: API 엔드포인트
```
GET /api/v1/stocks/{stockCode}/sec-filings
  - QueryParam: page(default=0), size(default=10), form(optional, 10-K/10-Q/8-K/4)
  - Response: PageResponse<SecFilingResponse>
```

#### FR-3-4: 캐싱 전략
- EDGAR Submissions 전체 JSON은 용량이 크므로 (수백 KB) **DB에 저장하지 않음**
- Redis 캐싱: TTL 1시간, 키 패턴 `sec:filings:{stockCode}`
- CIK 매핑 정보만 `StockEntity.secCik`에 영구 저장

#### FR-3-5: User-Agent 헤더 설정
- SEC EDGAR API는 `User-Agent` 헤더 필수: `"FinEasy contact@fineasy.app"`
- 미설정 시 `403` 응답 → `WebClient` Bean에 기본 헤더로 설정

#### FR-3-6: 중요 Filing 하이라이트 표시
- `8-K`, `Form 4`는 `isImportant=true` 기본 표시 (즉각적 공시)
- `10-K`, `10-Q`는 일반 중요도

---

## 비기능 요구사항

### NFR-1: Rate Limit 준수
- Finnhub 무료 티어: 분당 60회 → 스케줄러에서 호출 간 1초 sleep
- SEC EDGAR: 초당 10 request 제한 (robots.txt 기준) → 호출 간 100ms sleep
- DART API: 명시적 제한 없으나 비즈니스 로직상 종목별 on-demand 조회이므로 문제 없음

### NFR-2: 장애 격리
- Finnhub API 장애 시 → RSS 뉴스는 정상 동작 유지 (별도 스케줄러)
- DART 공시 API 장애 시 → 해당 탭에 "일시적으로 조회할 수 없습니다" 표시
- SEC EDGAR 장애 시 → 동일

### NFR-3: 법적 요구사항
- 뉴스 원문 복제 금지: 제목 + 출처 + 링크만 표시, content는 저장하지 않음 (Finnhub 뉴스 `summary` 필드는 미저장)
- DART/SEC 공시: 원문 링크로 이동 방식, 본문 캐시 없음
- AI 면책조항: 기존 `NewsAnalysisResponse.AI_DISCLAIMER` 상수 그대로 적용

### NFR-4: 성능
- 공시 탭: 최초 로딩 1초 이내 (Redis 캐시 히트 기준)
- 종목별 뉴스: 기존 staleTime 60초 유지

---

## 제약 조건

- **Finnhub API Key**: 현재 `.env`에 설정되지 않음 → `FINNHUB_API_KEY` 환경변수 추가 필요
- **SEC EDGAR**: 인증 불필요 (공개 API), User-Agent만 필수
- **DART API**: 이미 `DartApiClient` 구현 완료, `DartApiProperties` 재사용
- **DB 스키마 변경**: `stocks` 테이블에 `sec_cik` 컬럼 추가, `news_articles` 테이블에 `source_type` 컬럼 추가
- **Finnhub 무료 티어 제한**: 실시간 WebSocket, 감성 점수, 내부자 거래 API는 유료 → 공시 관련 Finnhub 기능은 제외

---

## 가정사항

- GA-1: Finnhub 무료 API 키는 사용자가 발급하여 환경변수에 설정한다
- GA-2: SEC EDGAR `company_tickers.json` 다운로드로 전체 티커↔CIK 매핑을 한 번에 처리한다 (개별 API 호출 불필요)
- GA-3: DART 공시 탭은 국내 종목만 표시한다 (DART corp_code 매핑 없는 해외 종목에는 SEC 탭만)
- GA-4: Finnhub 뉴스 수집은 Watchlist 등록 종목과 인기 상위 50종목에 한정한다 (전체 해외 종목 수집 시 Rate Limit 초과)

---

## 미결 사항

- MQ-1: **Finnhub 뉴스 수집 범위**: 모든 해외 종목(NASDAQ/NYSE/AMEX 전체)에 대해 수집할 경우 종목 수가 수천 개이므로 Rate Limit(분당 60회)으로는 불가. → **Watchlist + 인기 상위 50종목으로 제한** 방향이 현실적. 사용자 확인 필요.
- MQ-2: **SEC Filing 본문 표시**: 원문 링크로 새 탭 이동 vs 인앱 렌더링 중 어떤 방식을 선호하는가? (법적 리스크 고려 시 링크 방식 권장)
- MQ-3: **DART 공시 저장 여부**: 공시 이력을 DB에 저장하여 알림 기능을 추후 제공할 계획이 있는가?

---

## 데이터 모델

### 신규 컬럼 (stocks 테이블)
```sql
ALTER TABLE stocks ADD COLUMN sec_cik VARCHAR(20);
CREATE INDEX idx_stocks_sec_cik ON stocks(sec_cik);
```

### 신규 컬럼 (news_articles 테이블)
```sql
ALTER TABLE news_articles ADD COLUMN source_type VARCHAR(20) NOT NULL DEFAULT 'RSS';
CREATE INDEX idx_news_source_type ON news_articles(source_type);
```

### Response DTO 명세

**DartDisclosureResponse** (Java record)
```java
record DartDisclosureResponse(
    String rcpNo,         // 접수번호 (원문 링크 생성용)
    String reportName,    // 보고서명
    String submitterName, // 제출인명
    String receivedAt,    // 접수일 (yyyyMMdd)
    String corpName,      // 회사명
    String originalUrl,   // https://dart.fss.or.kr/dsaf001/main.do?rcpNo={rcpNo}
    boolean isImportant   // 중요 공시 여부
)
```

**SecFilingResponse** (Java record)
```java
record SecFilingResponse(
    String accessionNumber, // 접수번호 (하이픈 포함, e.g. "0001234567-24-000001")
    String form,            // 10-K, 10-Q, 8-K, 4 등
    String filingDate,      // 2024-03-15
    String description,     // primaryDocDescription
    String originalUrl,     // SEC 원문 링크
    boolean isImportant     // 8-K, Form 4는 true
)
```

**NewsArticleResponse 변경** (sourceType 필드 추가)
```java
record NewsArticleResponse(
    long id,
    String title,
    String originalUrl,
    String sourceName,
    String sourceType,    // "RSS" | "FINNHUB" (신규)
    LocalDateTime publishedAt,
    Sentiment sentiment,
    double sentimentScore
)
```

---

## 프론트엔드 화면 구성

### 종목 상세 페이지 (stock-detail-client.tsx) 변경사항

#### 국내 종목 (KRX/KOSDAQ)
```
[차트] [기업분석] [재무분석] [뉴스] [공시(DART)] [예측]
```
- "공시" 탭: DartDisclosureResponse 목록, 중요 공시 하이라이트(노란 배지)
- 공시 항목 클릭: DART 원문 새 탭 오픈

#### 해외 종목 (NASDAQ/NYSE/AMEX)
```
[차트] [기업분석] [뉴스] [공시(SEC)] [예측]
```
- "공시" 탭: SecFilingResponse 목록, Form 유형별 배지
- Filing 항목 클릭: SEC 원문 새 탭 오픈

#### 뉴스 섹션 변경사항
- Finnhub 수집 뉴스는 기존 "관련 뉴스" 카드에 통합 표시
- `sourceType === 'FINNHUB'` 뉴스에 "Finnhub" 소스 배지 표시 (별도 UI 분리 불필요)

---

## Finnhub API 스펙 요약

| 엔드포인트 | 설명 |
|-----------|------|
| `GET /v1/company-news` | 종목별 뉴스 조회 |
| Base URL | `https://finnhub.io/api` |
| Auth | `token` 쿼리 파라미터 |
| Rate Limit | 분당 60 req (무료) |

응답 샘플:
```json
[
  {
    "id": 123456,
    "headline": "Apple Reports Record Quarterly Revenue",
    "url": "https://...",
    "source": "Reuters",
    "datetime": 1710000000,
    "summary": "Apple Inc. reported...",
    "image": "https://...",
    "category": "company news"
  }
]
```
- `sentiment` 필드: 무료 티어 미지원 → `null` 처리 후 기존 감성 분석 적용

---

## SEC EDGAR API 스펙 요약

| 엔드포인트 | 설명 |
|-----------|------|
| `https://www.sec.gov/files/company_tickers.json` | 전체 티커↔CIK 매핑 JSON |
| `https://data.sec.gov/submissions/CIK{cik:010}.json` | 기업 제출 목록 |
| User-Agent 필수 | `"FinEasy contact@fineasy.app"` |

`company_tickers.json` 구조:
```json
{
  "0": {"cik_str": 320193, "ticker": "AAPL", "title": "Apple Inc."},
  "1": {"cik_str": 789019, "ticker": "MSFT", "title": "Microsoft Corp"}
}
```

CIK 10자리 패딩: `CIK0000320193.json` 형식

---

## DART 공시 API 스펙 요약

| 엔드포인트 | 설명 |
|-----------|------|
| `GET /list.json` | 공시 목록 조회 |
| Base URL | `https://opendart.fss.or.kr/api` |
| Auth | `crtfc_key` 쿼리 파라미터 |

요청 파라미터:
- `corp_code`: DART 기업 고유코드 (기존 `DartCorpCodeEntity.corpCode`)
- `bgn_de`: 시작일 (yyyyMMdd)
- `end_de`: 종료일 (yyyyMMdd)
- `pblntf_ty`: 공시유형 (선택, 생략 시 전체)
- `page_no`: 페이지 번호 (1부터)
- `page_count`: 페이지당 건수 (최대 100)

응답 필드: `rcept_no`, `corp_name`, `report_nm`, `rcept_dt`, `flr_nm`, `rm`(비고)
