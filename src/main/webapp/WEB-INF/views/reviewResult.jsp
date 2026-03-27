<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>문제 검토 — PrepAI</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/reviewResult.css">
    <script src="${pageContext.request.contextPath}/js/reviewResult.js" defer></script>
</head>
<body>

    <%-- 🔴 상단 고정 액션 바 🔴 --%>
    <div class="action-bar">
        <span class="question-count" id="question-count">문제 로딩 중...</span>

        <%-- 🔴 문제지 / 해설지 뷰 전환 토글 🔴 --%>
        <div class="view-toggle">
            <button class="btn-toggle active" id="btn-view-questions" onclick="switchView('questions')">문제지</button>
            <button class="btn-toggle" id="btn-view-answers" onclick="switchView('answers')">해설지</button>
        </div>

        <div class="action-buttons">
            <button class="btn" onclick="copyAll()">복사하기</button>
            <button class="btn btn-primary" id="btn-save" onclick="saveToDb()">DB에 저장</button>
            <a href="/question-form" class="btn">다시 생성하기</a>
            <%-- 터미널에 다시 생성하기 출력 --%>
            <script>
                console.log("%c[PrepAI] '다시 생성하기' 버튼이 활성화되었습니다. 클릭 시 /question-form 페이지로 이동합니다.", "color: green; font-weight: bold;");
            </script>
        </div>
    </div>

    <%-- 🔴 문제지 뷰 🔴 --%>
    <div id="view-questions" class="exam-container"></div>

    <%-- 🔴 해설지 뷰 (기본 숨김) 🔴 --%>
    <div id="view-answers" class="exam-container" style="display:none"></div>

    <%-- 🔴 토스트 메시지 영역 🔴 --%>
    <div id="toast"></div>

    <%-- 🔴 원본 AI 응답 — JS 파서가 읽는 숨김 데이터 🔴 --%>
    <textarea id="raw-data" style="display:none">${examResult}</textarea>
    <input type="hidden" id="exam-type" value="${examType}">

</body>
</html>
