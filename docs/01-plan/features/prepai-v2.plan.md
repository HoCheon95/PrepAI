# PrepAI v2 - AI 문제 생성 전용 플랫폼

> **Summary**: PrepAI는 Gemini AI로 영어 문제를 생성하고 DB에 저장하는 것에만 집중한다. 시험지/PDF 렌더링은 별도 프로그램이 담당하도록 역할을 완전 분리한다.
>
> **Project**: PrepAI
> **Version**: 2.0.0
> **Author**: 개발팀
> **Date**: 2026-03-25
> **Status**: Draft

---

## Executive Summary

| Perspective | Content |
|-------------|---------|
| **Problem** | PrepAI가 문제 생성과 시험지 렌더링을 동시에 처리하여 앱이 무겁고 느림. 생성 오류가 시험지 화면 자체를 깨트려 교사에게 직접 노출됨 |
| **Solution** | PrepAI = 문제 생성 + DB 저장 전담. 시험지/PDF 렌더링은 DB를 읽는 별도 프로그램으로 완전 분리 |
| **Function/UX Effect** | PrepAI는 가볍고 빠르게 동작. 생성된 문제를 리스트로 검토 후 DB 저장. 다른 프로그램이 DB에서 문제를 불러와 시험지 제작 |
| **Core Value** | 단일 책임 원칙 적용으로 각 앱이 자기 역할에만 집중 → 성능·안정성·유지보수성 동시 확보 |

---

## Context Anchor

> 플랜 전반에 걸쳐 설계·구현 문서에 복사해 컨텍스트 연속성을 확보한다.

| Key | Value |
|-----|-------|
| **WHY** | 문제 생성과 시험지 렌더링이 한 앱에 있어 무겁고 느림 → 역할 분리로 각각 빠르게 |
| **WHO** | 한국 고등학교 영어 선생님 (수능·모의고사 대비 문제 제작) |
| **RISK** | DB 스키마(questions + answers 2개 테이블)를 잘 설계해야 ExamMaker가 올바르게 읽을 수 있음. DB는 아직 미생성 — 연동은 DB 준비 후 진행 |
| **SUCCESS** | PrepAI는 문제 생성 + DB 저장만 담당. 시험지 관련 코드가 PrepAI에 없음 |
| **SCOPE** | Phase 1: AI 품질·안정성 / Phase 2: 생성 결과 검토 UX + DB 저장 / Phase 3: DB API (ExamMaker 연동용) |

---

## 1. Overview

### 1.1 Purpose

**핵심 아키텍처 결정**: PrepAI의 역할을 "AI 문제 생성 전담"으로 명확히 한정한다.

**현재 문제**:
- 문제 생성(Gemini API) + 시험지 렌더링(A4 레이아웃/페이지네이션)을 한 앱에서 처리
- 둘 다 무거운 작업이라 응답이 느리고, 생성 오류가 시험지 화면을 직접 깨트림
- 기능이 뒤엉켜 있어 한쪽을 고치면 다른 쪽이 깨지는 일이 반복됨

**해결 방향**:

```
[기존] PrepAI 단일 앱
  ┌─────────────────────────────────────────┐
  │  문제 생성(Gemini) + 시험지 렌더링(PDF)  │  ← 무겁고 느림
  └─────────────────────────────────────────┘

[개선] 두 앱으로 분리
  ┌──────────────────┐       ┌──────────────────────┐
  │  PrepAI          │  DB   │  ExamMaker (별도 앱)  │
  │  - Gemini 호출   │──────→│  - DB에서 문제 조회   │
  │  - 문제 파싱     │       │  - A4 시험지 렌더링   │
  │  - DB 저장       │       │  - PDF 출력           │
  └──────────────────┘       └──────────────────────┘
```

PrepAI v2의 목표: 왼쪽 박스를 제대로 만드는 것.

### 1.2 Background

한국 고등학교 영어 교육에서 수능/모의고사 유형 문제는 지문이 바뀔 때마다 새 문제가 필요하다. PrepAI는 Gemini API로 문제를 즉시 생성할 수 있지만, 생성 품질 불안정 + 앱 성능 저하로 교사의 신뢰를 얻지 못하고 있다. 역할 분리를 통해 PrepAI를 가볍고 빠른 문제 생성기로 만들고, 시험지 제작은 별도 앱에 위임한다.

### 1.3 Related Documents

- 기존 구현: `src/main/java/com/example/demo/geminiAI/GeminiService.java`
- 프론트엔드: `src/main/webapp/WEB-INF/views/result.jsp`, `questionForm.jsp`
- API 컨트롤러: `src/main/java/com/example/demo/controller/GeminiController.java`

---

## 2. Scope

### 2.1 In Scope (PrepAI가 담당할 것)

**Phase 1 - AI 품질 및 안정성 개선**
- [ ] 문제 유형별 전용 프롬프트 분리 및 고도화 (13개 유형)
- [ ] Few-shot 예시 추가 (각 유형별 Good/Bad 예시)
- [ ] 응답 형식 검증 로직 구현 (태그 존재 여부, 선택지 수, 정답 유효성)
- [ ] 형식 오류 발생 시 자동 재시도 (최대 3회)
- [ ] API 키 환경 변수 분리 (보안)

**Phase 2 - 생성 결과 검토 UX + DB 저장**
- [ ] 문제 생성 중 로딩 인디케이터 + 진행 메시지 표시
- [ ] 생성 완료 후 **문제 리스트 검토 화면** 표시 (시험지 렌더링 없음)
- [ ] 검토 화면에서 개별 문제 재생성 버튼
- [ ] 검토 화면에서 문제 텍스트 인라인 편집
- [ ] **"DB에 저장" 버튼** — 검토 완료된 문제 세트를 DB에 저장
- [ ] **"복사하기" 버튼** — 생성된 문제 텍스트 클립보드 복사
- [ ] 저장된 문제 목록 조회 페이지 (PrepAI 내에서)

**Phase 3 - DB 및 외부 연동 API**
- [ ] H2/MySQL DB 연동 (문제 저장 스키마 설계 — ExamMaker가 읽을 수 있도록)
- [ ] `GET /api/question-sets` — 저장된 문제 세트 목록 조회
- [ ] `GET /api/question-sets/{id}` — 문제 세트 상세 + 문제 전체 조회
- [ ] `GET /api/question-sets/{id}/questions` — 문제만 JSON 조회 (ExamMaker 연동용)

### 2.2 Out of Scope (PrepAI가 하지 않을 것)

- **시험지 렌더링** (A4 레이아웃, 페이지네이션) — ExamMaker 담당
- **PDF 출력/다운로드** — ExamMaker 담당
- **답안지 화면 렌더링** — ExamMaker 담당
- 사용자 인증/로그인 시스템
- 모바일 앱 버전
- 다른 AI 모델(OpenAI, Claude) 연동
- 학생용 풀이 기능 (채점, 오답노트)
- 영어 외 타 과목 지원

---

## 3. Requirements

### 3.1 Functional Requirements

| ID | Requirement | Priority | Status |
|----|-------------|----------|--------|
| FR-01 | 각 문제 유형별 전용 프롬프트 분리 및 고도화 | High | Pending |
| FR-02 | AI 응답 형식 검증 및 자동 재시도 (최대 3회) | High | Pending |
| FR-03 | 생성 중 로딩 상태 표시 (진행 메시지 포함) | High | Pending |
| FR-04 | 생성 완료 후 문제 리스트 검토 화면 표시 (시험지 렌더링 없음) | High | Pending |
| FR-05 | 검토 화면에서 개별 문제 재생성 API | Medium | Pending |
| FR-06 | 검토 화면에서 문제 텍스트 인라인 편집 | Medium | Pending |
| FR-07 | "DB에 저장" 버튼 — questions 테이블 + answers 테이블에 각각 저장 | High | Pending |
| FR-08 | "복사하기" 버튼 — 생성 문제 텍스트 클립보드 복사 | Medium | Pending |
| FR-09 | 저장된 문제 목록 조회 페이지 | Medium | Pending |
| FR-10 | GET /api/questions/{id} — 문제 JSON API (ExamMaker 연동용) | High | Pending |
| FR-10b | GET /api/answers/{question_id} — 답안지 JSON API (ExamMaker 연동용) | High | Pending |
| FR-11 | API 키 환경 변수 분리 (.env / application-local.properties) | High | Pending |
| FR-12 | 오류 발생 시 사용자 친화 에러 페이지 | Medium | Pending |
| FR-13 | PDF 추출 시 (A)(B)(C)(D) 단락 구분자 원문 그대로 보존 | High | In Progress |
| FR-14 | 문제 번호 인식: `번호.` 패턴으로 정확히 식별 — `[43~45]` 같은 섹션 헤더와 구분 | High | In Progress |

### 3.2 Non-Functional Requirements

| Category | Criteria | Measurement Method |
|----------|----------|-------------------|
| AI 형식 안정성 | 형식 오류율 < 5% | 100회 생성 테스트 후 오류 건수 측정 |
| 응답 시간 | 문제 생성 완료까지 평균 60초 이내 | Gemini API 호출 시간 측정 |
| 재시도 투명성 | 재시도 발생 시 서버 로그에 기록 | 로그 확인 |
| 보안 | API 키가 소스코드/git에 노출되지 않을 것 | .gitignore + 환경변수 확인 |
| DB 호환성 | ExamMaker가 DB 스키마를 변경 없이 읽을 수 있을 것 | ExamMaker 연동 테스트 |

---

## 4. Success Criteria

### 4.1 Definition of Done

- [ ] 13개 문제 유형 모두 전용 프롬프트로 분리 완료
- [ ] 형식 검증 로직이 서버 측에서 동작하고 재시도 로그가 남음
- [ ] 로딩 인디케이터가 생성 시작~완료 구간에 표시됨
- [ ] 생성 완료 후 **문제 리스트 검토 화면**이 표시됨 (시험지 렌더링 코드 없음)
- [ ] "DB에 저장" 버튼이 동작하고 문제가 DB에 기록됨
- [ ] "복사하기" 버튼이 동작하고 클립보드에 문제 텍스트가 복사됨
- [ ] `GET /api/questions/{id}` API가 문제 JSON을 반환함
- [ ] `GET /api/answers/{question_id}` API가 답안지 JSON을 반환함
- [ ] 문제와 답안지가 **별도 테이블**(questions, answers)에 저장됨
- [ ] **PrepAI 코드에 A4 레이아웃/PDF 관련 코드가 없음** (역할 분리 완료 기준)
- [ ] Gemini API 키가 환경 변수로 분리됨

### 4.2 Quality Criteria

- [ ] 형식 오류 발생률 5% 미만 (수동 테스트 100회 기준)
- [ ] 재시도 로직이 3회 내에 정상 응답을 반환함
- [ ] DB에 저장된 문제를 API로 조회했을 때 원본과 동일한 내용 반환

---

## 5. Risks and Mitigation

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Gemini 응답이 재시도 3회 후에도 형식 오류 | High | Medium | 3회 실패 시 사용자에게 오류 안내 + 부분 파싱 fallback 적용 |
| DB 스키마 설계 미흡으로 ExamMaker 연동 실패 | High | Medium | Phase 3 전에 ExamMaker가 필요한 필드 목록을 먼저 정의 |
| 프롬프트 변경 후 다른 유형 품질 저하 | Medium | Medium | 유형별 독립 프롬프트 관리, 변경 시 해당 유형만 테스트 |
| API 키 환경변수 이전 시 기존 배포 환경 설정 필요 | Low | High | application-local.properties + .gitignore로 단계적 이전 |

---

## 6. Impact Analysis

### 6.1 Changed Resources

| Resource | Type | Change Description |
|----------|------|--------------------|
| `GeminiService.java` | Java Service | 프롬프트 분리, 검증/재시도 로직 추가 |
| `GeminiController.java` | Java Controller | 응답을 reviewResult로 전달, 재생성/저장 API 추가 |
| `application.properties` | Config | API 키 환경변수로 이전 |
| `result.jsp` | View (JSP) | **제거 또는 미사용**: 시험지 렌더링 → Out of Scope |
| `reviewResult.jsp` | View (JSP) | **신규**: 문제 리스트 검토 뷰 + 저장/복사 버튼 |
| `reviewResult.js` | JavaScript | **신규**: 검토 인터랙션, 저장/복사 버튼 액션 |
| `questionForm.jsp` | View (JSP) | 로딩 인디케이터 UI 추가 |
| `questionForm.js` | JavaScript | 폼 제출 시 로딩 상태 처리 |
| `pom.xml` | Maven | JPA, H2/MySQL 의존성 추가 (Phase 3) |
| DB Entity (신규) | Java | `QuestionSet`, `Question` JPA 엔티티 (ExamMaker 호환 스키마) |

### 6.2 Current Consumers

| Resource | Operation | Code Path | Impact |
|----------|-----------|-----------|--------|
| `GeminiService.generateQuestion()` | READ | `GeminiController.generateQuestions()` | 검증/재시도 로직 추가 후 호환 유지 필요 |
| `application.properties` `gemini.api.key` | READ | `GeminiService` @Value 주입 | 환경변수 이전 시 로컬 설정 파일 필요 |
| `result.jsp` | VIEW | 현재 시험지 렌더링 담당 | Phase 2에서 reviewResult.jsp로 교체 |

### 6.3 Verification

- [ ] GeminiService 변경 후 기존 3가지 입력 모드(모의고사/외부지문/교과서) 모두 정상 동작 확인
- [ ] 환경변수 이전 후 로컬 및 배포 환경에서 API 키 주입 확인
- [ ] DB 저장 후 `/api/question-sets/{id}/questions` API 응답이 저장 데이터와 일치 확인

---

## 7. Architecture Considerations

### 7.1 Project Level Selection

| Level | Characteristics | Recommended For | Selected |
|-------|-----------------|-----------------|:--------:|
| **Starter** | Simple structure | Static sites | ☐ |
| **Dynamic** | Feature-based modules | Web apps with backend | ☑ |
| **Enterprise** | Strict layer separation | High-traffic systems | ☐ |

> **선택 이유**: 문제 생성 전담으로 역할이 단순화되므로 Dynamic 수준이 적절. PrepAI는 절대 시험지 렌더링 코드를 포함하지 않는다.

### 7.2 Key Architectural Decisions

| Decision | Options | Selected | Rationale |
|----------|---------|----------|-----------|
| 시험지 렌더링 | PrepAI 내 / 별도 앱 | **별도 앱(ExamMaker)** | PrepAI를 가볍게 유지, 역할 분리 |
| 프롬프트 관리 | 코드 내 하드코딩 / 외부 파일(.txt) | **외부 .txt 파일** | 코드 변경 없이 프롬프트 수정 가능 |
| 응답 검증 | 클라이언트 JS / 서버 Java | **서버 Java** | 신뢰할 수 있는 검증, 재시도 가능 |
| DB | H2 인메모리 / H2 파일 / MySQL | **H2 파일 모드** | 별도 DB 서버 불필요, 이후 MySQL 마이그레이션 용이 |
| ExamMaker 연동 | 직접 DB 접근 / REST API | **REST API** | 앱 간 결합도 최소화, 스키마 변경 영향 격리 |

### 7.3 Architecture Overview

```
[PrepAI v2 — 문제 생성 전담]

  questionForm.jsp
       ↓ (생성 요청)
  GeminiController
       ├→ PromptBuilder (유형별 프롬프트)
       ├→ GeminiService → Gemini API
       └→ ResponseValidator (형식 검증/재시도)
              ↓ (생성 완료)
       reviewResult.jsp
       ┌──────────────────────────────────┐
       │  생성된 문제 리스트 표시           │
       │  [개별 재생성]  [인라인 편집]      │
       │  [DB에 저장]    [복사하기]         │
       └──────────────────────────────────┘
              ↓ (저장 클릭)
         H2/MySQL DB
       ┌──────────────────────────────────┐
       │  question_sets 테이블             │
       │  questions 테이블                 │
       └──────────────────────────────────┘
              ↓ (REST API)
  GET /api/question-sets/{id}/questions
              ↓
  [ExamMaker — 별도 프로그램, PrepAI 밖]
       - DB에서 문제 JSON 조회
       - A4 시험지 렌더링
       - PDF 출력/다운로드
       - 답안지 생성

핵심 경계:
  PrepAI ↔ ExamMaker 경계 = REST API (또는 공유 DB 직접 접근)
  PrepAI 코드 = 시험지/PDF/답안지 관련 코드 없음
```

### 7.4 DB Schema (ExamMaker 연동 기준)

> **NOTE**: DB는 아직 생성되지 않았음. 사용자가 DB 준비 후 별도로 안내 예정.
> 현재는 스키마 설계만 확정해두고, 실제 연동은 DB 준비 후 진행.

PrepAI는 **2개의 테이블**에 데이터를 저장한다:

```
[테이블 1] questions  — 생성된 문제
[테이블 2] answers    — 각 문제의 답안지 (정답 + 해설)
```

```sql
-- 테이블 1: 문제 (questions)
-- 문제 본문, 선택지 등 문제 내용 저장
CREATE TABLE questions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_type   VARCHAR(100),       -- 문제 유형 (13개)
    question_number INT,                -- 문제 번호
    passage         TEXT,               -- 지문 원문
    input_mode      VARCHAR(50),        -- 모의고사/외부지문/교과서
    question_text   TEXT,               -- 문제 본문
    options         TEXT,               -- 선택지 JSON
    created_at      TIMESTAMP DEFAULT NOW()
);

-- 테이블 2: 답안지 (answers)
-- 정답 및 해설 저장 (ExamMaker가 답안지 렌더링 시 사용)
CREATE TABLE answers (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id     BIGINT REFERENCES questions(id),  -- 문제와 1:1 연결
    answer          VARCHAR(10),        -- 정답 (예: "3", "①")
    explanation     TEXT,               -- 해설
    created_at      TIMESTAMP DEFAULT NOW()
);
```

**두 테이블 분리 이유**:
- ExamMaker가 문제만 출력할 때는 `questions`만 조회
- ExamMaker가 답안지를 출력할 때는 `answers`를 별도 조회
- 문제와 정답을 분리 저장하면 학생 배포용(문제만) / 교사용(문제+정답) 분기 처리 용이

---

## 8. Convention Prerequisites

### 8.1 Conventions to Define/Verify

| Category | Current State | To Define | Priority |
|----------|---------------|-----------|:--------:|
| **API 응답 태그** | `[[TAG]]` 방식 유지 | 유형별 태그 일관성 규칙 | High |
| **프롬프트 파일명** | 없음 | `{questionType}.prompt.txt` 형식 | High |
| **에러 응답 형식** | 없음 | JSON `{error: string, retryCount: int}` | Medium |
| **환경 변수명** | `gemini.api.key` | `GEMINI_API_KEY` (환경변수) | High |
| **API 응답 형식** | 없음 | ExamMaker가 파싱할 JSON 구조 정의 | High |

### 8.2 Environment Variables Needed

| Variable | Purpose | Scope |
|----------|---------|-------|
| `GEMINI_API_KEY` | Gemini API 인증 | Server |
| `SPRING_DATASOURCE_URL` | DB 연결 (Phase 3) | Server |

---

## 9. Implementation Phases

### Phase 1 - AI 품질 및 안정성 (우선순위: 긴급)

| 순서 | 작업 | 예상 복잡도 |
|------|------|------------|
| 1 | API 키 환경변수 분리 | 낮음 |
| 2 | `PromptBuilder.java` 생성 - 13개 유형별 프롬프트 분리 | 중간 |
| 3 | `ResponseValidator.java` 생성 - 형식 검증 + 재시도 | 중간 |
| 4 | `GeminiService.java` 리팩토링 - 위 두 클래스 사용 | 중간 |
| 5 | 수동 테스트 (각 유형 5회 생성, 오류율 측정) | 낮음 |

### Phase 2 - 생성 결과 검토 UX + DB 저장 (우선순위: 높음)

| 순서 | 작업 | 예상 복잡도 |
|------|------|------------|
| 1 | `questionForm.jsp/.js` 로딩 인디케이터 추가 | 낮음 |
| 2 | `GeminiController.java` 응답을 `reviewResult.jsp`로 전달 | 낮음 |
| 3 | `reviewResult.jsp` 문제 리스트 검토 뷰 구현 | 중간 |
| 4 | `reviewResult.js` 개별 재생성 버튼 + 인라인 편집 | 중간 |
| 5 | `reviewResult.js` "복사하기" 버튼 → 클립보드 API | 낮음 |
| 6 | `POST /api/regenerate-question` 엔드포인트 (단일 문제 재생성) | 중간 |
| 7 | `POST /api/save-questions` 엔드포인트 + "DB에 저장" 버튼 연동 | 중간 |
| 8 | 에러 페이지/메시지 처리 | 낮음 |

### Phase 3 - DB API (ExamMaker 연동용) (우선순위: 중간)

| 순서 | 작업 | 예상 복잡도 |
|------|------|------------|
| 1 | `pom.xml` JPA + H2 의존성 추가 | 낮음 |
| 2 | `QuestionSet`, `Question` JPA 엔티티 설계 (ExamMaker 호환 스키마) | 중간 |
| 3 | `GET /api/question-sets` — 문제 세트 목록 | 낮음 |
| 4 | `GET /api/question-sets/{id}/questions` — 문제 JSON (ExamMaker 연동 핵심) | 중간 |
| 5 | `questionList.jsp` 저장된 문제 목록 조회 페이지 | 중간 |

---

## 10. Next Steps

1. [ ] `docs/02-design/features/prepai-v2.design.md` 작성 (`/pdca design prepai-v2`)
2. [ ] Phase 1부터 구현 시작 (AI 품질 먼저)
3. [ ] Phase 3 완료 후 ExamMaker 프로젝트 별도 생성

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-03-24 | 초기 플랜 작성 | 개발팀 |
| 0.2 | 2026-03-25 | 중간 검토 뷰 + 액션 버튼 분리 반영 | 개발팀 |
| 0.3 | 2026-03-25 | **아키텍처 전면 재설계**: PrepAI = 문제 생성 전담, 시험지/PDF = ExamMaker 별도 앱으로 완전 분리 | 개발팀 |
| 0.4 | 2026-03-25 | DB 저장 대상 확정: questions + answers 2개 테이블로 분리 저장. DB는 미생성 (추후 연동 예정) | 개발팀 |
| 0.5 | 2026-03-26 | FR-13/FR-14 추가: PDF 추출 품질 이슈 — (A)(B)(C)(D) 단락 구분자 보존 + 문제 번호 패턴(`번호.`) 정확 인식. PromptBuilder.java HINT 수정으로 부분 반영 | 개발팀 |
