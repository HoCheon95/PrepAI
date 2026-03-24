<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>실전 모의고사 시험지</title>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/html2pdf.js/0.10.1/html2pdf.bundle.min.js"></script>
    
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/result.css">
    
    <script src="${pageContext.request.contextPath}/js/result.js" defer></script>
</head>
<body>

    <textarea id="raw-data-container" style="display: none;">${examResult}</textarea>

    <div class="control-panel" data-html2canvas-ignore="true">
        <button class="btn" id="btn-std" onclick="toggleMode('student')" style="background-color: #ddd;">👨‍🎓 학생용 (문제만)</button>
        <button class="btn" id="btn-tch" onclick="toggleMode('teacher')">👨‍🏫 교사용 (해설포함)</button>
        <button class="btn btn-primary" onclick="downloadPDF()">📥 PDF 저장</button>
        <a href="/question-form" class="btn">↩️ 다시 출제하기</a>
    </div>

    <div class="paper-container" id="pdf-area">
        <div class="exam-header">
            <div>1</div>
            <div class="title">영 어 영 역</div>
            <div class="grade">고 등</div>
        </div>

        <div class="two-column-layout" id="examPaper"></div>
    </div>

</body>
</html>