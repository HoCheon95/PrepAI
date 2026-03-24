<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <script src="${pageContext.request.contextPath}/js/questionForm.js" defer></script>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/questionForm.css">
        <meta charset="UTF-8">
        <title>AI 모의고사 출제 폼</title>
    </head>
    <body>

        <div class="container">
            <h2>📝 AI 영어 모의고사 출제 센터</h2>

            <form action="/api/generate-questions" method="post" enctype="multipart/form-data">
                
                <div class="form-layout">
                    
                    <div class="left-column">
                        <div class="form-card">
                            <label class="form-label">📚 시험 과목 선택 (택 1)</label>
                            <div class="radio-group">
                                <label><input type="radio" name="examType" value="모의고사" checked onchange="toggleUI(this.value)">모의고사</label>
                                <label><input type="radio" name="examType" value="외부지문" onchange="toggleUI(this.value)">외부 지문</label>
                                <label><input type="radio" name="examType" value="교과서" onchange="toggleUI(this.value)">교과서</label>
                            </div>
                        </div>

                        <div class="form-card" id="mock-number-card">
                            <label class="form-label">📌 출제할 문제 번호 매핑 (다중 선택 가능)</label>
                            <div class="number-grid">
                                <% for(int i=18; i<=45; i++){%>
                                <div>
                                    <input type="checkbox" name="questionNos" id="q_num_<%=i%>" value="<%=i%>" class="hidden-cb">
                                    <label for="q_num_<%=i%>" class="number-label"><%=i%></label>
                                </div>
                                <% }%>
                            </div>
                        </div>

                        <div class="form-card" id="passage-input-card">
                            <label class="form-label" id="passage-label">📄 지문 파일 첨부</label>
                            <div id="text-area-wrapper" style="display: none;">
                                <textarea name="passageText" id="passageText" placeholder="이곳에 원문 지문을 붙여넣어 주세요..."></textarea>
                            </div>
                            <div style="margin-top: 15px;">
                                <p style="font-size:14px; color:#666; margin-bottom:5px;" id="file-help-text">※ 모의고사 원문 PDF 파일을 업로드해주세요.</p>
                                <input type="file" name="passageImage" accept=".pdf, image/*">
                            </div>
                        </div>

                        <div class="form-card">
                            <label class="form-label">🌟 난이도 선택</label>
                            <div class="radio-group" style="flex-direction: column;">
                                <label style="text-align: left;"><input type="radio" name="difficultyLevel" value="하" required> 하 (기본적인 내용 파악)</label>
                                <label style="text-align: left;"><input type="radio" name="difficultyLevel" value="중"> 중 (수능/내신 평균 수준)</label>
                                <label style="text-align: left;"><input type="radio" name="difficultyLevel" value="상"> 상 (매력적인 오답이 포함된 고난도)</label>
                            </div>
                        </div>
                    </div>

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
                                    <label><input type="checkbox" name="questionTypes" value="문장삽입"> 주어진 문장 들어가기에 적절한 곳 찾기 (Insertion)</label>
                                    <div><input type="number" name="count_문장삽입" class="number-input" min="1" max="10" value="1"> 개</div>
                                </div>
                                <div class="checkbox-item">
                                    <label><input type="checkbox" name="questionTypes" value="무관한문장"> 흐름과 관계 없는 문장 찾기 (Irrelevant)</label>
                                    <div><input type="number" name="count_무관한문장" class="number-input" min="1" max="10" value="1"> 개</div>
                                </div>
                                <div class="checkbox-item">
                                    <label><input type="checkbox" name="questionTypes" value="대명사찾기"> 가리키는 대상이 나머지 넷과 다른 것 찾기 (Pronoun)</label>
                                    <div><input type="number" name="count_대명사찾기" class="number-input" min="1" max="10" value="1"> 개</div>
                                </div>
                                <div class="checkbox-item">
                                    <label><input type="checkbox" name="questionTypes" value="내용일치"> 내용 일치/불일치 파악하기 (True/False)</label>
                                    <div><input type="number" name="count_내용일치" class="number-input" min="1" max="10" value="1"> 개</div>
                                </div>
                            </div>
                        </div>

                        <div class="form-card">
                            <label class="form-label">🔄 지문 변형 여부 (중복 선택 가능)</label>
                            <div class="checkbox-group">
                                <label style="display:flex; align-items:center; gap:8px;"><input type="checkbox" name="modification" value="원본그대로"> 원본 지문 그대로 출제 (내신 암기 확인용)</label>
                                <label style="display:flex; align-items:center; gap:8px;"><input type="checkbox" name="modification" value="지문변형"> 지문 변형 출제 (유의어 대체, 문장 구조 변경 등)</label>
                            </div>
                        </div>
                    </div>

                </div>

                <button type="submit" class="btn-submit">AI 문제 생성하기 🚀</button>
            </form>
        </div>

    </body>
</html>