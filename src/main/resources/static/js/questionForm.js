/* 🔴 모의고사 JSON 데이터를 메모리에 보관한다. 🔴 */
let mockExamQuestions = [];

/* 🔴 과목 선택 시 UI를 동적으로 변경하는 함수입니다. 🔴 */
function toggleUI(type) {
  const numberCard = document.getElementById("mock-number-card");
  const textAreaWrapper = document.getElementById("text-area-wrapper");
  const passageLabel = document.getElementById("passage-label");
  const fileHelpText = document.getElementById("file-help-text");
  const textArea = document.getElementById("passageText");
  const fileInput = document.getElementById("passageFileInput");

  if (type === "모의고사") {
    // 모의고사일 때: 번호판 표시, 텍스트창 숨김, JSON 파일 수락
    numberCard.style.display = "block";
    textAreaWrapper.style.display = "none";
    if (textArea) textArea.value = "";
    passageLabel.innerText = "📄 모의고사 JSON 파일 첨부";
    fileHelpText.innerText = "※ 문제 데이터가 담긴 JSON 파일을 업로드해주세요. (필수)";
    if (fileInput) fileInput.accept = ".json";
    mockExamQuestions = [];
  } else {
    // 외부지문, 교과서일 때: 번호판 숨김, 텍스트창 표시, PDF/이미지 파일 수락
    numberCard.style.display = "none";
    textAreaWrapper.style.display = "block";
    passageLabel.innerText = "📄 지문 텍스트 입력 또는 파일 첨부";
    fileHelpText.innerText = "※ 텍스트를 직접 입력하거나 문서를 업로드할 수 있습니다.";
    if (fileInput) fileInput.accept = ".pdf, image/*";

    // 숨겨진 번호 체크박스들의 체크를 모두 해제
    const checkboxes = document.querySelectorAll('input[name="questionNos"]');
    checkboxes.forEach((cb) => (cb.checked = false));
  }
}

// 🔴 페이지가 처음 열릴 때 현재 체크된 항목 기준으로 초기화합니다. 🔴
window.addEventListener("DOMContentLoaded", () => {
  const checkedTypeRadio = document.querySelector('input[name="examType"]:checked');
  if (checkedTypeRadio) {
    toggleUI(checkedTypeRadio.value);
  }

  // 🔴 모의고사 모드에서 JSON 파일 선택 시 파싱 후 메모리에 저장한다. 🔴
  const fileInput = document.getElementById("passageFileInput");
  if (fileInput) {
    fileInput.addEventListener("change", function () {
      const examType = document.querySelector('input[name="examType"]:checked')?.value;
      if (examType !== "모의고사") return;

      const file = this.files[0];
      if (!file) return;

      const reader = new FileReader();
      reader.onload = (e) => {
        try {
          mockExamQuestions = JSON.parse(e.target.result);
          console.log("[PrepAI] 모의고사 JSON 로드 완료:", mockExamQuestions.length + "문제");
        } catch (err) {
          console.error("[PrepAI] JSON 파싱 오류:", err);
          alert("올바른 JSON 형식이 아닙니다.");
          mockExamQuestions = [];
        }
      };
      reader.readAsText(file);
    });
  }

  // 🔴 폼 제출 시 모의고사 JSON 지문 추출 후 passageText에 주입하고 로딩 오버레이를 표시한다. 🔴
  const form = document.querySelector('form');
  if (form) {
    form.addEventListener('submit', function (e) {
      const examType = document.querySelector('input[name="examType"]:checked')?.value;

      if (examType === "모의고사") {
        const selectedNos = Array.from(document.querySelectorAll('input[name="questionNos"]:checked'))
          .map((cb) => parseInt(cb.value));

        if (selectedNos.length > 0 && mockExamQuestions.length > 0) {
          // 🔴 선택된 문제 번호에 해당하는 지문을 레이블과 함께 추출한다. 🔴
          const passages = selectedNos
            .map((num) => {
              const q = mockExamQuestions.find((q) => q.question_number === num);
              return q ? "[Question " + num + "]\n" + (q.passage || "") : null;
            })
            .filter(Boolean);

          if (passages.length > 0) {
            document.getElementById("passageText").value = passages.join("\n\n");
          }
        } else if (mockExamQuestions.length === 0) {
          e.preventDefault();
          alert("JSON 파일을 먼저 업로드해주세요.");
          return;
        }

        // 🔴 파일은 서버로 전송하지 않는다 — passageText로 대체된다. 🔴
        if (fileInput) fileInput.value = "";
      }

      const total = calcTotalQuestions();
      if (total > 0) showLoadingOverlay(total);
    });
  }
});

// 🔴 선택된 문제 유형의 총 문제 수를 계산한다. 🔴
function calcTotalQuestions() {
  return Array.from(document.querySelectorAll('input[name="questionTypes"]:checked'))
    .reduce((sum, cb) => {
      const countEl = document.querySelector(`input[name="count_${cb.value}"]`);
      return sum + (parseInt(countEl?.value) || 1);
    }, 0);
}

// 🔴 로딩 오버레이를 표시하고 게이지를 예상 시간 기반으로 채운다. 🔴
// 🔴 1문제 생성에 약 5초 소요 기준으로 게이지 속도를 계산한다. 🔴
// 🔴 최대 대기 시간(180초) 초과 시 에러 메시지를 표시한다. 🔴
function showLoadingOverlay(total) {
  const overlay   = document.getElementById('loading-overlay');
  const bar       = document.getElementById('loading-bar');
  const countEl   = document.getElementById('loading-count');
  const etaEl     = document.getElementById('loading-eta');

  overlay.style.display = 'flex';

  const totalMs   = Math.max(10000, total * 5000); // 최소 10초
  const maxWaitMs = 180000; // 🔴 서버 타임아웃(120초) + 여유 60초 = 최대 3분 🔴
  const tickMs    = 200;
  const maxPct    = 92;
  let   elapsed   = 0;

  const timer = setInterval(() => {
    elapsed += tickMs;

    // 🔴 최대 대기 시간 초과 시 인터벌을 해제하고 에러 안내를 표시한다. 🔴
    if (elapsed >= maxWaitMs) {
      clearInterval(timer);
      bar.style.width     = '100%';
      bar.style.background = '#dc3545';
      countEl.textContent = '응답 시간 초과';
      etaEl.textContent   = '서버가 응답하지 않습니다. 문제 수를 줄이거나 다시 시도해 주세요.';
      return;
    }

    const pct    = Math.min(maxPct, (elapsed / totalMs) * 100);
    const genNum = Math.min(total, Math.round((pct / 100) * total));
    const remS   = Math.max(0, Math.ceil((totalMs - elapsed) / 1000));

    bar.style.width     = pct.toFixed(1) + '%';
    countEl.textContent = `${genNum} / ${total}개`;
    etaEl.textContent   = remS > 0 ? `약 ${remS}초 남았습니다` : '거의 완료됐습니다...';
  }, tickMs);
}
