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
    } else {
        numberCard.style.display           = 'none';
        examSelectWrap.style.display       = 'none';
        externalSelectWrap.style.display   = 'block';
        textAreaWrapper.style.display      = 'block';
        passageLabel.innerText             = '📄 지문 선택 / 직접 입력';
        if (textArea) textArea.value       = '';
        const extSel = document.getElementById('externalPassageSelect');
        if (extSel) extSel.value           = '';
    }
}

// ── 지문 출처 토글 ────────────────────────────────────────────────

function togglePassageSource(value) {
    const card = document.getElementById('key-points-card');
    card.style.display = value === 'external' ? 'block' : 'none';
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
    const examType   = getRadioValue('examType');
    const difficulty = getRadioValue('difficultyLevel');
    const modification = getRadioValue('modification');

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
        const keyPoints = document.getElementById('keyPointsText').value.trim();
        if (!keyPoints) {
            alert('핵심 출제 포인트를 입력해주세요!');
            return;
        }
        finalPrompt =
`너는 고등학교 영어 내신 시험 출제 전문가야.
제공된 외부 지문(원서/부교재)을 바탕으로 아래 [출제 규칙]을 엄격하게 지켜서 내신 대비용 문제를 창조해줘.

[기본 규칙 - 외부 지문 무결성 🚨]
1. 이 프롬프트는 '외부 지문' 전용이며, 지문의 원문을 자의적으로 한국어로 번역하거나 변형하지 않는다.
2. 🔴 구문 및 시제 무결성: 지문 내 시제와 시간 부사의 호응을 철저히 검수한다. (예: six years ago와 같은 특정 과거 시점이 있으면 had had(과거완료)가 아닌 had(단순과거)가 쓰였는지 확인)
3. 🔴 힌트 노출 방지: 특정 어휘 문제의 정답이 바로 뒤 문장에 그대로 등장하여 정답을 유추할 수 있게 하는 '데이터 오염'을 방지한다. 반드시 유사한 의미의 다른 단어로 패러프레이징하여 오답 선지를 구성할 것.
4. 🔴 기호 및 서식 강제 유지: 숫자 범위에는 반드시 물결표(~) 기호를 생략 없이 출력한다. (예: 10~15단어)

[선택지 및 문제 출력 형식 규칙]
- 선택지는 ①, ②, ③, ④, ⑤ 원문자 기호를 사용하고, 각 선택지 사이에는 반드시 줄바꿈(Enter)을 두 번(\\n\\n) 넣어 세로로 분리한다.
- 문장 삽입 문제: 끼워 넣어야 할 문장은 발문 바로 아래에 [주어진 문장: (영어 문장 내용)] 형태로 대괄호를 사용하여 본문과 명확하게 분리 표기한다.

[내신 특화 출제 방향 🎯]
- 난이도: ${difficulty}
- [핵심 반영 포인트]: 사용자가 입력한 아래의 핵심 출제 포인트(어법, 핵심어 등)를 반드시 문제(특히 어법, 서술형, 빈칸)의 정답으로 최우선 반영하여 설계한다.
  👉 포인트 내용: ${keyPoints}

[유형별 특별 규칙 - 오류 방지 로직 🛠️]
- 대명사지칭: 밑줄 중 4개는 대상 A, 1개는 대상 B를 가리키도록 설계한다. 대상 B가 지문에 등장하도록 문장 구조를 최소한으로 수정한다.
- 서답형(주관식 단답): "본문에서 연속된 n개의 단어로 찾아 쓸 것"처럼 명확한 채점 기준을 제시한다.
- 서술형(조건영작):
  * 🔴 단어 수 검증: 실제 정답 문장의 단어 수를 직접 세어보고, 조건 범위(예: 10~15단어)가 정답을 포함하는지 반드시 확인한다.
  * 지문 내 타겟 문장을 [ TARGET SENTENCE REDACTED ]로 가리고 조건을 명시한다.

[해설 작성 규칙]
1. 정답 근거 인용 및 논리적 설명.
2. 매력적인 오답 선지가 틀린 이유 분석.
3. 해설 말투는 '~한다', '~해야 한다'로 객관적으로 통일한다.

========================================
▶ 출제할 문제 목록:
${typesString}

▶ 외부 영어 지문:
${passage}

========================================
🚨 [최종 도구 실행 명령]
위 규칙을 적용하여 생성한 시험지와 해설지를 바탕으로, 각각 별도의 Google Docs 문서로 생성해줘.`;

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

function getRadioValue(name) {
    const el = document.querySelector(`[name="${name}"]:checked`);
    return el ? el.value : '';
}
