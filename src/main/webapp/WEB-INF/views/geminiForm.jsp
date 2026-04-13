<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <script src="${pageContext.request.contextPath}/js/examData.js"></script>
    <script src="${pageContext.request.contextPath}/js/littlePrince.js"></script>
    <script src="${pageContext.request.contextPath}/js/geminiForm.js" defer></script>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/geminiForm.css">
    <meta charset="UTF-8">
    <title>Gemini 프롬프트 생성기</title>
</head>
<body>
<div class="container">
    <h2>🤖 Gemini 프롬프트 생성기</h2>
    <p class="subtitle">파라미터를 설정하고 프롬프트를 생성하여 Gemini 웹에 붙여넣으세요.</p>

    <div class="form-layout">

        <!-- 왼쪽 컬럼 -->
        <div class="left-column">

            <div class="form-card">
                <label class="form-label">📚 시험 과목 선택 (택 1)</label>
                <div class="radio-group">
                    <label><input type="radio" name="examType" value="모의고사" checked onchange="toggleUI(this.value)">모의고사</label>
                    <label><input type="radio" name="examType" value="외부지문" onchange="toggleUI(this.value)">외부 지문</label>
                    <label><input type="radio" name="examType" value="교과서" onchange="toggleUI(this.value)">교과서</label>
                </div>
            </div>

            <div class="form-card">
                <label class="form-label">📋 프롬프트 템플릿 선택</label>
                <div class="radio-group" style="flex-direction: column;">
                    <label style="text-align:left; display:flex; align-items:center; gap:8px;">
                        <input type="radio" name="passageSource" value="regular" checked onchange="togglePassageSource(this.value)">
                        수능/모의고사 기출용 (Alice 모의고사)
                    </label>
                    <label style="text-align:left; display:flex; align-items:center; gap:8px;">
                        <input type="radio" name="passageSource" value="external" onchange="togglePassageSource(this.value)">
                        외부 지문 내신용 (Alice 내신 모의고사)
                    </label>
                </div>
            </div>

            <!-- 외부 지문 전용: 핵심 출제 포인트 -->
            <div class="form-card" id="key-points-card" style="display:none;">
                <label class="form-label">🎯 핵심 출제 포인트 입력</label>
                <textarea id="keyPointsText" placeholder="선생님이 강조한 문법/어휘 포인트를 입력하세요.&#10;예) 과거완료 had p.p., It ~ that 강조 구문, 감정 형용사 어휘 등"></textarea>
            </div>

            <!-- 모의고사 번호 선택 (모의고사 전용) -->
            <div class="form-card" id="mock-number-card">
                <label class="form-label">📌 출제할 문제 번호 선택 (다중 선택 가능)</label>
                <div class="number-grid">
                    <% for(int i=18; i<=45; i++){%>
                    <div>
                        <input type="checkbox" name="questionNos" id="q_num_<%=i%>" value="<%=i%>" class="hidden-cb">
                        <label for="q_num_<%=i%>" class="number-label"><%=i%></label>
                    </div>
                    <% }%>
                </div>
            </div>

            <!-- 지문 입력 카드 -->
            <div class="form-card" id="passage-input-card">
                <label class="form-label" id="passage-label">📄 모의고사 선택</label>

                <!-- 모의고사 드롭다운 (모의고사 전용) -->
                <div id="exam-select-wrapper">
                    <select id="examSelect" class="exam-select">
                        <option value="">-- 시험지를 선택하세요 --</option>
                    </select>
                </div>

                <!-- 프리셋 지문 선택 (외부지문/교과서 전용) -->
                <div id="external-select-wrapper" style="display:none;">
                    <select id="externalPassageSelect" class="exam-select" onchange="onExternalPassageChange(this.value)">
                        <option value="">-- 프리셋 지문 선택 (선택사항) --</option>
                    </select>
                    <p style="font-size:13px; color:#6c757d; margin:8px 0 12px 2px;">↑ 선택하거나, 아래에 직접 붙여넣기 (둘 중 하나만 있으면 됩니다)</p>
                </div>

                <!-- 텍스트 입력 (외부지문/교과서) -->
                <div id="text-area-wrapper" style="display:none;">
                    <textarea id="passageText" placeholder="여기에 영어 지문을 붙여넣으세요..."></textarea>
                </div>
            </div>

            <div class="form-card">
                <label class="form-label">🌟 난이도 선택</label>
                <div class="radio-group" style="flex-direction: column;">
                    <label style="text-align:left;"><input type="radio" name="difficultyLevel" value="하"> 하 (기본적인 내용 파악)</label>
                    <label style="text-align:left;"><input type="radio" name="difficultyLevel" value="중" checked> 중 (수능/내신 평균 수준)</label>
                    <label style="text-align:left;"><input type="radio" name="difficultyLevel" value="상"> 상 (매력적인 오답이 포함된 고난도)</label>
                </div>
            </div>

            <div class="form-card">
                <label class="form-label">🔄 지문 변형 여부</label>
                <div class="radio-group" style="flex-direction: column;">
                    <label style="text-align:left; display:flex; align-items:center; gap:8px;">
                        <input type="radio" name="modification" value="원본그대로" checked> 원본 지문 그대로 출제
                    </label>
                    <label style="text-align:left; display:flex; align-items:center; gap:8px;">
                        <input type="radio" name="modification" value="지문변형"> 지문 변형 출제 (유의어 대체, 문장 구조 변경 등)
                    </label>
                </div>
            </div>

        </div>

        <!-- 오른쪽 컬럼 -->
        <div class="right-column">

            <div class="form-card">
                <label class="form-label">🎯 문제 유형 및 개수 선택</label>
                <div class="checkbox-group">
                    <div class="checkbox-item">
                        <label><input type="checkbox" name="questionTypes" value="빈칸추론"> 빈칸 추론 (Blank)</label>
                        <div><input type="number" name="count_빈칸추론" class="number-input" min="1" max="10" value="1"> 개</div>
                    </div>
                    <div class="checkbox-item">
                        <label><input type="checkbox" name="questionTypes" value="주제파악"> 글의 주제 파악 (Topic)</label>
                        <div><input type="number" name="count_주제파악" class="number-input" min="1" max="10" value="1"> 개</div>
                    </div>
                    <div class="checkbox-item">
                        <label><input type="checkbox" name="questionTypes" value="요지파악"> 글의 요지 파악 (Main Idea)</label>
                        <div><input type="number" name="count_요지파악" class="number-input" min="1" max="10" value="1"> 개</div>
                    </div>
                    <div class="checkbox-item">
                        <label><input type="checkbox" name="questionTypes" value="제목추론"> 제목 추론 (Title)</label>
                        <div><input type="number" name="count_제목추론" class="number-input" min="1" max="10" value="1"> 개</div>
                    </div>
                    <div class="checkbox-item">
                        <label><input type="checkbox" name="questionTypes" value="요약문"> 요약문 완성 (Summary)</label>
                        <div><input type="number" name="count_요약문" class="number-input" min="1" max="10" value="1"> 개</div>
                    </div>
                    <div class="checkbox-item">
                        <label><input type="checkbox" name="questionTypes" value="순서배열"> 내용 순서 배열 (Ordering)</label>
                        <div><input type="number" name="count_순서배열" class="number-input" min="1" max="10" value="1"> 개</div>
                    </div>
                    <div class="checkbox-item">
                        <label><input type="checkbox" name="questionTypes" value="어법문제"> 어법상 틀린 것 찾기 (Grammar)</label>
                        <div><input type="number" name="count_어법문제" class="number-input" min="1" max="10" value="1"> 개</div>
                    </div>
                    <div class="checkbox-item">
                        <label><input type="checkbox" name="questionTypes" value="어휘문제"> 문맥상 낱말의 쓰임이 틀린 것 찾기 (Vocabulary)</label>
                        <div><input type="number" name="count_어휘문제" class="number-input" min="1" max="10" value="1"> 개</div>
                    </div>
                    <div class="checkbox-item">
                        <label><input type="checkbox" name="questionTypes" value="문장삽입"> 주어진 문장 들어가기 (Insertion)</label>
                        <div><input type="number" name="count_문장삽입" class="number-input" min="1" max="10" value="1"> 개</div>
                    </div>
                    <div class="checkbox-item">
                        <label><input type="checkbox" name="questionTypes" value="무관한문장"> 흐름과 관계 없는 문장 찾기 (Irrelevant)</label>
                        <div><input type="number" name="count_무관한문장" class="number-input" min="1" max="10" value="1"> 개</div>
                    </div>
                    <div class="checkbox-item">
                        <label><input type="checkbox" name="questionTypes" value="대명사찾기"> 가리키는 대상이 다른 것 찾기 (Pronoun)</label>
                        <div><input type="number" name="count_대명사찾기" class="number-input" min="1" max="10" value="1"> 개</div>
                    </div>
                    <div class="checkbox-item">
                        <label><input type="checkbox" name="questionTypes" value="내용일치"> 내용 일치/불일치 파악하기 (True/False)</label>
                        <div><input type="number" name="count_내용일치" class="number-input" min="1" max="10" value="1"> 개</div>
                    </div>
                    <div class="checkbox-item subjective-divider">
                        <span class="subjective-label">── 주관식 ──</span>
                    </div>
                    <div class="checkbox-item">
                        <label><input type="checkbox" name="questionTypes" value="서답형"> 서답형 (Short Answer)</label>
                        <div><input type="number" name="count_서답형" class="number-input" min="1" max="10" value="1"> 개</div>
                    </div>
                    <div class="checkbox-item">
                        <label><input type="checkbox" name="questionTypes" value="서술형"> 서술형 (Descriptive)</label>
                        <div><input type="number" name="count_서술형" class="number-input" min="1" max="10" value="1"> 개</div>
                    </div>
                </div>
            </div>

        </div>
    </div>

    <button type="button" class="btn-generate" onclick="generatePrompt()">프롬프트 생성하기 ✨</button>

    <!-- 프롬프트 출력 영역 -->
    <div id="prompt-output-section" style="display:none;">
        <div class="prompt-output-header">
            <span class="prompt-output-title">생성된 프롬프트</span>
            <button class="btn-copy" id="copyBtn" onclick="copyPrompt()">📋 클립보드에 복사</button>
        </div>
        <textarea id="promptOutput" class="prompt-output-textarea" readonly></textarea>
        <p class="prompt-hint">위 프롬프트를 복사하여 <strong>Gemini 웹(gemini.google.com)</strong>에 붙여넣으세요.</p>
    </div>

</div>
</body>
</html>
