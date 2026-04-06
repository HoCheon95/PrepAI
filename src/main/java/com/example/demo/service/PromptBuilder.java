package com.example.demo.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

// 🔴 문제 유형별 프롬프트 조립 책임을 GeminiController에서 분리한다. 🔴
@Component
public class PromptBuilder {

    // 🔴 일반 문제 생성용 전체 프롬프트를 조립하는 진입점 메서드다. 🔴
    public String build(
            String examType,
            String passageText,
            List<String> questionNos,
            List<String> questionTypes,
            String difficultyLevel,
            List<String> modifications,
            boolean hasFile,
            boolean hasText,
            Map<String, String> counts) {

        StringBuilder prompt = new StringBuilder();
        appendOutputFormat(prompt);
        appendExamConditions(prompt, difficultyLevel, modifications);
        appendPassageSource(prompt, examType, passageText, questionNos, hasFile, hasText);
        appendQuestionTypes(prompt, questionTypes, counts);
        return prompt.toString();
    }

    // 🔴 단일 문제 재생성용 프롬프트를 조립한다. (Phase 2에서 사용) 🔴
    public String buildSingle(String questionType, String passageText, String difficultyLevel) {
        StringBuilder prompt = new StringBuilder();
        appendOutputFormat(prompt);
        if (difficultyLevel != null) {
            prompt.append("[EXAM CONDITIONS]\n");
            prompt.append("- Difficulty Level: ").append(difficultyLevel).append("\n\n");
        }
        prompt.append("[PASSAGE SOURCE & EXTRACTION]\n");
        prompt.append("TASK: Use the text provided below as the base passage.\n");
        prompt.append("[TEXT]\n").append(passageText).append("\n\n");
        prompt.append("[QUESTION TYPES TO GENERATE]\n");
        prompt.append("CRITICAL: DO NOT reuse the original question from the text. Create completely NEW question.\n");
        prompt.append("- Make 3 options clearly incorrect, and 2 options (including the answer) highly confusing.\n\n");
        prompt.append("- ").append(questionType).append(": 1개\n");
        appendTypeRule(prompt, questionType);
        return prompt.toString();
    }

    // 🔴 모든 문제에 공통으로 적용되는 출력 형식 규칙을 추가한다. 🔴
    // 🔴 태그는 반드시 단독 줄에 위치해야 Gemini가 형식을 올바르게 따른다. 🔴
    private void appendOutputFormat(StringBuilder p) {
        p.append("[STRICT FORMATTING RULES]\n");
        p.append("1. Do NOT use markdown like **bold** or *italic*.\n");
        p.append("2. If you need to underline a word, MUST use HTML tags: <u>underlined word</u>\n");
        p.append("3. For 'Fill in the blank' questions, MUST use [ ________ ] to represent the blank.\n");
        p.append("4. CRITICAL: Output raw text only. No markdown code blocks.\n\n");

        p.append("[STRICT OUTPUT STRUCTURE]\n");
        p.append("For EACH question, you MUST follow this tag system exactly:\n");
        p.append("[[QUESTION]]\n");
        p.append("(Question number. Question text in Korean)\n");
        p.append("[[PASSAGE]]\n");
        p.append("(English Passage ONLY. NEVER include Korean text inside [[PASSAGE]]. If Summary question, put '↓' and summary at the bottom.)\n");
        p.append("[[OPTIONS]]\n");
        p.append("(1) (Option 1)\n");
        p.append("(2) (Option 2)\n");
        p.append("(3) (Option 3)\n");
        p.append("(4) (Option 4)\n");
        p.append("(5) (Option 5)\n");
        p.append("[[ANSWER]]\n");
        p.append("(Correct option number)\n");
        p.append("[[EXPLANATION]]\n");
        p.append("(Detailed explanation in Korean)\n");
        p.append("---SEP---\n\n");
    }

    // 🔴 난이도와 지문 변형 여부를 조건으로 추가한다. 🔴
    private void appendExamConditions(StringBuilder p, String difficultyLevel, List<String> modifications) {
        p.append("[EXAM CONDITIONS]\n");
        if (difficultyLevel != null) {
            p.append("- Difficulty Level: ").append(difficultyLevel).append("\n");
        }
        if (modifications != null && modifications.contains("지문변형")) {
            p.append("- Passage Modification: Modify the original passage but keep the core meaning.\n");
        } else {
            p.append("- Passage Modification: Use the original passage EXACTLY as it is.\n");
        }
        p.append("\n");
    }

    // 🔴 시험 유형에 따라 지문 소스 지시를 분기 처리한다. 🔴
    private void appendPassageSource(StringBuilder p, String examType, String passageText,
                                     List<String> questionNos, boolean hasFile, boolean hasText) {
        p.append("[PASSAGE SOURCE & EXTRACTION]\n");

        // 🔴 파일이 존재하고 모의고사 모드일 때: PDF에서 특정 문제 번호의 지문을 추출하도록 지시한다. 🔴
        if (hasFile && "모의고사".equals(examType) && questionNos != null && !questionNos.isEmpty()) {
            String targetNums = String.join(", ", questionNos);
            p.append("Target Question Numbers: [").append(targetNums).append("]\n");
            p.append("TASK: Read the attached document carefully. Extract the English reading passage for EACH Target Question Number.\n");
            p.append("HINT: A real question ALWAYS starts with 'N.' (e.g., '43.'). Do NOT treat section headers like '[43~45]' as question numbers — they are just group labels.\n");
            p.append("HINT: For each Target Question Number, find the line starting with 'N.' and put that Korean question text into [[QUESTION]]. Extract the full English passage into [[PASSAGE]].\n");
            p.append("HINT: If the passage contains labeled sections like (A), (B), (C), (D), you MUST preserve ALL section labels exactly as they appear inside [[PASSAGE]].\n\n");
        }
        // 🔴 JSON에서 추출한 지문 텍스트로 모의고사 문제를 생성할 때: 레이블된 지문을 문제 번호별로 사용한다. 🔴
        else if ("모의고사".equals(examType) && hasText && questionNos != null && !questionNos.isEmpty()) {
            String targetNums = String.join(", ", questionNos);
            p.append("Target Question Numbers: [").append(targetNums).append("]\n");
            p.append("TASK: The text below contains English passages labeled by question number (e.g., [Question 18]). ");
            p.append("For each Target Question Number, use the corresponding labeled passage as the base [[PASSAGE]] to create questions.\n");
            p.append("HINT: If the passage contains labeled sections like (A), (B), (C), (D), you MUST preserve ALL section labels exactly as they appear inside [[PASSAGE]].\n");
            p.append("[TEXT]\n").append(passageText).append("\n\n");
        }
        // 🔴 텍스트만 있을 때: 입력된 텍스트를 지문으로 사용한다. 🔴
        else if (hasText) {
            p.append("TASK: Use the text provided below as the base passage.\n");
            if ("외부지문".equals(examType)) {
                // 🔴 긴 외부지문을 각 문제마다 6~8문장 청크로 분할해서 사용하도록 강제한다. 🔴
                p.append("CRITICAL RULE FOR EXTERNAL TEXT: You MUST divide the provided text into smaller chunks of exactly 6 to 8 sentences each.\n");
                p.append("Use ONE chunk (6-8 sentences) as the base [PASSAGE] for EACH question you generate. Do NOT use the entire lengthy text for a single question.\n\n");
            }
            p.append("[TEXT]\n").append(passageText).append("\n\n");
        }
        // 🔴 지문이 없을 때: AI가 임의로 지문을 생성하도록 한다. 🔴
        else {
            p.append("TASK: No specific passage provided. Please generate a random high school level English passage and base the questions on it.\n\n");
        }
    }

    // 🔴 요청된 총 문제 개수를 반환한다. GeminiController가 생성 결과 검증에 사용한다. 🔴
    public int countTotal(List<String> questionTypes, Map<String, String> counts) {
        int total = 0;
        if (questionTypes != null) {
            for (String type : questionTypes) {
                String countStr = counts.get("count_" + type);
                try { total += Integer.parseInt(countStr); } catch (Exception ignored) {}
            }
        }
        return total;
    }

    // 🔴 선택된 문제 유형과 개수, 각 유형별 생성 규칙을 추가한다. 🔴
    private void appendQuestionTypes(StringBuilder p, List<String> questionTypes, Map<String, String> counts) {
        // 🔴 총 문제 개수를 계산해서 Gemini에게 명시적으로 알려준다. 🔴
        int totalCount = countTotal(questionTypes, counts);

        p.append("[QUESTION TYPES TO GENERATE]\n");
        p.append("You MUST generate exactly ").append(totalCount).append(" questions in total. Do NOT stop early. Complete ALL questions before ending.\n");
        p.append("CRITICAL: DO NOT reuse the original question from the text. Create completely NEW questions.\n");
        p.append("- Make 3 options clearly incorrect, and 2 options (including the answer) highly confusing.\n\n");

        if (questionTypes != null) {
            for (String type : questionTypes) {
                String count = counts.get("count_" + type);
                p.append("- ").append(type).append(": ").append(count).append("개\n");
                appendTypeRule(p, type);
            }
        }
    }

    // 🔴 문제 유형별 생성 규칙을 한 줄 영어로 추가한다. (gemini-prompt-rules.skill.md 준수) 🔴
    private void appendTypeRule(StringBuilder p, String type) {
        if (type.contains("빈칸")) {
            p.append("  -> RULE: Replace a phrase with [ ________ ]. The answer MUST be a SYNONYM, not the exact original text.\n");
        } else if (type.contains("순서")) {
            p.append("  -> RULE: Provide a starting sentence, then shuffle the rest into (A), (B), and (C).\n");
        } else if (type.contains("요약")) {
            p.append("  -> RULE: At the bottom of [[PASSAGE]], put '↓' and a 1-2 sentence summary with blanks (A) and (B).\n");
        } else if (type.contains("주제") || type.contains("요지") || type.contains("제목")) {
            p.append("  -> RULE: Options MUST be in English.\n");
        } else if (type.contains("어법")) {
            p.append("  -> RULE: Underline 5 parts and label them (1) to (5). One is incorrect.\n");
        } else if (type.contains("어휘")) {
            p.append("  -> RULE: Underline 5 words labeled (1) to (5). Replace one with an inappropriate word.\n");
        } else if (type.contains("문장삽입")) {
            p.append("  -> RULE: Extract one sentence into [[QUESTION]]. Place spots (1) to (5) in the [[PASSAGE]].\n");
        } else if (type.contains("무관한")) {
            p.append("  -> RULE: Insert ONE irrelevant sentence. Number 5 sentences (1) to (5).\n");
        } else if (type.contains("대명사")) {
            p.append("  -> RULE: Underline 5 pronouns labeled (a) to (e). One refers to a different entity.\n");
        } else if (type.contains("내용일치")) {
            p.append("  -> RULE: Write 5 factual statements in Korean. Exactly ONE MUST be FALSE.\n");
        }
    }
}
