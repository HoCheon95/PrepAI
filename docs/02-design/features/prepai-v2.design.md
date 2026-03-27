# PrepAI v2 - 설계 문서

> **Feature**: prepai-v2 (AI 문제 생성 전용 플랫폼)
> **Architecture**: Option C — 실용적 균형
> **Date**: 2026-03-25
> **Status**: Draft

---

## Context Anchor

| Key | Value |
|-----|-------|
| **WHY** | 문제 생성과 시험지 렌더링이 한 앱에 있어 무겁고 느림 → 역할 분리로 각각 빠르게 |
| **WHO** | 한국 고등학교 영어 선생님 (수능·모의고사 대비 문제 제작) |
| **RISK** | DB 스키마(questions + answers 2개 테이블)를 잘 설계해야 ExamMaker가 올바르게 읽을 수 있음. DB는 아직 미생성 — 연동은 DB 준비 후 진행 |
| **SUCCESS** | PrepAI는 문제 생성 + DB 저장만 담당. 시험지 관련 코드가 PrepAI에 없음 |
| **SCOPE** | Phase 1: AI 품질·안정성 / Phase 2: 생성 결과 검토 UX + DB 저장 / Phase 3: DB API (ExamMaker 연동용) |

---

## 1. Overview

### 1.1 선택된 아키텍처: Option C — 실용적 균형

`reviewResult.jsp` 신규 생성으로 핵심 역할 분리를 달성하면서, `QuestionSaveService` 하나로 question + answer 동시 저장. 오버엔지니어링 없이 플랜 목표를 달성한다.

### 1.2 현재 코드베이스 상태

```
이미 완료된 것 (변경 불필요):
  ✅ ResponseValidator.java — 형식 검증 + 최대 3회 재시도
  ✅ GeminiService.java     — Gemini 2.5 Flash API 호출
  ✅ application.properties — API 키 환경변수 분리 완료
  ✅ questionForm.jsp        — 입력 폼 (12개 유형, 파일 업로드)

수정 완료:
  🔧 PromptBuilder.java     — FR-13/FR-14: PDF 추출 품질 개선 (appendPassageSource HINT 수정)

변경/신규 필요:
  🔧 GeminiController.java  — "result" → "reviewResult", 저장 API 추가
  🔧 pom.xml                — JPA + H2 의존성 추가
  🔧 application.properties — H2 datasource 설정 추가
  🆕 reviewResult.jsp        — 중간 검토 뷰 (핵심 신규)
  🆕 reviewResult.js         — 파서 + 버튼 인터랙션
  🆕 QuestionSaveService.java — question + answer DB 저장
  🆕 Question.java           — JPA Entity
  🆕 Answer.java             — JPA Entity
  🆕 QuestionRepository.java — Spring Data JPA
  🆕 AnswerRepository.java   — Spring Data JPA
```

### 1.3 AI 응답 형식 (기존 태그 시스템)

현재 Gemini 응답은 다음 구조로 오며, 이것을 파싱해서 저장한다:

```
[[QUESTION]]
(문제 번호. 문제 텍스트 한국어)
[[PASSAGE]]
(영어 지문)
[[OPTIONS]]
(1) 선택지1
(2) 선택지2
(3) 선택지3
(4) 선택지4
(5) 선택지5
[[ANSWER]]
(정답 번호)
[[EXPLANATION]]
(해설 한국어)
---SEP---
```

---

## 2. Architecture

### 2.1 전체 흐름

```
questionForm.jsp
      ↓ POST /api/generate-questions (multipart)
GeminiController.generateQuestions()
      ├→ PromptBuilder.build()              (기존)
      ├→ GeminiService.getGeminiResponse()  (기존)
      └→ ResponseValidator.validateWithRetry() (기존)
              ↓ aiResponse (raw string)
      ModelAndView("reviewResult")          ← 변경 포인트
              ↓
reviewResult.jsp + reviewResult.js
      ├─ 파싱: ---SEP--- 분리 → 문제 카드 렌더링
      ├─ [재생성] → POST /api/regenerate-question
      ├─ [복사하기] → Clipboard API
      └─ [DB에 저장] → POST /api/save-questions
              ↓
GeminiController.saveQuestions()
      └→ QuestionSaveService.save()
              ├─ QuestionRepository → questions 테이블
              └─ AnswerRepository  → answers 테이블

ExamMaker 연동 (Phase 3 — DB 준비 후):
      GET /api/questions
      GET /api/questions/{id}
      GET /api/answers/{questionId}
```

### 2.2 패키지 구조

```
com.example.demo/
├── controller/
│   ├── GeminiController.java    (수정)
│   └── PageController.java      (기존 유지)
├── geminiAI/
│   └── GeminiService.java       (기존 유지)
├── service/
│   ├── PromptBuilder.java       (기존 유지)
│   ├── ResponseValidator.java   (기존 유지)
│   └── QuestionSaveService.java (신규)
├── entity/
│   ├── Question.java            (신규)
│   └── Answer.java              (신규)
├── repository/
│   ├── QuestionRepository.java  (신규)
│   └── AnswerRepository.java    (신규)
└── DemoApplication.java         (기존 유지)
```

---

## 3. DB Schema

### 3.1 questions 테이블

```sql
CREATE TABLE questions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_type   VARCHAR(100),   -- 문제 유형 (빈칸추론, 주제파악 등)
    question_number INT,            -- 문제 순서 (1, 2, 3...)
    passage         TEXT,           -- 영어 지문 ([[PASSAGE]] 내용)
    input_mode      VARCHAR(50),    -- 모의고사/외부지문/교과서
    question_text   TEXT,           -- 문제 본문 ([[QUESTION]] 내용)
    options         TEXT,           -- 선택지 JSON (["(1) ...","(2) ...",…])
    created_at      TIMESTAMP DEFAULT NOW()
);
```

### 3.2 answers 테이블

```sql
CREATE TABLE answers (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id     BIGINT NOT NULL,  -- questions.id 참조
    answer          VARCHAR(10),      -- 정답 번호 ([[ANSWER]] 내용)
    explanation     TEXT,             -- 해설 ([[EXPLANATION]] 내용)
    created_at      TIMESTAMP DEFAULT NOW()
);
```

### 3.3 관계

```
questions 1 ──── 1 answers
  id  ←────────── question_id
```

---

## 4. Component Design

### 4.1 GeminiController.java 변경 사항

**변경 1 — 라우팅**: `"result"` → `"reviewResult"` + examType 전달

```java
ModelAndView mav = new ModelAndView("reviewResult");  // 핵심 변경
mav.addObject("examResult", aiResponse);
mav.addObject("examType", examType);                  // 저장 시 필요
return mav;
```

**추가 1 — 저장 API**:

```java
@PostMapping("/api/save-questions")
public ResponseEntity<Map<String, Object>> saveQuestions(
    @RequestBody Map<String, Object> payload)
// payload: { examType, questions: [{questionType, questionText, passage, options, answer, explanation}] }
// response: { savedCount: N, ids: [1,2,3,...] }
```

**추가 2 — 단일 재생성 API** (기존 `buildSingle` 활용):

```java
@PostMapping("/api/regenerate-question")
public ResponseEntity<String> regenerateQuestion(
    @RequestParam String questionType,
    @RequestParam String passageText,
    @RequestParam(required = false) String difficultyLevel)
// response: 원시 AI 응답 텍스트 (단일 문제 ---SEP--- 포함)
```

**추가 3 — ExamMaker 연동 API** (Phase 3):

```java
@GetMapping("/api/questions")
@GetMapping("/api/questions/{id}")
@GetMapping("/api/answers/{questionId}")
```

### 4.2 PromptBuilder.java — FR-13/FR-14 PDF 추출 품질 개선

**변경 위치**: `appendPassageSource()` 메서드 내 HINT 3줄

**문제**: 기존 HINT가 `[43~45]` 섹션 헤더를 문제 번호로 오인하고, 단락 구분자 `(A)(B)(C)(D)`를 제거하며, 한국어 문제 텍스트를 누락시킴

**변경 내용**:
```java
// Before
p.append("HINT: Locate the number, skip the Korean text, and EXTRACT ONLY THE ENGLISH PASSAGE.\n\n");

// After (3개 HINT로 교체)
p.append("HINT: A real question ALWAYS starts with 'N.' (e.g., '43.'). Do NOT treat section headers like '[43~45]' as question numbers — they are just group labels.\n");
p.append("HINT: For each Target Question Number, find the line starting with 'N.' and put that Korean question text into [[QUESTION]]. Extract the full English passage into [[PASSAGE]].\n");
p.append("HINT: If the passage contains labeled sections like (A), (B), (C), (D), you MUST preserve ALL section labels exactly as they appear inside [[PASSAGE]].\n\n");
```

**FR-13 — 단락 구분자 보존**: `(A)`, `(B)`, `(C)`, `(D)` 레이블을 [[PASSAGE]]에 원문 그대로 유지
**FR-14 — 문제 번호 패턴**: `번호.` (예: `43.`)로 시작하는 줄만 진짜 문제로 인식. `[43~45]` 헤더 무시

---

### 4.3 QuestionSaveService.java

```
책임: AI 응답 raw string 파싱 → questions + answers 테이블 저장

입력:
  - rawAiResponse: String  (---SEP--- 구분 전체 응답)
  - examType: String       (모의고사/외부지문/교과서)
  - questionTypes: List<String> (각 문제의 유형, 순서대로)

처리 흐름:
  1. rawAiResponse.split("---SEP---") → 블록 배열
  2. 각 블록 파싱:
     [[QUESTION]]  → questionText
     [[PASSAGE]]   → passage
     [[OPTIONS]]   → options (JSON 배열로 변환)
     [[ANSWER]]    → answer
     [[EXPLANATION]] → explanation
  3. Question 엔티티 생성 → questionRepository.save(question) → savedId
  4. Answer 엔티티 생성 (questionId = savedId) → answerRepository.save(answer)
  5. 저장된 Question 목록 반환

에러 처리:
  - 개별 문제 파싱/저장 실패 시 → 해당 문제만 스킵 + 경고 로그
  - 나머지 문제는 계속 저장 진행
```

### 4.4 Question.java (JPA Entity)

```java
@Entity
@Table(name = "questions")
public class Question {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_type")
    private String questionType;

    @Column(name = "question_number")
    private Integer questionNumber;

    @Column(columnDefinition = "TEXT")
    private String passage;

    @Column(name = "input_mode")
    private String inputMode;

    @Column(name = "question_text", columnDefinition = "TEXT")
    private String questionText;

    @Column(columnDefinition = "TEXT")
    private String options;        // JSON 문자열

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() { this.createdAt = LocalDateTime.now(); }

    // getters/setters
}
```

### 4.5 Answer.java (JPA Entity)

```java
@Entity
@Table(name = "answers")
public class Answer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    private String answer;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() { this.createdAt = LocalDateTime.now(); }

    // getters/setters
}
```

### 4.6 reviewResult.jsp 구조

```html
<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <title>문제 검토</title>
    <link rel="stylesheet" href=".../css/reviewResult.css">
    <script src=".../js/reviewResult.js" defer></script>
</head>
<body>
  <div class="review-container">

    <!-- 상단 액션 바 -->
    <div class="action-bar">
      <span id="question-count">총 N개 문제</span>
      <div class="action-buttons">
        <button onclick="copyAll()">복사하기</button>
        <button onclick="saveToDb()" class="btn-primary">DB에 저장</button>
        <a href="/question-form" class="btn">다시 생성하기</a>
      </div>
    </div>

    <!-- 문제 카드 목록 (JS가 동적 렌더링) -->
    <div id="question-list"></div>

  </div>

  <!-- 원본 데이터 -->
  <textarea id="raw-data" style="display:none">${examResult}</textarea>
  <input type="hidden" id="exam-type" value="${examType}">
</body>
</html>
```

### 4.7 reviewResult.js 핵심 로직

```javascript
// 파싱 함수: 태그 사이 내용 추출
function extractBetween(block, startTag, endTag) {
    const start = block.indexOf(startTag);
    if (start === -1) return '';
    const contentStart = start + startTag.length;
    const end = endTag ? block.indexOf(endTag, contentStart) : block.length;
    return block.substring(contentStart, end === -1 ? block.length : end).trim();
}

// 전체 파싱
function parseAiResponse(raw) {
    return raw.split('---SEP---')
        .map(b => b.trim()).filter(Boolean)
        .map((block, idx) => ({
            index:       idx,
            question:    extractBetween(block, '[[QUESTION]]',    '[[PASSAGE]]'),
            passage:     extractBetween(block, '[[PASSAGE]]',     '[[OPTIONS]]'),
            options:     extractBetween(block, '[[OPTIONS]]',     '[[ANSWER]]'),
            answer:      extractBetween(block, '[[ANSWER]]',      '[[EXPLANATION]]'),
            explanation: extractBetween(block, '[[EXPLANATION]]', null)
        }));
}

// DB 저장
async function saveToDb() {
    const questions = collectCurrentState();
    const res = await fetch('/api/save-questions', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({
            examType: document.getElementById('exam-type').value,
            questions
        })
    });
    const data = await res.json();
    alert(`${data.savedCount}개 문제가 DB에 저장되었습니다.`);
}

// 복사하기
function copyAll() {
    const text = collectCurrentState()
        .map((q, i) => [
            `[문제 ${i+1}] ${q.question}`,
            q.passage,
            q.options,
            `정답: ${q.answer}`
        ].join('\n')).join('\n\n---\n\n');
    navigator.clipboard.writeText(text).then(() => alert('복사 완료!'));
}
```

---

## 5. API 명세

### 5.1 저장 API

**POST `/api/save-questions`**

Request Body:
```json
{
  "examType": "모의고사",
  "questions": [
    {
      "questionType": "빈칸추론",
      "questionNumber": 1,
      "questionText": "다음 빈칸에 들어갈...",
      "passage": "Although the...",
      "options": "(1) curiosity\n(2) anxiety\n...",
      "answer": "3",
      "explanation": "이 글은..."
    }
  ]
}
```

Response:
```json
{ "savedCount": 3, "ids": [1, 2, 3] }
```

### 5.2 ExamMaker 연동 API (Phase 3)

**GET `/api/questions`**
```json
[
  { "id": 1, "questionType": "빈칸추론", "questionText": "...", "passage": "...", "options": "...", "createdAt": "..." },
  ...
]
```

**GET `/api/questions/{id}`**  — 단일 문제 상세

**GET `/api/answers/{questionId}`**
```json
{ "id": 1, "questionId": 1, "answer": "3", "explanation": "이 글은..." }
```

---

## 6. pom.xml 추가 의존성

```xml
<!-- JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- H2 (파일 모드 — DB 준비 전 임시) -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

---

## 7. application.properties 추가 설정

```properties
# H2 파일 모드 (나중에 MySQL로 교체)
spring.datasource.url=jdbc:h2:file:./prepai-db
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect

# H2 콘솔 (개발용)
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

---

## 8. 예외 처리 설계

| 상황 | 처리 |
|------|------|
| AI 응답 파싱 실패 (태그 없음) | 해당 블록 스킵 + 경고 로그, 나머지 저장 계속 |
| DB 저장 실패 | 클라이언트에 에러 메시지 반환, 부분 저장 결과 포함 |
| 재생성 API 실패 | 기존 카드 유지, 화면에 토스트 에러 메시지 |
| 빈 문제 목록 저장 시도 | JS 클라이언트 validation: 0개면 저장 버튼 비활성화 |
| ResponseValidator 3회 실패 | 기존 동작 유지 (RuntimeException → 에러 페이지) |

---

## 9. result.jsp 처리 방침

기존 `result.jsp`는 **삭제하지 않고 보존**한다.
- `GeminiController`에서 `"reviewResult"`로 라우팅 변경 후 result.jsp는 더 이상 호출되지 않음
- A4 레이아웃, html2pdf.js 코드는 result.jsp에 그대로 남음
- ExamMaker 개발 시 참고 자료로 활용

---

## 10. 검증 체크리스트

- [ ] 문제 생성 후 `reviewResult.jsp` 로드 (result.jsp 대신)
- [ ] `---SEP---` 파싱이 정확히 동작하고 문제 카드 수 일치
- [ ] [재생성] 버튼: 해당 카드만 교체됨
- [ ] [복사하기] 버튼: 클립보드에 전체 문제 텍스트 복사됨
- [ ] [DB에 저장]: questions + answers 테이블 각각 저장 확인 (H2 콘솔)
- [ ] `GET /api/questions` 저장된 문제 반환
- [ ] `GET /api/answers/{id}` 정답+해설 반환
- [ ] `GeminiController`에 A4/PDF 관련 코드 없음

---

## 11. Implementation Guide

### 11.1 Module Map

| Module | 작업 | 대상 파일 |
|--------|------|-----------|
| **M1** DB 기반 | JPA + H2 설정, 엔티티, 리포지토리 | `pom.xml`, `application.properties`, `Question.java`, `Answer.java`, `QuestionRepository.java`, `AnswerRepository.java` |
| **M2** 검토 뷰 | 라우팅 변경 + 검토 화면 구현 | `GeminiController.java`, `reviewResult.jsp`, `reviewResult.js` |
| **M3** 저장 기능 | 저장 서비스 + 저장 API | `QuestionSaveService.java`, `GeminiController.java` (+saveQuestions) |
| **M4** ExamMaker API | 조회 REST API | `GeminiController.java` (+getQuestions, +getAnswers) |

### 11.2 Session Guide

```
세션 1: M1 (DB 기반)
  pom.xml + application.properties 수정
  → Question.java, Answer.java 엔티티
  → QuestionRepository.java, AnswerRepository.java
  → 앱 기동 + H2 콘솔(/h2-console)에서 테이블 생성 확인

세션 2: M2 (검토 뷰)
  GeminiController "result" → "reviewResult" 1줄 변경
  → reviewResult.jsp 구조 작성
  → reviewResult.js 파서 + 카드 렌더링 구현
  → 실제 문제 생성해서 카드 표시 확인

세션 3: M3 (저장 기능)
  QuestionSaveService.java 작성
  → GeminiController POST /api/save-questions 추가
  → reviewResult.js 저장/복사 버튼 연동
  → H2 콘솔에서 저장 데이터 직접 확인

세션 4: M4 (ExamMaker API) — DB 준비 완료 후 진행
  GeminiController GET 엔드포인트 추가
  → ExamMaker에서 호출 테스트
```

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 1.0 | 2026-03-25 | 초기 설계 — Option C 선택, questions/answers 2테이블 구조 확정 | 개발팀 |
| 1.1 | 2026-03-26 | FR-13/FR-14 반영: PromptBuilder HINT 수정 — (A)(B)(C)(D) 단락 구분자 보존, `번호.` 패턴 문제 번호 인식 | 개발팀 |
