# PrepAI v2 Design Document

> **Summary**: PromptBuilder + ResponseValidator 분리, 로딩 UX, 재생성/편집, DB 저장 기능 설계 (Option C - 실용적 균형)
>
> **Project**: PrepAI
> **Version**: 2.0.0
> **Author**: 개발팀
> **Date**: 2026-03-24
> **Status**: Draft
> **Planning Doc**: [prepai-v2.plan.md](../01-plan/features/prepai-v2.plan.md)

---

## Context Anchor

> Plan 문서에서 복사. Design → Do 컨텍스트 연속성 확보용.

| Key | Value |
|-----|-------|
| **WHY** | 교사가 직접 문제를 만드는 데 소요하는 시간 절감 + AI 품질 불신 해소 |
| **WHO** | 한국 고등학교 영어 선생님 (수능·모의고사 대비 문제 제작) |
| **RISK** | Gemini API 응답 형식 불안정, DB 도입 시 기존 JSP 구조와의 정합성 |
| **SUCCESS** | 형식 오류율 < 5%, 교사 피드백 만족도 4/5 이상, 문제 저장·재사용 가능 |
| **SCOPE** | Phase 1: 품질·안정성 / Phase 2: UX 개선 / Phase 3: DB 저장 |

---

## 1. Overview

### 1.1 Design Goals

- `GeminiController`의 비대한 프롬프트 빌드 로직을 `PromptBuilder`로 분리
- 응답 형식 검증과 재시도 책임을 `ResponseValidator`에 위임
- 기존 JSP/JS 파일에 최소 침습적으로 UX 기능(로딩, 재생성, 편집) 추가
- Phase 3에서 JPA + H2로 문제 저장 기능 도입 (기존 구조 영향 최소화)

### 1.2 Design Principles

- **Single Responsibility**: 프롬프트 생성 / 검증 / 저장 책임을 각각 분리
- **Backward Compatibility**: 기존 `/api/generate-questions` 동작 유지
- **Minimal Refactoring**: 새 클래스 추가 위주, 기존 클래스 대규모 변경 지양
- **gemini-prompt-rules.skill.md 준수**: 태그 시스템, 포맷 규칙 유지

---

## 2. Architecture

### 2.0 Architecture Comparison

| Criteria | Option A: 최소 변경 | Option B: 클린 | **Option C: 실용적 (선택)** |
|----------|:--:|:--:|:--:|
| 신규 파일 | 0 | 10+ | 6 |
| 변경 파일 | 3 | 2 | 5 |
| 복잡도 | 낮음 | 높음 | **중간** |
| 유지보수성 | 중간 | 높음 | **높음** |
| 작업량 | 낮음 | 높음 | **중간** |

**선택**: Option C — 기존 구조를 유지하면서 역할 분리. 과도한 리팩토링 없이 유지보수성 확보.

### 2.1 Component Diagram

```
[Browser]
    │  POST /api/generate-questions
    ▼
[GeminiController]
    │  PromptBuilder.build(questionTypes, examType, ...)
    ├──▶ [PromptBuilder]  ← 신규 @Component
    │       └── 유형별 프롬프트 조립 반환
    │
    │  geminiService.getGeminiResponse(prompt, file)
    ├──▶ [GeminiService]  ← 기존 (maxOutputTokens 추가됨)
    │       └── Gemini 2.5 Flash API 호출
    │
    │  validator.validate(response) → 실패 시 최대 3회 재시도
    ├──▶ [ResponseValidator]  ← 신규 @Component
    │
    │  ModelAndView("result")
    ▼
[result.jsp]  ← 재생성 버튼 + 인라인 편집 UI 추가
    │
    │  POST /api/regenerate-question  (Phase 2)
    ▼
[GeminiController.regenerateQuestion()]  ← 신규 엔드포인트

[Phase 3 추가]
    │  POST /api/save-questions
    ▼
[QuestionSetRepository]  ← JPA
    ▼
[H2 DB] (question_set, question 테이블)
```

### 2.2 Data Flow — 문제 생성

```
사용자 폼 제출
  → GeminiController.generateQuestions()
  → PromptBuilder.build()로 프롬프트 생성
  → GeminiService.getGeminiResponse() 호출
  → ResponseValidator.validate() 결과 검증
      ├── 성공: result.jsp 렌더링
      └── 실패: 재시도 (최대 3회) → 3회 실패 시 에러 페이지
```

### 2.3 Dependencies

| Component | Depends On | Purpose |
|-----------|-----------|---------|
| GeminiController | PromptBuilder, ResponseValidator, GeminiService | 요청 처리 조율 |
| PromptBuilder | - (순수 로직) | 문제 유형별 프롬프트 조립 |
| ResponseValidator | - (순수 로직) | [[TAG]] + ---SEP--- 형식 검증 |
| GeminiService | google-genai SDK | Gemini API 호출 |
| QuestionSetRepository (P3) | JPA, QuestionSet Entity | DB 저장/조회 |

---

## 3. Data Model

### 3.1 Phase 3 JPA Entities

```java
// QuestionSet.java — 문제 세트 (한 번 생성한 결과 전체)
@Entity
public class QuestionSet {
    @Id @GeneratedValue
    private Long id;

    private String title;          // 예: "2025 수능 18번 빈칸추론 변형"
    private String examType;       // 모의고사 / 외부지문 / 교과서
    private String difficultyLevel;
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "questionSet", cascade = CascadeType.ALL)
    private List<Question> questions;
}

// Question.java — 개별 문제
@Entity
public class Question {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne
    private QuestionSet questionSet;

    private String questionType;   // 빈칸추론, 주제파악, ...
    private String questionText;   // [[QUESTION]] 내용
    private String passage;        // [[PASSAGE]] 내용
    private String options;        // [[OPTIONS]] 내용 (줄바꿈 포함 raw text)
    private String answer;         // [[ANSWER]] 내용
    private String explanation;    // [[EXPLANATION]] 내용
}
```

### 3.2 Entity Relationships

```
[QuestionSet] 1 ──── N [Question]
```

### 3.3 DB Schema (H2 자동 생성)

```sql
-- spring.jpa.hibernate.ddl-auto=update 로 자동 생성됨
-- H2 파일 모드: spring.datasource.url=jdbc:h2:file:./data/prepai

question_set (id, title, exam_type, difficulty_level, created_at)
question     (id, question_set_id, question_type, question_text,
              passage, options, answer, explanation)
```

---

## 4. API Specification

### 4.1 Endpoint List

| Method | Path | Description | 단계 |
|--------|------|-------------|------|
| POST | `/api/generate-questions` | 기존 문제 생성 (유지) | 기존 |
| POST | `/api/regenerate-question` | 단일 문제 재생성 | Phase 2 |
| POST | `/api/save-questions` | 문제 세트 DB 저장 | Phase 3 |
| GET | `/api/question-sets` | 저장된 세트 목록 | Phase 3 |
| GET | `/api/question-sets/{id}` | 저장된 세트 상세 조회 | Phase 3 |
| GET | `/question-list` | 목록 페이지 (JSP) | Phase 3 |

### 4.2 Detailed Specification

#### `POST /api/regenerate-question` (Phase 2)

**Request (form-data):**
```
questionType   : 빈칸추론
passageText    : (원본 지문 텍스트)
difficultyLevel: 중
```

**Response:** `result.jsp`와 동일한 형식의 단일 문제 HTML fragment
또는 raw AI 응답 텍스트 (JS에서 파싱)

#### `POST /api/save-questions` (Phase 3)

**Request (JSON):**
```json
{
  "title": "2025 수능 18번 변형",
  "examType": "모의고사",
  "difficultyLevel": "중",
  "rawResponse": "[[QUESTION]]...---SEP---[[QUESTION]]..."
}
```

**Response (200 OK):**
```json
{ "id": 1, "message": "저장 완료" }
```

---

## 5. UI/UX Design

### 5.1 로딩 인디케이터 (Phase 2) — `questionForm.jsp`

```
[폼 제출 버튼 클릭]
    ↓
┌─────────────────────────────────┐
│  문제를 생성하고 있습니다...      │
│  ████████████░░░░  AI 분석 중   │
│  예상 시간: 20~60초              │
└─────────────────────────────────┘
  (버튼 disabled, 폼 오버레이 표시)
```

- 폼 submit 이벤트에서 오버레이 div `display:block`
- fetch/XHR 완료 시 오버레이 제거

### 5.2 재생성 버튼 + 인라인 편집 (Phase 2) — `result.jsp`

```
┌──────────────────────────────────────────────┐
│ 1번. 다음 빈칸에 들어갈 말로 가장 적절한 것은?  │
│                                              │
│ [지문 내용...]                                │
│                                              │
│ ① opt1  ② opt2  ③ opt3  ④ opt4  ⑤ opt5    │
│ 정답: ③                                      │
│                                              │
│ [✏️ 편집]  [🔄 재생성]                        │  ← 신규 버튼
└──────────────────────────────────────────────┘
```

- **편집 버튼**: 해당 문제 div를 `contenteditable="true"`로 전환
- **재생성 버튼**: 해당 문제의 `questionType`, `passageText`를 추출하여 `/api/regenerate-question` 호출 후 해당 문제 div 교체

### 5.3 저장/목록 (Phase 3) — `result.jsp`, `questionList.jsp`

```
result.jsp 하단:
  [💾 이 문제 세트 저장하기]  → POST /api/save-questions

questionList.jsp:
  ┌─────────────────────────────────────────┐
  │ 📂 저장된 문제 목록                       │
  ├─────────────────────────────────────────┤
  │ 2026-03-24 │ 수능 18번 변형 │ 중 │ [보기] │
  │ 2026-03-23 │ 외부지문 연습  │ 상 │ [보기] │
  └─────────────────────────────────────────┘
```

---

## 6. PromptBuilder 상세 설계

### 6.1 클래스 구조

```java
@Component
public class PromptBuilder {

    // 진입점: GeminiController에서 호출
    public String build(
        String examType,
        String passageText,
        List<String> questionNos,
        List<String> questionTypes,
        String difficultyLevel,
        List<String> modifications,
        boolean hasFile,
        Map<String, String> counts
    ) {
        StringBuilder prompt = new StringBuilder();
        appendOutputFormat(prompt);        // 공통 출력 포맷
        appendExamConditions(prompt, difficultyLevel, modifications);
        appendPassageSource(prompt, examType, passageText, questionNos, hasFile);
        appendQuestionTypes(prompt, questionTypes, counts);
        return prompt.toString();
    }

    private void appendOutputFormat(StringBuilder p) { ... }
    private void appendExamConditions(StringBuilder p, ...) { ... }
    private void appendPassageSource(StringBuilder p, ...) { ... }
    private void appendQuestionTypes(StringBuilder p, ...) { ... }
    private void appendTypeRule(StringBuilder p, String type) { ... }
}
```

### 6.2 단일 문제 재생성용 프롬프트

```java
public String buildSingle(String questionType, String passageText, String difficultyLevel) {
    // 1개 문제만 생성하는 축약 프롬프트
    // appendOutputFormat + appendTypeRule(questionType) 만 포함
}
```

---

## 7. ResponseValidator 상세 설계

### 7.1 클래스 구조

```java
@Component
public class ResponseValidator {

    private static final int MAX_RETRY = 3;

    // GeminiService + PromptBuilder를 받아 검증+재시도 일괄 처리
    public String validateWithRetry(
        String initialResponse,
        String prompt,
        MultipartFile file,
        GeminiService geminiService
    ) {
        String response = initialResponse;
        for (int i = 0; i < MAX_RETRY; i++) {
            if (isValid(response)) return response;
            log.warn("응답 형식 오류 ({}회차), 재시도...", i + 1);
            response = geminiService.getGeminiResponse(prompt, file);
        }
        if (!isValid(response)) throw new RuntimeException("AI 응답 형식 오류: 3회 재시도 실패");
        return response;
    }

    public boolean isValid(String response) {
        // 1. [[QUESTION]] 태그 최소 1개 존재
        // 2. [[OPTIONS]] 존재
        // 3. [[ANSWER]] 존재
        // 4. ---SEP--- 존재 (다중 문제일 때)
        return response.contains("[[QUESTION]]")
            && response.contains("[[OPTIONS]]")
            && response.contains("[[ANSWER]]")
            && response.contains("---SEP---");
    }
}
```

---

## 8. Error Handling

### 8.1 에러 시나리오

| 시나리오 | 처리 방법 | 사용자 노출 |
|---------|---------|-----------|
| Gemini API 호출 실패 | catch → 에러 뷰 반환 | "AI 서버 오류입니다. 잠시 후 다시 시도해 주세요." |
| 형식 오류 3회 초과 | RuntimeException → 에러 뷰 | "문제 형식 오류가 반복됩니다. 문제 수를 줄여보세요." |
| DB 저장 실패 | DataAccessException catch | "저장에 실패했습니다. 다시 시도해 주세요." |
| 재생성 중 오류 | JS에서 fetch 오류 처리 | 해당 문제 아래 인라인 에러 메시지 |

### 8.2 에러 뷰

- `src/main/webapp/WEB-INF/views/error.jsp` 신규 생성
- 에러 메시지 + "돌아가기" 버튼

---

## 9. Security Considerations

- [ ] `GEMINI_API_KEY` 환경변수 분리 (`application-local.properties` 사용)
- [ ] `application.properties`에 `gemini.api.key=${GEMINI_API_KEY}` 형태로 변경
- [ ] `.gitignore`에 `application-local.properties` 추가
- [ ] DB는 로컬 H2 파일 모드 → 외부 노출 없음
- [ ] XSS: JSP에서 사용자 입력 출력 시 `<c:out>` 또는 JSTL escaping 사용

---

## 10. Test Plan

### 10.1 Test Scope

| Type | Target | Method |
|------|--------|--------|
| 수동 테스트 | ResponseValidator.isValid() | 형식 맞는/틀린 샘플 문자열로 검증 |
| 수동 테스트 | PromptBuilder.build() | 각 문제 유형 선택 후 생성된 프롬프트 콘솔 확인 |
| 통합 테스트 | 재생성 API | 단일 문제 재생성 후 파싱 성공 여부 |
| 통합 테스트 | DB 저장/조회 | 저장 후 목록 페이지에서 확인 |

### 10.2 Test Cases

- [ ] 빈칸추론 1개 생성 → `[[QUESTION]]`, `---SEP---` 포함 확인
- [ ] 6개 유형 동시 선택 → 형식 오류 없이 모두 반환
- [ ] 의도적으로 빈 응답 주입 → ResponseValidator가 재시도 발동 확인
- [ ] 문제 저장 → questionList.jsp에서 조회 확인

---

## 11. Implementation Guide

### 11.1 File Structure

```
src/main/java/com/example/demo/
├── controller/
│   └── GeminiController.java      ← 수정 (PromptBuilder, Validator 주입)
├── geminiAI/
│   └── GeminiService.java         ← 수정 완료 (maxOutputTokens)
├── service/                        ← 신규 패키지
│   ├── PromptBuilder.java          ← 신규
│   └── ResponseValidator.java      ← 신규
├── entity/                         ← 신규 패키지 (Phase 3)
│   ├── QuestionSet.java            ← 신규
│   └── Question.java               ← 신규
└── repository/                     ← 신규 패키지 (Phase 3)
    └── QuestionSetRepository.java  ← 신규

src/main/webapp/WEB-INF/views/
├── questionForm.jsp               ← 수정 (로딩 오버레이 추가)
├── result.jsp                     ← 수정 (재생성 버튼, 저장 버튼)
├── questionList.jsp               ← 신규 (Phase 3)
└── error.jsp                      ← 신규

src/main/resources/static/
├── css/
│   ├── questionForm.css           ← 수정 (로딩 오버레이 스타일)
│   └── result.css                 ← 수정 (버튼 스타일)
└── js/
    ├── questionForm.js            ← 수정 (로딩 이벤트)
    └── result.js                  ← 수정 (재생성/편집/저장 인터랙션)
```

### 11.2 Implementation Order

**Phase 1 — AI 품질·안정성 (먼저 구현)**
1. [ ] `application.properties` API 키 환경변수 분리
2. [ ] `PromptBuilder.java` 생성 — 기존 GeminiController 프롬프트 로직 이전
3. [ ] `ResponseValidator.java` 생성 — 형식 검증 + 재시도
4. [ ] `GeminiController.java` 리팩토링 — 위 두 컴포넌트 주입
5. [ ] `error.jsp` 생성

**Phase 2 — UX 개선**
6. [ ] `questionForm.jsp` + `questionForm.js` — 로딩 오버레이
7. [ ] `GeminiController.regenerateQuestion()` 엔드포인트 추가
8. [ ] `result.jsp` + `result.js` — 재생성 버튼 + 인라인 편집

**Phase 3 — DB 저장**
9. [ ] `pom.xml` JPA + H2 의존성 추가
10. [ ] `QuestionSet.java`, `Question.java` 엔티티 생성
11. [ ] `QuestionSetRepository.java` 생성
12. [ ] `GeminiController` 저장/조회 API 추가
13. [ ] `questionList.jsp` 생성
14. [ ] `result.jsp` 저장 버튼 추가

### 11.3 Session Guide

> `/pdca do prepai-v2 --scope module-1` 형태로 단계별 구현 가능

#### Module Map

| Module | Scope Key | Description | 예상 작업량 |
|--------|-----------|-------------|:-----------:|
| Phase 1: 품질 안정성 | `module-1` | PromptBuilder, ResponseValidator, API 키 분리, GeminiController 리팩토링 | 중간 |
| Phase 2: UX 개선 | `module-2` | 로딩 오버레이, 재생성 API, 재생성 버튼, 인라인 편집 | 중간 |
| Phase 3: DB 저장 | `module-3` | JPA 설정, 엔티티, 저장/조회 API, 목록 페이지 | 중간 |

#### Recommended Session Plan

| Session | Phase | Scope | 내용 |
|---------|-------|-------|------|
| Session 1 | Plan + Design | 전체 | 완료 |
| Session 2 | Do | `--scope module-1` | PromptBuilder + Validator + 리팩토링 |
| Session 3 | Do | `--scope module-2` | 로딩 UX + 재생성 기능 |
| Session 4 | Do | `--scope module-3` | DB 저장 기능 |
| Session 5 | Check + Report | 전체 | Gap 분석 + 완료 보고서 |

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-03-24 | 초기 설계 (Option C 선택) | 개발팀 |
