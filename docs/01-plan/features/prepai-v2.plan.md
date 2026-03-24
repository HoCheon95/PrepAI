# PrepAI v2 - AI 영어 문제 품질 향상 계획서

> **Summary**: AI 문제 품질 개선, 응답 안정화, 문제 저장/재생성 기능 추가로 실사용 가능한 영어 문제 생성 플랫폼 완성
>
> **Project**: PrepAI
> **Version**: 2.0.0
> **Author**: 개발팀
> **Date**: 2026-03-24
> **Status**: Draft

---

## Executive Summary

| Perspective | Content |
|-------------|---------|
| **Problem** | AI가 생성하는 영어 시험 문제의 품질이 불안정하고, 형식 오류가 자주 발생하며, 생성 후 저장/수정 수단이 없어 교사가 실무에서 활용하기 어려움 |
| **Solution** | 프롬프트 엔지니어링 고도화 + 응답 검증/재시도 로직 + 로딩 UX + 문제 재생성/편집 + DB 저장 기능 도입 |
| **Function/UX Effect** | 문제 생성 후 즉시 수정·저장·재사용 가능하고, 생성 중 진행 상태를 확인할 수 있어 교사 워크플로우가 크게 단축됨 |
| **Core Value** | 영어 교사가 수업·시험 준비 시간을 절반 이상 단축할 수 있는 신뢰할 수 있는 AI 문제 생성 도구 |

---

## Context Anchor

> 플랜 전반에 걸쳐 설계·구현 문서에 복사해 컨텍스트 연속성을 확보한다.

| Key | Value |
|-----|-------|
| **WHY** | 교사가 직접 문제를 만드는 데 소요하는 시간 절감 + AI 품질 불신 해소 |
| **WHO** | 한국 고등학교 영어 선생님 (수능·모의고사 대비 문제 제작) |
| **RISK** | Gemini API 응답 형식 불안정, DB 도입 시 기존 JSP 구조와의 정합성 |
| **SUCCESS** | 형식 오류율 < 5%, 교사 피드백 만족도 4/5 이상, 문제 저장·재사용 가능 |
| **SCOPE** | Phase 1: 품질·안정성 / Phase 2: UX 개선 / Phase 3: DB 저장 |

---

## 1. Overview

### 1.1 Purpose

현재 PrepAI는 기본 문제 생성 기능은 동작하지만, 교사가 실무에서 사용하기에는 다음과 같은 한계가 있다:

- Gemini 응답에서 `[[TAG]]` 누락, `---SEP---` 구분자 오류가 자주 발생
- 생성된 문제 중 일부만 수정하거나 재생성하는 수단이 없음
- 생성 시간이 20-60초임에도 로딩 피드백이 없음
- 생성된 문제를 저장할 수 없어 매번 새로 생성해야 함

### 1.2 Background

한국 고등학교 영어 교육에서 수능/모의고사 유형 문제는 지문이 바뀔 때마다 새 문제가 필요하다. 기존 문제은행은 지문 고정 문제 위주라 다양한 지문에 대응하기 어렵다. PrepAI는 임의 지문으로 즉시 문제를 생성할 수 있지만, AI 품질 불안정으로 교사의 신뢰를 얻지 못하고 있다.

### 1.3 Related Documents

- 기존 구현: `src/main/java/com/example/demo/geminiAI/GeminiService.java`
- 프론트엔드: `src/main/webapp/WEB-INF/views/result.jsp`, `questionForm.jsp`
- API 컨트롤러: `src/main/java/com/example/demo/controller/GeminiController.java`

---

## 2. Scope

### 2.1 In Scope

**Phase 1 - AI 품질 및 안정성 개선**
- [ ] 문제 유형별 전용 프롬프트 분리 및 고도화 (13개 유형)
- [ ] Few-shot 예시 추가 (각 유형별 Good/Bad 예시)
- [ ] 응답 형식 검증 로직 구현 (태그 존재 여부, 선택지 수, 정답 유효성)
- [ ] 형식 오류 발생 시 자동 재시도 (최대 3회)
- [ ] API 키 환경 변수 분리 (보안)

**Phase 2 - UX 개선**
- [ ] 문제 생성 중 로딩 인디케이터 + 진행 메시지 표시
- [ ] 결과 페이지에서 개별 문제 재생성 버튼
- [ ] 결과 페이지에서 문제 텍스트 인라인 편집 기능
- [ ] 오류 발생 시 사용자 친화적 메시지 표시

**Phase 3 - 문제 저장 (DB)**
- [ ] H2/MySQL DB 연동 (문제 저장 테이블 설계)
- [ ] 생성된 문제 세트 저장 기능
- [ ] 저장된 문제 목록 조회 페이지
- [ ] 저장된 문제 불러오기 및 PDF 재출력

### 2.2 Out of Scope

- 사용자 인증/로그인 시스템 (별도 계획 필요)
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
| FR-04 | 결과 페이지에서 개별 문제 재생성 API | High | Pending |
| FR-05 | 결과 페이지에서 문제 텍스트 인라인 편집 | Medium | Pending |
| FR-06 | 생성된 문제 세트 DB 저장 | Medium | Pending |
| FR-07 | 저장된 문제 목록 조회 페이지 | Medium | Pending |
| FR-08 | 저장된 문제 불러오기 및 PDF 재출력 | Medium | Pending |
| FR-09 | API 키 환경 변수 분리 (.env / application-local.properties) | High | Pending |
| FR-10 | 오류 발생 시 사용자 친화 에러 페이지 | Medium | Pending |

### 3.2 Non-Functional Requirements

| Category | Criteria | Measurement Method |
|----------|----------|-------------------|
| AI 형식 안정성 | 형식 오류율 < 5% | 100회 생성 테스트 후 오류 건수 측정 |
| 응답 시간 | 문제 생성 완료까지 평균 60초 이내 | Gemini API 호출 시간 측정 |
| 재시도 투명성 | 재시도 발생 시 서버 로그에 기록 | 로그 확인 |
| 보안 | API 키가 소스코드/git에 노출되지 않을 것 | .gitignore + 환경변수 확인 |
| 사용성 | 로딩 상태가 항상 표시될 것 | 수동 QA |

---

## 4. Success Criteria

### 4.1 Definition of Done

- [ ] 13개 문제 유형 모두 전용 프롬프트로 분리 완료
- [ ] 형식 검증 로직이 서버 측에서 동작하고 재시도 로그가 남음
- [ ] 로딩 인디케이터가 생성 시작~완료 구간에 표시됨
- [ ] 개별 문제 재생성 버튼이 결과 페이지에 동작함
- [ ] 문제 세트를 DB에 저장하고 다시 불러올 수 있음
- [ ] Gemini API 키가 환경 변수로 분리됨
- [ ] 오류 발생 시 사용자에게 명확한 메시지가 표시됨

### 4.2 Quality Criteria

- [ ] 형식 오류 발생률 5% 미만 (수동 테스트 100회 기준)
- [ ] 재시도 로직이 3회 내에 정상 응답을 반환함
- [ ] 기존 기능 (PDF 다운로드, 학생/교사 뷰) 정상 동작 유지

---

## 5. Risks and Mitigation

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Gemini 응답이 재시도 3회 후에도 형식 오류 | High | Medium | 3회 실패 시 사용자에게 오류 안내 + 부분 파싱 fallback 적용 |
| 프롬프트 변경 후 다른 유형 품질 저하 | Medium | Medium | 유형별 독립 프롬프트 관리, 변경 시 해당 유형만 테스트 |
| DB 도입 시 기존 JSP 구조 복잡도 증가 | Medium | High | 간단한 H2 인메모리 DB로 시작, JPA Entity 최소화 |
| API 키 환경변수 이전 시 기존 배포 환경 설정 필요 | Low | High | application-local.properties + .gitignore로 단계적 이전 |
| 재생성 API 추가 시 서버 부하 증가 | Low | Low | 동일 Gemini API 호출, 별도 rate limit 없음 |

---

## 6. Impact Analysis

### 6.1 Changed Resources

| Resource | Type | Change Description |
|----------|------|--------------------|
| `GeminiService.java` | Java Service | 프롬프트 분리, 검증/재시도 로직 추가 |
| `GeminiController.java` | Java Controller | 재생성 API 엔드포인트 추가, 저장 API 추가 |
| `application.properties` | Config | API 키 환경변수로 이전 |
| `result.jsp` | View (JSP) | 재생성 버튼, 인라인 편집, 저장 버튼 UI 추가 |
| `result.js` | JavaScript | 재생성/편집/저장 인터랙션 로직 추가 |
| `questionForm.jsp` | View (JSP) | 로딩 인디케이터 UI 추가 |
| `questionForm.js` | JavaScript | 폼 제출 시 로딩 상태 처리 |
| `pom.xml` | Maven | JPA, H2/MySQL 의존성 추가 (Phase 3) |
| DB Entity (신규) | Java | QuestionSet, Question 엔티티 |

### 6.2 Current Consumers

| Resource | Operation | Code Path | Impact |
|----------|-----------|-----------|--------|
| `GeminiService.generateQuestion()` | READ | `GeminiController.generateQuestions()` | 검증/재시도 로직 추가 후 호환 유지 필요 |
| `application.properties` `gemini.api.key` | READ | `GeminiService` @Value 주입 | 환경변수 이전 시 로컬 설정 파일 필요 |
| `result.jsp` 파싱 로직 | READ | `result.js` `---SEP---` 파서 | 신규 태그 추가 시 파서 업데이트 필요 |

### 6.3 Verification

- [ ] GeminiService 변경 후 기존 3가지 입력 모드(모의고사/외부지문/교과서) 모두 정상 동작 확인
- [ ] 환경변수 이전 후 로컬 및 배포 환경에서 API 키 주입 확인
- [ ] 결과 페이지 UI 변경 후 PDF 다운로드 레이아웃 깨지지 않음 확인

---

## 7. Architecture Considerations

### 7.1 Project Level Selection

| Level | Characteristics | Recommended For | Selected |
|-------|-----------------|-----------------|:--------:|
| **Starter** | Simple structure | Static sites | ☐ |
| **Dynamic** | Feature-based modules | Web apps with backend | ☑ |
| **Enterprise** | Strict layer separation | High-traffic systems | ☐ |

> **선택 이유**: 현재 Spring Boot MVC 구조를 유지하면서 기능을 확장하는 Dynamic 수준이 적절. 마이크로서비스 전환은 불필요.

### 7.2 Key Architectural Decisions

| Decision | Options | Selected | Rationale |
|----------|---------|----------|-----------|
| 프롬프트 관리 | 코드 내 하드코딩 / 외부 파일(.txt) / DB | **외부 .txt 파일** | 코드 변경 없이 프롬프트 수정 가능 |
| 응답 검증 | 클라이언트 JS / 서버 Java | **서버 Java** | 신뢰할 수 있는 검증, 재시도 가능 |
| DB | H2 인메모리 / H2 파일 / MySQL | **H2 파일 모드** | 별도 DB 서버 불필요, 이후 MySQL 마이그레이션 용이 |
| ORM | JDBC Template / JPA | **JPA (Spring Data)** | Spring Boot와 자연스러운 통합 |
| 로딩 UX | 단순 스피너 / 단계별 메시지 | **단계별 메시지** | 60초 대기 시 사용자 불안감 해소 |

### 7.3 Architecture Overview

```
현재 구조:
  Browser → GeminiController → GeminiService → Gemini API

목표 구조 (v2):
  Browser → GeminiController ─┬→ PromptBuilder (유형별 프롬프트)
                              ├→ GeminiService → Gemini API
                              ├→ ResponseValidator (형식 검증/재시도)
                              └→ QuestionRepository → H2 DB

신규 엔드포인트:
  POST /api/regenerate-question   (단일 문제 재생성)
  POST /api/save-questions        (문제 세트 저장)
  GET  /api/question-sets         (저장된 세트 목록)
  GET  /api/question-sets/{id}    (저장된 세트 상세)

신규 파일:
  src/main/java/.../service/PromptBuilder.java        (프롬프트 관리)
  src/main/java/.../service/ResponseValidator.java    (응답 검증)
  src/main/java/.../entity/QuestionSet.java           (JPA 엔티티)
  src/main/java/.../entity/Question.java              (JPA 엔티티)
  src/main/java/.../repository/QuestionSetRepository.java
  src/main/resources/prompts/                         (유형별 프롬프트 파일)
  src/main/webapp/WEB-INF/views/questionList.jsp      (저장 목록 페이지)
```

---

## 8. Convention Prerequisites

### 8.1 Existing Project Conventions

- [ ] `CLAUDE.md` 코딩 컨벤션 섹션 없음 (추가 권장)
- [ ] ESLint/Prettier 설정 없음 (JSP 프로젝트 특성상 스킵 가능)
- [ ] Java 코드 스타일: 현재 특별한 컨벤션 문서 없음

### 8.2 Conventions to Define/Verify

| Category | Current State | To Define | Priority |
|----------|---------------|-----------|:--------:|
| **API 응답 태그** | `[[TAG]]` 방식 유지 | 유형별 태그 일관성 규칙 | High |
| **프롬프트 파일명** | 없음 | `{questionType}.prompt.txt` 형식 | High |
| **에러 응답 형식** | 없음 | JSON `{error: string, retryCount: int}` | Medium |
| **환경 변수명** | `gemini.api.key` | `GEMINI_API_KEY` (환경변수), `gemini.api.key=${GEMINI_API_KEY}` | High |

### 8.3 Environment Variables Needed

| Variable | Purpose | Scope | To Be Created |
|----------|---------|-------|:-------------:|
| `GEMINI_API_KEY` | Gemini API 인증 | Server | ☑ |
| `SPRING_DATASOURCE_URL` | DB 연결 (Phase 3) | Server | ☑ |

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

### Phase 2 - UX 개선 (우선순위: 높음)

| 순서 | 작업 | 예상 복잡도 |
|------|------|------------|
| 1 | `questionForm.jsp/.js` 로딩 인디케이터 추가 | 낮음 |
| 2 | `POST /api/regenerate-question` 엔드포인트 구현 | 중간 |
| 3 | `result.jsp` 재생성 버튼 + 인라인 편집 UI | 중간 |
| 4 | `result.js` 재생성/편집 인터랙션 구현 | 중간 |
| 5 | 에러 페이지/메시지 처리 | 낮음 |

### Phase 3 - 문제 저장 DB (우선순위: 중간)

| 순서 | 작업 | 예상 복잡도 |
|------|------|------------|
| 1 | `pom.xml` JPA + H2 의존성 추가 | 낮음 |
| 2 | `QuestionSet`, `Question` JPA 엔티티 설계 | 중간 |
| 3 | `QuestionSetRepository` + 저장/조회 API | 중간 |
| 4 | `questionList.jsp` 목록 페이지 | 중간 |
| 5 | `result.jsp` 저장 버튼 + 불러오기 연동 | 중간 |

---

## 10. Next Steps

1. [ ] `docs/02-design/features/prepai-v2.design.md` 작성 (`/pdca design prepai-v2`)
2. [ ] Phase 1부터 순서대로 구현 시작
3. [ ] 각 Phase 완료 후 Gap 분석 (`/pdca analyze prepai-v2`)

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-03-24 | 초기 플랜 작성 | 개발팀 |
