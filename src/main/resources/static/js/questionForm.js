/* 🔴 과목 선택 시 UI를 동적으로 변경하는 함수입니다. 🔴 */
function toggleUI(type) {
  const numberCard = document.getElementById("mock-number-card");
  const textAreaWrapper = document.getElementById("text-area-wrapper");
  const passageLabel = document.getElementById("passage-label");
  const fileHelpText = document.getElementById("file-help-text");
  const textArea = document.getElementById("passageText");

  if (type === "모의고사") {
    // 모의고사일 때: 번호판 표시, 텍스트창 숨김
    numberCard.style.display = "block";
    textAreaWrapper.style.display = "none";
    if (textArea) textArea.value = ""; // 텍스트 초기화
    passageLabel.innerText = "📄 모의고사 PDF 파일 첨부";
    fileHelpText.innerText = "※ 문제를 추출할 모의고사 원문 PDF 파일을 업로드해주세요. (필수)";
  } else {
    // 외부지문, 교과서일 때: 번호판 숨김, 텍스트창 표시
    numberCard.style.display = "none";
    textAreaWrapper.style.display = "block";
    passageLabel.innerText = "📄 지문 텍스트 입력 또는 파일 첨부";
    fileHelpText.innerText = "※ 텍스트를 직접 입력하거나 문서를 업로드할 수 있습니다.";

    // 숨겨진 번호 체크박스들의 체크를 모두 해제
    const checkboxes = document.querySelectorAll('input[name="questionNos"]');
    checkboxes.forEach((cb) => (cb.checked = false));
  }
}

// 🔴 페이지가 처음 열릴 때 현재 체크된 항목 기준으로 초기화합니다. 🔴
window.addEventListener("DOMContentLoaded", (event) => {
  const checkedTypeRadio = document.querySelector('input[name="examType"]:checked');
  if (checkedTypeRadio) {
    toggleUI(checkedTypeRadio.value);
  }

  // 🔴 폼 제출 시 로딩 오버레이를 표시한다. 🔴
  const form = document.querySelector('form');
  if (form) {
    form.addEventListener('submit', function () {
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
function showLoadingOverlay(total) {
  const overlay  = document.getElementById('loading-overlay');
  const bar      = document.getElementById('loading-bar');
  const countEl  = document.getElementById('loading-count');
  const etaEl    = document.getElementById('loading-eta');

  overlay.style.display = 'flex';

  const totalMs  = Math.max(10000, total * 5000); // 최소 10초
  const tickMs   = 200;
  const maxPct   = 92; // 92%까지만 채우고 서버 응답 대기
  let   elapsed  = 0;

  setInterval(() => {
    elapsed += tickMs;
    const pct    = Math.min(maxPct, (elapsed / totalMs) * 100);
    const genNum = Math.min(total, Math.round((pct / 100) * total));
    const remS   = Math.max(0, Math.ceil((totalMs - elapsed) / 1000));

    bar.style.width     = pct.toFixed(1) + '%';
    countEl.textContent = `${genNum} / ${total}개`;
    etaEl.textContent   = remS > 0 ? `약 ${remS}초 남았습니다` : '거의 완료됐습니다...';
  }, tickMs);
}
