// 🔴 생성된 문제를 검토하는 화면의 인터랙션 로직이다. 🔴
// 🔴 문제지 / 해설지 두 뷰를 전환하며 편집·재생성·저장 기능을 제공한다. 🔴

let parsedQuestions = [];
let currentView = 'questions';  // 'questions' | 'answers'

// ── 파싱 헬퍼 ───────────────────────────────────────────────────

// 🔴 태그 사이의 텍스트를 추출한다. endTag가 null이면 블록 끝까지 가져온다. 🔴
function extractBetween(block, startTag, endTag) {
    const start = block.indexOf(startTag);
    if (start === -1) return '';
    const contentStart = start + startTag.length;
    const end = endTag ? block.indexOf(endTag, contentStart) : block.length;
    return block.substring(contentStart, end === -1 ? block.length : end).trim();
}

// 🔴 AI 응답 전체를 파싱해 문제 객체 배열로 변환한다. 🔴
function parseAiResponse(raw) {
    return raw.split('---SEP---')
        .map(b => b.trim())
        .filter(Boolean)
        .map((block, idx) => ({
            index:       idx,
            question:    cleanText(extractBetween(block, '[[QUESTION]]',    '[[PASSAGE]]')),
            passage:     cleanText(extractBetween(block, '[[PASSAGE]]',     '[[OPTIONS]]')),
            options:     extractBetween(block, '[[OPTIONS]]',     '[[ANSWER]]'),
            answer:      extractBetween(block, '[[ANSWER]]',      '[[EXPLANATION]]').trim(),
            explanation: cleanText(extractBetween(block, '[[EXPLANATION]]', null))
        }));
}

// 🔴 AI 응답에 섞인 이상 기호(단독 ?, ? —>, 인코딩 오류 등)를 제거한다. 🔴
function cleanText(str) {
    if (!str) return '';
    return str
        .split('\n')
        .filter(line => !/^\s*\?\s*$/.test(line))          // 단독 "?" 줄 제거
        .filter(line => !/^\?\s*[-—>]/.test(line))          // "? --->" 형태 제거
        .filter(line => !/^\s*\?\s+/.test(line))            // "? " 로 시작하는 줄 제거
        .join('\n')
        .trim();
}

// ── 텍스트 렌더링 ────────────────────────────────────────────────

// 🔴 HTML 특수문자를 이스케이프한다. 🔴
function escapeHtml(str) {
    if (!str) return '';
    return str
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

// 🔴 HTML을 이스케이프하되 AI가 사용하는 <u> 밑줄 태그만 허용한다. 🔴
function renderText(str) {
    if (!str) return '';
    return escapeHtml(str)
        .replace(/&lt;u&gt;/g, '<u>')
        .replace(/&lt;\/u&gt;/g, '</u>');
}

// 🔴 선택지 (1)~(5) 형식을 ①~⑤ 원문자로 변환해 가독성을 높인다. 🔴
function formatOptions(str) {
    if (!str) return '';
    return renderText(str)
        .replace(/^\(1\)\s*/gm, '① ')
        .replace(/^\(2\)\s*/gm, '② ')
        .replace(/^\(3\)\s*/gm, '③ ')
        .replace(/^\(4\)\s*/gm, '④ ')
        .replace(/^\(5\)\s*/gm, '⑤ ');
}

// ── 완전성 검증 ──────────────────────────────────────────────────

// 🔴 문제·선택지·정답이 모두 있어야 완전한 문제로 판단한다. 🔴
function isComplete(q) {
    return q.question.trim() !== ''
        && q.options.trim()  !== ''
        && q.answer.trim()   !== '';
}

// ── 카드 빌더 ────────────────────────────────────────────────────

// 🔴 문제지용 단일 문제 블록 HTML을 생성한다. 불완전한 문제는 경고 배너를 표시한다. 🔴
function buildExamQuestion(q) {
    const incomplete = !isComplete(q);
    return `<div class="exam-question${incomplete ? ' q-incomplete' : ''}" id="card-${q.index}">
        ${incomplete ? `<div class="incomplete-banner">⚠️ 불완전한 문제 — 생성 중 응답이 끊겼습니다. 재생성하거나 삭제하세요.</div>` : ''}
        <div class="q-header">
            <span class="q-num">${q.index + 1}.</span>
            <div class="q-body">
                <div class="q-text-view" id="qtext-view-${q.index}">${renderText(q.question)}</div>
                <textarea class="q-text-edit" id="qtext-edit-${q.index}"
                          oninput="syncEdit(${q.index})">${escapeHtml(q.question)}</textarea>
            </div>
            <div class="q-actions">
                <button class="btn-sm btn-edit" onclick="toggleEdit(${q.index})">편집</button>
                <button class="btn-sm btn-regen" onclick="regenerate(${q.index})">재생성</button>
            </div>
        </div>
        ${q.passage ? `<div class="q-passage">${renderText(q.passage)}</div>` : ''}
        <div class="q-options">${formatOptions(q.options)}</div>
    </div>`;
}

// 🔴 해설지용 단일 답안 블록 HTML을 생성한다. 🔴
function buildAnswerItem(q) {
    return `<div class="answer-item" id="ans-${q.index}">
        <div class="ans-header">
            <span class="ans-num">문제 ${q.index + 1}.</span>
            <span class="ans-correct">정답&nbsp;${escapeHtml(q.answer)}</span>
        </div>
        ${q.explanation
            ? `<div class="ans-explanation">${renderText(q.explanation)}</div>`
            : '<div class="ans-explanation ans-empty">해설이 없습니다.</div>'}
    </div>`;
}

// ── 렌더링 ───────────────────────────────────────────────────────

// 🔴 문제지 + 해설지 두 컨테이너를 동시에 채운다. 불완전 문제가 있으면 카운트에 경고를 표시한다. 🔴
function renderAll() {
    document.getElementById('view-questions').innerHTML =
        parsedQuestions.map(buildExamQuestion).join('');
    document.getElementById('view-answers').innerHTML =
        parsedQuestions.map(buildAnswerItem).join('');

    const total      = parsedQuestions.length;
    const incomplete = parsedQuestions.filter(q => !isComplete(q)).length;
    const countEl    = document.getElementById('question-count');

    if (incomplete > 0) {
        countEl.innerHTML =
            `총 ${total}개 문제 <span class="count-warn">⚠️ ${incomplete}개 불완전</span>`;
    } else {
        countEl.textContent = `총 ${total}개 문제`;
    }
}

// 🔴 문제지 ↔ 해설지 뷰를 전환한다. 🔴
function switchView(view) {
    currentView = view;
    document.getElementById('view-questions').style.display = view === 'questions' ? 'block' : 'none';
    document.getElementById('view-answers').style.display   = view === 'answers'   ? 'block' : 'none';
    document.getElementById('btn-view-questions').classList.toggle('active', view === 'questions');
    document.getElementById('btn-view-answers').classList.toggle('active',   view === 'answers');
}

// ── 편집 ─────────────────────────────────────────────────────────

// 🔴 편집 모드 토글 — 뷰↔편집 textarea 전환 🔴
function toggleEdit(index) {
    const view = document.getElementById(`qtext-view-${index}`);
    const edit = document.getElementById(`qtext-edit-${index}`);
    const btn  = document.querySelector(`#card-${index} .btn-edit`);
    const isEditing = edit.style.display === 'block';

    if (isEditing) {
        view.style.display = 'block';
        edit.style.display = 'none';
        btn.classList.remove('active');
    } else {
        view.style.display = 'none';
        edit.style.display = 'block';
        btn.classList.add('active');
        edit.focus();
    }
}

// 🔴 편집 textarea 내용을 parsedQuestions 배열과 뷰에 실시간 반영한다. 🔴
function syncEdit(index) {
    const val = document.getElementById(`qtext-edit-${index}`).value;
    parsedQuestions[index].question = val;
    document.getElementById(`qtext-view-${index}`).innerHTML = renderText(val);
}

// ── 재생성 ───────────────────────────────────────────────────────

// 🔴 개별 문제 재생성 — 문제지·해설지 두 블록을 동시에 교체한다. 🔴
function regenerate(index) {
    const q    = parsedQuestions[index];
    const card = document.getElementById(`card-${index}`);
    card.innerHTML = `<div class="card-loading">재생성 중...</div>`;

    const firstLine = q.question.split('\n')[0] || '';
    const params = new URLSearchParams({
        questionType: firstLine,
        passageText:  q.passage || q.question
    });

    fetch('/api/regenerate-question', { method: 'POST', body: params })
        .then(r => { if (!r.ok) throw new Error(); return r.text(); })
        .then(raw => {
            const newQ = parseAiResponse(raw)[0];
            if (newQ) {
                newQ.index = index;
                parsedQuestions[index] = newQ;
                card.outerHTML = buildExamQuestion(newQ);
                const ansEl = document.getElementById(`ans-${index}`);
                if (ansEl) ansEl.outerHTML = buildAnswerItem(newQ);
            } else {
                card.outerHTML = buildExamQuestion(q);
                showToast('재생성 결과를 파싱하지 못했습니다.');
            }
        })
        .catch(() => {
            card.outerHTML = buildExamQuestion(q);
            showToast('재생성 중 오류가 발생했습니다.');
        });
}

// ── 복사 / 저장 ──────────────────────────────────────────────────

// 🔴 전체 문제를 텍스트로 클립보드에 복사한다. 🔴
function copyAll() {
    // 🔴 현재 '해설지' 버튼에 'active' 클래스가 있는지 확인하여 뷰 상태를 파악한다. 🔴
    const isAnswerView = document.getElementById('btn-view-answers').classList.contains('active');

    const text = parsedQuestions.map((q, i) => {
        // 🔴 문제지, 해설지 공통으로 들어갈 기본 항목을 배열로 구성한다. 🔴
        const content = [
            `[문제 ${i + 1}]`,
            q.question,
            '',
            q.passage || '',
            '',
            q.options
        ];

        // 🔴 뷰 상태에 따라 정답과 해설 추가 여부를 결정한다. 🔴
        if (isAnswerView) {
            // 🔴 해설지 뷰일 경우 정답과 해설(데이터가 존재할 경우)을 모두 추가한다. 🔴
            content.push(`정답: ${q.answer}`);
            if (q.explanation) {
                content.push(`해설: ${q.explanation}`);
            }
        }

        return content.filter(Boolean).join('\n');
    }).join('\n\n' + '─'.repeat(40) + '\n\n');
        
    navigator.clipboard.writeText(text)
        .then(() => showToast('클립보드에 복사되었습니다.'))
        .catch(() => showToast('복사 실패 — 브라우저 권한을 확인하세요.'));
}

// 🔴 검토·편집이 끝난 문제 목록을 /api/save-questions로 POST해 DB에 저장한다. 🔴
// 🔴 불완전한 문제(선택지 또는 정답 누락)는 저장에서 제외하고 사용자에게 알린다. 🔴
function saveToDb() {
    const complete   = parsedQuestions.filter(isComplete);
    const skipped    = parsedQuestions.length - complete.length;

    if (complete.length === 0) {
        showToast('저장 가능한 완전한 문제가 없습니다. 재생성해 주세요.');
        return;
    }
    if (skipped > 0) {
        const proceed = confirm(
            `⚠️ ${skipped}개 문제는 불완전하여 저장에서 제외됩니다.\n` +
            `${complete.length}개 문제만 저장하시겠습니까?`
        );
        if (!proceed) return;
    }

    const btn = document.getElementById('btn-save');
    btn.disabled = true;
    btn.textContent = '저장 중...';

    const examType = document.getElementById('exam-type').value || '모의고사';
    const payload = {
        examType,
        questions: complete.map(q => ({
            question:    q.question    || '',
            passage:     q.passage     || '',
            options:     q.options     || '',
            answer:      q.answer      || '',
            explanation: q.explanation || ''
        }))
    };

    fetch('/api/save-questions', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    })
    .then(r => { if (!r.ok) throw new Error(); return r.json(); })
    .then(data => {
        showToast(`${data.savedCount}개 문제가 DB에 저장되었습니다.`);
        btn.textContent = '저장 완료 ✓';
        btn.style.background = '#16a34a';
        btn.style.borderColor = '#16a34a';
    })
    .catch(() => {
        showToast('저장 중 오류가 발생했습니다.');
        btn.disabled = false;
        btn.textContent = 'DB에 저장';
    });
}

// ── 유틸 ─────────────────────────────────────────────────────────

// 🔴 토스트 메시지를 잠깐 표시하고 자동으로 숨긴다. 🔴
function showToast(msg) {
    const toast = document.getElementById('toast');
    toast.textContent = msg;
    toast.classList.add('show');
    setTimeout(() => toast.classList.remove('show'), 3000);
}

// ── 초기화 ───────────────────────────────────────────────────────

// 🔴 페이지 로드 시 raw-data textarea의 AI 응답을 파싱해서 렌더링한다. 🔴
document.addEventListener('DOMContentLoaded', function () {
    const raw = document.getElementById('raw-data').value;
    if (!raw || raw.trim() === '') {
        document.getElementById('view-questions').innerHTML =
            '<p class="empty-msg">생성된 문제가 없습니다.</p>';
        return;
    }

    parsedQuestions = parseAiResponse(raw);

    if (parsedQuestions.length === 0) {
        document.getElementById('view-questions').innerHTML =
            '<p class="empty-msg">문제를 파싱하지 못했습니다. 다시 생성해 주세요.</p>';
        return;
    }

    renderAll();
});
