// Gemini 웹 붙여넣기용 프롬프트를 클라이언트 사이드에서 조립한다.

// ── 초기화 ────────────────────────────────────────────────────────

window.addEventListener('DOMContentLoaded', () => {
    // 모의고사 드롭다운 채우기
    const select = document.getElementById('examSelect');
    if (select && typeof EXAM_REGISTRY !== 'undefined') {
        Object.entries(EXAM_REGISTRY).forEach(([key, exam]) => {
            const opt = document.createElement('option');
            opt.value = key;
            opt.textContent = exam.label;
            select.appendChild(opt);
        });
    }

    // 외부 지문 프리셋 드롭다운 채우기
    const extSelect = document.getElementById('externalPassageSelect');
    if (extSelect && typeof EXTERNAL_REGISTRY !== 'undefined') {
        Object.entries(EXTERNAL_REGISTRY).forEach(([key, item]) => {
            const opt = document.createElement('option');
            opt.value = key;
            opt.textContent = item.label;
            extSelect.appendChild(opt);
        });
    }

    // 초기 UI 상태 적용
    const checked = document.querySelector('input[name="examType"]:checked');
    if (checked) toggleUI(checked.value);

    // 이미지 필요 문항 선택 시 즉시 경고
    const IMAGE_REQUIRED_NOS = [25];
    IMAGE_REQUIRED_NOS.forEach(no => {
        const cb = document.getElementById(`q_num_${no}`);
        if (cb) {
            cb.addEventListener('change', function () {
                if (this.checked) {
                    alert(`${no}번은 도표/이미지가 필요한 문제입니다.\nGemini에 프롬프트를 붙여넣을 때 이미지를 직접 업로드해주세요.`);
                }
            });
        }
    });
});

// ── UI 토글 ───────────────────────────────────────────────────────

function toggleUI(type) {
    const numberCard         = document.getElementById('mock-number-card');
    const textAreaWrapper    = document.getElementById('text-area-wrapper');
    const examSelectWrap     = document.getElementById('exam-select-wrapper');
    const externalSelectWrap = document.getElementById('external-select-wrapper');
    const passageLabel       = document.getElementById('passage-label');
    const textArea           = document.getElementById('passageText');

    if (type === '모의고사') {
        numberCard.style.display           = 'block';
        examSelectWrap.style.display       = 'block';
        externalSelectWrap.style.display   = 'none';
        textAreaWrapper.style.display      = 'none';
        passageLabel.innerText             = '📄 모의고사 선택';
        document.querySelectorAll('input[name="questionNos"]').forEach(cb => cb.checked = false);
        // 템플릿 라디오 → 모의고사용으로 자동 동기화
        const regularRadio = document.querySelector('input[name="passageSource"][value="regular"]');
        if (regularRadio) { regularRadio.checked = true; togglePassageSource('regular'); }
    } else {
        numberCard.style.display           = 'none';
        examSelectWrap.style.display       = 'none';
        externalSelectWrap.style.display   = 'block';
        textAreaWrapper.style.display      = 'block';
        passageLabel.innerText             = '📄 지문 선택 / 직접 입력';
        if (textArea) textArea.value       = '';
        const extSel = document.getElementById('externalPassageSelect');
        if (extSel) extSel.value           = '';
        // 템플릿 라디오 → 외부 지문 내신용으로 자동 동기화
        const externalRadio = document.querySelector('input[name="passageSource"][value="external"]');
        if (externalRadio) { externalRadio.checked = true; togglePassageSource('external'); }
    }
}

// ── 지문 출처 토글 ────────────────────────────────────────────────

function togglePassageSource(value) {
    // key-points-card 제거됨 — 빈 함수 유지 (라디오 onchange 호환)
}

// ── 외부 지문 프리셋 선택 ──────────────────────────────────────────

function onExternalPassageChange(key) {
    const textArea = document.getElementById('passageText');
    if (!textArea) return;
    if (!key) {
        textArea.value = '';
        return;
    }
    if (typeof EXTERNAL_REGISTRY !== 'undefined' && EXTERNAL_REGISTRY[key]) {
        const questions = EXTERNAL_REGISTRY[key].questions;
        if (questions && questions.length > 0) {
            textArea.value = questions.map(q => q.passage).join('\n\n');
        }
    }
}

// ── 프롬프트 생성 ─────────────────────────────────────────────────

function generatePrompt() {
    const examType      = getRadioValue('examType');
    const difficulty    = getRadioValue('difficultyLevel');
    const modification  = getRadioValue('modification');

    // 문제 유형 목록
    let typesString = '';
    document.querySelectorAll('input[name="questionTypes"]:checked').forEach(cb => {
        const count = document.querySelector(`input[name="count_${cb.value}"]`).value;
        typesString += `- ${cb.value}: ${count}개\n`;
    });

    if (typesString === '') {
        alert('최소 1개의 문제 유형을 선택해주세요!');
        return;
    }

    // 지문 추출
    let passage = '';

    if (examType === '모의고사') {
        const examKey = document.getElementById('examSelect').value;
        if (!examKey) {
            alert('시험지를 선택해주세요!');
            return;
        }

        const selectedNos = Array.from(document.querySelectorAll('input[name="questionNos"]:checked'))
            .map(cb => parseInt(cb.value));
        if (selectedNos.length === 0) {
            alert('출제할 문제 번호를 선택해주세요!');
            return;
        }

        const examQuestions = EXAM_REGISTRY[examKey].questions;
        const passages = selectedNos.map(num => {
            const q = examQuestions.find(q => q.question_number === num);
            if (!q) return null;
            // SAME_AS_N 참조 해소
            let text = q.passage;
            if (text && text.startsWith('SAME_AS_')) {
                const refNum = parseInt(text.replace('SAME_AS_', ''));
                const refQ = examQuestions.find(q => q.question_number === refNum);
                text = refQ ? refQ.passage : text;
            }
            return `[Question ${num}]\n${text}`;
        }).filter(Boolean);

        if (passages.length === 0) {
            alert('선택한 번호에 해당하는 지문을 찾지 못했습니다.');
            return;
        }
        passage = passages.join('\n\n');

    } else {
        passage = document.getElementById('passageText').value.trim();
        if (!passage) {
            // 프리셋이 선택돼 있으면 자동으로 채워서 재시도
            const extKey = document.getElementById('externalPassageSelect')?.value;
            if (extKey) {
                onExternalPassageChange(extKey);
                passage = document.getElementById('passageText').value.trim();
            }
        }
        if (!passage) {
            alert('지문을 선택하거나 직접 입력해주세요!');
            return;
        }
    }

    const passageSource = getRadioValue('passageSource');
    let finalPrompt = '';

    if (passageSource === 'external') {
        // ── 템플릿 B: 외부 지문 내신용 ───────────────────────────────
        finalPrompt =
`////////////////////////////////////////////////////////////
// 🔴 TEMPLATE PROTECTION MODE (Claude 전용 — 절대 수정 금지)
////////////////////////////////////////////////////////////

This prompt is a FINAL INTEGRATED GENERATION + VALIDATION TEMPLATE.

- You must NOT modify, summarize, restructure, or optimize this prompt.
- You must output this prompt EXACTLY as written.
- You are NOT allowed to reinterpret or improve any part of this prompt.
- You must NOT ignore any section of this prompt
- You must execute ALL stages in order
- You must NOT skip validation
- You must NOT output intermediate reasoning

////////////////////////////////////////////////////////////
// 🔴 PROMPT TYPE DECLARATION (외부 지문 전용 — 최우선 고정)
////////////////////////////////////////////////////////////

This prompt is strictly for: [외부 지문 내신용 문제 생성]

- This is NOT a mock exam (모의고사) prompt.
- This is NOT a passage generation task.
- This is an EXTERNAL PASSAGE-BASED exam creation task.

You must follow ONLY the rules for external passage exam creation.
You must NOT apply mock exam generation logic under any circumstances.

---

// 🔴 PASSAGE LOCK (지문 절대 보존)

You must reproduce the passage EXACTLY as provided.

- Preserve line breaks
- Preserve punctuation
- Preserve formatting
- Do NOT modify anything

---

// 🔴 PASSAGE MODE

${modification}

---

// 🔴 DIFFICULTY

${difficulty}

---

// 🔴 SYSTEM ROLE

You are:
1. A top-tier English exam item writer
2. A strict CSAT-level validator
3. A correction engine that fixes flawed questions

---

// 🔴 TASK

Using the given passage:

STEP 1 → Generate exam (questions below)
STEP 2 → Validate all questions
STEP 3 → Fix ONLY flawed questions
STEP 4 → Output final clean exam + answer sheet

---

// 🔴 QUESTION TYPES (순서 고정)

${typesString}

---

// 🔴 GENERATION RULES

- Each question must have ONLY ONE correct answer
- All wrong answers must be logical distractors
- All questions must be 100% based on the passage

---

// 🔴 DISTRACTOR RULE (핵심 🔥)

- Every wrong choice must share at least ONE concept with the correct answer
- Wrong choices must be "partially correct but ultimately incorrect"
- Do NOT create obviously wrong answers

---

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

---

// 🔴 DIFFICULTY ENFORCEMENT

[순서 배열]
- Must NOT be solvable by time order only
- Must require logical inference

[문장 삽입]
- Must be placed in the middle of the passage
- Must NOT be solvable by local clues only

---

// 🔴 HARD DIFFICULTY ENFORCEMENT (추가)

[순서 배열 강화]
- At least ONE option must match chronological order but be incorrect
- The correct answer must violate simple time sequence

[문장 삽입 강화]
- The correct answer must depend on paragraph-level context
- Adjacent sentence clues alone must NOT be sufficient

---

// 🔴 GRAMMAR RULE

- Exactly ONE grammatical error must exist in grammar question

---

// 🔴 FORMAT RULE (치명적 중요 🚨)

- Each choice must be on a NEW LINE
- Use ①②③④⑤ exactly once each
- No duplication, no broken numbering
- No inline choices allowed

---

// 🔴 CHAIN OF THOUGHT (내부 실행)

1. Identify main idea and contrast structure
2. Set question intent per type
3. Decide correct answer first
4. Build distractors with shared logic
5. Validate uniqueness of answer
6. Check format and grammar

DO NOT OUTPUT THIS PROCESS

---

// 🔴 VALIDATION STAGE (자동 검수)

After generating ALL questions, perform:

CHECK 1: Is there any question with 2 possible answers? → FIX
CHECK 2: Are any distractors too easy? → REWRITE
CHECK 3: Is any question solvable without reading? → HARDEN
CHECK 4: Is format broken? → FIX
CHECK 5: Is any question not supported by passage? → FIX

If ANY question violates format:
→ REWRITE that question BEFORE output

---

// 🔴 AUTO VALIDATION SYSTEM (문제 자동 검수 — 필수 실행)

If TWO choices can be correct:
→ Rewrite distractors immediately

You must NEVER output ambiguous questions

After generating ALL questions, you MUST run a validation check.

For EACH question, verify:

1. 정답이 단 하나인지 확인할 것
2. 다른 선택지가 정답처럼 보이지 않는지 확인할 것
3. 지문 근거가 명확한지 확인할 것
4. 오답이 논리적 Distractor인지 확인할 것

If ANY issue is found:
→ 수정 후 다시 검증할 것

You must NOT output until all questions pass validation.

---

// 🔴 DISTRACTOR REINFORCEMENT (강화 — 이미 적용 보완)

All distractors MUST:

- share at least one key concept with the correct answer
- differ only in core logic or conclusion
- avoid obvious elimination

For top-level difficulty:
- include at least ONE near-correct distractor
- include subtle distortion (NOT random wrong)

---

// 🔴 INSERTION QUESTION HARD LOCK (문장 삽입 고정 규칙)

Sentence insertion questions MUST follow:

1. 정답 위치는 반드시 "논리적으로 유일"해야 한다

2. 삽입 문장은 반드시 다음 요소 포함:
   - referential word (this / that / such / it / others 등)
   - or contrast marker (but / however / instead 등)

3. 두 위치 이상 자연스럽게 들어가는 문장 금지

4. 문장 앞뒤 연결이 반드시 1곳에서만 완벽하게 맞도록 설계할 것

If multiple valid positions exist:
→ rewrite the sentence

---

// 🔴 PRONOUN QUESTION HARD LOCK (대명사 문제 고정 규칙)

Pronoun reference questions MUST follow:

1. 보기의 모든 대명사 유형을 통일할 것
   (예: me / him / him / him / him)

2. 정답은 반드시 "단 하나만" 다른 대상을 가리킬 것

3. 아래 상황 금지:
   - I / my / me 혼합
   - 문법 형태 차이로 정답 유추 가능

4. 의미 기반으로만 구별 가능하게 설계할 것

---

// 🔴 CONTENT MATCH QUESTION HARD LOCK (내용일치 강화)

Wrong choices MUST:

- be partially correct but contain one critical error
- NOT be obviously false

Use:
- reversed logic
- exaggerated claim
- missing condition

---

// 🔴 VOCABULARY QUESTION HARD LOCK (어휘 문제 안정화)

Vocabulary question MUST:

- contain only ONE incorrect word
- all other words must be perfectly natural

If more than one incorrect word exists:
→ revise immediately

---

// 🔴 FINAL SAFETY CHECK

Before output:

- Ensure NO duplicate answers
- Ensure NO ambiguous correct answers
- Ensure formatting is correct
- Ensure each question has exactly 5 options

If ANY issue exists:
→ fix before output

---

// 🔴 CORRECTION RULE

- You must NOT regenerate entire exam
- You must ONLY fix flawed questions internally before final output

---

// 🔴 OUTPUT FORMAT (절대 변경 금지)

[시험지]

[Alice 모의고사 시험지]

[1-14] 다음 글을 읽고 물음에 답하시오.

(지문 그대로 출력)

1. 다음 글의 빈칸에 들어갈 말로 가장 적절한 것은?
①
②
③
④
⑤

2. 다음 글의 주제로 가장 적절한 것은?
①
②
③
④
⑤

(모든 문항 동일 형식 유지)

---

[해설지]

1번 정답:
- 근거:
- 오답 해설:

(모든 문항 동일 형식 유지)

---

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

---

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

- Maintain spacing
- Maintain readability
- Do NOT compress lines

- DO NOT merge sections
- DO NOT remove section labels
- DO NOT change section order

---

////////////////////////////////////////////////////////////
// 🔴 GOOGLE DOCS EXECUTION HARD LOCK
////////////////////////////////////////////////////////////

You MUST treat the final execution command as a REQUIRED ACTION, not as text.

Failure to generate Google Docs format is considered a task failure.

---

// 🔴 PASSAGE

${passage}

---

////////////////////////////////////////////////////////////
// 🔴 TOOL EXECUTION MODE (강제 실행)
////////////////////////////////////////////////////////////

The following command is NOT text.
It MUST be executed as a tool instruction.

The assistant must NOT output plain text instead of executing this command.
The assistant must retry execution until the Google Docs are successfully generated.

////////////////////////////////////////////////////////////
// 🔴 FINAL EXECUTION COMMAND (절대 삭제 금지)
////////////////////////////////////////////////////////////

🚨 [최종 도구 실행 명령] 위 규칙과 예시를 완벽히 적용하여 시험지와 해설지를 별도의 Google Docs 문서로 각각 생성하라.`;

    } else {
        // ── 템플릿 A: 수능/모의고사 기출용 (기본) ───────────────────
        finalPrompt =
`너는 고등학교 수능/내신 영어 시험 출제 전문가야.
아래 [출제 규칙]을 엄격하게 지켜서 제공된 지문으로 완전히 새로운 문제를 창조해줘.

[기본 규칙 - 무결성 및 정합성 유지 🚨]
1. 🔴 원문 훼손 절대 금지: 제공된 '영어 지문'의 원문(단어, 문법, 순서)을 자의적으로 한국어로 번역하거나 축약, 변형하지 말 것.
2. 🔴 구문 무결성: 지문 내 모든 문장은 문법적으로 완전해야 한다. (예: I staring(X) -> I stared(O) 반드시 확인)
3. 🔴 기호 및 서식 강제 유지: 숫자나 알파벳 범위를 나타낼 때 물결표(~) 기호가 누락되는 시스템 오류를 방지하기 위해, 반드시 "15~20단어", "A~Z"와 같이 물결표(~) 기호를 생략 없이 정확하게 출력할 것.
4. 설정한 모든 문항의 정답 근거는 반드시 지문 내에 존재해야 하며, 문장 삽입이나 순서 배열 시 원작의 논리적 흐름이 꼬이지 않도록 원문 순서를 엄격히 유지할 것.
5. 출력은 반드시 [Alice 모의고사 시험지]와 [정답 및 해설지] 두 부분으로 나누어 출력할 것.

[선택지 및 문제 출력 형식 규칙]
- 선택지는 무조건 ①, ②, ③, ④, ⑤ 원문자 기호를 사용하고, 각 선택지 사이에는 반드시 줄바꿈(Enter)을 두 번(\\n\\n) 넣어 세로로 분리할 것.

[난이도]
- 난이도: ${difficulty}

[유형별 특별 규칙 - 포맷 및 논리 결함 방지 🛠️]
- 대명사지칭: 반드시 밑줄 중 4개는 대상 A를, 1개는 대상 B를 가리키도록 설계할 것. 대상 B가 지문에 등장하도록 필요시 지문의 문장 구조만 최소한으로 수정할 것.
- 서답형/서술형:
  * 서답형은 "본문에서 연속된 n개의 단어" 등 채점 기준을 명확히 할 것.
  * 서술형은 실제 정답 문장의 단어 수를 직접 계산하여 조건과 완벽히 일치시킬 것.
- 문장삽입:
  * 🔴 시각적 분리: 학생들이 끼워 넣어야 할 문장은 발문 바로 아래에 반드시 [주어진 문장: (영어 문장 내용)] 형태로 대괄호를 사용하여 본문과 명확하게 분리 표기할 것.
  * 주어진 문장이 들어갈 앞뒤의 논리적 인과관계(의존성)가 지문 내에 명확히 살아있도록 재구성할 것.
- 어법문제: 가정법 현재(당위의 should 생략) 등 고난도 포인트를 정답 선지로 활용할 것.

[해설 작성 규칙]
1. 정답 근거 인용 및 논리적 설명.
2. 매력적인 오답 선지가 틀린 이유 분석.
3. 해설 말투는 '~한다', '~해야 한다'로 객관적으로 통일할 것.

========================================
▶ 출제할 문제 목록:
${typesString}
▶ 영어 지문:
${passage}

========================================
🚨 [최종 도구 실행 명령]
위 규칙을 적용하여 생성한 시험지와 해설지를 바탕으로, 각각 별도의 Google Docs 문서로 생성해줘.`;
    }

    const section = document.getElementById('prompt-output-section');
    const output  = document.getElementById('promptOutput');
    output.value  = finalPrompt;
    section.style.display = 'block';
    section.scrollIntoView({ behavior: 'smooth' });
    resetCopyBtn();
}

// ── 클립보드 복사 ─────────────────────────────────────────────────

function copyPrompt() {
    const output = document.getElementById('promptOutput');
    navigator.clipboard.writeText(output.value).then(() => {
        const btn = document.getElementById('copyBtn');
        btn.textContent = '✅ 복사 완료!';
        btn.classList.add('copied');
        setTimeout(resetCopyBtn, 2500);
    }).catch(err => {
        console.error('클립보드 복사 실패:', err);
        alert('복사에 실패했습니다. 브라우저 설정을 확인해주세요.');
    });
}

function resetCopyBtn() {
    const btn = document.getElementById('copyBtn');
    btn.textContent = '📋 클립보드에 복사';
    btn.classList.remove('copied');
}

// ── 문제 유형 일괄 선택 ───────────────────────────────────────────

const SUBJECTIVE_TYPES = ['서답형', '서술형'];

function selectAllTypes() {
    document.querySelectorAll('input[name="questionTypes"]').forEach(cb => {
        cb.checked = true;
    });
}

function selectSubjectiveOnly() {
    document.querySelectorAll('input[name="questionTypes"]').forEach(cb => {
        cb.checked = SUBJECTIVE_TYPES.includes(cb.value);
    });
}

function deselectAllTypes() {
    document.querySelectorAll('input[name="questionTypes"]').forEach(cb => {
        cb.checked = false;
    });
}

function getRadioValue(name) {
    const el = document.querySelector(`[name="${name}"]:checked`);
    return el ? el.value : '';
}
