<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <title>이미지 문제 생성 테스트</title>
  <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
  <style>
    *, *::before, *::after { box-sizing: border-box; }
    body { margin: 0; background: #f1f5f9; font-family: 'Malgun Gothic', sans-serif; font-size: 13px; color: #1e293b; }
    a { color: #2563eb; text-decoration: none; }
    .container { max-width: 900px; margin: 0 auto; padding: 20px; }

    /* Header */
    .page-header { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 18px; }
    .page-header h1 { margin: 0 0 3px; font-size: 20px; }
    .page-header .sub { font-size: 12px; color: #64748b; }
    .header-links { font-size: 12px; display: flex; gap: 12px; padding-top: 4px; }

    /* Upload card */
    .upload-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 8px; padding: 16px; margin-bottom: 16px; display: flex; align-items: center; gap: 14px; flex-wrap: wrap; }
    .upload-card label { font-weight: 700; white-space: nowrap; }
    button { padding: 7px 16px; border-radius: 6px; border: 1px solid #cbd5e1; background: #fff; cursor: pointer; font-size: 12px; font-family: inherit; color: #374151; }
    button:hover { background: #f8fafc; }
    button.btn-primary { background: #2563eb; color: #fff; border-color: #2563eb; font-weight: 700; font-size: 13px; padding: 9px 26px; }
    button.btn-primary:hover { background: #1d4ed8; }
    button:disabled { opacity: 0.45; cursor: not-allowed; }
    #uploadStatus { font-size: 12px; color: #64748b; }
    .loading-spinner { display: inline-block; width: 15px; height: 15px; border: 2px solid #dbeafe; border-top-color: #2563eb; border-radius: 50%; animation: spin .7s linear infinite; vertical-align: middle; margin-right: 5px; }
    @keyframes spin { to { transform: rotate(360deg); } }

    /* 문제 카드 목록 */
    .questions-panel { display: flex; flex-direction: column; gap: 18px; }

    /* 개별 문제 카드 */
    .q-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden; }
    .q-card-header { display: flex; align-items: center; gap: 10px; padding: 10px 14px; border-bottom: 1px solid #e2e8f0; background: #f8fafc; }
    .q-num-badge { background: #2563eb; color: #fff; font-weight: 700; font-size: 12px; padding: 2px 10px; border-radius: 12px; white-space: nowrap; }
    .q-type-badge { font-size: 12px; color: #64748b; }

    /* 차트 영역 */
    .chart-area { padding: 16px; border-bottom: 1px solid #e2e8f0; background: #f8fafc; display: flex; justify-content: center; }
    .chart-area canvas { max-width: 100%; }
    .chart-error { padding: 12px 16px; font-size: 11px; color: #ef4444; background: #fee2e2; }

    /* 문제 테이블 */
    .q-table { width: 100%; border-collapse: collapse; }
    .q-table tr { border-bottom: 1px solid #f1f5f9; }
    .q-table tr:last-child { border-bottom: none; }
    .f-label { padding: 7px 10px; width: 100px; background: #f8fafc; border-right: 1px solid #e2e8f0; vertical-align: top; font-size: 11px; color: #64748b; white-space: nowrap; }
    .f-val { padding: 8px 12px; word-break: break-word; line-height: 1.7; }
    .f-passage { padding: 10px 14px; font-family: 'Batang', 'Times New Roman', serif; font-size: 14px; line-height: 2; white-space: pre-wrap; word-break: break-word; }
    .f-expl { padding: 8px 12px; color: #374151; line-height: 1.7; font-size: 12px; }
    .answer-badge { display: inline-block; background: #2563eb; color: #fff; padding: 2px 10px; border-radius: 12px; font-size: 12px; font-weight: 700; }

    /* 빈 상태 */
    .empty-state { text-align: center; color: #94a3b8; padding: 60px 20px; background: #fff; border: 1px solid #e2e8f0; border-radius: 8px; font-size: 13px; line-height: 1.8; }

    /* Raw 응답 */
    .raw-section { margin-top: 14px; }
    .raw-toggle { font-size: 11px; color: #94a3b8; cursor: pointer; text-decoration: underline; }
    .raw-box { display: none; background: #0f172a; color: #94a3b8; font-family: monospace; font-size: 11px; padding: 12px; border-radius: 6px; white-space: pre-wrap; word-break: break-all; max-height: 300px; overflow-y: auto; margin-top: 6px; }
    .error-box { color: #ef4444; padding: 14px; background: #fee2e2; border-radius: 8px; }
  </style>
</head>
<body>
<div class="container">

  <!-- Header -->
  <div class="page-header">
    <div>
      <h1>이미지 문제 생성 테스트</h1>
      <div class="sub">그래프 이미지 1장 → Gemini → 5개 유형 문제 + 새 그래프 동시 생성 (API 1회)</div>
    </div>
    <div class="header-links">
      <a href="/test/json">← JSON 뷰어</a>
      <a href="/test/pdf">PDF 테스트</a>
    </div>
  </div>

  <!-- Upload -->
  <div class="upload-card">
    <label>그래프 이미지</label>
    <input type="file" id="imageFile" accept="image/*">
    <button class="btn-primary" id="genBtn" onclick="generateQuestions()" disabled>
      5개 유형 문제 생성 (API 1회)
    </button>
    <span id="uploadStatus">이미지를 선택하면 버튼이 활성화됩니다.</span>
  </div>

  <!-- 문제 카드들 -->
  <div class="questions-panel" id="questionsPanel">
    <div class="empty-state" id="emptyHint">
      그래프 이미지를 업로드하고 "5개 유형 문제 생성"을 클릭하세요.<br>
      한 번의 API 호출로 <strong>도표 불일치 · 도표 일치 · 빈칸 추론 · 제목 파악 · 요약 완성</strong> 5가지 유형이 생성됩니다.<br>
      각 문제마다 <strong>새로운 그래프</strong>가 함께 생성됩니다.
    </div>
  </div>

  <!-- Raw 응답 -->
  <div class="raw-section" id="rawSection" style="display:none">
    <span class="raw-toggle" onclick="toggleRaw()">▶ Gemini 원본 응답 보기</span>
    <pre id="rawBox" class="raw-box"></pre>
  </div>

</div>

<script>
const TYPE_NAMES = ['도표 불일치', '도표 일치', '빈칸 추론', '제목 파악', '요약 완성'];
const CHART_COLORS = [
  ['#3b82f6','#93c5fd'],
  ['#10b981','#6ee7b7'],
  ['#f59e0b','#fcd34d'],
  ['#8b5cf6','#c4b5fd'],
  ['#ef4444','#fca5a5']
];

window.onload = function() {
  document.getElementById('imageFile').addEventListener('change', onImageChange);
};

function onImageChange(e) {
  const file = e.target.files[0];
  if (!file) return;
  document.getElementById('genBtn').disabled = false;
  document.getElementById('uploadStatus').textContent = file.name + ' 선택됨.';
}

async function generateQuestions() {
  const file = document.getElementById('imageFile').files[0];
  if (!file) { alert('이미지 파일을 선택하세요.'); return; }

  const btn      = document.getElementById('genBtn');
  const statusEl = document.getElementById('uploadStatus');
  const panel    = document.getElementById('questionsPanel');
  const rawSec   = document.getElementById('rawSection');

  btn.disabled = true;
  statusEl.innerHTML = '<span class="loading-spinner"></span>Gemini 분석 중... (30~60초 소요)';
  panel.innerHTML = '<div class="empty-state"><span class="loading-spinner"></span>이미지를 읽고 5개 유형의 문제와 새 그래프를 생성하고 있습니다...</div>';
  rawSec.style.display = 'none';

  const formData = new FormData();
  formData.append('image', file);

  try {
    const res = await fetch('/api/test/image-question', { method: 'POST', body: formData });
    if (!res.ok) throw new Error('HTTP ' + res.status);
    const data = await res.json();

    btn.disabled = false;

    if (data.error) {
      statusEl.textContent = '오류 발생';
      panel.innerHTML = '<div class="error-box">오류: ' + escHtml(data.error) + '</div>';
      return;
    }

    const qs = data.questions || [];
    const elapsed = ((data.elapsedMs || 0) / 1000).toFixed(1);
    statusEl.innerHTML = '생성 완료 — <strong>' + qs.length + '개</strong> 문제 (' + elapsed + 's, API 1회)';

    renderQuestions(qs);

    document.getElementById('rawBox').textContent = data.rawResponse || '';
    rawSec.style.display = '';

  } catch(e) {
    btn.disabled = false;
    statusEl.textContent = '요청 실패';
    panel.innerHTML = '<div class="error-box">오류: ' + escHtml(e.message) + '</div>';
  }
}

function renderQuestions(qs) {
  const panel = document.getElementById('questionsPanel');
  if (!qs || qs.length === 0) {
    panel.innerHTML = '<div class="error-box">문제를 파싱하지 못했습니다. 원본 응답을 확인하세요.</div>';
    return;
  }

  panel.innerHTML = qs.map((q, idx) => buildCard(q, idx)).join('');

  // Chart.js 렌더링 (DOM 삽입 후)
  qs.forEach((q, idx) => {
    if (q.chartdata) {
      renderChart('chart-q' + (idx + 1), q.chartdata, idx);
    }
  });
}

// 수능 스타일 색상 팔레트 (진한 색 + 연한 색 쌍)
const PALETTE = [
  '#1e40af', '#3b82f6', '#0f766e', '#14b8a6',
  '#b45309', '#f59e0b', '#7c3aed', '#a78bfa',
  '#be123c', '#f87171', '#0369a1', '#38bdf8'
];
const PIE_PALETTE = ['#1e40af','#0f766e','#b45309','#7c3aed','#be123c','#0369a1','#4d7c0f'];

function renderChart(canvasId, chartDataStr, colorIdx) {
  const canvas = document.getElementById(canvasId);
  if (!canvas) return;

  let cfg;
  try {
    // Gemini가 JSON 앞뒤에 텍스트를 붙이는 경우 대비해 JSON 부분만 추출
    const jsonMatch = chartDataStr.match(/\{[\s\S]*\}/);
    cfg = JSON.parse(jsonMatch ? jsonMatch[0] : chartDataStr);
  } catch(e) {
    canvas.parentElement.innerHTML = '<div class="chart-error">차트 데이터 파싱 오류: ' + escHtml(e.message) + '<br><small style="word-break:break-all">' + escHtml(chartDataStr.slice(0, 300)) + '</small></div>';
    return;
  }

  const chartType = cfg.type === 'horizontalBar' ? 'bar' : (cfg.type || 'bar');
  const isHorizontal = cfg.type === 'horizontalBar';
  const isPieFamily = chartType === 'pie' || chartType === 'doughnut';

  // 데이터셋 색상 자동 적용
  if (cfg.datasets) {
    cfg.datasets.forEach((ds, i) => {
      if (isPieFamily) {
        if (!ds.backgroundColor) ds.backgroundColor = PIE_PALETTE;
        if (!ds.borderColor) ds.borderColor = '#fff';
        ds.borderWidth = 2;
      } else {
        const base = PALETTE[(colorIdx * 2 + i * 3) % PALETTE.length];
        if (!ds.backgroundColor) {
          ds.backgroundColor = chartType === 'line' ? base + '33' : base;
        }
        if (!ds.borderColor) ds.borderColor = base;
        if (chartType === 'line') {
          ds.fill = false;
          ds.tension = 0.3;
          ds.pointRadius = 5;
          ds.pointHoverRadius = 7;
          ds.borderWidth = 2;
        } else {
          ds.borderWidth = 0;
          ds.borderRadius = 3;
        }
      }
    });
  }

  const chartConfig = {
    type: chartType,
    data: {
      labels: cfg.labels || [],
      datasets: cfg.datasets || []
    },
    options: {
      indexAxis: isHorizontal ? 'y' : 'x',
      responsive: true,
      maintainAspectRatio: true,
      plugins: {
        legend: {
          display: isPieFamily || (cfg.datasets && cfg.datasets.length > 1),
          position: isPieFamily ? 'right' : 'top',
          labels: { font: { size: 12 }, padding: 12 }
        },
        title: {
          display: !!cfg.title,
          text: cfg.title || '',
          font: { size: 14, weight: 'bold' },
          padding: { top: 8, bottom: 12 },
          color: '#1e293b'
        },
        // 막대/선 위에 값 표시
        datalabels: false
      },
      scales: isPieFamily ? {} : {
        y: {
          max: cfg.yMax || undefined,
          beginAtZero: true,
          grid: { color: '#e2e8f0' },
          ticks: { font: { size: 11 } },
          title: {
            display: !isHorizontal && !!cfg.yAxisLabel,
            text: cfg.yAxisLabel || '',
            font: { size: 11 },
            color: '#64748b'
          }
        },
        x: {
          grid: { display: isHorizontal },
          ticks: { font: { size: 11 } },
          title: {
            display: isHorizontal && !!cfg.yAxisLabel,
            text: isHorizontal ? (cfg.yAxisLabel || '') : '',
            font: { size: 11 },
            color: '#64748b'
          }
        }
      },
      animation: { duration: 600 }
    },
    plugins: [{
      // 막대/점 위에 값 직접 표시
      id: 'dataLabels',
      afterDatasetsDraw(chart) {
        if (isPieFamily) return;
        const ctx = chart.ctx;
        chart.data.datasets.forEach((ds, i) => {
          const meta = chart.getDatasetMeta(i);
          if (meta.hidden) return;
          meta.data.forEach((el, j) => {
            const val = ds.data[j];
            if (val == null) return;
            ctx.save();
            ctx.font = 'bold 11px Malgun Gothic, sans-serif';
            ctx.fillStyle = '#1e293b';
            ctx.textAlign = 'center';
            ctx.textBaseline = isHorizontal ? 'middle' : 'bottom';
            const x = isHorizontal ? el.x + 16 : el.x;
            const y = isHorizontal ? el.y : el.y - 4;
            ctx.fillText(val, x, y);
            ctx.restore();
          });
        });
      }
    }]
  };

  new Chart(canvas, chartConfig);
}

function buildCard(q, idx) {
  const typeName = TYPE_NAMES[idx] || ('유형 ' + (idx + 1));
  const canvasId = 'chart-q' + (idx + 1);
  let rows = '';

  // instruction
  if (q.instruction) {
    rows += fRow('instruction', escHtml(q.instruction));
  }

  // passage
  if (q.passage) {
    rows += '<tr><td class="f-label">passage</td><td class="f-passage">' + escHtml(q.passage) + '</td></tr>';
  }

  // choices
  if (q.choices && q.choices.trim() !== 'NULL') {
    rows += '<tr><td class="f-label">choices</td><td class="f-val" style="white-space:pre-wrap;">' + escHtml(q.choices) + '</td></tr>';
  }

  // answer
  if (q.answer) {
    rows += fRow('answer', '<span class="answer-badge">정답 ' + escHtml(q.answer.trim()) + '번</span>');
  }

  // explanation
  if (q.explanation) {
    rows += '<tr><td class="f-label">해설</td><td class="f-expl">' + escHtml(q.explanation) + '</td></tr>';
  }

  // chart area
  const chartSection = '<div class="chart-area"><canvas id="' + canvasId + '" width="620" height="300"></canvas></div>';

  return '<div class="q-card">'
    + '<div class="q-card-header">'
    +   '<span class="q-num-badge">Q' + (idx + 1) + '</span>'
    +   '<span class="q-type-badge">' + escHtml(typeName) + '</span>'
    + '</div>'
    + chartSection
    + '<table class="q-table">' + rows + '</table>'
    + '</div>';
}

function fRow(label, value) {
  return '<tr><td class="f-label">' + label + '</td><td class="f-val">'
       + (value || '<span style="color:#94a3b8;font-style:italic;">NULL</span>')
       + '</td></tr>';
}

function toggleRaw() {
  const box = document.getElementById('rawBox');
  const t = document.querySelector('.raw-toggle');
  const on = box.style.display === 'block';
  box.style.display = on ? 'none' : 'block';
  t.textContent = on ? '▶ Gemini 원본 응답 보기' : '▼ Gemini 원본 응답 닫기';
}

function escHtml(s) {
  if (s == null) return '';
  return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}
</script>
</body>
</html>
