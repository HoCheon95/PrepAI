<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>문제 JSON 뷰어 &amp; PDF 정확도 테스트</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/json.css">
</head>
<body>
<div class="container">

  <div class="page-header">
    <h1>문제 JSON 뷰어</h1>
    <div class="header-links">
      <a href="/test/pdf">← PDF 테스트 페이지</a>
      <a href="/question-form">문제 생성</a>
    </div>
  </div>

  <div class="selector">
    <div class="selector-top">
      <span>JSON 파일</span>
      <input type="file" id="jsonFile" accept=".json,.txt" style="font-size:12px;">
    </div>
    <div class="selector-top" style="margin-top:8px;">
      <span>문제 번호 선택</span>
      <button onclick="selectAll()">전체 선택</button>
      <button onclick="clearAll()">전체 해제</button>
      <button onclick="selectRange(18,30)">18~30</button>
      <button onclick="selectRange(31,40)">31~40</button>
      <button onclick="selectRange(41,45)">41~45</button>
      <button class="btn-primary" onclick="renderJsonCards()">선택 문제 보기 →</button>
    </div>
    <div class="checkboxes" id="checkboxes">
      <span style="color:#94a3b8;font-size:12px;font-style:italic;">JSON 파일을 먼저 업로드하세요.</span>
    </div>
  </div>

  <div class="tab-bar">
    <button class="tab active" data-tab="json" onclick="switchTab('json')">JSON 뷰 (정답 데이터)</button>
    <button class="tab" data-tab="pdf" onclick="switchTab('pdf')">PDF 정확도 테스트</button>
  </div>

  <div id="jsonTab" class="tab-content">
    <div id="jsonCards" class="empty-state">위에서 문제 번호를 선택한 후 "선택 문제 보기"를 클릭하세요.</div>
  </div>

  <div id="pdfTab" class="tab-content" style="display:none">
    <div class="upload-area">
      <span class="upload-label">PDF 파일</span>
      <input type="file" id="pdfFile" accept=".pdf">
      <button class="btn-primary" onclick="runComparison()">추출 &amp; 비교 시작</button>
      <span id="compareStatus"></span>
    </div>
    <div id="compareResults" class="empty-state">PDF를 업로드하고 비교를 시작하세요.</div>
  </div>
</div>

<script src="${pageContext.request.contextPath}/js/json.js"></script>

</body>
</html>