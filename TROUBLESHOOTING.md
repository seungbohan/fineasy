# FinEasy 트러블슈팅 기록

## 1. CORS 에러 — `allowedOrigins cannot contain "*" when allowCredentials is true`

**환경:** EC2 배포 후 프론트엔드에서 로그인/API 호출 실패

**오류 메시지:**
```
java.lang.IllegalArgumentException: When allowCredentials is true, allowedOrigins cannot contain the special value "*"
```

**원인:**
- EC2 `.env`에 `CORS_ALLOWED_ORIGINS=*`로 설정
- Spring Security의 CORS 설정에서 `allowCredentials(true)`와 `allowedOrigins("*")`는 동시 사용 불가

**해결:**
- `SecurityConfig.java`에서 origins에 `*`가 포함되면 `setAllowedOriginPatterns(List.of("*"))`를 사용하도록 분기 처리
```java
if (origins.contains("*")) {
    config.setAllowedOriginPatterns(List.of("*"));
} else {
    config.setAllowedOrigins(origins);
}
```

**파일:** `backend/src/main/java/com/fineasy/security/SecurityConfig.java`

---

## 2. 포트 충돌 — `Port 8080 already in use`

**환경:** EC2에서 백엔드 Docker 컨테이너 재시작 시

**오류 메시지:**
```
Web server failed to start. Port 8080 was already in use.
```

**원인:**
- 이전에 호스트에서 직접 실행한 Java 프로세스(PID 2288)가 8080 포트를 점유 중

**해결:**
```bash
kill 2288
docker compose up -d backend
```

**교훈:** Docker 컨테이너 외부에서 직접 실행한 프로세스가 남아있지 않은지 확인

---

## 3. 프론트엔드 변경사항 미반영 — Docker 빌드 캐시

**환경:** EC2에서 `docker compose up -d frontend` 후 이전 UI 그대로 표시

**원인:**
- `docker compose up -d`는 이미지가 존재하면 재빌드하지 않음
- 코드 변경 후에도 캐시된 이미지를 계속 사용

**해결:**
```bash
docker compose build --no-cache frontend
docker compose up -d frontend
```

**교훈:** 코드 변경 후 반드시 `--no-cache`로 빌드

---

## 4. 브라우저 캐시 — 배포 후에도 이전 UI 표시

**환경:** EC2 배포 완료 확인 후에도 브라우저에서 이전 UI 표시

**원인:**
- 브라우저가 이전 JavaScript 번들을 캐시하고 있음
- Next.js의 정적 자산이 브라우저에 캐시됨

**해결:**
- `Ctrl + Shift + R` (강력 새로고침)으로 캐시 무시 후 페이지 로드

---

## 5. Git 추적 파일 제거 — `.gitignore` 추가 후에도 파일이 추적됨

**환경:** `.claude/`, `CLAUDE.md` 등을 `.gitignore`에 추가했지만 여전히 git에 포함

**원인:**
- `.gitignore`는 이미 추적(tracked) 중인 파일에는 적용되지 않음

**해결:**
```bash
git rm -r --cached .claude/
git rm --cached CLAUDE.md
git rm -r --cached frontend/.claude/
```

**교훈:** `.gitignore`에 추가한 후 `git rm --cached`로 추적 해제 필요

---

## 6. DART API SSL 핸드셰이크 실패 — Docker 컨테이너 내부

**환경:** EC2 Docker 컨테이너에서 DART API(opendart.fss.or.kr) 호출 시

**오류 메시지:**
```
javax.net.ssl.SSLHandshakeException: Received fatal alert: handshake_failure
[r.netty.http.client.HttpClientConnect] The connection observed an error
Failed to download DART corpCode.xml: Retries exhausted: 2/2
```

**원인:**
- DART 서버가 `TLS_RSA_WITH_AES_128_GCM_SHA256` cipher만 지원
- Java 17은 보안상 `TLS_RSA_*` cipher를 기본 비활성화 (`jdk.tls.disabledAlgorithms`에 포함)
- EC2 호스트의 curl로는 정상 연결되지만, Docker 컨테이너 내 Java Netty에서만 실패

**확인 방법:**
```bash
# 호스트에서 DART 서버 cipher 확인
echo | openssl s_client -connect opendart.fss.or.kr:443 2>&1 | grep Cipher
# 결과: Cipher is AES128-GCM-SHA256 (= TLS_RSA_WITH_AES_128_GCM_SHA256)

# 컨테이너 내 Java 비활성화 알고리즘 확인
docker exec fineasy-backend grep -A 5 "^jdk.tls.disabledAlgorithms=" /opt/java/openjdk/conf/security/java.security
# 결과에 TLS_RSA_* 포함
```

**해결:**

1. `Dockerfile`에서 `jdk.tls.disabledAlgorithms`에서 `TLS_RSA_*` 제거:
```dockerfile
RUN sed -i '/^jdk.tls.disabledAlgorithms=/,/^$/{ s/TLS_RSA_\*, //g; s/ECDH, //g; }' \
    /opt/java/openjdk/conf/security/java.security
```

2. `DartApiClient.java`에서 WebClient에 커스텀 SSL 설정 추가:
```java
SslContext sslContext = SslContextBuilder.forClient()
        .trustManager(InsecureTrustManagerFactory.INSTANCE)
        .protocols("TLSv1.2", "TLSv1.3")
        .build();
HttpClient httpClient = HttpClient.create()
        .secure(spec -> spec.sslContext(sslContext));
builder.clientConnector(new ReactorClientHttpConnector(httpClient));
```

**파일:** `backend/Dockerfile`, `backend/src/main/java/com/fineasy/external/dart/DartApiClient.java`

---

## 7. DART XML 파싱 OOM — `OutOfMemoryError: Java heap space`

**환경:** EC2(916MB RAM)에서 DART corpCode.xml 파싱 시

**오류 메시지:**
```
java.lang.OutOfMemoryError: Java heap space
  at com.sun.org.apache.xerces.internal.dom.DeferredDocumentImpl.getNodeObject
  at DartCorpCodeSyncService.parseCorpCodeXml(DartCorpCodeSyncService.java:207)
```

**원인:**
- DART corpCode.xml ZIP(3.5MB)을 압축 해제하면 수십MB
- 115,439개 기업 데이터를 DOM 파서로 전부 메모리에 로드
- EC2 인스턴스 메모리가 916MB로 매우 제한적

**해결:**
- DOM 파서를 SAX 스트리밍 파서로 변경
- SAX는 한 요소씩 순차 처리하므로 메모리 사용량 최소화
```java
// Before (DOM) — 전체 XML 트리를 메모리에 로드
DocumentBuilder builder = factory.newDocumentBuilder();
Document doc = builder.parse(new ByteArrayInputStream(xmlBytes));  // OOM!

// After (SAX) — 스트리밍 방식으로 한 요소씩 처리
SAXParser parser = factory.newSAXParser();
parser.parse(new ByteArrayInputStream(xmlBytes), new DefaultHandler() {
    @Override
    public void endElement(String uri, String localName, String qName) {
        if ("list".equals(qName) && stockCode != null && !stockCode.isBlank()) {
            entities.add(new DartCorpCodeEntity(corpCode, corpName, stockCode));
        }
    }
});
```

**결과:** 115,439개 파싱 → 3,949개 상장사 추출 및 DB 저장 성공

**파일:** `backend/src/main/java/com/fineasy/external/dart/DartCorpCodeSyncService.java`

---

## 8. AI 브리핑 로딩 실패 — `AI 브리핑을 불러올 수 없습니다`

**환경:** 메인 페이지에서 관심종목 AI 브리핑 카드

**원인 (복합적):**
1. 백엔드 초기화 중 (FRED/DART 동기화에 3~5분 소요) — 서버 준비 전 요청
2. 관심종목에 태깅된 뉴스가 DB에 아직 없음
3. OpenAI API 키가 컨테이너에 전달되지 않았을 수 있음

**확인 방법:**
```bash
# OpenAI API 키 확인
docker exec fineasy-backend env | grep -i openai

# 백엔드 로그에서 AI 관련 에러 확인
docker logs fineasy-backend 2>&1 | grep -i 'openai\|briefing\|AI'
```

**해결:**
- 백엔드 완전 시작 후 재시도 (초기화 완료까지 대기)
- `.env`에 `OPENAI_API_KEY` 확인
- 뉴스 수집 스케줄러가 관심종목 관련 뉴스를 수집할 때까지 대기
