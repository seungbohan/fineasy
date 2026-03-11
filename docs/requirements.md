# 요구사항 정의서 및 구현 계획서

**프로젝트명**: FinEasy - 초보자를 위한 금융 웹 플랫폼
**작성일**: 2026-02-20
**작성자**: @senior-requirements-planner
**버전**: v1.0

---

## 목차

1. [배경 및 목적](#1-배경-및-목적)
2. [기능 요구사항](#2-기능-요구사항)
3. [비기능 요구사항](#3-비기능-요구사항)
4. [시스템 아키텍처](#4-시스템-아키텍처)
5. [화면(페이지) 목록 및 와이어프레임 설명](#5-화면페이지-목록-및-와이어프레임-설명)
6. [API 엔드포인트 목록](#6-api-엔드포인트-목록)
7. [데이터 모델](#7-데이터-모델)
8. [구현 우선순위 및 마일스톤](#8-구현-우선순위-및-마일스톤)
9. [리스크 및 제약사항](#9-리스크-및-제약사항)
10. [미결 사항](#10-미결-사항)

---

## 1. 배경 및 목적

### 배경

금융 투자에 관심 있는 초보자들은 복잡한 금융 용어, 어려운 차트 분석, 방대한 경제 뉴스로 인해 진입 장벽을 느낀다. 기존 증권 앱(토스증권, 카카오페이증권 등)은 UX는 개선되었으나, 초보자가 '왜 이 주식을 사야 하는지'를 이해하도록 돕는 교육적 기능이 부족하다.

### 목적

- 초보자가 금융 개념을 쉽게 이해하고 투자 판단을 내릴 수 있도록 지원
- AI를 활용하여 복잡한 데이터를 초보자 언어로 번역·설명
- 토스증권 수준의 직관적인 UX로 금융 서비스 접근성 향상

### 타겟 사용자

- 주식 투자를 막 시작한 20~40대 초보 투자자
- 금융 뉴스를 읽고 싶지만 경제 용어가 어려운 일반인
- 투자 전 공부를 원하는 잠재 투자자

---

## 2. 기능 요구사항

### 2.1 메인 화면 (홈 대시보드)

#### FR-1-1: 시장 지수 표시
- 코스피, 코스닥, 나스닥, S&P500, 다우존스 실시간 지수 표시
- 전일 대비 등락폭 및 등락률 (색상: 상승=빨강, 하락=파랑, 보합=회색)
- 지수별 미니 스파크라인 차트 (최근 5일)
- 갱신 주기: 15초 (장중), 1시간 (장외)

#### FR-1-2: 인기 종목 섹션
- 거래량 상위 10종목, 등락률 상위/하위 10종목 탭 전환
- 종목명, 현재가, 등락률, 거래량 표시
- 각 종목 클릭 시 종목 상세 페이지로 이동

#### FR-1-3: 관심 종목 리스트
- 비로그인: 로컬스토리지 기반 관심 종목 최대 10개 저장
- 로그인: 서버 기반 관심 종목 최대 30개 저장
- 관심 종목 추가/삭제 기능 (하트/북마크 아이콘)
- 관심 종목이 없을 경우 "관심 종목을 추가해보세요" 온보딩 메시지

#### FR-1-4: 빠른 검색
- 헤더 상단 검색창 (종목명, 종목코드, 금융 용어 통합 검색)
- 자동완성 드롭다운 (타이핑 300ms 디바운스)
- 검색 결과: 종목과 용어를 구분하여 표시
- 최근 검색어 최대 5개 저장

#### FR-1-5: 오늘의 시장 요약
- AI가 생성한 오늘의 시장 상황 1문단 요약 (6시간 주기 갱신)
- "더보기" 클릭 시 상세 분석 페이지로 이동

---

### 2.2 종목 상세 페이지

#### FR-2-1: 실시간 주가 차트
- 기간 선택: 1일, 1주, 1개월, 3개월, 1년, 전체
- 차트 유형: 라인 차트(기본), 캔들스틱 차트 전환 가능
- 기술적 지표 오버레이: 5일/20일/60일 이동평균선
- 거래량 막대 차트 하단 동기화 표시
- 차트 라이브러리: Recharts 또는 TradingView Lightweight Charts

#### FR-2-2: 기업 기본 정보
- 종목명, 종목코드, 거래소 구분 (KRX/NASDAQ 등)
- 현재가, 전일 대비 등락, 52주 최고/최저
- 시가총액, 발행주식수
- 재무 지표: PER, PBR, EPS, 배당수익률
- 각 재무 지표 옆 "?" 버튼 클릭 시 용어 설명 팝오버 (인라인)

#### FR-2-3: AI 분석 리포트
- 기술적 분석 요약 (RSI 과매수/과매도 여부, MACD 신호 등)
- 초보자 친화적 요약: "현재 이 주식은 [상태]입니다. [이유] 때문에 [조언]."
- 투자 의견 표시: 매수/중립/매도 (단순 신호, 투자 권유 아님 명시)
- 분석 갱신 시각 표시
- 면책조항 문구 반드시 포함

#### FR-2-4: 관련 뉴스 피드
- 해당 종목 관련 최신 뉴스 최대 20건
- 뉴스 제목, 출처, 게시 시간, 감성 분석 결과 (긍정/부정/중립 배지)
- 뉴스 클릭 시 원문 링크로 이동 (새 탭)
- 뉴스 내 금융 용어 인라인 팝오버 지원 (핵심 용어 자동 태깅)

#### FR-2-5: 관심 종목 추가
- 상세 페이지 내 하트/북마크 버튼
- 로그인 여부에 따라 서버/로컬 저장

---

### 2.3 금융·경제 용어 사전

#### FR-3-1: 용어 검색
- 검색창에서 용어 실시간 검색 (300ms 디바운스)
- 초성 검색 지원 (예: "ㅈ" 입력 시 "주식", "주가" 등 노출)
- 검색 결과 없을 경우: "이 용어가 없나요? 추가 요청하기" 버튼

#### FR-3-2: 카테고리 분류
- 카테고리 목록:
  - 주식 기초 (주식, 주가, 시가총액 등)
  - 재무 지표 (PER, PBR, ROE 등)
  - 기술적 분석 (이동평균, RSI, MACD 등)
  - 채권/금리 (국채, 금리, 기준금리 등)
  - 거시경제 (GDP, CPI, 인플레이션 등)
  - 파생상품 (옵션, 선물, ETF 등)
- 카테고리별 필터링 및 정렬 (가나다순, 난이도순)

#### FR-3-3: 용어 상세 설명
- 용어명 (한국어 + 영어 병기)
- 난이도 표시: 초급/중급/고급 뱃지
- 쉬운 설명 (2~3문장, 초등학생도 이해 가능한 수준)
- 상세 설명 (접기/펼치기)
- 관련 용어 링크 (최대 5개)
- 예시 문장: "예를 들어, PER이 10배라면..."

#### FR-3-4: 인라인 팝오버 (앱 전체 공통)
- 등록된 금융 용어가 뉴스, 분석 리포트 등에 등장하면 밑줄 점선 스타일 적용
- 용어 클릭/탭 시 팝오버로 간단 설명 표시 (2~3문장)
- "자세히 보기" 링크로 용어 사전 상세 페이지 이동

---

### 2.4 AI 주가 분석

#### FR-4-1: 기술적 분석 지표 계산
- 이동평균 (MA): 5일, 20일, 60일, 120일
- RSI (Relative Strength Index): 14일 기준
- MACD: 12일 EMA - 26일 EMA, Signal 9일
- 볼린저 밴드: 20일 MA ± 2 표준편차
- 거래량 분석: 평균 거래량 대비 현재 거래량 비율

#### FR-4-2: AI 분석 리포트 생성
- 입력: 최근 60일 OHLCV 데이터 + 계산된 기술적 지표
- LLM 프롬프트로 초보자 친화적 리포트 생성 (OpenAI GPT-4o 활용)
- 출력 구조:
  - 한 줄 요약 (핵심 신호)
  - 현재 상황 설명 (3~5문장)
  - 주목할 점 (불릿 포인트 3개)
  - 투자 시 고려사항 (면책조항 포함)
- 캐싱: 동일 종목 동일 날짜 결과 DB 저장 (재생성 최소화)

#### FR-4-3: 분석 이력 조회
- 과거 AI 분석 리포트 최대 30일치 조회 가능
- 리포트 날짜별 아카이브

---

### 2.5 뉴스 분석 및 주가 예측

#### FR-5-1: 거시경제 데이터 수집
- 수집 항목:
  - 기준금리 (한국은행, 미국 FED)
  - CPI (소비자물가지수)
  - GDP 성장률 (분기)
  - 환율 (USD/KRW, JPY/KRW, EUR/KRW)
  - 국제 유가 (WTI, 브렌트유)
  - 금 가격
- 데이터 출처: FRED API (미국), 한국은행 OpenAPI, Investing.com 스크래핑
- 갱신 주기: 일 1회 (오전 7시 배치)

#### FR-5-2: 뉴스 수집 및 감성 분석
- 뉴스 출처: 네이버 금융 뉴스 RSS, Google News RSS, Yahoo Finance RSS
- 수집 주기: 1시간 간격
- 감성 분석: OpenAI API 또는 KoBERT 기반 감성 분류
  - 결과: 긍정(Positive) / 부정(Negative) / 중립(Neutral) + 신뢰도 점수 (0~1)
- 뉴스-종목 연관 태깅: 종목명/코드 기반 자동 태깅

#### FR-5-3: 주가 방향성 예측
- 예측 범위: 다음 1거래일, 1주일 방향성 (상승/하락/보합)
- 입력 데이터:
  - 최근 30일 주가 데이터 (OHLCV)
  - 기술적 지표 (RSI, MACD, 볼린저 밴드)
  - 최근 7일 뉴스 감성 점수 평균
  - 거시경제 지표 최신값
- AI 모델: OpenAI GPT-4o (few-shot prompting) 또는 별도 ML 모델
- 출력: 방향성 + 신뢰도(%) + 주요 근거 3가지
- 면책조항 필수 포함: "이 예측은 투자 권유가 아닙니다"

#### FR-5-4: 예측 근거 설명
- 초보자 친화적 설명 생성
  - 예: "최근 삼성전자 관련 뉴스가 긍정적이고, RSI 지표가 과매도 구간에서 회복 중이어서 단기 상승 가능성을 봅니다."
- 사용된 데이터 소스 명시
- 이전 예측 정확도 이력 표시 (신뢰성 확보)

---

### 2.6 학습 센터

#### FR-6-1: 투자 기초 가이드
- 콘텐츠 목록 (정적 콘텐츠, 마크다운 기반):
  - 주식이란 무엇인가
  - 증권 계좌 개설하는 방법
  - 분산투자란 무엇인가
  - 장기투자 vs 단기투자
  - 리스크 관리 기초
- 각 아티클: 읽기 시간 표시, 난이도 뱃지

#### FR-6-2: 경제 뉴스 읽는 방법
- 뉴스 분석 튜토리얼 (실제 뉴스 예시 사용)
- 핵심 경제 지표 해석법
- 뉴스와 주가의 관계

#### FR-6-3: 차트 보는 방법
- 캔들스틱 차트 기초
- 이동평균선 이해하기
- 거래량의 의미
- 인터랙티브 예시 차트 포함

#### FR-6-4: 학습 진도 추적 (선택, Phase 3)
- 로그인 사용자: 읽은 아티클 체크 저장
- 진도율 표시 (예: "기초 가이드 3/5 완료")

---

## 3. 비기능 요구사항

### 3.1 성능 (Performance)

| 항목 | 목표값 |
|------|-------|
| 메인 페이지 초기 로딩 | 3초 이내 (LCP 기준) |
| API 응답 시간 (일반) | 200ms 이내 |
| API 응답 시간 (AI 분석) | 10초 이내 (스트리밍 응답 적용) |
| 실시간 주가 갱신 지연 | 15초 이내 |
| 동시 접속자 | Phase 1: 100명, Phase 3: 1,000명 |

- 차트 데이터는 CDN 캐싱 적용 (정적 데이터)
- AI 분석 결과는 Redis 캐시 (TTL 6시간)
- 데이터베이스 쿼리 최적화 (인덱스, 페이지네이션)

### 3.2 보안 (Security)

- HTTPS 필수 (TLS 1.3)
- 인증: JWT 기반 (Access Token 1시간, Refresh Token 30일)
- API Rate Limiting:
  - 비로그인: 100 req/min per IP
  - 로그인: 500 req/min per user
- OpenAI API 키 서버 사이드 관리 (클라이언트 노출 금지)
- 외부 API 키 환경 변수 관리 (.env, 절대 코드 커밋 금지)
- SQL Injection, XSS 방어 (Spring Security, React 기본 방어)
- 개인정보: 최소 수집 원칙 (이메일, 닉네임만)

### 3.3 확장성 (Scalability)

- 백엔드: Docker 컨테이너화, 수평 확장 가능한 Stateless 설계
- 데이터베이스: PostgreSQL (운영), H2 (개발/테스트)
- 캐시: Redis (실시간 주가, AI 분석 결과)
- 메시지 큐: 뉴스 수집 파이프라인에 비동기 처리 적용 (Spring @Async 또는 Kafka - Phase 3)

### 3.4 가용성 (Availability)

- Phase 1 목표: 99% (월 약 7시간 다운타임 허용)
- 외부 API 장애 시 Fallback: 캐시된 마지막 데이터 표시 + 사용자 안내 배너

### 3.5 반응형 디자인

- 브레이크포인트:
  - 모바일: 320px ~ 767px
  - 태블릿: 768px ~ 1199px
  - 데스크톱: 1200px+
- 모바일 우선(Mobile-First) 개발 원칙
- 터치 친화적 UI (버튼 최소 크기 44x44px)

### 3.6 접근성 (Accessibility)

- WCAG 2.1 Level AA 준수 목표
- 색상만으로 정보 구분 금지 (색맹 고려, 아이콘/레이블 병행)
- 스크린 리더 지원 (aria-label, role 속성)
- 키보드 네비게이션 지원

### 3.7 국제화 (i18n)

- Phase 1: 한국어 단일 지원
- Phase 3: 영어 추가 지원 구조 준비 (i18n 라이브러리 초기부터 도입)

---

## 4. 시스템 아키텍처

### 4.1 전체 아키텍처 개요

```
[사용자 브라우저]
       |
       | HTTPS
       v
[Next.js 프론트엔드] ──── CDN (정적 자산)
       |
       | REST API / WebSocket
       v
[Spring Boot 백엔드 API]
       |
  ┌────┼────────────────────┐
  v    v                    v
[PostgreSQL]  [Redis]  [외부 API 연동 서비스]
                              |
                    ┌─────────┼──────────┐
                    v         v          v
              [주식 시세 API] [뉴스 API] [거시경제 API]
                                         |
                                    [OpenAI API]
```

### 4.2 프론트엔드 스택

| 항목 | 기술 | 선택 이유 |
|------|------|----------|
| 프레임워크 | Next.js 14 (App Router) | SSR/SSG로 SEO 및 초기 로딩 성능, React 기반 |
| 언어 | TypeScript | 타입 안전성, 대형 프로젝트 유지보수성 |
| 상태관리 | Zustand | 경량, 간단한 API (Redux 대비 보일러플레이트 최소화) |
| 서버상태 | TanStack Query (React Query) | API 캐싱, 자동 리패칭, 로딩/에러 상태 관리 |
| 스타일링 | Tailwind CSS | 빠른 UI 개발, 디자인 일관성 |
| UI 컴포넌트 | shadcn/ui | 접근성 준수, 커스터마이징 용이 |
| 차트 | TradingView Lightweight Charts | 금융 차트 특화, 고성능 |
| 패키지 관리 | npm |  |

### 4.3 백엔드 스택

| 항목 | 기술 | 선택 이유 |
|------|------|----------|
| 언어 | Java 17 | Record 활용, 표준 언어로 유지보수 용이 |
| 프레임워크 | Spring Boot 3.x | 프로젝트 기술 스택 준수 |
| 빌드 | Gradle (Kotlin DSL) | 프로젝트 기술 스택 준수 |
| ORM | Spring Data JPA + Hibernate | 표준 ORM, 생산성 |
| 보안 | Spring Security | JWT 인증/인가 |
| DB | PostgreSQL 16 | JSONB 지원, 성능 |
| 캐시 | Redis 7 | 실시간 데이터, AI 결과 캐싱 |
| 문서화 | SpringDoc OpenAPI (Swagger) | API 문서 자동화 |
| 테스트 | JUnit 5, Mockito, Testcontainers | |

### 4.4 외부 API 연동 계획

| 서비스 | API | 용도 | 비용 |
|--------|-----|------|------|
| 한국 주식 시세 | 한국투자증권 OpenAPI 또는 키움증권 OpenAPI | 실시간/과거 주가 | 무료 (계좌 개설 필요) |
| 미국 주식 시세 | Alpha Vantage API (Free tier) 또는 Yahoo Finance | 미국 주가 | 무료 (Rate Limit 있음) |
| 뉴스 | NewsAPI.org 또는 네이버 뉴스 검색 API | 금융 뉴스 수집 | 무료 플랜 존재 |
| 거시경제 | FRED API (미연준) + 한국은행 ECOS API | 금리, GDP, CPI 등 | 무료 |
| 환율 | ExchangeRate-API 또는 한국은행 API | 환율 정보 | 무료 |
| AI/LLM | OpenAI API (GPT-4o) | 분석 리포트, 감성 분석 | 사용량 기반 과금 |

### 4.5 AI/ML 파이프라인

```
[데이터 수집 스케줄러 (Spring Scheduler)]
        |
   ┌────┴────────┐
   v             v
[주가 수집]   [뉴스 수집]   [거시경제 수집]
   |             |                |
   v             v                v
[PostgreSQL 원시 데이터 저장]
        |
        v
[분석 서비스 (AnalysisService)]
   - 기술적 지표 계산 (자체 구현 또는 TA4J 라이브러리)
   - 뉴스 감성 점수 집계
   - 거시경제 지표 정규화
        |
        v
[OpenAI API 호출]
   - 기술적 분석 리포트 생성
   - 주가 방향성 예측
   - 초보자 친화적 설명 생성
        |
        v
[Redis 캐시 저장 (TTL 6시간)]
        |
        v
[API 응답 → 프론트엔드]
```

---

## 5. 화면(페이지) 목록 및 와이어프레임 설명

### 5.1 네비게이션 구조

```
/ (홈)
├── /stocks/:stockCode (종목 상세)
├── /dictionary (용어 사전)
│   └── /dictionary/:termId (용어 상세)
├── /analysis (AI 분석 허브)
├── /news (뉴스 피드)
├── /learn (학습 센터)
│   └── /learn/:articleId (학습 아티클)
├── /login
├── /signup
└── /mypage (로그인 필요)
```

### 5.2 페이지별 구성 요소

#### 5.2.1 홈 (/)

```
┌─────────────────────────────────────┐
│ [로고]  [검색창]  [로그인/마이페이지] │  ← 고정 헤더
├─────────────────────────────────────┤
│ 📊 오늘의 시장                        │
│ [코스피 +1.2%] [코스닥 -0.3%]        │
│ [나스닥 +0.8%] [S&P500 +0.5%]       │
│ ─────────────────────────────────── │
│ 오늘의 시장 요약 (AI 생성)            │
│ "오늘 시장은 ..."  [더보기]           │
│ ─────────────────────────────────── │
│ 🔥 인기 종목    [거래량 | 상승 | 하락]│
│ [삼성전자 72,000 +2.1%]             │
│ [SK하이닉스 ...]                     │
│ ─────────────────────────────────── │
│ ⭐ 내 관심 종목                      │
│ [관심 종목 추가하기 버튼]             │
└─────────────────────────────────────┘
│ [홈][종목][사전][분석][학습]          │  ← 하단 네비게이션 (모바일)
```

**토스증권 참고 포인트**:
- 카드 형태의 섹션 구분
- 흰 배경, 섹션 간 연한 회색 구분선
- 숫자 폰트: 가독성 높은 타블러 수치 폰트 적용
- 최소한의 텍스트, 핵심 정보만 노출

#### 5.2.2 종목 상세 (/stocks/:stockCode)

```
┌─────────────────────────────────────┐
│ [← 뒤로]  삼성전자 (005930)  [♥]    │  ← 헤더
├─────────────────────────────────────┤
│  72,000원  ▲ 1,500 (+2.1%)         │
│  ──────────────────────────────     │
│  [차트 영역 - 360px 높이]            │
│  [1일][1주][1월][3월][1년][전체]     │
│  ──────────────────────────────     │
│  📋 기업 정보                        │
│  시가총액  430조    [?]              │
│  PER      12.3배   [?]             │
│  PBR      1.2배    [?]             │
│  ──────────────────────────────     │
│  🤖 AI 분석 (2024-02-20 09:00 기준)  │
│  "현재 삼성전자는 단기 상승 신호..."  │
│  [상세 분석 보기 →]                  │
│  ──────────────────────────────     │
│  📰 관련 뉴스                        │
│  [긍정] 삼성전자 HBM 수주...         │
│  [중립] 삼성전자 실적 발표...         │
└─────────────────────────────────────┘
```

**토스증권 참고 포인트**:
- 종목명 + 코드 + 북마크 버튼 헤더
- 현재가 크게, 등락 작게 표시
- 기술적 지표는 팝오버로 설명 지원
- AI 분석 섹션에 로봇 아이콘으로 AI 생성 표시

#### 5.2.3 용어 사전 (/dictionary)

```
┌─────────────────────────────────────┐
│  금융 용어 사전                      │
│  [🔍 용어 검색...]                   │
│  ──────────────────────────────     │
│  [전체][주식][재무지표][기술분석]     │
│  [채권/금리][거시경제][파생상품]      │
│  ──────────────────────────────     │
│  ㄱ                                  │
│  가치투자  [초급]  설명 미리보기...   │
│  거래량    [초급]  설명 미리보기...   │
│  공매도    [중급]  설명 미리보기...   │
│  ──────────────────────────────     │
│  ㄴ                                  │
│  나스닥    [초급]  설명 미리보기...   │
└─────────────────────────────────────┘
```

#### 5.2.4 AI 분석 허브 (/analysis)

```
┌─────────────────────────────────────┐
│  AI 주가 분석                        │
│  [종목 검색 후 분석 시작]             │
│  ──────────────────────────────     │
│  📈 오늘의 시장 분석                  │
│  [코스피 종합 분석 카드]              │
│  ──────────────────────────────     │
│  🔮 주가 예측 (면책조항 포함)         │
│  종목 선택: [삼성전자 ▼]             │
│  예측 기간: [1거래일][1주일]          │
│  [분석 시작]                         │
│  ──────────────────────────────     │
│  [예측 결과 카드]                    │
│  상승 가능성 65%  ↑                  │
│  근거 1: RSI 과매도 회복 중          │
│  근거 2: 긍정 뉴스 증가              │
│  근거 3: 거래량 평균 초과            │
│  ⚠️ 이 예측은 투자 권유가 아닙니다.   │
└─────────────────────────────────────┘
```

#### 5.2.5 학습 센터 (/learn)

```
┌─────────────────────────────────────┐
│  투자 학습 센터                      │
│  ──────────────────────────────     │
│  🏁 투자 기초                        │
│  [주식이란?     초급  5분 읽기  ✓]   │
│  [계좌 개설법   초급  3분 읽기  ✓]   │
│  [분산투자      초급  7분 읽기   ]   │
│  ──────────────────────────────     │
│  📰 뉴스 읽는 법                     │
│  [경제 뉴스 기초 중급  10분 읽기  ]  │
│  ──────────────────────────────     │
│  📊 차트 분석                        │
│  [캔들스틱 기초  중급   8분 읽기  ]  │
└─────────────────────────────────────┘
```

---

## 6. API 엔드포인트 목록

### 6.1 기본 규칙

- Base URL: `https://api.fineasy.kr/api/v1`
- 응답 포맷: JSON
- 인증: `Authorization: Bearer {accessToken}` (인증 필요 엔드포인트만)
- 공통 에러 응답:
  ```json
  {
    "success": false,
    "error": {
      "code": "ERROR_CODE",
      "message": "사용자 친화적 에러 메시지"
    }
  }
  ```
- 공통 성공 응답:
  ```json
  {
    "success": true,
    "data": { ... },
    "meta": { "timestamp": "2026-02-20T09:00:00Z" }
  }
  ```

### 6.2 인증 API

| 메서드 | URL | 설명 | 인증 필요 |
|--------|-----|------|-----------|
| POST | /auth/signup | 회원가입 | X |
| POST | /auth/login | 로그인 | X |
| POST | /auth/refresh | 토큰 갱신 | X |
| POST | /auth/logout | 로그아웃 | O |

**POST /auth/signup 요청**:
```json
{
  "email": "user@example.com",
  "password": "password123!",
  "nickname": "투자초보"
}
```

**POST /auth/login 응답**:
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "eyJ...",
    "user": {
      "id": 1,
      "email": "user@example.com",
      "nickname": "투자초보"
    }
  }
}
```

### 6.3 시장 지수 API

| 메서드 | URL | 설명 | 인증 필요 |
|--------|-----|------|-----------|
| GET | /market/indices | 주요 지수 목록 조회 | X |
| GET | /market/summary | 오늘의 시장 AI 요약 | X |

**GET /market/indices 응답**:
```json
{
  "success": true,
  "data": {
    "indices": [
      {
        "code": "KOSPI",
        "name": "코스피",
        "currentValue": 2650.33,
        "changeAmount": 15.22,
        "changeRate": 0.58,
        "sparklineData": [2620, 2630, 2640, 2645, 2650]
      }
    ],
    "updatedAt": "2026-02-20T09:15:00Z"
  }
}
```

### 6.4 종목 API

| 메서드 | URL | 설명 | 인증 필요 |
|--------|-----|------|-----------|
| GET | /stocks/search?q={query} | 종목 검색 (자동완성) | X |
| GET | /stocks/popular | 인기 종목 (거래량/등락률) | X |
| GET | /stocks/{stockCode} | 종목 기본 정보 | X |
| GET | /stocks/{stockCode}/price | 현재가 및 등락 | X |
| GET | /stocks/{stockCode}/chart | 주가 차트 데이터 | X |
| GET | /stocks/{stockCode}/financials | 재무 정보 | X |
| GET | /stocks/{stockCode}/news | 관련 뉴스 | X |

**GET /stocks/{stockCode}/chart 요청 파라미터**:
```
period: 1D | 1W | 1M | 3M | 1Y | ALL
type: LINE | CANDLE
```

**GET /stocks/{stockCode}/chart 응답**:
```json
{
  "success": true,
  "data": {
    "stockCode": "005930",
    "stockName": "삼성전자",
    "period": "1M",
    "candles": [
      {
        "date": "2026-01-20",
        "open": 71000,
        "high": 73000,
        "low": 70500,
        "close": 72000,
        "volume": 15000000
      }
    ],
    "indicators": {
      "ma5": [71200, 71500, ...],
      "ma20": [70800, 71000, ...],
      "ma60": [69500, 70000, ...]
    }
  }
}
```

### 6.5 관심 종목 API

| 메서드 | URL | 설명 | 인증 필요 |
|--------|-----|------|-----------|
| GET | /watchlist | 관심 종목 목록 | O |
| POST | /watchlist/{stockCode} | 관심 종목 추가 | O |
| DELETE | /watchlist/{stockCode} | 관심 종목 삭제 | O |

### 6.6 AI 분석 API

| 메서드 | URL | 설명 | 인증 필요 |
|--------|-----|------|-----------|
| GET | /analysis/{stockCode}/technical | 기술적 분석 결과 | X |
| GET | /analysis/{stockCode}/report | AI 분석 리포트 | X |
| GET | /analysis/{stockCode}/prediction | 주가 예측 | X |
| GET | /analysis/{stockCode}/history | 분석 이력 | X |

**GET /analysis/{stockCode}/report 응답**:
```json
{
  "success": true,
  "data": {
    "stockCode": "005930",
    "generatedAt": "2026-02-20T09:00:00Z",
    "summary": "삼성전자는 단기 상승 신호를 보이고 있습니다.",
    "description": "현재 RSI가 45로 중립 구간에 있으며...",
    "keyPoints": [
      "RSI 지표가 과매도 구간에서 회복 중",
      "20일 이동평균선 상향 돌파",
      "거래량이 평균 대비 120% 수준"
    ],
    "disclaimer": "이 분석은 참고용이며 투자 권유가 아닙니다.",
    "technicalSignals": {
      "rsi": { "value": 45.2, "signal": "NEUTRAL" },
      "macd": { "value": 150, "signal": "BULLISH" },
      "bollingerBand": { "position": "MIDDLE", "signal": "NEUTRAL" }
    }
  }
}
```

**GET /analysis/{stockCode}/prediction 응답**:
```json
{
  "success": true,
  "data": {
    "stockCode": "005930",
    "period": "1D",
    "direction": "UP",
    "confidence": 65,
    "reasons": [
      "최근 7일 뉴스 감성 점수 평균: 긍정(0.72)",
      "RSI 과매도 구간에서 회복 중",
      "전일 대비 거래량 30% 증가"
    ],
    "disclaimer": "이 예측은 AI 모델 기반이며 투자 권유가 아닙니다. 실제 투자 시 전문가 상담을 권장합니다.",
    "generatedAt": "2026-02-20T09:00:00Z"
  }
}
```

### 6.7 뉴스 API

| 메서드 | URL | 설명 | 인증 필요 |
|--------|-----|------|-----------|
| GET | /news | 전체 금융 뉴스 | X |
| GET | /news/{newsId} | 뉴스 상세 | X |
| GET | /news/macro | 거시경제 뉴스 | X |

**GET /news 요청 파라미터**:
```
page: 1 (기본값)
size: 20 (기본값)
sentiment: POSITIVE | NEGATIVE | NEUTRAL (선택)
stockCode: 005930 (선택, 종목 필터)
```

### 6.8 용어 사전 API

| 메서드 | URL | 설명 | 인증 필요 |
|--------|-----|------|-----------|
| GET | /terms | 용어 목록 | X |
| GET | /terms/search?q={query} | 용어 검색 | X |
| GET | /terms/{termId} | 용어 상세 | X |
| GET | /terms/categories | 카테고리 목록 | X |
| GET | /terms/category/{categoryId} | 카테고리별 용어 목록 | X |

**GET /terms/{termId} 응답**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "PER",
    "nameEn": "Price-Earnings Ratio",
    "category": "재무지표",
    "difficulty": "BEGINNER",
    "simpleDescription": "주가가 주당순이익의 몇 배인지 나타내는 지표입니다. 낮을수록 저평가, 높을수록 고평가로 볼 수 있습니다.",
    "detailedDescription": "PER(주가수익비율)은...",
    "exampleSentence": "삼성전자의 PER이 10배라는 것은 현재 주가가 연간 순이익의 10배 수준이라는 뜻입니다.",
    "relatedTerms": [
      { "id": 2, "name": "PBR" },
      { "id": 3, "name": "EPS" }
    ]
  }
}
```

### 6.9 거시경제 API

| 메서드 | URL | 설명 | 인증 필요 |
|--------|-----|------|-----------|
| GET | /macro/indicators | 거시경제 지표 목록 | X |
| GET | /macro/indicators/{indicatorCode} | 지표 상세 및 이력 | X |

### 6.10 학습 콘텐츠 API

| 메서드 | URL | 설명 | 인증 필요 |
|--------|-----|------|-----------|
| GET | /learn/articles | 학습 아티클 목록 | X |
| GET | /learn/articles/{articleId} | 아티클 상세 | X |
| POST | /learn/articles/{articleId}/complete | 읽음 처리 | O |

---

## 7. 데이터 모델

### 7.1 엔티티 목록

#### User (사용자)
```
- id: Long (PK)
- email: String (UNIQUE, NOT NULL)
- passwordHash: String (NOT NULL)
- nickname: String (NOT NULL)
- createdAt: LocalDateTime
- updatedAt: LocalDateTime
- isActive: Boolean (기본값 true)
```

#### Stock (종목)
```
- id: Long (PK)
- stockCode: String (UNIQUE, NOT NULL) -- 예: 005930
- stockName: String (NOT NULL) -- 삼성전자
- market: Enum [KRX, NASDAQ, NYSE, KOSDAQ]
- sector: String
- isActive: Boolean
- createdAt: LocalDateTime
```

#### StockPrice (주가 이력)
```
- id: Long (PK)
- stock: Stock (FK)
- tradeDate: LocalDate (NOT NULL)
- openPrice: BigDecimal
- highPrice: BigDecimal
- lowPrice: BigDecimal
- closePrice: BigDecimal
- volume: Long
- createdAt: LocalDateTime
-- UNIQUE: (stock_id, trade_date)
-- INDEX: (stock_id, trade_date DESC)
```

#### StockRealtimePrice (실시간 주가 - Redis 캐시)
```
-- Redis Hash Key: realtime:price:{stockCode}
- currentPrice: BigDecimal
- changeAmount: BigDecimal
- changeRate: Double
- volume: Long
- updatedAt: Instant
```

#### NewsArticle (뉴스 기사)
```
- id: Long (PK)
- title: String (NOT NULL)
- content: String (TEXT)
- originalUrl: String (NOT NULL, UNIQUE)
- sourceName: String
- publishedAt: LocalDateTime
- sentiment: Enum [POSITIVE, NEGATIVE, NEUTRAL]
- sentimentScore: Double -- 0.0 ~ 1.0
- createdAt: LocalDateTime
```

#### NewsStockTag (뉴스-종목 연관)
```
- id: Long (PK)
- newsArticle: NewsArticle (FK)
- stock: Stock (FK)
-- UNIQUE: (news_article_id, stock_id)
```

#### FinancialTerm (금융 용어)
```
- id: Long (PK)
- name: String (NOT NULL, UNIQUE) -- 한국어
- nameEn: String -- 영어
- category: TermCategory (FK)
- difficulty: Enum [BEGINNER, INTERMEDIATE, ADVANCED]
- simpleDescription: String (NOT NULL) -- 짧은 설명 (팝오버용)
- detailedDescription: String (TEXT) -- 상세 설명
- exampleSentence: String
- createdAt: LocalDateTime
- updatedAt: LocalDateTime
```

#### TermCategory (용어 카테고리)
```
- id: Long (PK)
- name: String (NOT NULL, UNIQUE)
- displayOrder: Integer
```

#### TermRelation (용어 연관관계)
```
- termId: Long (FK)
- relatedTermId: Long (FK)
-- PK: (term_id, related_term_id)
```

#### Watchlist (관심 종목)
```
- id: Long (PK)
- user: User (FK)
- stock: Stock (FK)
- createdAt: LocalDateTime
-- UNIQUE: (user_id, stock_id)
-- INDEX: (user_id)
```

#### StockAnalysisReport (AI 분석 리포트)
```
- id: Long (PK)
- stock: Stock (FK)
- reportDate: LocalDate
- summary: String
- description: String (TEXT)
- keyPoints: JSONB -- ["key1", "key2", "key3"]
- technicalSignals: JSONB -- RSI, MACD 등 수치
- disclaimer: String
- generatedAt: LocalDateTime
-- UNIQUE: (stock_id, report_date)
-- INDEX: (stock_id, report_date DESC)
```

#### StockPrediction (주가 예측)
```
- id: Long (PK)
- stock: Stock (FK)
- predictionDate: LocalDate
- targetPeriod: Enum [ONE_DAY, ONE_WEEK]
- direction: Enum [UP, DOWN, SIDEWAYS]
- confidence: Integer -- 0~100
- reasons: JSONB -- ["reason1", "reason2", "reason3"]
- disclaimer: String
- generatedAt: LocalDateTime
-- INDEX: (stock_id, prediction_date DESC)
```

#### MacroIndicator (거시경제 지표)
```
- id: Long (PK)
- indicatorCode: String (NOT NULL) -- GDP, CPI, FED_RATE 등
- indicatorName: String
- value: Double
- unit: String -- %, 억달러, 원/달러 등
- recordDate: LocalDate
- source: String
-- INDEX: (indicator_code, record_date DESC)
```

#### LearnArticle (학습 아티클)
```
- id: Long (PK)
- title: String
- content: String (TEXT, 마크다운)
- category: Enum [BASICS, NEWS_READING, CHART_ANALYSIS]
- difficulty: Enum [BEGINNER, INTERMEDIATE, ADVANCED]
- estimatedReadMinutes: Integer
- displayOrder: Integer
- isPublished: Boolean
```

#### UserArticleProgress (학습 진도 - Phase 3)
```
- id: Long (PK)
- user: User (FK)
- article: LearnArticle (FK)
- completedAt: LocalDateTime
-- UNIQUE: (user_id, article_id)
```

### 7.2 관계 다이어그램 (텍스트)

```
User ──<< Watchlist >>── Stock
                            |
                            |──< StockPrice
                            |──< StockAnalysisReport
                            |──< StockPrediction
                            |──< NewsStockTag >>── NewsArticle

FinancialTerm >>── TermCategory
FinancialTerm >>── TermRelation <<── FinancialTerm (자기참조)

MacroIndicator (독립)
LearnArticle (독립)
User ──< UserArticleProgress >── LearnArticle
```

---

## 8. 구현 우선순위 및 마일스톤

### Phase 1: MVP (목표: 6주)

**목표**: 핵심 화면 동작, 실제 주가 데이터 연동, 용어 사전 기본 기능

| 주차 | 백엔드 | 프론트엔드 |
|------|--------|-----------|
| 1주 | 프로젝트 셋업, DB 스키마 생성, 회원 CRUD | 프로젝트 셋업, 디자인 시스템, 공통 레이아웃 |
| 2주 | 종목 기본 API, 외부 주가 API 연동 | 홈 화면, 시장 지수 위젯 |
| 3주 | 뉴스 수집 파이프라인, 뉴스 API | 종목 검색, 인기 종목 리스트 |
| 4주 | 용어 사전 API, 용어 데이터 시딩 | 종목 상세 페이지 (차트 포함) |
| 5주 | 관심 종목 API, 인증 완성 | 용어 사전 페이지, 인라인 팝오버 |
| 6주 | 통합 테스트, 버그 수정 | 반응형 디자인 완성, 통합 테스트 |

**Phase 1 완료 기준**:
- [ ] 종목 검색 및 현재가 조회 동작
- [ ] 주가 차트 표시 (라인, 캔들스틱)
- [ ] 금융 용어 사전 조회 가능
- [ ] 관심 종목 추가/삭제 (비로그인 포함)
- [ ] 관련 뉴스 표시
- [ ] 회원가입/로그인

---

### Phase 2: AI 기능 (목표: Phase 1 완료 후 4주)

**목표**: AI 분석 리포트 및 주가 예측 기능 완성

| 주차 | 작업 내용 |
|------|----------|
| 7주 | 기술적 지표 계산 서비스 (RSI, MACD, 볼린저 밴드) |
| 8주 | OpenAI API 연동, AI 분석 리포트 생성 |
| 9주 | 뉴스 감성 분석 파이프라인, 거시경제 데이터 수집 |
| 10주 | 주가 방향성 예측 API, Redis 캐싱 적용 |

**Phase 2 완료 기준**:
- [ ] AI 분석 리포트 생성 및 조회 동작
- [ ] RSI, MACD, 볼린저 밴드 차트 오버레이
- [ ] 뉴스 감성 분석 배지 표시
- [ ] 주가 예측 결과 및 근거 표시
- [ ] AI 결과 Redis 캐싱

---

### Phase 3: 고급 기능 (목표: Phase 2 완료 후 4주)

**목표**: 학습 센터, 거시경제 대시보드, 성능 최적화, i18n

| 주차 | 작업 내용 |
|------|----------|
| 11주 | 학습 센터 콘텐츠 등록, 학습 진도 API |
| 12주 | 거시경제 지표 대시보드 |
| 13주 | 성능 최적화 (Redis, CDN, 쿼리 튜닝) |
| 14주 | 영어 i18n 구조 준비, 접근성 개선, 최종 QA |

---

## 9. 리스크 및 제약사항

### 9.1 기술적 리스크

| 리스크 | 심각도 | 완화 방안 |
|--------|--------|----------|
| 한국 실시간 주가 API 접근 (한국투자증권 API는 개인 계좌 연동 필요) | 높음 | Phase 1에서 15분 지연 데이터 사용 (무료 API 활용), 실시간은 Phase 2 이후 검토 |
| OpenAI API 비용 초과 | 중간 | Redis 캐싱 필수, TTL 6시간, 사용량 모니터링 알림 설정 |
| 뉴스 크롤링 차단 (robots.txt, IP 차단) | 중간 | RSS 피드 우선 사용, NewsAPI 유료 플랜 예산 확보 |
| 주가 예측 정확도 낮음 | 낮음 (비즈니스 리스크) | 예측 정확도 이력 투명 공개, 면책조항 강화 |
| Spring Boot + Next.js CORS 설정 | 낮음 | 초기 설정 단계에서 처리 |

### 9.2 법적 고려사항

- **투자 권유 금지**: 모든 AI 분석, 예측 결과에 "이 내용은 투자 권유가 아닙니다" 문구 필수 표시
- **금융투자업 인가**: 실제 주문 기능 없음 (분석/조회만 제공). 주문 기능 추가 시 금융투자업 인가 필요
- **개인정보 처리**: 개인정보처리방침 페이지 필수 (최소 수집, 보관 기간 명시)
- **저작권**: 뉴스 원문 복제 금지. 제목, 요약, 링크만 표시하고 원문은 해당 언론사 링크로 이동
- **데이터 출처 표시**: 사용하는 외부 API의 이용 약관 확인 및 출처 표시 의무 준수

### 9.3 외부 API 의존성

- **주가 API 장애**: 마지막 캐시 데이터 표시 + "실시간 데이터를 가져올 수 없습니다" 배너
- **OpenAI API 장애**: 캐시된 마지막 분석 결과 표시 + 생성 시각 표시
- **뉴스 API 장애**: 마지막 수집 뉴스 표시 + 업데이트 시각 표시
- **모든 외부 API**: Circuit Breaker 패턴 적용 (Resilience4j 사용)

### 9.4 예산 예상

| 항목 | 월 예상 비용 |
|------|------------|
| OpenAI API (GPT-4o) | $50~$200 (사용량에 따라 변동) |
| 서버 호스팅 (AWS/GCP) | $30~$100 |
| 주가 데이터 API (유료 플랜) | $0~$50 |
| 뉴스 API | $0~$50 |
| 합계 | $80~$400/월 |

---

## 10. 미결 사항 (결정 완료 - 2026-02-21)

| # | 항목 | 결정 사항 |
|---|------|----------|
| 1 | **주가 데이터 API** | **한국투자증권 OpenAPI** 사용 (계좌 개설 필요, 무료) |
| 2 | **실시간 vs 지연 데이터** | Phase 1~2는 **15분 지연 데이터**로 진행. 실시간(WebSocket)은 추후 검토 |
| 3 | **초기 용어 데이터** | **외부 데이터셋 활용**하여 시딩 |
| 4 | **AI 분석 갱신 주기** | **6시간마다** 갱신 (하루 약 4회, 비용 절약) |
| 5 | **서비스명** | **FinEasy** 확정 |
| 6 | **로그인 방식** | 이메일/비밀번호 + **Google OAuth + 카카오 OAuth** 추가 |
| 7 | **모바일 앱** | 우선 **웹 반응형만** 개발. 앱은 웹 완성 후 추후 결정 |

---

*이 문서는 @senior-requirements-planner가 생성하였으며, @ui-design-engineer 및 @senior-clean-architect의 입력 문서로 사용됩니다.*
