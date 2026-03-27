<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>PDF 지문 추출 테스트</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: 'Malgun Gothic', sans-serif; background: #f0f2f5; padding: 32px; color: #1e293b; }

        .page-header {
            display: flex;
            align-items: center;
            gap: 12px;
            margin-bottom: 28px;
        }
        .page-header h1 { font-size: 20px; font-weight: 800; }
        .badge-dev {
            font-size: 11px;
            background: #fef3c7;
            color: #92400e;
            border: 1px solid #fcd34d;
            border-radius: 20px;
            padding: 2px 10px;
            font-weight: 600;
        }

        .card {
            background: #fff;
            border-radius: 10px;
            padding: 24px 28px;
            box-shadow: 0 1px 4px rgba(0,0,0,0.08);
            margin-bottom: 20px;
        }
        .card h2 { font-size: 14px; font-weight: 700; color: #475569; margin-bottom: 16px; }

        .form-row { display: flex; gap: 12px; align-items: flex-end; flex-wrap: wrap; }

        label { font-size: 13px; font-weight: 600; color: #374151; display: block; margin-bottom: 6px; }

        input[type="file"] {
            font-size: 13px;
            border: 1px solid #cbd5e1;
            border-radius: 6px;
            padding: 8px 10px;
            background: #f8fafc;
            cursor: pointer;
            flex: 1;
            min-width: 200px;
        }

        input[type="text"], input[type="number"] {
            font-size: 14px;
            border: 1px solid #cbd5e1;
            border-radius: 6px;
            padding: 9px 12px;
            width: 140px;
            font-family: inherit;
        }
        input[type="text"] { width: 200px; }
        input[type="text"]:focus, input[type="number"]:focus { outline: none; border-color: #2563eb; }

        .page-info {
            font-size: 12px;
            color: #64748b;
            margin-top: 6px;
            height: 16px;
        }
        .page-info.loading { color: #93c5fd; }
        .page-info.loaded { color: #16a34a; font-weight: 600; }

        .hint {
            font-size: 11px;
            color: #94a3b8;
            margin-top: 5px;
        }

        .btn-extract {
            padding: 9px 22px;
            background: #2563eb;
            color: #fff;
            border: none;
            border-radius: 6px;
            font-size: 14px;
            font-weight: 700;
            cursor: pointer;
            font-family: inherit;
        }
        .btn-extract:hover { background: #1d4ed8; }
        .btn-extract:disabled { background: #93c5fd; cursor: not-allowed; }

        /* 결과 영역 */
        #result-area { display: none; }

        .result-meta {
            font-size: 13px;
            color: #64748b;
            margin-bottom: 16px;
        }
        .result-meta strong { color: #1e293b; }

        /* 이미지 필요 문제 요약 배너 */
        .image-summary {
            background: #fef9c3;
            border: 1px solid #fde68a;
            border-radius: 8px;
            padding: 10px 16px;
            font-size: 13px;
            color: #92400e;
            margin-bottom: 16px;
            max-width: 760px;
        }
        .image-summary strong { font-weight: 700; }
        .badge-image {
            font-size: 11px;
            background: #fde68a;
            color: #78350f;
            border-radius: 4px;
            padding: 1px 7px;
            font-weight: 700;
            margin-left: 6px;
        }

        .passage-block {
            border: 1px solid #e2e8f0;
            border-radius: 8px;
            margin-bottom: 14px;
            overflow: hidden;
            max-width: 760px;   /* 시험지 페이지 너비 기준 */
        }
        .passage-header {
            background: #f1f5f9;
            padding: 10px 16px;
            font-size: 13px;
            font-weight: 700;
            color: #334155;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .passage-status {
            font-size: 11px;
            padding: 2px 8px;
            border-radius: 20px;
            font-weight: 600;
        }
        .status-ok   { background: #dcfce7; color: #166534; }
        .status-fail { background: #fee2e2; color: #991b1b; }

        .passage-body {
            padding: 14px 16px;
            background: #fff;
            max-height: 500px;
            overflow-y: auto;
            display: flex;
            flex-direction: column;
            gap: 10px;
        }

        /* DB 필드 행 레이아웃 */
        .q-fields { gap: 0 !important; padding: 6px 16px !important; }
        .f-row {
            display: flex;
            gap: 16px;
            font-size: 13px;
            padding: 5px 0;
            border-bottom: 1px solid #f1f5f9;
            align-items: flex-start;
        }
        .f-row:last-child { border-bottom: none; }
        .f-label {
            min-width: 130px;
            font-family: 'Courier New', monospace;
            font-size: 12px;
            font-weight: 700;
            color: #64748b;
            padding-top: 2px;
            flex-shrink: 0;
        }
        .f-val {
            color: #1e293b;
            white-space: pre-wrap;
            line-height: 1.7;
            font-family: 'Malgun Gothic', sans-serif;
        }
        .f-passage {
            font-family: 'Batang', '바탕', serif;
            line-height: 1.9;
        }
        .f-null { color: #94a3b8; font-style: italic; font-size: 12px; }
        .f-type { color: #2563eb; font-weight: 700; }
        .f-val u, .f-passage u { text-decoration: underline; font-style: normal; }

        .raw-toggle {
            font-size: 12px;
            color: #94a3b8;
            cursor: pointer;
            text-decoration: underline;
            margin-top: 8px;
            display: inline-block;
        }
        .raw-box {
            display: none;
            margin-top: 12px;
            background: #0f172a;
            color: #94a3b8;
            padding: 16px;
            border-radius: 8px;
            font-size: 12px;
            white-space: pre-wrap;
            overflow-x: auto;
            max-height: 400px;
            overflow-y: auto;
        }

        .spinner {
            display: none;
            width: 20px; height: 20px;
            border: 3px solid #e2e8f0;
            border-top-color: #2563eb;
            border-radius: 50%;
            animation: spin 0.7s linear infinite;
            margin-left: 8px;
        }
        @keyframes spin { to { transform: rotate(360deg); } }
    </style>
</head>
<body>

<div class="page-header">
    <h1>📄 PDF 지문 추출 테스트</h1>
    <span class="badge-dev">개발용 — 페이지 이미지 추출 모드</span>
</div>

<div class="card">
    <h2>추출 설정</h2>
    <div class="form-row">
        <div style="flex:1; min-width:220px;">
            <label for="pdfFile">모의고사 PDF 파일</label>
            <input type="file" id="pdfFile" accept=".pdf" onchange="onFileChange()">
            <p class="page-info" id="page-info"></p>
        </div>
        <div>
            <label for="startPage">시작 페이지 <span style="font-weight:400;color:#94a3b8">(1-indexed)</span></label>
            <input type="number" id="startPage" value="1" min="1">
            <p class="hint">다음 페이지도 함께 전송됩니다 (2페이지 걸침 대응)</p>
        </div>
        <div>
            <label for="questionNos">추출할 문제 번호 <span style="font-weight:400;color:#94a3b8">(쉼표/공백 구분)</span></label>
            <input type="text" id="questionNos" placeholder="예: 23, 24, 25" value="25">
        </div>
        <div style="display:flex; align-items:center; gap:8px;">
            <button class="btn-extract" id="btn-extract" onclick="runExtract()">Gemini 추출</button>
            <button class="btn-extract" id="btn-text" onclick="runTextExtract()" style="background:#16a34a;">📄 전체 파싱 (API 절약)</button>
            <button class="btn-extract" id="btn-scan" onclick="runScan()" style="background:#7c3aed;">📋 페이지 스캔</button>
            <div class="spinner" id="spinner"></div>
        </div>
    </div>
    <p style="font-size:12px; color:#94a3b8; margin-top:12px;">
        ※ PDFBox로 페이지를 PNG 변환 후 Gemini Vision으로 지문 추출합니다. 문제 생성 없이 추출만 수행합니다.
    </p>
</div>

<div class="card" id="scan-area" style="display:none;">
    <h2>📋 페이지 스캔 결과 <span style="font-size:12px;font-weight:400;color:#94a3b8">— 어떤 페이지에 어떤 문제가 있는지 확인</span></h2>
    <div id="scan-table"></div>
</div>

<div class="card" id="result-area">
    <h2>추출 결과</h2>
    <p class="result-meta" id="result-meta"></p>
    <div id="passages-container"></div>
    <span class="raw-toggle" onclick="toggleRaw()">▶ Gemini 원본 응답 보기</span>
    <div class="raw-box" id="raw-box"></div>
</div>

<script>
let totalPages = 0;

async function onFileChange() {
    const fileEl = document.getElementById('pdfFile');
    const infoEl = document.getElementById('page-info');
    const startEl = document.getElementById('startPage');

    if (!fileEl.files[0]) return;

    infoEl.textContent = '페이지 수 확인 중...';
    infoEl.className = 'page-info loading';

    const form = new FormData();
    form.append('passageImage', fileEl.files[0]);

    try {
        const res  = await fetch('/api/test/pdf-page-count', { method: 'POST', body: form });
        const data = await res.json();
        if (data.error) {
            infoEl.textContent = '⚠️ ' + data.error;
            infoEl.className = 'page-info';
        } else {
            totalPages = data.totalPages;
            infoEl.textContent = '총 ' + totalPages + ' 페이지';
            infoEl.className = 'page-info loaded';
            startEl.max = totalPages;
        }
    } catch (e) {
        infoEl.textContent = '페이지 수 확인 실패';
        infoEl.className = 'page-info';
    }
}

async function runExtract() {
    const fileEl    = document.getElementById('pdfFile');
    const nosEl     = document.getElementById('questionNos');
    const startEl   = document.getElementById('startPage');
    const btn       = document.getElementById('btn-extract');
    const spinner   = document.getElementById('spinner');
    const resultArea = document.getElementById('result-area');

    if (!fileEl.files[0]) { alert('PDF 파일을 선택하세요.'); return; }
    if (!nosEl.value.trim()) { alert('문제 번호를 입력하세요.'); return; }

    btn.disabled = true;
    spinner.style.display = 'block';
    resultArea.style.display = 'none';

    const form = new FormData();
    form.append('passageImage', fileEl.files[0]);
    form.append('questionNos',  nosEl.value.trim());
    form.append('startPage',    startEl.value);

    try {
        const res  = await fetch('/api/test/pdf-extract', { method: 'POST', body: form });
        const data = await res.json();
        renderResult(data);
    } catch (e) {
        alert('오류: ' + e.message);
    } finally {
        btn.disabled = false;
        spinner.style.display = 'none';
    }
}

function renderResult(data) {
    const resultArea = document.getElementById('result-area');
    const metaEl     = document.getElementById('result-meta');
    const container  = document.getElementById('passages-container');
    const rawBox     = document.getElementById('raw-box');

    if (data.error) {
        metaEl.innerHTML = `<span style="color:#dc2626">오류: ${data.error}</span>`;
        resultArea.style.display = 'block';
        return;
    }

    const parsed = data.parsed || {};
    const nums   = Object.keys(parsed);
    const okCount = nums.filter(n => !parsed[n].startsWith('❌') && !parsed[n].startsWith('⚠️')).length;

    metaEl.innerHTML =
        `요청: <strong>${data.requestedNos}</strong> &nbsp;|&nbsp; ` +
        `전송 페이지: <strong>${data.startPage}~${data.startPage + 1}</strong> &nbsp;|&nbsp; ` +
        `추출 성공: <strong style="color:#16a34a">${okCount}개</strong> / ${nums.length}개 &nbsp;|&nbsp; ` +
        `소요 시간: <strong>${data.elapsedMs}ms</strong>`;

    container.innerHTML = '';
    for (const [num, text] of Object.entries(parsed)) {
        const isFail = text.startsWith('❌') || text.startsWith('⚠️');
        const statusClass = isFail ? 'status-fail' : 'status-ok';
        const statusText  = isFail ? '추출 실패' : '추출 성공';
        if (isFail) {
            container.innerHTML +=
                '<div class="passage-block">' +
                    '<div class="passage-header">문제 ' + num + '번 지문' +
                        '<span class="passage-status ' + statusClass + '">' + statusText + '</span></div>' +
                    '<div class="passage-body"><div class="q-instruction">' + escHtmlKeepUnderline(text) + '</div></div>' +
                '</div>';
        } else {
            container.innerHTML += buildQuestionCard(num, text, ' 지문');
        }
    }

    rawBox.textContent = data.rawResponse || '';
    resultArea.style.display = 'block';
}

async function runTextExtract() {
    const fileEl  = document.getElementById('pdfFile');
    const startEl = document.getElementById('startPage');
    const btn     = document.getElementById('btn-text');
    const spinner = document.getElementById('spinner');
    const resultArea = document.getElementById('result-area');

    if (!fileEl.files[0]) { alert('PDF 파일을 선택하세요.'); return; }

    btn.disabled = true;
    spinner.style.display = 'block';
    resultArea.style.display = 'none';

    const form = new FormData();
    form.append('passageImage', fileEl.files[0]);
    form.append('startPage', startEl.value);

    try {
        const res  = await fetch('/api/test/pdf-text-extract', { method: 'POST', body: form });
        const data = await res.json();

        const metaEl    = document.getElementById('result-meta');
        const container = document.getElementById('passages-container');
        const rawBox    = document.getElementById('raw-box');

        if (data.error) {
            metaEl.innerHTML = `<span style="color:#dc2626">오류: ${data.error}</span>`;
        } else {
            const parsed = data.parsed || {};
            const qNums  = Object.keys(parsed);
            metaEl.innerHTML =
                '전체 파싱: <strong>1~' + data.totalPages + '페이지</strong> &nbsp;|&nbsp; ' +
                '추출 문제: <strong style="color:#16a34a">' + qNums.length + '개</strong> &nbsp;|&nbsp; ' +
                '<span style="color:#16a34a;font-weight:700;">API 호출 없음</span>';

            if (qNums.length > 0) {
                // 이미지 필요 문제 감지 → 요약 배너
                const imageNums = qNums.filter(n => parseQuestion(n, parsed[n]).image_type !== null);
                let bannerHtml = '';
                if (imageNums.length > 0) {
                    const detail = imageNums.map(n => {
                        const it = parseQuestion(n, parsed[n]).image_type;
                        return n + '번(' + (IMAGE_LABEL[it] || it) + ')';
                    }).join(', ');
                    bannerHtml = '<div class="image-summary">🖼 <strong>이미지가 필요한 문제 ' + imageNums.length + '개</strong>: ' + detail + '</div>';
                }
                container.innerHTML = bannerHtml + qNums.map(num => buildQuestionCard(num, parsed[num])).join('');
            } else {
                container.innerHTML =
                    '<div class="passage-block">' +
                        '<div class="passage-header">원문 텍스트 <span class="passage-status status-ok">성공</span></div>' +
                        '<div class="passage-body"><div class="passage-box">' + escHtml(data.extractedText) + '</div></div>' +
                    '</div>';
            }
            rawBox.textContent = data.extractedText;
        }
        resultArea.style.display = 'block';
    } catch (e) {
        alert('오류: ' + e.message);
    } finally {
        btn.disabled = false;
        spinner.style.display = 'none';
    }
}

async function runScan() {
    const fileEl = document.getElementById('pdfFile');
    const btn    = document.getElementById('btn-scan');
    const spinner = document.getElementById('spinner');
    if (!fileEl.files[0]) { alert('PDF 파일을 선택하세요.'); return; }

    btn.disabled = true;
    spinner.style.display = 'block';

    const form = new FormData();
    form.append('passageImage', fileEl.files[0]);

    try {
        const res  = await fetch('/api/test/pdf-scan', { method: 'POST', body: form });
        const data = await res.json();
        const area  = document.getElementById('scan-area');
        const table = document.getElementById('scan-table');

        if (data.error) {
            table.innerHTML = '<span style="color:#dc2626">' + data.error + '</span>';
        } else {
            const pm = data.pageMap || {};
            let html = '<p style="font-size:12px;color:#64748b;margin-bottom:12px;">총 ' + data.totalPages + '페이지 — PDF 내부 페이지 번호(1-indexed) 기준입니다.</p>';
            html += '<table style="border-collapse:collapse;font-size:13px;width:100%">';
            html += '<tr style="background:#f1f5f9"><th style="padding:6px 12px;text-align:left;border:1px solid #e2e8f0">PDF 페이지</th><th style="padding:6px 12px;text-align:left;border:1px solid #e2e8f0">발견된 문제 번호</th></tr>';
            for (const [key, nums] of Object.entries(pm)) {
                const pNum = key.replace('p', '');
                const numsStr = nums.length ? nums.join(', ') : '(없음)';
                const style = nums.length ? '' : 'color:#94a3b8';
                html += '<tr><td style="padding:6px 12px;border:1px solid #e2e8f0;font-weight:700">' + pNum + '</td><td style="padding:6px 12px;border:1px solid #e2e8f0;' + style + '">' + numsStr + '</td></tr>';
            }
            html += '</table>';
            table.innerHTML = html;
        }
        area.style.display = 'block';
    } catch (e) {
        alert('오류: ' + e.message);
    } finally {
        btn.disabled = false;
        spinner.style.display = 'none';
    }
}

function toggleRaw() {
    const box = document.getElementById('raw-box');
    box.style.display = box.style.display === 'block' ? 'none' : 'block';
}

function escHtml(str) {
    return str.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
}

// 🔴 <u> 태그만 허용하고 나머지는 이스케이프 처리한다 (밑줄 표시 보존용) 🔴
function escHtmlKeepUnderline(str) {
    return str
        .replace(/&/g,'&amp;')
        .replace(/<u>/g, '\x00u\x00')
        .replace(/<\/u>/g, '\x00/u\x00')
        .replace(/</g,'&lt;')
        .replace(/>/g,'&gt;')
        .replace(/\x00u\x00/g, '<u>')
        .replace(/\x00\/u\x00/g, '</u>');
}

// 🔴 문제 텍스트를 DB 저장 필드로 분리한다 🔴
// 반환: { type, instruction, section_inst, given_sentence, passage, choices, image_type, answer }
function parseQuestion(num, fullText) {
    const lines = fullText.split('\n');

    // ── 1. instruction 줄 탐색 ("N." 로 시작하는 줄) ──
    const instrPat = new RegExp('^\\s*' + num + '\\.');
    let instrIdx = -1;
    for (let i = 0; i < lines.length; i++) {
        if (instrPat.test(lines[i])) { instrIdx = i; break; }
    }

    // ── 2. instruction 이전 줄 → section_inst(한국어 전체) + sectionEngLines(영어) ──
    const beforeLines = instrIdx >= 0 ? lines.slice(0, instrIdx) : [];
    const sectionInstParts = [], sectionEngLines = [];
    for (const line of beforeLines) {
        const t = line.trim();
        if (!t) continue;
        const kor = (t.match(/[\uAC00-\uD7A3]/g) || []).length;
        const eng = (t.match(/[a-zA-Z]/g) || []).length;
        // 한국어가 조금이라도 있으면 section 지시문, 순수 영어만이면 section 지문
        if (kor > 0) sectionInstParts.push(t);
        else         sectionEngLines.push(t);
    }
    const section_inst = sectionInstParts.join(' ').trim() || null;

    // ── 3. instruction 줄 텍스트 분석 ──
    const instrLineRaw = instrIdx >= 0 ? lines[instrIdx].trim() : '';
    const instrAfterNum = instrLineRaw.replace(new RegExp('^\\s*' + num + '\\.\\s*'), '');
    // 한국어가 전혀 없으면 영어 지문 시작 (31번 같은 케이스)
    const instrHasKor = (instrAfterNum.match(/[\uAC00-\uD7A3]/g) || []).length > 0;
    const instrIsEngPassage = !instrHasKor && instrAfterNum.length > 3;

    // ── 4. instruction 여러 줄 수집 (21번, 42번 같은 케이스) ──
    let instrLines = instrIsEngPassage ? [] : (instrAfterNum ? [instrAfterNum] : []);
    let instrPassagePrefix = instrIsEngPassage ? instrAfterNum : '';
    let afterStartIdx = instrIdx >= 0 ? instrIdx + 1 : 0;

    for (let i = afterStartIdx; i < lines.length; i++) {
        const t = lines[i].trim();
        if (!t) { afterStartIdx = i + 1; break; }          // 빈 줄 = instruction 끝
        const kor = (t.match(/[\uAC00-\uD7A3]/g) || []).length;
        const eng = (t.match(/[a-zA-Z]/g) || []).length;
        if (kor === 0 && eng >= 5) { afterStartIdx = i; break; } // 영어 줄 = passage 시작
        instrLines.push(t);
        afterStartIdx = i + 1;
    }
    const instruction = instrLines.join(' ').trim() || null;

    // ── 5. instruction 이후 나머지 텍스트 ──
    const remainingText = (instrPassagePrefix ? instrPassagePrefix + '\n' : '')
                        + lines.slice(afterStartIdx).join('\n');

    // ── 6. 이미지 필요 여부 사전 감지 (선택지 분리 전) ──
    const instrKw = (instruction || '') + ' ' + (section_inst || '');
    let image_type = null;
    if (instrKw.includes('도표') || instrKw.includes('그래프')) image_type = 'CHART';
    else if (instrKw.includes('그림에서') || instrKw.includes('그림의')) image_type = 'IMAGE';
    else if (instrKw.includes('표를 보면서') || instrKw.includes('다음 표')) image_type = 'TABLE';
    else if (instrKw.includes('지도')) image_type = 'MAP';

    // ── 7. 선택지 분리 (줄 첫 머리 ①②③④⑤) ──
    // 도표/그림 문제는 ①②③④⑤가 지문 내부에 있으므로 분리하지 않는다
    const choiceIdx = image_type ? -1 : remainingText.search(/\n[①②③④⑤]/);
    let bodyText = remainingText.trim(), choices = null;
    if (choiceIdx >= 0) {
        bodyText = remainingText.substring(0, choiceIdx).trim();
        choices  = remainingText.substring(choiceIdx + 1).trim();
    }

    // ── 8. passage 결정 ──
    //   섹션 문제: section 영어 지문 + bodyText(있으면)
    //   독립 문제: bodyText 그대로
    let passage;
    if (sectionEngLines.length > 0) {
        passage = sectionEngLines.join('\n').trim();
        if (bodyText) passage = passage + '\n' + bodyText;
    } else {
        passage = bodyText || null;
    }

    // ── 9. 유형 감지 ──
    let type = 'READING';
    if (instrKw.includes('들어가기에') || instrKw.includes('가장 적절한 곳')) type = 'INSERTION';
    else if (instrKw.includes('순서'))                    type = 'ORDER';
    else if (instrKw.includes('빈칸'))                    type = 'BLANK';
    else if (instrKw.includes('낱말') || instrKw.includes('어법')) type = 'VOCABULARY';
    if (section_inst && type === 'READING')               type = 'LONG';

    // ── 10. ORDER / INSERTION: given_sentence 분리 ──
    let given_sentence = null;
    if ((type === 'ORDER' || type === 'INSERTION') && passage) {
        const plines = passage.split('\n');
        let splitAt = -1;
        for (let i = 0; i < plines.length; i++) {
            const t = plines[i].trim();
            if (t.match(/^\(A\)/) || t.match(/\( ?①/) || t.match(/^①/)) { splitAt = i; break; }
        }
        if (splitAt > 0) {
            given_sentence = plines.slice(0, splitAt).join('\n').trim();
            passage = plines.slice(splitAt).join('\n').trim();
        }
    }

    return {
        type, instruction,
        section_inst,
        given_sentence,
        passage: passage || null,
        choices: choices || null,
        image_type,
        answer: null
    };
}

// 🔴 한 행의 필드를 렌더링한다 (label + value) 🔴
function fieldRow(label, value, style) {
    const valHtml = (value === null || value === undefined || value === '')
        ? '<span class="f-null">NULL</span>'
        : '<span class="f-val' + (style ? ' f-' + style : '') + '">' + escHtmlKeepUnderline(String(value)) + '</span>';
    return '<div class="f-row"><span class="f-label">' + label + '</span>' + valHtml + '</div>';
}

// 🔴 이미지 유형 → 한글 레이블 변환 🔴
const IMAGE_LABEL = { CHART:'도표/그래프', IMAGE:'그림', TABLE:'표', MAP:'지도' };

// 🔴 문제 카드 HTML을 생성한다 — DB 필드 형식으로 표시 🔴
function buildQuestionCard(num, fullText, labelSuffix) {
    const q = parseQuestion(num, fullText);
    const TYPE_LABEL = { READING:'일반 독해', INSERTION:'문장 삽입', ORDER:'순서 배열',
                         BLANK:'빈칸 추론', VOCABULARY:'어휘/어법', LONG:'장문' };
    const imageBadge = q.image_type
        ? '<span class="badge-image">🖼 ' + (IMAGE_LABEL[q.image_type] || q.image_type) + '</span>'
        : '';
    let rows = '';
    rows += fieldRow('question_type',   q.type + ' — ' + (TYPE_LABEL[q.type] || q.type), 'type');
    rows += fieldRow('image_type',      q.image_type ? (IMAGE_LABEL[q.image_type] + ' 필요') : null);
    rows += fieldRow('section_inst',    q.section_inst);
    rows += fieldRow('instruction',     q.instruction);
    rows += fieldRow('given_sentence',  q.given_sentence, 'passage');
    rows += fieldRow('passage',         q.passage, 'passage');
    rows += fieldRow('choices',         q.choices);
    rows += fieldRow('answer',          null);
    return '<div class="passage-block">' +
        '<div class="passage-header">문제 ' + num + '번' + (labelSuffix || '') + imageBadge +
            '<span class="passage-status status-ok">추출 성공</span></div>' +
        '<div class="passage-body q-fields">' + rows + '</div>' +
        '</div>';
}
</script>

</body>
</html>
