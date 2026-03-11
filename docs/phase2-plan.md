# Phase 2 구현 계획서: AI 기능

**프로젝트**: FinEasy - 초보자를 위한 금융 웹 플랫폼
**작성일**: 2026-02-21
**작성자**: @senior-requirements-planner
**버전**: v1.0
**전제**: Phase 1 완료 기준 (프론트엔드 Next.js 14 + 백엔드 Spring Boot 3.2 구조 확립)

---

## 목차

1. [현재 코드베이스 분석 결과](#1-현재-코드베이스-분석-결과)
2. [요구사항 정의서](#2-요구사항-정의서)
3. [백엔드 구현 계획](#3-백엔드-구현-계획)
4. [프론트엔드 구현 계획](#4-프론트엔드-구현-계획)
5. [구현 우선순위 및 작업 순서](#5-구현-우선순위-및-작업-순서)
6. [리스크 및 완화 방안](#6-리스크-및-완화-방안)
7. [미결 사항](#7-미결-사항)

---

## 1. 현재 코드베이스 분석 결과

### 1.1 백엔드 현황

**패키지 구조**: `com.fineasy`
```
com.fineasy/
├── config/          - 설정 (Jackson, KIS API, Redis 등)
├── controller/      - REST 컨트롤러 (분석, 뉴스, 주식 등 모두 존재)
├── dto/             - 요청/응답 DTO (record 형태)
├── entity/          - JPA 엔티티
├── exception/       - 예외 처리 (GlobalExceptionHandler 포함)
├── external/        - 외부 연동 (KIS API 어댑터, Mock 구현체)
├── repository/      - Spring Data JPA 리포지토리
├── security/        - JWT 인증
└── service/         - 비즈니스 로직 (AiAnalysisProvider 인터페이스 포함)
```

**Phase 2와 관련된 핵심 기존 코드**:

| 파일 | 상태 | Phase 2에서 할 일 |
|------|------|-----------------|
| `AiAnalysisProvider.java` | 인터페이스 정의 완료 | 실제 OpenAI 구현체 추가 |
| `MockAiAnalysisProvider.java` | Mock 더미 데이터 반환 | OpenAI 구현체로 교체 (Mock은 유지) |
| `AnalysisService.java` | 조회 로직만 존재 | 캐싱 로직, 기술적 지표 계산 연동 추가 |
| `AnalysisController.java` | 4개 엔드포인트 완성 | `period` 파라미터 추가 필요 |
| `StockAnalysisReportEntity.java` | 엔티티 완성 | 변경 없음 (스키마 호환) |
| `StockPredictionEntity.java` | 엔티티 완성 | 변경 없음 |
| `NewsArticleEntity.java` | 엔티티 완성 | 감성 분석 결과 저장 구조 이미 있음 |
| `StockPriceEntity.java` | OHLCV 저장 완성 | 변경 없음 (데이터 조회에 활용) |
| `MacroIndicatorEntity.java` | 엔티티 완성 | 변경 없음 (수집 스케줄러 추가) |
| `StockPriceRepository.java` | 기간별 조회 완성 | 변경 없음 (기술적 지표 계산에 활용) |
| `application.yml` | Redis 의존성 포함, dev에서 Redis 제외 | dev 프로파일 Redis 설정 추가 필요 |

**핵심 발견 사항**:
- `build.gradle`에 Redis, Resilience4j, WebFlux 의존성이 이미 선언되어 있음
- `application-dev.yml`에서 Redis AutoConfiguration을 제외하고 있어, Phase 2에서 Redis 활성화 시 dev 프로파일 수정 필요
- OpenAI 관련 의존성이 없음 (추가 필요)
- 뉴스 수집 스케줄러가 없음 (추가 필요)
- 기술적 지표 계산 로직이 없음 (신규 추가)

### 1.2 프론트엔드 현황

**관련 파일**:

| 파일 | 상태 | Phase 2에서 할 일 |
|------|------|-----------------|
| `app/analysis/page.tsx` | UI 완성 (Mock 데이터 연동) | 실제 API 연동 (이미 apiClient 사용 중) |
| `hooks/use-analysis.ts` | `useAnalysisReport`, `usePrediction` 완성 | `period` 파라미터 지원 추가 |
| `mocks/analysis.ts` | Mock 데이터 정의됨 | 실제 API 전환 후 불필요해짐 |
| `types/index.ts` | `AnalysisReport`, `Prediction` 타입 완성 | `TechnicalAnalysis` 타입 추가 필요 |
| `components/shared/sentiment-badge.tsx` | 감성 배지 컴포넌트 있음 | 변경 없음 |

**핵심 발견 사항**:
- `apiClient`가 이미 구현되어 실제 API 호출 준비 완료
- `staleTime: 6 * 60 * 60 * 1000` (6시간)이 이미 설정되어 Redis 캐싱 전략과 일치
- 분석 페이지 UI가 이미 완성 상태 (기술적 지표 카드 포함)
- 주가 예측 기간 선택 UI가 없음 (1D/1W 전환 기능 추가 필요)
- 기술적 지표 시계열 차트 오버레이(RSI, MACD, 볼린저밴드)가 차트 컴포넌트에 없음

---

## 2. 요구사항 정의서

### 2.1 배경 및 목적

Phase 1에서 구축한 주가 조회, 뉴스 피드, 용어 사전, 관심 종목 기능 위에 AI 분석 기능을 추가한다.
현재 `MockAiAnalysisProvider`가 더미 데이터를 반환하는 골격(skeleton)이 있으며, Phase 2의 목표는 이 골격을 실제 OpenAI API와 연동된 구현체로 채우는 것이다.

### 2.2 기능 요구사항

#### FR-P2-1: 기술적 지표 계산 서비스

- **FR-P2-1-1**: DB에 저장된 `StockPriceEntity` 데이터를 기반으로 기술적 지표를 계산한다.
  - 이동평균(MA): 5일, 20일, 60일, 120일 (단순이동평균, SMA)
  - RSI: 14일 기준 (Wilder's smoothing 방식)
  - MACD: 12일 EMA - 26일 EMA, 시그널선 9일 EMA, 히스토그램(MACD - Signal)
  - 볼린저 밴드: 20일 SMA 중심선, 상단(+2 표준편차), 하단(-2 표준편차)
  - 거래량 분석: 20일 평균 거래량 대비 현재 거래량 비율

- **FR-P2-1-2**: 계산에 필요한 최소 데이터 조건
  - RSI 계산: 최소 15일 데이터 필요 (14일 + 1일 초기화)
  - MACD 계산: 최소 27일 데이터 필요 (26일 EMA + 1일)
  - 볼린저 밴드: 최소 20일 데이터 필요
  - 데이터 부족 시: 계산 가능한 지표만 반환하고, 부족한 지표는 null 반환 (에러 아님)

- **FR-P2-1-3**: 계산 결과의 신호(Signal) 판단 기준
  - RSI > 70: OVERBOUGHT, RSI < 30: OVERSOLD, 그 외: NEUTRAL
  - MACD > Signal: BULLISH, MACD < Signal: BEARISH, 동일: NEUTRAL
  - 볼린저밴드 위치: 상단 밴드 이상: UPPER(BEARISH), 하단 밴드 이하: LOWER(BULLISH), 그 외: MIDDLE(NEUTRAL)

#### FR-P2-2: OpenAI API 연동 및 AI 분석 리포트

- **FR-P2-2-1**: `AiAnalysisProvider` 인터페이스의 OpenAI 구현체(`OpenAiAnalysisProvider`)를 신규 작성한다.

- **FR-P2-2-2**: AI 분석 리포트 생성 입력 데이터
  - 최근 60일 OHLCV 데이터 (DB에서 조회)
  - 계산된 기술적 지표 (RSI, MACD, 볼린저밴드, MA, 거래량)
  - 종목명 및 종목 코드

- **FR-P2-2-3**: AI 분석 리포트 출력 구조 (기존 `AnalysisReportResponse` 유지)
  - `summary`: 한 줄 요약 (핵심 신호, 최대 100자)
  - `description`: 현재 상황 설명 (3~5문장, 초보자 친화적 언어)
  - `keyPoints`: 주목할 점 3가지 (문자열 리스트)
  - `technicalSignals`: RSI, MACD, 볼린저밴드 신호 맵
  - `disclaimer`: 고정 면책 문구

- **FR-P2-2-4**: 캐싱 전략 (동일 종목 동일 날짜 재생성 방지)
  - 1단계: Redis 캐시 확인 (TTL 6시간, Key: `analysis:report:{stockCode}:{date}`)
  - 2단계: Redis Miss 시 DB 확인 (`StockAnalysisReportRepository.findByStockIdAndReportDate`)
  - 3단계: DB Miss 시 OpenAI API 호출 후 DB 저장 + Redis 캐시 저장
  - 동일 날짜 재요청은 반드시 캐시 또는 DB에서 반환 (API 중복 호출 금지)

- **FR-P2-2-5**: OpenAI API 장애 시 Fallback
  - Resilience4j Circuit Breaker 적용
  - 장애 시: DB에 저장된 가장 최근 리포트 반환 + 생성 일시 표기
  - DB에도 없는 경우: 적절한 에러 응답 반환 (500 대신 503 + 메시지)

#### FR-P2-3: 뉴스 감성 분석 파이프라인

- **FR-P2-3-1**: 뉴스 수집 스케줄러
  - 수집 주기: 1시간 간격 (`@Scheduled`)
  - 수집 대상: RSS 피드 (네이버 금융 뉴스, Yahoo Finance RSS)
  - 중복 방지: `originalUrl` 유니크 제약으로 이미 처리됨 (upsert 불필요, insert 시 중복 무시)

- **FR-P2-3-2**: 감성 분석 적용
  - 수집된 신규 뉴스에 대해 OpenAI API로 감성 분류 (POSITIVE/NEGATIVE/NEUTRAL)
  - 신뢰도 점수(0.0 ~ 1.0)와 함께 저장
  - `NewsArticleEntity`의 `sentiment`, `sentimentScore` 필드 업데이트

- **FR-P2-3-3**: 종목 자동 태깅
  - 뉴스 제목에서 DB에 등록된 종목명/종목코드 검색
  - 매칭 시 `news_stock_tags` 조인 테이블에 연관 저장

- **FR-P2-3-4**: 감성 분석도 캐싱 대상에서 제외 (실시간성 우선)
  - 단, OpenAI API 배치 호출 최적화 (뉴스 최대 5건을 하나의 API 호출로 처리)

#### FR-P2-4: 주가 방향성 예측 API

- **FR-P2-4-1**: 예측 입력 데이터
  - 최근 30일 OHLCV 데이터
  - 계산된 기술적 지표 (RSI, MACD, 볼린저밴드)
  - 최근 7일 뉴스 감성 점수 평균 (종목 관련 뉴스)
  - 최근 거시경제 지표 최신값 (KR_BASE_RATE, US_FED_RATE, USD_KRW 환율)

- **FR-P2-4-2**: 예측 기간 선택 지원
  - 현재 `AnalysisController.getPrediction()`은 기간을 "1D"로 하드코딩
  - `?period=1D|1W` 쿼리 파라미터 추가
  - 기간에 따라 프롬프트 전략 차별화

- **FR-P2-4-3**: 예측 출력 구조 (기존 `PredictionResponse` 유지)
  - `direction`: UP/DOWN/SIDEWAYS
  - `confidence`: 0~100 정수
  - `reasons`: 근거 3가지 (초보자 친화적 언어)
  - `disclaimer`: 필수 면책 문구

- **FR-P2-4-4**: 예측 캐싱 전략 (리포트와 동일 방식)
  - Redis TTL 6시간, Key: `analysis:prediction:{stockCode}:{period}:{date}`

#### FR-P2-5: Redis 캐싱 적용

- **FR-P2-5-1**: 캐싱 대상 및 TTL
  - AI 분석 리포트: TTL 6시간
  - AI 주가 예측: TTL 6시간
  - 기술적 지표: TTL 1시간 (더 자주 갱신)

- **FR-P2-5-2**: dev 환경에서 Redis 활성화
  - 현재 `application-dev.yml`에서 Redis AutoConfiguration이 제외되어 있음
  - Phase 2 시작 전 Redis 연결 설정 추가 필요 (로컬 Redis 또는 Docker)

- **FR-P2-5-3**: Redis 장애 시 Fallback
  - Redis 접근 실패 시 캐시를 건너뛰고 DB/OpenAI 직접 조회 (최선 노력 방식)
  - 로그 경고만 기록, 사용자에게 에러 노출 금지

### 2.3 비기능 요구사항 (Phase 2 특화)

| 항목 | 목표값 | 근거 |
|------|-------|------|
| AI 분석 응답 시간 (캐시 HIT) | 100ms 이내 | Redis 캐시에서 반환 |
| AI 분석 응답 시간 (캐시 MISS) | 10초 이내 | OpenAI API 호출 + 스트리밍 |
| 예측 응답 시간 (캐시 MISS) | 10초 이내 | 동일 |
| OpenAI API 비용 절감 | 동일 종목 동일 날짜 1회만 호출 | DB+Redis 이중 캐싱 |
| 뉴스 감성 분석 지연 | 수집 후 5분 이내 | 배치 처리 |

### 2.4 가정사항

- OpenAI GPT-4o API 키가 환경변수로 제공된다.
- Phase 1의 주가 데이터 수집이 정상 동작하여 DB에 충분한 `StockPriceEntity` 데이터가 있다.
- 뉴스 RSS 피드 URL은 별도 확정 후 설정 파일에 등록된다.
- Redis 서버가 로컬 개발 환경에서 실행 가능하다 (Docker 사용 권장).

### 2.5 미결 사항

- [ ] OpenAI GPT-4o 프롬프트 최종 버전 (초보자 친화도, 한국어 품질 검토 필요)
- [ ] 뉴스 RSS 피드 URL 목록 확정 (네이버 금융 RSS 정책 확인 필요)
- [ ] 뉴스 감성 분석 배치 크기 결정 (1건씩 vs 최대 5건 배치)
- [ ] 거시경제 지표 수집 스케줄러 포함 여부 (Phase 2 또는 Phase 3)
- [ ] AI 응답 스트리밍 적용 여부 (SSE/WebSocket 구현 복잡도 고려)

---

## 3. 백엔드 구현 계획

### 3.1 신규 추가 파일 목록

```
com.fineasy/
├── config/
│   └── OpenAiConfig.java                    [신규] OpenAI WebClient 설정
│   └── RedisConfig.java                     [신규] Redis 직렬화 설정
│   └── CacheConfig.java                     [신규] Spring Cache 설정
├── external/
│   └── openai/
│       ├── OpenAiClient.java                [신규] OpenAI API WebClient 래퍼
│       ├── OpenAiAnalysisProvider.java      [신규] AiAnalysisProvider 실제 구현체
│       └── OpenAiPromptBuilder.java         [신규] 프롬프트 생성 유틸리티
├── service/
│   ├── TechnicalIndicatorService.java       [신규] 기술적 지표 계산 서비스
│   ├── NewsCollectorService.java            [신규] 뉴스 수집 스케줄러
│   └── NewsSentimentService.java            [신규] 뉴스 감성 분석 서비스
└── scheduler/
    ├── NewsCollectionScheduler.java         [신규] 뉴스 수집 스케줄 작업
    └── MacroDataCollectionScheduler.java    [신규] 거시경제 수집 스케줄 작업 (선택)
```

### 3.2 기존 파일 수정 목록

| 파일 | 수정 내용 |
|------|---------|
| `AnalysisService.java` | Redis 캐시 조회/저장 로직 추가, TechnicalIndicatorService 의존성 주입 |
| `AnalysisController.java` | `/prediction` 엔드포인트에 `period` 쿼리 파라미터 추가 |
| `application.yml` | OpenAI API 설정 키 추가 (`openai.api-key`, `openai.model`) |
| `application-dev.yml` | Redis 설정 활성화 (기존 exclude 제거), Redis 로컬 연결 설정 추가 |
| `build.gradle` | OpenAI 관련 의존성 없음 (WebFlux로 직접 호출하므로 추가 불필요) |

### Phase A: 기반 설정 (선행 작업, 약 0.5주)

#### A-1: Redis 활성화 및 설정

**목표**: dev 환경에서 Redis를 활성화하고 캐시 직렬화 설정을 완료한다.

**작업 내용**:
1. `application-dev.yml` 수정
   - `spring.autoconfigure.exclude`에서 Redis 관련 항목 제거
   - `spring.data.redis.host: localhost`, `spring.data.redis.port: 6379` 추가
2. `RedisConfig.java` 신규 작성
   - `RedisTemplate<String, String>` 빈 등록 (JSON 직렬화)
   - Key Serializer: `StringRedisSerializer`
   - Value Serializer: `GenericJackson2JsonRedisSerializer`
3. `CacheConfig.java` 신규 작성
   - `@EnableCaching` 활성화
   - TTL별 `CacheConfiguration` 정의: 6시간(AI 분석/예측), 1시간(기술적 지표)

**영향 파일**: `application-dev.yml`, `RedisConfig.java`, `CacheConfig.java`

**완료 조건**: 로컬 Redis 연결 후 키 저장/조회 단위 테스트 통과

**예상 리스크**: dev 환경에 Redis가 없는 경우 Docker Compose 파일 제공 필요

---

#### A-2: OpenAI 설정 추가

**목표**: OpenAI API 호출을 위한 WebClient 설정과 환경변수 연동을 완료한다.

**작업 내용**:
1. `application.yml` 수정: OpenAI 설정 추가
   ```yaml
   openai:
     api-key: ${OPENAI_API_KEY}
     model: gpt-4o
     base-url: https://api.openai.com/v1
     timeout: 30s
   ```
2. `OpenAiConfig.java` 신규 작성
   - `WebClient` 빈 등록 (baseUrl, timeout, Authorization 헤더)
   - `@ConfigurationProperties`로 설정값 바인딩

**영향 파일**: `application.yml`, `OpenAiConfig.java`

**완료 조건**: OpenAI API 키 없이도 빈 생성 성공 (환경변수 없으면 더미 키로 동작, 실제 호출은 실패)

---

### Phase B: 핵심 AI 기능 구현 (주차 7~8, 약 2주)

#### B-1: 기술적 지표 계산 서비스

**목표**: DB에 저장된 주가 데이터로 RSI, MACD, 볼린저밴드, MA를 계산하는 서비스를 구현한다.

**작업 내용**:
1. `TechnicalIndicatorService.java` 신규 작성
   - 의존성: `StockPriceRepository`, `StockRepository`
   - 메서드 목록:
     ```java
     // 종목 코드로 최근 N일 주가 조회 후 지표 계산
     TechnicalAnalysisResponse calculate(String stockCode)

     // 내부 계산 메서드
     private double calculateRsi(List<BigDecimal> closes, int period)
     private double[] calculateEma(List<BigDecimal> values, int period)
     private MacdResult calculateMacd(List<BigDecimal> closes)
     private BollingerResult calculateBollingerBands(List<BigDecimal> closes)
     private double calculateSma(List<BigDecimal> values, int period)
     ```
   - 데이터 부족 시 처리: 계산 가능한 지표만 반환, null 허용
   - RSI 계산 공식: Wilder's Smoothing Method
     ```
     초기 RS = 14일간 평균상승폭 / 14일간 평균하락폭
     이후 RS = (이전 평균상승폭 × 13 + 현재상승폭) / (이전 평균하락폭 × 13 + 현재하락폭)
     RSI = 100 - (100 / (1 + RS))
     ```
   - EMA 계산: Exponential 가중치 방식
     ```
     k = 2 / (기간 + 1)
     EMA_t = 가격_t × k + EMA_{t-1} × (1 - k)
     ```

2. `AnalysisService.java` 수정
   - `TechnicalIndicatorService` 의존성 추가
   - `getTechnicalAnalysis()` 메서드가 Mock 대신 실제 계산 서비스 호출하도록 변경
   - Redis 캐시 적용 (`@Cacheable(cacheNames = "technical", key = "#stockCode")`)

**영향 파일**: `TechnicalIndicatorService.java` (신규), `AnalysisService.java` (수정)

**완료 조건**:
- 삼성전자(005930) 60일 데이터로 RSI 계산 결과가 0~100 범위 내
- MACD 히스토그램 = MACD값 - 시그널값 검증
- 볼린저 밴드 상단 > 중심선 > 하단 검증
- JUnit 5 단위 테스트 작성 (계산 정확도 검증)

---

#### B-2: OpenAI 분석 리포트 구현체

**목표**: `AiAnalysisProvider` 인터페이스의 실제 OpenAI 구현체를 작성한다.

**작업 내용**:
1. `OpenAiClient.java` 신규 작성
   - WebClient 기반 OpenAI Chat Completions API 호출
   - 요청 형식: `POST /v1/chat/completions`
   - 응답 파싱: `choices[0].message.content` 추출
   - Timeout 설정: 30초
   - 재시도: Resilience4j Retry 최대 2회

2. `OpenAiPromptBuilder.java` 신규 작성
   - 분석 리포트용 시스템/사용자 프롬프트 빌더
   - 예측용 시스템/사용자 프롬프트 빌더
   - 출력 형식을 JSON으로 요청하는 프롬프트 템플릿

   **분석 리포트 프롬프트 전략**:
   ```
   System: "당신은 주식 분석 전문가입니다. 초보 투자자가 이해할 수 있는 쉬운 언어로 분석하세요.
           응답은 반드시 JSON 형식으로 제공하세요. 투자 권유 문구는 절대 포함하지 마세요."
   User:   "[종목명] 분석 데이터:
           - 최근 60일 주가 요약 (시작가, 종가, 최고/최저)
           - RSI: {value} ({signal})
           - MACD: {value}, Signal: {signal}, Histogram: {histogram}
           - 볼린저밴드: 상단 {upper}, 중심 {middle}, 하단 {lower}, 현재 위치: {position}
           - 거래량: 현재 {current}, 평균 {average}, 비율 {ratio}배

           다음 JSON 구조로 분석을 제공하세요:
           { summary, description, keyPoints: [3개 항목] }"
   ```

3. `OpenAiAnalysisProvider.java` 신규 작성
   - `AiAnalysisProvider` 인터페이스 구현
   - `@Primary` 어노테이션 (MockAiAnalysisProvider 보다 우선 등록)
   - `@ConditionalOnProperty(name = "openai.api-key")` 또는 환경변수 존재 시 활성화
   - `generateReport()`: 기술적 지표 + OpenAI 호출 + `AnalysisReportResponse` 반환
   - `generatePrediction()`: 기술적 지표 + 뉴스 감성 + 거시경제 + OpenAI 호출
   - OpenAI 응답 JSON 파싱 후 `AnalysisReportResponse` 매핑

4. `AnalysisService.java` 수정 (캐싱 추가)
   - `getReport()` 메서드 수정:
     ```
     1. Redis 캐시 조회 (Key: "analysis:report:{stockCode}:{오늘날짜}")
     2. Redis Miss → DB 조회 (findByStockIdAndReportDate)
     3. DB Miss → OpenAI 호출 → DB 저장 → Redis 저장
     4. Redis/DB Hit → 즉시 반환
     ```
   - `getPrediction()` 메서드 수정: 동일 패턴

**영향 파일**: `OpenAiClient.java` (신규), `OpenAiPromptBuilder.java` (신규), `OpenAiAnalysisProvider.java` (신규), `AnalysisService.java` (수정)

**완료 조건**:
- OpenAI API 키 환경변수 있을 때 실제 리포트 생성 성공
- 같은 날 같은 종목 두 번 요청 시 DB에서 반환 (OpenAI 두 번째 호출 없음)
- Redis 캐시에 분석 결과가 저장됨 (redis-cli로 확인)
- OpenAI API 장애 시 DB의 마지막 리포트 반환

---

### Phase C: 뉴스 감성 분석 파이프라인 (주차 9, 약 1주)

#### C-1: 뉴스 수집 스케줄러

**목표**: RSS 피드에서 1시간마다 금융 뉴스를 수집하고 DB에 저장한다.

**작업 내용**:
1. `NewsCollectionScheduler.java` 신규 작성
   - `@EnableScheduling` 활성화 (`FinEasyApplication.java` 또는 별도 설정 클래스)
   - `@Scheduled(fixedRate = 3600000)` - 1시간 간격
   - RSS XML 파싱: Spring WebFlux WebClient + `Jaxb` 또는 직접 XML 파싱
   - 수집 대상 RSS URL (환경변수 또는 설정 파일로 관리)
   - 기사 저장: `originalUrl` 중복 시 INSERT 무시 (`ON CONFLICT DO NOTHING` 또는 try-catch)

2. `NewsCollectorService.java` 신규 작성
   - RSS XML 파싱 로직
   - `NewsArticleEntity` 생성 (title, originalUrl, sourceName, publishedAt)
   - `content`는 현재 미수집 (제목+링크만, 저작권 준수)
   - 신규 기사 식별 후 감성 분석 서비스 호출 연결

**영향 파일**: `NewsCollectionScheduler.java` (신규), `NewsCollectorService.java` (신규)

**완료 조건**: 1시간마다 스케줄러 실행 로그 확인, 신규 기사 DB 저장 확인

---

#### C-2: 뉴스 감성 분석 서비스

**목표**: 수집된 뉴스 제목을 OpenAI API로 감성 분류한다.

**작업 내용**:
1. `NewsSentimentService.java` 신규 작성
   - 배치 처리: 미분류 뉴스 최대 5건을 하나의 OpenAI API 호출로 처리
   - 프롬프트 전략:
     ```
     System: "다음 뉴스 제목들의 주식 투자 관점 감성을 분류하세요.
             각 항목에 대해 POSITIVE/NEGATIVE/NEUTRAL과 신뢰도(0.0~1.0)를 JSON 배열로 반환하세요."
     User:   "1. [뉴스제목1]\n2. [뉴스제목2]\n..."
     ```
   - 응답 파싱 후 `NewsArticleEntity`의 `sentiment`, `sentimentScore` 업데이트

2. 종목 자동 태깅 로직
   - `NewsArticleEntity.title`에서 `StockRepository.findAll()`의 종목명/종목코드 검색
   - 매칭 시 `taggedStocks` 관계에 해당 `StockEntity` 추가
   - 성능 고려: 종목 목록 캐싱 (메모리 내 Map 유지)

**영향 파일**: `NewsSentimentService.java` (신규), `NewsArticleEntity.java` (수정: 감성 업데이트 메서드 추가)

**완료 조건**: 수집된 뉴스의 감성 분류 결과가 DB에 저장, 종목 태깅 확인

---

### Phase D: 주가 예측 및 마무리 (주차 10, 약 1주)

#### D-1: 주가 예측 API 완성

**목표**: 기술적 지표 + 뉴스 감성 + 거시경제를 입력으로 주가 방향성을 예측한다.

**작업 내용**:
1. `AnalysisController.java` 수정
   - `getPrediction()` 에 `@RequestParam(defaultValue = "1D") String period` 추가
   - Swagger 문서 파라미터 설명 추가

2. `AnalysisService.getPrediction()` 수정
   - `period` 파라미터 전달 (`"1D"` 또는 `"1W"`)
   - 예측 입력 데이터 조합:
     - `TechnicalIndicatorService.calculate(stockCode)` 호출
     - `NewsArticleRepository`에서 최근 7일 종목 관련 뉴스 감성 점수 평균 계산
     - `MacroIndicatorRepository`에서 주요 거시경제 지표 최신값 조회

3. `OpenAiPromptBuilder` 예측 프롬프트 구현
   - 1D/1W에 따른 프롬프트 차별화
   - 출력 JSON 형식: `{ direction, confidence, reasons: [3개] }`

**영향 파일**: `AnalysisController.java` (수정), `AnalysisService.java` (수정), `OpenAiPromptBuilder.java` (수정)

**완료 조건**: 1D/1W 예측 결과가 서로 다른 내용으로 반환됨, 신뢰도 0~100 범위 검증

---

#### D-2: 통합 테스트 및 Fallback 검증

**목표**: 외부 API 장애 시나리오를 검증하고 Circuit Breaker 동작을 확인한다.

**작업 내용**:
1. Resilience4j 설정 (`application.yml`에 추가)
   ```yaml
   resilience4j:
     circuitbreaker:
       instances:
         openai:
           failure-rate-threshold: 50
           wait-duration-in-open-state: 30s
           sliding-window-size: 10
     retry:
       instances:
         openai:
           max-attempts: 2
           wait-duration: 1s
   ```

2. `@CircuitBreaker(name = "openai", fallbackMethod = "getReportFromDb")` 적용
   - Fallback 메서드: DB에서 마지막 리포트 반환

3. 통합 테스트 시나리오
   - OpenAI API Mock 서버로 성공/실패 케이스 테스트
   - Redis 캐시 HIT/MISS 테스트
   - 데이터 부족 시 기술적 지표 null 처리 테스트

**완료 조건**: Circuit Breaker가 열린 상태에서 Fallback이 DB 데이터를 반환함

---

## 4. 프론트엔드 구현 계획

### 4.1 변경 범위 요약

Phase 2에서 프론트엔드 변경은 **최소화**가 원칙이다. Phase 1에서 이미 UI 골격과 API 연동 코드가 준비되어 있으므로, Mock에서 실제 데이터로 전환 + 추가 UI 기능 확장에 집중한다.

### 4.2 신규/수정 파일 목록

| 파일 | 변경 유형 | 내용 |
|------|---------|------|
| `types/index.ts` | 수정 | `TechnicalAnalysis` 타입 추가, `Prediction`의 `period` 리터럴 타입 확인 |
| `hooks/use-analysis.ts` | 수정 | `usePrediction`에 `period` 파라미터 추가 |
| `app/analysis/page.tsx` | 수정 | 예측 기간(1D/1W) 탭 선택 UI 추가 |
| `components/stocks/stock-chart.tsx` | 수정 | 기술적 지표 오버레이(RSI, MACD, 볼린저밴드) 차트 섹션 추가 |
| `app/stocks/[stockCode]/page.tsx` | 수정 | 기술적 지표 섹션 추가 (차트 하단) |
| `hooks/use-analysis.ts` | 수정 | `useTechnicalAnalysis` 훅 추가 |

### Phase FE-1: 타입 및 훅 업데이트

**목표**: 백엔드 Phase B-1의 기술적 지표 응답에 맞는 타입과 훅을 준비한다.

**작업 내용**:
1. `types/index.ts` 수정 - `TechnicalAnalysis` 타입 추가
   ```typescript
   export interface TechnicalAnalysis {
     stockCode: string;
     rsi: { value: number; signal: string } | null;
     macd: { value: number; signal: number; histogram: number; trend: string } | null;
     bollingerBand: { upper: number; middle: number; lower: number; position: string } | null;
     volume: { current: number; average: number; ratio: number };
     calculatedAt: string;
   }
   ```

2. `hooks/use-analysis.ts` 수정
   ```typescript
   // 기존 usePrediction에 period 파라미터 추가
   export function usePrediction(stockCode: string, period: '1D' | '1W' = '1D')

   // 신규: 기술적 지표 조회 훅
   export function useTechnicalAnalysis(stockCode: string)
   ```

**완료 조건**: TypeScript 타입 에러 없이 빌드 성공

---

### Phase FE-2: 분석 페이지 예측 기간 선택 UI 추가

**목표**: `/analysis` 페이지에 1거래일/1주일 예측 기간 탭을 추가한다.

**작업 내용**:
- `app/analysis/page.tsx` 수정
  - `useState`로 `period: '1D' | '1W'` 상태 관리
  - 기존 예측 카드 위에 탭 UI 추가 (shadcn/ui `Tabs` 컴포넌트 활용)
  - `usePrediction(selectedStock, period)` 호출로 변경
  - 기간별 설명 문구 추가: "1거래일 후", "1주일 후"

**완료 조건**: 탭 전환 시 다른 예측 결과가 표시됨

---

### Phase FE-3: 종목 상세 페이지 기술적 지표 섹션 추가

**목표**: `/stocks/[stockCode]` 페이지 차트 하단에 기술적 지표 요약 카드를 추가한다.

**작업 내용**:
- `app/stocks/[stockCode]/page.tsx` 수정
  - `useTechnicalAnalysis(stockCode)` 훅 호출
  - 차트 하단에 RSI / MACD / 볼린저밴드 신호 카드 3개 추가
  - 각 카드: 지표명, 수치, 신호(매수/중립/매도 뱃지)
  - 기존 분석 페이지의 기술적 지표 카드 UI 패턴 재사용

**완료 조건**: 종목 상세 페이지에서 실시간 기술적 지표 확인 가능

---

### Phase FE-4: Mock 데이터 제거 (선택, 백엔드 안정화 후)

**목표**: 실제 API 연동이 안정화되면 Mock 파일을 제거하거나 개발용으로 유지한다.

**작업 내용**:
- `mocks/analysis.ts` - 개발/테스트용으로 유지 (삭제하지 않음)
- `apiClient`가 실제 API를 호출하는지 환경변수로 제어 (선택)

---

## 5. 구현 우선순위 및 작업 순서

### 전체 작업 순서 (4주 계획)

```
[7주차] 기반 + 기술적 지표
  ┌─ A-1: Redis 활성화 (0.5일)
  ├─ A-2: OpenAI 설정 (0.5일)
  ├─ B-1: TechnicalIndicatorService 구현 (3일)
  └─ FE-1: 타입/훅 업데이트 (1일)

[8주차] AI 분석 리포트
  ┌─ B-2: OpenAiClient + OpenAiAnalysisProvider 구현 (4일)
  └─ AnalysisService 캐싱 로직 (1일)

[9주차] 뉴스 감성 분석
  ┌─ C-1: 뉴스 수집 스케줄러 (2일)
  ├─ C-2: 뉴스 감성 분석 서비스 (2일)
  └─ FE-2: 예측 기간 탭 UI (1일)

[10주차] 예측 완성 + 통합
  ┌─ D-1: 주가 예측 기간 파라미터 + 프롬프트 (2일)
  ├─ D-2: Circuit Breaker + 통합 테스트 (2일)
  └─ FE-3: 종목 상세 기술적 지표 섹션 (1일)
```

### 의존 관계 체인

```
A-1 (Redis) ────────────────────────────────── B-2 캐싱 → D-1 예측 캐싱
A-2 (OpenAI 설정) ──── B-2 (OpenAI 구현) ────────────────────────────────
B-1 (지표 계산) ─────── B-2 (리포트 입력) ──── D-1 (예측 입력)
                        FE-1 (타입) ──── FE-3 (UI)
C-1 (뉴스 수집) ─────── C-2 (감성 분석) ─────── D-1 (예측 입력)
```

### 우선순위 결정 근거

1. **B-1 (기술적 지표)** 최우선 이유: 리포트, 예측 모두에 입력값으로 사용되며 외부 API 불필요 (자체 계산)
2. **A-1/A-2 (설정류)** 조기 완료 이유: 뒤에 오는 모든 기능의 전제 조건
3. **C-1/C-2 (뉴스)** 뒤에 두는 이유: 뉴스 감성은 예측의 보조 입력이며 없어도 기본 예측 가능
4. **D-2 (통합 테스트)** 마지막 이유: 모든 컴포넌트 완성 후 전체 흐름 검증

---

## 6. 리스크 및 완화 방안

| 리스크 | 발생 가능성 | 영향도 | 완화 방안 |
|--------|-----------|--------|---------|
| OpenAI API 응답 형식 불일치 (JSON 파싱 실패) | 중 | 높 | 응답 파싱에 try-catch + 재요청 1회, 실패 시 Mock 응답 반환 |
| 기술적 지표 계산 오류 (부동소수점 정밀도) | 중 | 중 | BigDecimal 사용, JUnit 테스트로 계산식 검증 |
| OpenAI API 비용 초과 | 중 | 높 | DB+Redis 이중 캐싱 엄격 적용, 개발 환경에서 Mock 구현체 우선 사용 |
| 뉴스 RSS 피드 URL 변경 또는 차단 | 높 | 중 | URL을 설정 파일로 외부화, 대체 RSS 목록 확보 |
| Redis 연결 실패 시 서비스 중단 | 낮 | 높 | Redis 장애 시 캐시 없이 DB/OpenAI 직접 조회 (Best Effort) |
| 주가 데이터 부족 (60일 미만) | 중 | 중 | 데이터 부족 시 null 반환, 프론트엔드에서 "데이터 부족" 표시 |
| OpenAI 한국어 품질 부족 | 낮 | 중 | 프롬프트에 한국어 초보자 친화 예시 포함, 검토 후 반복 개선 |

---

## 7. 미결 사항

다음 항목은 개발 착수 전 또는 해당 Phase 진입 전 확인이 필요하다.

1. **OpenAI API 키 조달**: `OPENAI_API_KEY` 환경변수를 개발 팀에서 직접 발급하여 `.env` 파일에 설정해야 한다. (코드에 절대 하드코딩 금지)

2. **뉴스 RSS 피드 URL 확정**: 수집 대상 RSS URL이 현재 미확정 상태이다. 착수 전 다음 중 선택 필요:
   - 네이버 금융 뉴스 RSS (https://finance.naver.com/news/news_list.naver?mode=RSS 등)
   - Yahoo Finance RSS
   - NewsAPI.org 유료 플랜

3. **로컬 Redis 실행 방법**: 팀 개발 환경에서 Redis를 실행하는 표준 방법 결정 필요 (Docker Compose 파일 제공 권장)

4. **OpenAI 응답 언어 품질 검토**: 프롬프트 초안 작성 후 실제 API 호출로 결과 검토 및 반복 개선 필요 (A/B 테스트)

5. **거시경제 수집 스케줄러 포함 여부**: Phase 2에서 `MacroDataCollectionScheduler` 구현을 포함할지, Phase 3으로 미룰지 결정 필요. 현재 계획에서는 선택(optional) 처리함.

---

## Phase 2 완료 기준 체크리스트

- [ ] 기술적 지표(RSI, MACD, 볼린저밴드) 실제 데이터 기반 계산 동작
- [ ] AI 분석 리포트 OpenAI API 연동 및 한국어 초보자 친화 결과 생성
- [ ] 동일 종목 동일 날짜 재요청 시 Redis/DB 캐시에서 반환 (API 미호출 확인)
- [ ] 뉴스 1시간 주기 수집 및 감성 분석 결과 저장
- [ ] 주가 예측 1D/1W 두 기간 모두 동작
- [ ] OpenAI API 장애 시 Fallback (DB 마지막 데이터 반환) 동작
- [ ] 프론트엔드 예측 기간 탭 전환 UI 동작
- [ ] 종목 상세 페이지 기술적 지표 카드 표시
- [ ] 면책조항 모든 AI 결과에 포함 확인

---

*이 문서는 @senior-requirements-planner가 기존 코드베이스를 분석하여 생성하였으며,
@ui-design-engineer 및 @senior-clean-architect의 입력 문서로 사용됩니다.*
