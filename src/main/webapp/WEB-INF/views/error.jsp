<%@ page contentType="text/html;charset=UTF-8" language="java" isErrorPage="true" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>오류 발생 - PrepAI</title>
    <style>
        body {
            font-family: 'Malgun Gothic', sans-serif;
            background: #f5f5f5;
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
            margin: 0;
        }
        .error-box {
            background: white;
            border-radius: 12px;
            padding: 48px 40px;
            max-width: 520px;
            width: 100%;
            box-shadow: 0 4px 20px rgba(0,0,0,0.08);
            text-align: center;
        }
        .error-icon {
            font-size: 48px;
            margin-bottom: 16px;
        }
        h2 {
            color: #333;
            margin: 0 0 12px;
            font-size: 22px;
        }
        .error-message {
            color: #e74c3c;
            background: #fdf0ef;
            border: 1px solid #f5c6c6;
            border-radius: 8px;
            padding: 14px 18px;
            font-size: 14px;
            line-height: 1.6;
            text-align: left;
            margin: 20px 0;
            word-break: break-word;
        }
        .tip {
            color: #666;
            font-size: 13px;
            margin-bottom: 28px;
            line-height: 1.7;
        }
        .back-btn {
            display: inline-block;
            background: #4A90D9;
            color: white;
            padding: 12px 32px;
            border-radius: 8px;
            text-decoration: none;
            font-size: 15px;
            font-weight: bold;
            transition: background 0.2s;
        }
        .back-btn:hover {
            background: #3578c7;
        }
    </style>
</head>
<body>
    <div class="error-box">
        <div class="error-icon">⚠️</div>
        <h2>문제 생성 중 오류가 발생했습니다</h2>

        <div class="error-message">
            <%
                String msg = (String) request.getAttribute("errorMessage");
                if (msg == null && exception != null) {
                    msg = exception.getMessage();
                }
                if (msg == null) {
                    msg = "알 수 없는 오류가 발생했습니다.";
                }
                out.print(msg);
            %>
        </div>

        <p class="tip">
            💡 <strong>해결 방법</strong><br>
            · 문제 유형 수나 개수를 줄여보세요.<br>
            · 지문이 너무 짧은 경우 더 긴 지문을 사용해보세요.<br>
            · 잠시 후 다시 시도해 주세요.
        </p>

        <a href="/question-form" class="back-btn">← 돌아가기</a>
    </div>
</body>
</html>
