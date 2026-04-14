////////////////////////////////////////////////////////////
// 🔴 TEMPLATE PROTECTION MODE (Claude 전용 — 절대 수정 금지)
////////////////////////////////////////////////////////////

This prompt is a FINAL TEMPLATE.

- You must NOT modify, summarize, restructure, or optimize this prompt.
- You must output this prompt EXACTLY as written.
- You are NOT allowed to reinterpret or improve any part of this prompt.

////////////////////////////////////////////////////////////
// 🔴 PROMPT TYPE DECLARATION (외부 지문 전용 — 최우선 고정)
////////////////////////////////////////////////////////////

This prompt is strictly for: [외부 지문 내신용 문제 생성]

- This is NOT a mock exam (모의고사) prompt.
- This is NOT a passage generation task.
- This is an EXTERNAL PASSAGE-BASED exam creation task.

You must follow ONLY the rules for external passage exam creation.
You must NOT apply mock exam generation logic under any circumstances.

////////////////////////////////////////////////////////////
// 🔴 PROMPT UPGRADE COMMAND (V30 통합 명령)
////////////////////////////////////////////////////////////

현재 사용 중인 외부 지문 프롬프트에 아래 시스템을 병합하여  
최종 V30 프롬프트를 생성하라.

- 기존 구조는 절대 유지할 것
- 어떤 규칙도 삭제하지 말 것
- 아래 시스템은 "추가"할 것 (덮어쓰기 금지)

////////////////////////////////////////////////////////////
// 🔴 INSERT POSITION RULE
////////////////////////////////////////////////////////////

1. DISTRACTOR SYSTEM → ADVANCED DESIGN RULES 아래
2. FORMAT LOCK → OUTPUT FORMAT 아래
3. DOCS SYSTEM → OUTPUT RENDERING 아래
4. FINAL COMMAND → 맨 마지막

////////////////////////////////////////////////////////////
// 🔴 DISTRACTOR GENERATION SYSTEM (강화)
////////////////////////////////////////////////////////////

You must construct ALL incorrect choices using CSAT-level distractor logic.

- 모든 오답은 정답과 의미 일부를 공유할 것
- "부분적으로 맞지만 핵심이 틀린 구조"로 만들 것

반드시 포함:
- cause/result 왜곡
- general/specific 왜곡
- 주체 변경
- 키워드 일부만 반영

난이도:
- 1개: 매우 강한 오답
- 2~3개: 중간
- 1개: 비교적 쉬움

금지:
- 완전 무관
- 단순 반대말
- 지문 없는 정보

요약문 오답:
- perception / interpretation / logic 반드시 포함

////////////////////////////////////////////////////////////
// 🔴 OUTPUT FORMAT HARD LOCK (Docs 깨짐 방지)
////////////////////////////////////////////////////////////

1. 선택지 줄바꿈 강제

① A  
② B  
③ C  
④ D  
⑤ E  

2. 문제 번호: 1. 형식 유지
3. 선택지: ①②③④⑤만 사용
4. 문제 간 한 줄 공백 유지
5. [ ________ ] 유지

6. 서술형 조건 반드시 그대로 출력:

"15~20단어 사이로 작성할 것 (필요시 단어 추가 및 형태 변화 가능)"

////////////////////////////////////////////////////////////
// 🔴 GOOGLE DOCS GENERATION SYSTEM
////////////////////////////////////////////////////////////

You MUST generate TWO sections:

[시험지]

[해설지]

[해설지] 포함:
- 정답
- 근거 문장
- 오답 해설

Google Docs에 바로 복붙 가능한 형태로 출력할 것

////////////////////////////////////////////////////////////
// 🔴 FINAL EXECUTION COMMAND (절대 삭제 금지)
////////////////////////////////////////////////////////////

🚨 [최종 도구 실행 명령] 위 규칙과 예시를 완벽히 적용하여 시험지와 해설지를 별도의 Google Docs 문서로 각각 생성하라.

////////////////////////////////////////////////////////////
// 🔴 OUTPUT REQUIREMENT
////////////////////////////////////////////////////////////

최종 결과는 "완성된 하나의 프롬프트"로 출력할 것
(설명 금지, 코드 블록 금지, 수정 설명 금지)

////////////////////////////////////////////////////////////