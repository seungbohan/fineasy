# Phase 4 구현 계획서: 뉴스 강화 + 공시 기능

**작성일**: 2026-03-15
**기준 문서**: `docs/phase4-requirements.md`

---

## 구현 전략 요약

세 기능의 의존성을 분석한 결과, 다음 순서가 최적이다.

```
Phase 4-A: DB 스키마 확장 (공통 기반)
      ↓
Phase 4-B: DART 공시 (기존 DartApiClient 재사용, 구현 난이도 낮음)
      ↓
Phase 4-C: SEC EDGAR 공시 (CIK 매핑 배치 → Filing 조회)
      ↓
Phase 4-D: Finnhub 뉴스 수집 (스케줄러 + API Key 필요)
      ↓
Phase 4-E: 프론트엔드 통합 (공시 탭 + 뉴스 통합)
```

**Phase 4-B와 4-C는 병렬 구현 가능**. 4-D는 Finnhub API Key가 없으면 Mock으로 우선 구현.

---

## Phase 4-A: DB 스키마 확장 (공통 기반)

### 목표
뉴스 출처 구분(`source_type`)과 SEC CIK 매핑(`sec_cik`) 컬럼을 DB에 추가한다.
JPA DDL-auto를 사용하므로 엔티티 변경만으로 처리한다.

### 작업 목록

**A-1. `NewsArticleEntity` 수정**
- 파일: `backend/src/main/java/com/fineasy/entity/NewsArticleEntity.java`
- `sourceType` 필드 추가 (`VARCHAR(20)`, NOT NULL, DEFAULT `'RSS'`)
- 불변 생성자 패턴 유지: 기존 생성자에 `sourceType` 파라미터 추가, 기존 `NewsCollectorService` 호출부는 `"RSS"` 전달로 수정

**A-2. `StockEntity` 수정**
- 파일: `backend/src/main/java/com/fineasy/entity/StockEntity.java`
- `secCik` 필드 추가 (`VARCHAR(20)`, nullable)
- `updateSecCik(String secCik)` 변경 메서드 추가

**A-3. `NewsArticleResponse` DTO 수정**
- 파일: `backend/src/main/java/com/fineasy/dto/response/NewsArticleResponse.java`
- `sourceType` 필드 추가

**A-4. `NewsCollectorService` 수정**
- `collectFromRss()` 내 `NewsArticleEntity` 생성 시 `sourceType = "RSS"` 전달

### 완료 조건
- 애플리케이션 기동 시 DDL이 자동 적용되어 두 컬럼이 정상 추가됨
- 기존 `NewsCollectorService`가 `sourceType = "RSS"` 로 정상 저장됨

### 예상 리스크
- JPA DDL-auto가 `validate`로 설정된 운영 환경에서는 수동 migration 스크립트 필요

---

## Phase 4-B: DART 공시 기능

### 목표
국내 종목의 최신 공시 목록을 종목 상세 페이지에서 조회한다.

### 작업 목록

**B-1. `DartApiClient`에 공시 목록 조회 메서드 추가**
- 파일: `backend/src/main/java/com/fineasy/external/dart/DartApiClient.java`
- 신규 메서드: `fetchDisclosureList(String corpCode, String bgnDe, String endDe, String pblntfTy, int pageNo, int pageCount)`
- 경로: `/list.json`
- 상태 코드 `"000"` 확인 후 `list` 배열 반환, 그 외 `null`

**B-2. `DartDisclosureResponse` DTO 생성**
- 파일: `backend/src/main/java/com/fineasy/dto/response/DartDisclosureResponse.java`
- Java record:
  ```java
  record DartDisclosureResponse(
      String rcpNo,
      String reportName,
      String submitterName,
      String receivedAt,
      String corpName,
      String originalUrl,
      boolean isImportant
  )
  ```

**B-3. `DartDisclosureService` 생성**
- 파일: `backend/src/main/java/com/fineasy/external/dart/DartDisclosureService.java`
- `@ConditionalOnExpression("!'${dart.api.key:}'.isEmpty()")` 적용
- `getDisclosures(String stockCode, int page, int size)` 메서드
  - `DartCorpCodeRepository.findByStockCode(stockCode)` → `corpCode` 조회
  - `DartApiClient.fetchDisclosureList()` 호출
  - 중요 공시 판별: 보고서명 키워드 매칭 (`isImportant` 계산)
  - `RedisCacheHelper`로 30분 캐싱 (키: `dart:disclosure:{stockCode}:{page}`)

**B-4. `StockController`에 공시 엔드포인트 추가**
- 파일: `backend/src/main/java/com/fineasy/controller/StockController.java`
- `GET /api/v1/stocks/{stockCode}/disclosures`
  - `@Autowired(required = false) DartDisclosureService` 주입 (DART key 없을 때 대응)
  - DART key 없거나 국내 종목 아닌 경우 → 404 또는 빈 목록 반환

### 완료 조건
- `GET /api/v1/stocks/005930/disclosures` 호출 시 삼성전자 공시 목록 반환
- 두 번째 동일 요청 시 Redis 캐시에서 응답 (응답 시간 현저히 감소)
- DART key 미설정 환경에서도 애플리케이션 정상 기동

### 예상 리스크
- DART API의 `corp_code`는 종목코드와 다름 → 기존 `DartCorpCodeEntity` UNIQUE 제약이 있으므로 누락 종목 발생 가능
- DART API SSL 인증서 이슈가 이미 `InsecureTrustManagerFactory`로 우회 처리됨 (기존 코드 그대로 활용)

---

## Phase 4-C: SEC EDGAR 공시 기능

### 목표
해외 종목에 SEC Filing 목록을 제공한다. CIK 매핑을 선행해야 Filing 조회가 가능하다.

### 작업 목록

**C-1. `SecEdgarApiClient` 생성**
- 파일: `backend/src/main/java/com/fineasy/external/sec/SecEdgarApiClient.java`
- `WebClient` 빈 생성, baseUrl: `https://data.sec.gov`
- 기본 헤더: `User-Agent: FinEasy contact@fineasy.app`
- `downloadCompanyTickers()`: `https://www.sec.gov/files/company_tickers.json` 다운로드 (byte[] → String)
- `fetchSubmissions(String cik10)`: `https://data.sec.gov/submissions/CIK{cik10}.json` 조회

**C-2. `SecEdgarCikSyncService` 생성**
- 파일: `backend/src/main/java/com/fineasy/external/sec/SecEdgarCikSyncService.java`
- `syncCikMapping()` 메서드
  - `company_tickers.json` 1회 다운로드 → 파싱 (`ticker → cik_str` 맵 생성)
  - `StockRepository.findAllByMarketIn(List.of(NASDAQ, NYSE, AMEX))` 조회
  - 티커 매칭하여 `StockEntity.updateSecCik(cik10)` 업데이트
  - `@SchedulerLock(name = "syncSecCik")` 적용, 주 1회 실행 (`@Scheduled(cron = "0 0 3 * * MON")`)
  - `@ConditionalOnProperty(name = "data-provider.type", havingValue = "kis")` 적용

**C-3. `SecFilingResponse` DTO 생성**
- 파일: `backend/src/main/java/com/fineasy/dto/response/SecFilingResponse.java`
- Java record (FR-3-3 명세 참조)

**C-4. `SecEdgarFilingService` 생성**
- 파일: `backend/src/main/java/com/fineasy/external/sec/SecEdgarFilingService.java`
- `getFilings(String stockCode, int page, int size, String form)` 메서드
  - `StockRepository.findByStockCode(stockCode)` → `secCik` 조회
  - `secCik == null` 이면 `EntityNotFoundException` 또는 빈 목록 반환
  - `SecEdgarApiClient.fetchSubmissions(cik10)` 호출
  - `filings.recent` 배열에서 대상 form 유형 필터링 + 페이징 처리
  - 원문 URL 생성: `https://www.sec.gov/Archives/edgar/data/{cik}/{accNum}/{primaryDoc}`
  - Redis 캐싱: TTL 1시간, 키 `sec:filings:{stockCode}`

**C-5. `StockController`에 SEC 엔드포인트 추가**
- `GET /api/v1/stocks/{stockCode}/sec-filings`
  - 해외 종목 (`market = NASDAQ/NYSE/AMEX`) 에서만 동작
  - 국내 종목 접근 시 `400 Bad Request`

**C-6. `application.properties`에 SEC 설정 추가**
- `sec.edgar.user-agent=FinEasy contact@fineasy.app`
- `SecEdgarApiProperties` record 생성

### 완료 조건
- `SecEdgarCikSyncService.syncCikMapping()` 실행 후 AAPL의 `StockEntity.secCik = "0000320193"` 저장 확인
- `GET /api/v1/stocks/AAPL/sec-filings` 호출 시 최신 10-K, 10-Q, 8-K 목록 반환
- AMEX 포함 3개 해외 시장 Market enum 정상 처리

### 예상 리스크
- `company_tickers.json` 파일이 수백 KB 이며 구조가 변경될 수 있음
- SEC EDGAR `submissions` JSON의 `filings.recent`는 최근 1년치만 포함; 오래된 Filing은 `files` 배열 내 추가 요청 필요 (Phase 4에서는 최근 1년 이내만 표시)
- CIK가 없는 종목 (상장폐지, OTC 등)에 대한 예외 처리 필요

---

## Phase 4-D: Finnhub 뉴스 수집

### 목표
해외 종목별 최신 뉴스를 Finnhub API로 수집하여 기존 뉴스 테이블에 통합한다.

### 작업 목록

**D-1. `FinnhubApiProperties` 생성**
- 파일: `backend/src/main/java/com/fineasy/config/FinnhubApiProperties.java`
- `@ConfigurationProperties(prefix = "finnhub.api")` record
  - `key`, `baseUrl`

**D-2. `application.properties` 수정**
- `finnhub.api.key=${FINNHUB_API_KEY:}`
- `finnhub.api.base-url=https://finnhub.io/api`

**D-3. `FinnhubApiClient` 생성**
- 파일: `backend/src/main/java/com/fineasy/external/finnhub/FinnhubApiClient.java`
- `@ConditionalOnExpression("!'${finnhub.api.key:}'.isEmpty()")` 적용
- `fetchCompanyNews(String symbol, String from, String to)`: 날짜 형식 `yyyy-MM-dd`
- 응답: `List<FinnhubNewsItem>` (inner record)
  ```java
  record FinnhubNewsItem(long id, String headline, String url, String source, long datetime)
  ```
  - `summary` 필드는 법적 이유로 수집하지 않음

**D-4. `FinnhubNewsCollectorService` 생성**
- 파일: `backend/src/main/java/com/fineasy/external/finnhub/FinnhubNewsCollectorService.java`
- `collectNewsForStock(StockEntity stock)` 메서드
  - Finnhub API 호출 → `NewsArticleEntity` 변환 (sourceType = `"FINNHUB"`)
  - `newsArticleRepository.findExistingUrls()` 로 중복 체크
  - `news_stock_tags`에 해당 종목 태깅
  - **`summary` 저장 안 함** (저작권 회피), `content = null`

**D-5. `FinnhubNewsScheduler` 생성**
- 파일: `backend/src/main/java/com/fineasy/scheduler/FinnhubNewsScheduler.java`
- `@ConditionalOnProperty(name = "finnhub.news.enabled", havingValue = "true")`
- `@ConditionalOnExpression("!'${finnhub.api.key:}'.isEmpty()")`
- 수집 대상 선정:
  ```
  1. StockRepository.findWatchlistStocks() → 해외 종목 필터
  2. 인기 상위 50 해외 종목 (MarketService.getTopOverseasStocks(50))
  ```
- Rate Limit 준수: 종목별 API 호출 후 `Thread.sleep(1100)` (분당 54회 한도로 여유 확보)
- `@Scheduled(fixedRate = 3600000)` (1시간마다)
- `@SchedulerLock(name = "collectFinnhubNews", lockAtLeastFor = "PT10M", lockAtMostFor = "PT50M")`

**D-6. `NewsSentimentService` 연동 확인**
- Finnhub 수집 뉴스가 sentiment = null 상태로 저장된 후
- 기존 `NewsCollectionScheduler.retryUnanalyzedArticles()`가 미분석 뉴스를 배치 재처리하는지 확인
- Finnhub 뉴스도 동일 흐름 적용 (headline 기반 keyword 감성 분석)

### 완료 조건
- Finnhub API Key 설정 후 `FinnhubNewsScheduler` 정상 실행
- AAPL 관련 뉴스가 `news_articles`에 `source_type = 'FINNHUB'`로 저장
- `news_stock_tags`에 AAPL 종목 태깅 확인
- `GET /api/v1/stocks/AAPL/news` 응답에 Finnhub 뉴스 포함 확인
- Finnhub API Key 미설정 시 `FinnhubApiClient` 빈 미생성, 스케줄러 비활성화 상태에서 애플리케이션 정상 기동

### 예상 리스크
- Finnhub 무료 티어 일일 호출 한도: API 문서에 명시 없으나 과도한 호출 시 `429` 응답 가능
- `Thread.sleep()` 사용으로 스케줄러 스레드 블로킹 → 수집 종목 수가 많으면 1시간 이내 완료 불보장. 수집 대상을 50종목 이내로 제한하면 50초 이내 완료

---

## Phase 4-E: 프론트엔드 통합

### 목표
공시 탭 추가와 뉴스 sourceType 구분 UI를 구현한다.

### 작업 목록

**E-1. `types/index.ts`에 신규 타입 추가**
```typescript
export interface DartDisclosure {
  rcpNo: string;
  reportName: string;
  submitterName: string;
  receivedAt: string;   // "20240315" 형식
  corpName: string;
  originalUrl: string;
  isImportant: boolean;
}

export interface SecFiling {
  accessionNumber: string;
  form: string;          // "10-K", "10-Q", "8-K", "4"
  filingDate: string;    // "2024-03-15"
  description: string;
  originalUrl: string;
  isImportant: boolean;
}
```
- `NewsArticle` 타입에 `sourceType?: 'RSS' | 'FINNHUB'` 필드 추가

**E-2. `hooks/use-disclosures.ts` 생성**
```typescript
export function useDartDisclosures(stockCode: string, market: string)
export function useSecFilings(stockCode: string, market: string, form?: string)
```
- 국내 종목 (KRX/KOSDAQ): `useDartDisclosures` 활성화
- 해외 종목: `useSecFilings` 활성화
- staleTime: 30분

**E-3. `components/stocks/disclosure-card.tsx` 신규 생성**
- 공시 목록 카드 컴포넌트
- 국내/해외 Props 분기 (`type: 'dart' | 'sec'`)
- 중요 공시 하이라이트: 노란 배지 (`isImportant=true` 시)
- Form 유형 배지: 10-K(파랑), 10-Q(파랑 아웃라인), 8-K(주황), 4(회색)
- 날짜 포맷: DART는 `"20240315"` → `"2024.03.15"`, SEC는 ISO 형식 그대로

**E-4. `stock-detail-client.tsx` 수정**
- 탭 네비게이션 추가 (`tabs.tsx` 컴포넌트 재사용)
  - 국내 종목 탭: `뉴스` / `공시`
  - 해외 종목 탭: `뉴스` / `SEC 공시`
- 탭 상태는 `useState`로 관리 (URL param 불필요)
- 뉴스 카드에 `sourceType === 'FINNHUB'` 시 `Finnhub` 소스 배지 표시

**E-5. `hooks/use-news.ts` 수정**
- `toNewsArticle()` 변환 함수에 `sourceType` 매핑 추가

### 완료 조건
- 삼성전자(KRX) 상세 페이지에 "공시" 탭 표시 및 DART 공시 목록 정상 렌더링
- 애플(NASDAQ) 상세 페이지에 "SEC 공시" 탭 표시 및 Filing 목록 정상 렌더링
- 공시 항목 클릭 시 원문 링크 새 탭 오픈
- 중요 공시에 노란 하이라이트 배지 표시
- Finnhub 수집 뉴스에 소스명 표시

---

## 검증 계획

### A 단계 검증
- [ ] DB 컬럼 추가 확인 (H2 콘솔 또는 PostgreSQL `\d news_articles`, `\d stocks`)
- [ ] 기존 RSS 뉴스 수집 정상 동작 (`source_type = 'RSS'` 저장)

### B 단계 검증
- [ ] `GET /api/v1/stocks/005930/disclosures` → 공시 목록 반환
- [ ] DART API key 미설정 시 빈 목록 또는 적절한 에러 응답
- [ ] Redis 캐시 키 `dart:disclosure:005930:0` 존재 확인

### C 단계 검증
- [ ] `syncCikMapping()` 실행 후 AAPL `sec_cik = '0000320193'` 저장
- [ ] `GET /api/v1/stocks/AAPL/sec-filings` → 10-K, 10-Q, 8-K 포함 목록 반환
- [ ] 국내 종목에서 `GET /api/v1/stocks/005930/sec-filings` → 400 응답

### D 단계 검증
- [ ] Finnhub 수집 후 `news_articles` 에 `source_type = 'FINNHUB'` 뉴스 확인
- [ ] `news_stock_tags` 테이블에 해당 뉴스-종목 연결 확인
- [ ] `GET /api/v1/stocks/AAPL/news` 응답에 Finnhub 뉴스 포함

### E 단계 검증
- [ ] 국내 종목 상세: "공시" 탭 클릭 시 DART 목록 표시
- [ ] 해외 종목 상세: "SEC 공시" 탭 클릭 시 Filing 목록 표시
- [ ] 공시 항목 클릭 시 새 탭에서 DART/SEC 원문 오픈
- [ ] 중요 공시 하이라이트 정상 표시
- [ ] 모바일 반응형 레이아웃 확인

---

## 구현 우선순위 결정 근거

| 기능 | 비즈니스 임팩트 | 구현 난이도 | 우선순위 |
|------|--------------|------------|---------|
| DART 공시 (4-B) | 높음 (국내 사용자 대상) | 낮음 (기존 DartApiClient 재사용) | 1순위 |
| SEC 공시 (4-C) | 중간 (해외 종목 보유자 대상) | 중간 (CIK 매핑 배치 필요) | 2순위 |
| Finnhub 뉴스 (4-D) | 높음 (실시간성 개선) | 중간 (Rate Limit 관리) | 3순위 |
| 프론트 통합 (4-E) | 의존성 없이 병렬 가능 | 낮음 | 4-B/C와 병렬 |

---

## 신규 파일 목록 (백엔드)

```
backend/src/main/java/com/fineasy/
├── config/
│   └── FinnhubApiProperties.java              (신규)
├── dto/response/
│   ├── DartDisclosureResponse.java            (신규)
│   └── SecFilingResponse.java                 (신규)
├── external/
│   ├── dart/
│   │   └── DartDisclosureService.java         (신규)
│   ├── finnhub/
│   │   ├── FinnhubApiClient.java              (신규)
│   │   └── FinnhubNewsCollectorService.java   (신규)
│   └── sec/
│       ├── SecEdgarApiClient.java             (신규)
│       ├── SecEdgarCikSyncService.java        (신규)
│       └── SecEdgarFilingService.java         (신규)
└── scheduler/
    └── FinnhubNewsScheduler.java              (신규)
```

## 수정 파일 목록 (백엔드)

```
backend/src/main/java/com/fineasy/
├── entity/
│   ├── NewsArticleEntity.java                 (sourceType 추가)
│   └── StockEntity.java                       (secCik 추가)
├── dto/response/
│   └── NewsArticleResponse.java               (sourceType 추가)
├── service/
│   └── NewsCollectorService.java              (sourceType = "RSS" 전달)
├── controller/
│   └── StockController.java                   (공시 엔드포인트 2개 추가)
└── resources/
    └── application.properties                 (Finnhub/SEC 설정 추가)
```

## 신규 파일 목록 (프론트엔드)

```
frontend/src/
├── components/stocks/
│   └── disclosure-card.tsx                    (신규)
└── hooks/
    └── use-disclosures.ts                     (신규)
```

## 수정 파일 목록 (프론트엔드)

```
frontend/src/
├── types/index.ts                             (DartDisclosure, SecFiling 추가, NewsArticle 수정)
├── hooks/
│   └── use-news.ts                            (sourceType 매핑)
└── app/stocks/[stockCode]/
    └── stock-detail-client.tsx                (공시 탭 추가)
```
