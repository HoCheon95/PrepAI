// ═══════════════════════════════════════════════════════
// 정답 데이터 (2025년 3월 고2 모의고사, 18~45번)
// ═══════════════════════════════════════════════════════
let QUESTIONS = []; // 데이터를 담을 변수 🔴

// 파일을 읽어오는 함수 🔴
async function loadLocalJson(event) {
  const file = event.target.files[0];
  if (!file) return;

  const reader = new FileReader();

  reader.onload = (e) => {
    try {
      const content = e.target.result;
      QUESTIONS = JSON.parse(content); // JSON 문자열을 객체로 변환하여 저장한다 🔴
      console.log('데이터 로드 완료:', QUESTIONS);
      alert('JSON 파일을 성공적으로 불러왔습니다!');
      
      initCheckboxes(); // 데이터를 다 불러온 후 체크박스를 그린다 🔴
    } catch (error) {
      console.error('JSON 파싱 중 오류 발생:', error);
      alert('올바른 JSON 형식이 아닙니다.');
    }
  };

  reader.readAsText(file); // 텍스트 형식으로 읽는다 🔴
}

// ═══════════════════════════════════════════════════════
// 초기화 및 이벤트 연결
// ═══════════════════════════════════════════════════════
window.onload = function () {
  // 새롭게 추가한 JSON 전용 입력창에 이벤트를 연결한다 🔴
  const jsonFileInput = document.getElementById('jsonFile');
  if (jsonFileInput) {
    jsonFileInput.addEventListener('change', loadLocalJson);
  }

  initCheckboxes();
};

// 중복 선언된 함수를 하나로 정리한다 🔴
function initCheckboxes() {
  const container = document.getElementById("checkboxes");
  container.innerHTML = ''; // 파일 재업로드를 대비해 기존 체크박스를 모두 지운다 🔴

  QUESTIONS.forEach((q) => {
    const label = document.createElement("label");
    label.className = "cb-label checked";
    label.dataset.num = q.question_number;
    label.innerHTML = '<input type="checkbox" value="' + q.question_number + '" checked> ' + q.question_number;
    
    label.querySelector("input").addEventListener("change", function () {
      label.classList.toggle("checked", this.checked);
    });
    
    container.appendChild(label);
  });
}

function getSelected() {
  return [...document.querySelectorAll("#checkboxes input:checked")].map((cb) => parseInt(cb.value));
}

function selectAll() {
  document.querySelectorAll("#checkboxes input").forEach((cb) => {
    cb.checked = true;
    cb.closest(".cb-label").classList.add("checked");
  });
}

function clearAll() {
  document.querySelectorAll("#checkboxes input").forEach((cb) => {
    cb.checked = false;
    cb.closest(".cb-label").classList.remove("checked");
  });
}

function selectRange(start, end) {
  document.querySelectorAll("#checkboxes input").forEach((cb) => {
    const v = parseInt(cb.value);
    const on = v >= start && v <= end;
    cb.checked = on;
    cb.closest(".cb-label").classList.toggle("checked", on);
  });
}

// ═══════════════════════════════════════════════════════
// 탭 전환
// ═══════════════════════════════════════════════════════
function switchTab(tab) {
  document.getElementById("jsonTab").style.display = tab === "json" ? "" : "none";
  document.getElementById("pdfTab").style.display = tab === "pdf" ? "" : "none";
  document.getElementById("sheetTab").style.display = tab === "sheet" ? "" : "none";
  document.querySelectorAll(".tab").forEach((btn) => {
    btn.classList.toggle("active", btn.dataset.tab === tab);
  });
}

// ═══════════════════════════════════════════════════════
// JSON 뷰 렌더링
// ═══════════════════════════════════════════════════════
function renderJsonCards() {
  const selected = getSelected();
  const container = document.getElementById("jsonCards");
  if (selected.length === 0) {
    container.innerHTML = '<div class="empty-state">문제를 선택하세요.</div>';
    switchTab("json");
    return;
  }
  const qs = QUESTIONS.filter((q) => selected.includes(q.question_number));
  container.innerHTML = qs.map((q) => buildJsonCard(q)).join("");
  switchTab("json");
}

function buildJsonCard(q) {
  const nums = ["①", "②", "③", "④", "⑤"];
  let rows = "";

  if (q.section_inst) {
    rows += fRow("section_inst", escHtml(q.section_inst));
  }
  rows += fRowHtml("instruction", q.instruction);

  if (q.given_sentence) {
    rows += fRowHtml("given_sentence", q.given_sentence);
  }

  if (q.passage) {
    rows += '<tr><td class="f-label">passage</td><td class="f-passage">' + q.passage + "</td></tr>";
  }

  if (q.word_notes && q.word_notes.length > 0) {
    const notesHtml = q.word_notes
      .map((n) => '<span class="word-note-item"><b>' + escHtml(n.word) + "</b> " + escHtml(n.meaning) + "</span>")
      .join("");
    rows += '<tr><td class="f-label">word_notes</td><td class="f-val"><div class="word-notes-list">' + notesHtml + "</div></td></tr>";
  }

  if (q.choices && q.choices.length > 0) {
    const choiceHtml = q.choices
      .map((c, i) => {
        const isAns = i + 1 === q.answer;
        return '<div class="choice-item' + (isAns ? " is-answer" : "") + '">' + nums[i] + " " + escHtml(c) + (isAns ? " ✓" : "") + "</div>";
      })
      .join("");
    rows += '<tr><td class="f-label">choices</td><td class="f-val"><div class="choices-list">' + choiceHtml + "</div></td></tr>";
    rows += fRow("answer", '<span class="answer-badge">정답 ' + q.answer + "번</span>");
  } else {
    // choices가 없는 경우 (어법/어휘/도표/삽입/무관) — 정답만 표시
    rows += fRow("answer", '<span class="answer-badge">정답 ' + q.answer + "번</span>");
  }

  return (
    '<div class="q-card">' +
    '<div class="q-header">' +
    '<span class="q-num">' +
    q.question_number +
    "번</span>" +
    '<span class="q-type">' +
    escHtml(q.question_type) +
    "</span>" +
    "</div>" +
    '<table class="q-table">' +
    rows +
    "</table>" +
    "</div>"
  );
}

function fRow(label, value) {
  return '<tr><td class="f-label">' + label + '</td><td class="f-val">' + (value || '<span class="f-null">NULL</span>') + "</td></tr>";
}

function fRowHtml(label, value) {
  // value already contains HTML (e.g., <u> tags)
  return '<tr><td class="f-label">' + label + '</td><td class="f-val">' + (value || '<span class="f-null">NULL</span>') + "</td></tr>";
}

function escHtml(s) {
  if (s == null) return "";
  return String(s).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

// ═══════════════════════════════════════════════════════
// PDF 정확도 비교
// ═══════════════════════════════════════════════════════
async function runComparison() {
  const file = document.getElementById("pdfFile").files[0];
  if (!file) {
    alert("PDF 파일을 선택해주세요.");
    return;
  }

  const selected = getSelected();
  if (selected.length === 0) {
    alert("비교할 문제 번호를 선택해주세요.");
    return;
  }

  const statusEl = document.getElementById("compareStatus");
  const resultsEl = document.getElementById("compareResults");

  statusEl.innerHTML = '<span class="loading-spinner"></span>PDF 추출 중... (잠시 기다려 주세요)';
  resultsEl.innerHTML = '<div class="loading-state"><span class="loading-spinner"></span>분석 중...</div>';

  const formData = new FormData();
  formData.append("passageImage", file);

  try {
    const res = await fetch("/api/test/pdf-text-extract", { method: "POST", body: formData });
    if (!res.ok) throw new Error("HTTP " + res.status);
    const data = await res.json();

    if (data.error) {
      statusEl.innerHTML = "";
      resultsEl.innerHTML = '<div style="color:#ef4444;padding:16px">추출 오류: ' + escHtml(data.error) + "</div>";
      return;
    }

    statusEl.innerHTML = "추출 완료";
    renderComparison(selected, data.parsed || {});
  } catch (e) {
    statusEl.innerHTML = "";
    resultsEl.innerHTML = '<div style="color:#ef4444;padding:16px">오류: ' + escHtml(e.message) + "</div>";
  }
}

function renderComparison(selectedNums, pdfParsed) {
  const qs = QUESTIONS.filter((q) => selectedNums.includes(q.question_number));
  const scores = [];

  const cardsHtml = qs
    .map((q) => {
      const rawText = pdfParsed[String(q.question_number)] || null;
      const pdfFields = rawText ? extractPdfFields(rawText, q.question_number) : null;

      const jsonPassageClean = stripHtml(q.passage || "");
      const jsonInstrClean = stripHtml(q.instruction || "");

      const passageSim = pdfFields ? textSimilarity(jsonPassageClean, pdfFields.passage) : 0;
      const instrSim = pdfFields ? textSimilarity(jsonInstrClean, pdfFields.instruction) : 0;
      // 지문 가중치 2 : instruction 가중치 1
      const avgSim = pdfFields ? Math.round((passageSim * 2 + instrSim) / 3) : 0;
      scores.push({ num: q.question_number, score: avgSim, found: !!pdfFields });

      const simClass = avgSim >= 80 ? "sim-good" : avgSim >= 50 ? "sim-ok" : "sim-bad";
      const pctClass = avgSim >= 80 ? "good" : avgSim >= 50 ? "ok" : "bad";

      let bodyHtml;
      if (!pdfFields) {
        bodyHtml = '<p class="missing-q">❌ PDF에서 ' + q.question_number + "번을 찾지 못했습니다.</p>";
      } else {
        bodyHtml = buildCmpTable(q, pdfFields, passageSim, instrSim);
      }

      return (
        '<div class="cmp-card">' +
        '<div class="cmp-header" onclick="toggleCmp(this)">' +
        '<span class="q-info">' +
        q.question_number +
        "번 &mdash; " +
        escHtml(q.question_type) +
        "</span>" +
        '<span class="sim-badge ' +
        simClass +
        '">' +
        avgSim +
        "%</span>" +
        "</div>" +
        '<div class="cmp-body hidden">' +
        bodyHtml +
        "</div>" +
        "</div>"
      );
    })
    .join("");

  // 전체 요약
  const found = scores.filter((s) => s.found).length;
  const missing = scores.filter((s) => !s.found).length;
  const avg = found > 0 ? Math.round(scores.filter((s) => s.found).reduce((a, s) => a + s.score, 0) / found) : 0;
  const high = scores.filter((s) => s.score >= 80).length;
  const mid = scores.filter((s) => s.score >= 50 && s.score < 80).length;
  const low = scores.filter((s) => s.score > 0 && s.score < 50).length;

  const avgClass = avg >= 80 ? "stat-green" : avg >= 50 ? "stat-yellow" : "stat-red";

  const summaryHtml =
    '<div class="summary-bar">' +
    '<div class="summary-stat ' +
    avgClass +
    '"><div class="stat-num">' +
    avg +
    '%</div><div class="stat-label">전체 평균 유사도</div></div>' +
    '<div class="summary-stat stat-blue"><div class="stat-num">' +
    found +
    "/" +
    scores.length +
    '</div><div class="stat-label">문제 추출 성공</div></div>' +
    '<div class="summary-stat stat-green"><div class="stat-num">' +
    high +
    '</div><div class="stat-label">80% 이상 (우수)</div></div>' +
    '<div class="summary-stat stat-yellow"><div class="stat-num">' +
    mid +
    '</div><div class="stat-label">50~79% (보통)</div></div>' +
    '<div class="summary-stat stat-red"><div class="stat-num">' +
    (low + missing) +
    '</div><div class="stat-label">50% 미만 (불량)</div></div>' +
    "</div>";

  document.getElementById("compareResults").innerHTML = summaryHtml + cardsHtml;
}

function buildCmpTable(jsonQ, pdfFields, passageSim, instrSim) {
  const pPct = '<span class="sim-pct ' + (passageSim >= 80 ? "good" : passageSim >= 50 ? "ok" : "bad") + '">' + passageSim + "%</span>";
  const iPct = '<span class="sim-pct ' + (instrSim >= 80 ? "good" : instrSim >= 50 ? "ok" : "bad") + '">' + instrSim + "%</span>";

  const jsonInstr = escHtml(stripHtml(jsonQ.instruction || ""));
  const jsonPassage = escHtml(stripHtml(jsonQ.passage || ""));
  const pdfInstr = escHtml(pdfFields.instruction || "");
  const pdfPassage = escHtml(pdfFields.passage || "");

  return (
    '<table class="cmp-table">' +
    '<thead><tr><th>필드</th><th class="col-json">JSON 정답</th><th class="col-pdf">PDF 추출</th><th class="col-sim">유사도</th></tr></thead>' +
    "<tbody>" +
    '<tr><td class="field-name">instruction</td>' +
    '<td class="cmp-text col-json">' +
    jsonInstr +
    "</td>" +
    '<td class="cmp-text col-pdf">' +
    pdfInstr +
    "</td>" +
    '<td class="col-sim">' +
    iPct +
    "</td></tr>" +
    '<tr><td class="field-name">passage</td>' +
    '<td class="cmp-text tall col-json">' +
    jsonPassage +
    "</td>" +
    '<td class="cmp-text tall col-pdf">' +
    pdfPassage +
    "</td>" +
    '<td class="col-sim">' +
    pPct +
    "</td></tr>" +
    "</tbody></table>"
  );
}

// ═══════════════════════════════════════════════════════
// PDF 텍스트에서 필드 추출 (간이 파서)
// ═══════════════════════════════════════════════════════
function extractPdfFields(rawText, qNum) {
  const lines = rawText.replace(/\r\n/g, "\n").replace(/\r/g, "\n").split("\n");
  const instrLines = [],
    passageLines = [];
  let phase = "instr";

  for (const line of lines) {
    const t = line.trim();
    if (!t) continue;

    // 문제 번호 접두사 제거 (예: "18. 다음 글의...")
    const prefixRe = new RegExp("^" + qNum + "\\.\\s*");
    const stripped = prefixRe.test(t) ? t.replace(prefixRe, "") : null;
    if (stripped !== null) {
      if (stripped) instrLines.push(stripped);
      continue;
    }

    // 선택지 줄 → 탐색 중단 (choices는 비교에서 제외)
    if (/^[①②③④⑤]/.test(t)) break;

    if (phase === "instr") {
      const kor = (t.match(/[\uAC00-\uD7A3]/g) || []).length;
      const eng = (t.match(/[a-zA-Z]/g) || []).length;
      if (kor === 0 && eng >= 5) {
        phase = "passage";
        passageLines.push(t);
      } else {
        instrLines.push(t);
      }
    } else {
      passageLines.push(t);
    }
  }

  return {
    instruction: instrLines.join(" ").trim(),
    passage: passageLines.join("\n").trim(),
  };
}

// ═══════════════════════════════════════════════════════
// 유사도 계산 (단어 집합 Jaccard)
// ═══════════════════════════════════════════════════════
function textSimilarity(a, b) {
  if (!a && !b) return 100;
  if (!a || !b) return 0;
  const norm = (s) =>
    s
      .replace(/[^\w\s\uAC00-\uD7A3]/g, " ")
      .replace(/\s+/g, " ")
      .trim()
      .toLowerCase();
  const na = norm(a),
    nb = norm(b);
  if (na === nb) return 100;
  const wa = new Set(na.split(" ").filter((w) => w.length > 1));
  const wb = new Set(nb.split(" ").filter((w) => w.length > 1));
  if (wa.size === 0 && wb.size === 0) return 100;
  if (wa.size === 0 || wb.size === 0) return 0;
  let inter = 0;
  for (const w of wa) if (wb.has(w)) inter++;
  const union = wa.size + wb.size - inter;
  return Math.round((inter / union) * 100);
}

function stripHtml(s) {
  if (!s) return "";
  return s
    .replace(/<[^>]+>/g, "")
    .replace(/&nbsp;/g, " ")
    .replace(/&amp;/g, "&")
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&quot;/g, '"')
    .replace(/\s+/g, " ")
    .trim();
}

function toggleCmp(header) {
  const body = header.nextElementSibling;
  body.classList.toggle("hidden");
}

// ═══════════════════════════════════════════════════════
// 시험지 뷰 렌더링 — 실제 DOM 높이 기반 A4 페이지 분할
// ═══════════════════════════════════════════════════════

function renderExamSheet() {
  const selected = getSelected();
  if (selected.length === 0) { alert("문제를 선택하세요."); return; }
  if (QUESTIONS.length === 0) { alert("JSON 파일을 먼저 업로드하세요."); return; }

  const qs = QUESTIONS.filter((q) => selected.includes(q.question_number));
  const qHtmlList = qs.map((q) => buildSheetQuestionHtml(q));

  // 🔴 오프스크린 컨테이너 — 실제 CSS가 적용된 상태로 높이 측정, 사용자에게 보이지 않음 🔴
  const offscreen = document.createElement("div");
  offscreen.style.cssText = "position:fixed;left:-9999px;top:0;width:210mm;visibility:hidden;";
  document.body.appendChild(offscreen);

  // 첫 번째 페이지 생성
  let pageIdx = 0;
  let { page, leftCol, rightCol } = createSheetPage(pageIdx);
  offscreen.appendChild(page);
  let currentCol = leftCol;

  for (const html of qHtmlList) {
    const el = document.createElement("div");
    el.innerHTML = html;
    currentCol.appendChild(el);

    // 🔴 실제 렌더링 후 scrollHeight vs clientHeight 비교로 넘침 감지 🔴
    if (currentCol.scrollHeight > currentCol.clientHeight) {
      currentCol.removeChild(el);

      if (currentCol === leftCol) {
        // 왼쪽 열 꽉 참 → 오른쪽 열 시도
        currentCol = rightCol;
        currentCol.appendChild(el);

        if (currentCol.scrollHeight > currentCol.clientHeight) {
          // 오른쪽 열도 꽉 참 → 새 페이지
          currentCol.removeChild(el);
          pageIdx++;
          const np = createSheetPage(pageIdx);
          offscreen.appendChild(np.page);
          leftCol = np.leftCol;
          rightCol = np.rightCol;
          currentCol = leftCol;
          currentCol.appendChild(el);
        }
      } else {
        // 오른쪽 열 꽉 참 → 새 페이지
        pageIdx++;
        const np = createSheetPage(pageIdx);
        offscreen.appendChild(np.page);
        leftCol = np.leftCol;
        rightCol = np.rightCol;
        currentCol = leftCol;
        currentCol.appendChild(el);
      }
    }
  }

  // 오프스크린 → 실제 컨테이너로 이동
  const container = document.getElementById("examSheetPages");
  container.innerHTML = "";
  while (offscreen.firstChild) {
    container.appendChild(offscreen.firstChild);
  }
  document.body.removeChild(offscreen);

  switchTab("sheet");
}

// 🔴 A4 페이지 한 장 DOM 구조 생성 🔴
function createSheetPage(idx) {
  const page = document.createElement("div");
  page.className = "sheet-paper";

  const header = document.createElement("div");
  header.className = "sheet-exam-header";
  header.innerHTML =
    "<div>1</div>" +
    '<div class="sheet-title">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;영 어 영 역</div>' +
    '<div class="sheet-grade">고 등</div>';

  const cols = document.createElement("div");
  cols.className = "sheet-cols";

  const leftCol = document.createElement("div");
  leftCol.className = "sheet-col";
  const rightCol = document.createElement("div");
  rightCol.className = "sheet-col";
  cols.appendChild(leftCol);
  cols.appendChild(rightCol);

  const pageNum = document.createElement("div");
  pageNum.className = "sheet-page-num";
  pageNum.textContent = "- " + (idx + 1) + " -";

  page.appendChild(header);
  page.appendChild(cols);
  page.appendChild(pageNum);

  return { page, leftCol, rightCol };
}
function buildSheetQuestionHtml(q) {
  const nums = ["①", "②", "③", "④", "⑤"];
  const sectionHtml = q.section_inst
    ? '<div class="sheet-section-inst">' + escHtml(q.section_inst) + "</div>"
    : "";
  const titleHtml =
    '<div class="sheet-q-title">' +
    q.question_number + ". " + (q.instruction || "") +
    "</div>";
  const givenHtml = q.given_sentence
    ? '<div class="sheet-given">' + q.given_sentence + "</div>"
    : "";
  // 🔴 word_notes가 있으면 지문 박스 우측 하단에 단어 주석을 표시한다. 🔴
  const wordNotesHtml =
    q.word_notes && q.word_notes.length > 0
      ? '<div class="sheet-word-notes">' +
        q.word_notes
          .map((n) => "<span><b>" + escHtml(n.word) + "</b> " + escHtml(n.meaning) + "</span>")
          .join("") +
        "</div>"
      : "";
  const passageHtml = q.passage
    ? '<div class="sheet-q-passage">' + q.passage + wordNotesHtml + "</div>"
    : "";
  const choicesHtml =
    q.choices && q.choices.length > 0
      ? '<div class="sheet-q-options">' +
        q.choices.map((c, i) => nums[i] + " " + escHtml(c)).join("<br>") +
        "</div>"
      : "";
  const answerHtml =
    '<div class="sheet-answer-only">✅ 정답: ' + q.answer + "번</div>";
  return (
    '<div class="sheet-q-item">' +
    sectionHtml + titleHtml + givenHtml + passageHtml + choicesHtml + answerHtml +
    "</div>"
  );
}

function toggleSheetMode(mode) {
  const answers = document.querySelectorAll(".sheet-answer-only");
  const btnStd = document.getElementById("btn-sheet-std");
  const btnTch = document.getElementById("btn-sheet-tch");
  if (mode === "teacher") {
    answers.forEach((el) => (el.style.display = "block"));
    btnTch.classList.add("active");
    btnStd.classList.remove("active");
  } else {
    answers.forEach((el) => (el.style.display = "none"));
    btnStd.classList.add("active");
    btnTch.classList.remove("active");
  }
}

// ── 편집 모드 토글 ──
let _editMode = false;
function toggleEditMode() {
  _editMode = !_editMode;
  const papers = document.querySelectorAll(".sheet-paper");
  const btn = document.getElementById("btn-edit-toggle");
  const hint = document.getElementById("edit-hint");
  const toolbar = document.getElementById("format-toolbar");

  papers.forEach((p) => {
    p.contentEditable = _editMode ? "true" : "false";
    p.classList.toggle("edit-mode", _editMode);
  });

  btn.classList.toggle("active", _editMode);
  hint.style.display = _editMode ? "inline" : "none";
  toolbar.style.display = _editMode ? "flex" : "none";
}

// ── 서식 명령 (execCommand는 deprecated이나 contenteditable 편집에서는 표준 대안 없음) ──
function fmt(cmd) {
  // @ts-ignore
  document.execCommand(cmd, false, null); // eslint-disable-line no-undef
}
