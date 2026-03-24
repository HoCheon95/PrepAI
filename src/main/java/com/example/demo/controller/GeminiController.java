package com.example.demo.controller;

import com.example.demo.geminiAI.GeminiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Map;

@RestController
public class GeminiController {

    @Autowired
    private GeminiService geminiService;

    @GetMapping("/api/chat")
    public String chatWithGemini(@RequestParam String prompt) {
        return geminiService.getGeminiResponse(prompt);
    }

    @PostMapping(value = "/api/generate-questions", produces = "text/html; charset=UTF-8")
    public ModelAndView generateQuestions(
            @RequestParam(value = "examType", defaultValue="모의고사") String examType,
            @RequestParam(value = "passageText", required = false) String passageText,
            @RequestParam(value = "questionNos", required = false) List<String> questionNos, 
            @RequestParam(value = "questionTypes", required = false) List<String> questionTypes,
            @RequestParam(value = "difficultyLevel", required = false) String difficultyLevel,
            @RequestParam(value = "modification", required = false) List<String> modifications,
            @RequestParam(value = "passageImage", required = false) MultipartFile passageImage,
            @RequestParam Map<String, String> allParams) {

        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are an expert English exam creator for high school students.\n");
        prompt.append("Create high-quality questions based on the [PASSAGE].\n\n");
        
        prompt.append("[STRICT FORMATTING RULES]\n");
        prompt.append("1. Do NOT use markdown like **bold** or *italic*.\n");
        prompt.append("2. If you need to underline a word, MUST use HTML tags like this: <u>underlined word</u>\n");
        prompt.append("3. For 'Fill in the blank' questions, MUST use [ ________ ] to represent the blank.\n");
        prompt.append("4. CRITICAL: Output raw text only. No markdown code blocks.\n\n");

        prompt.append("[STRICT OUTPUT STRUCTURE]\n");
        prompt.append("For EACH question, you MUST follow this tag system exactly:\n");
        prompt.append("[[QUESTION]]\n(Question number. Question text in Korean)\n");
        prompt.append("[[PASSAGE]]\n(English Passage here. If Summary question, put '↓' and summary at the bottom.)\n");
        prompt.append("[[OPTIONS]]\n(1) (Option 1)\n(2) (Option 2)\n(3) (Option 3)\n(4) (Option 4)\n(5) (Option 5)\n");   
        prompt.append("[[ANSWER]]\n(Correct option number)\n");
        prompt.append("[[EXPLANATION]]\n(Detailed explanation in Korean)\n");
        prompt.append("---SEP---\n\n");

        prompt.append("[EXAM CONDITIONS]\n");
        if (difficultyLevel != null) {
            prompt.append("- Difficulty Level: ").append(difficultyLevel).append("\n");
        }
        if (modifications != null && modifications.contains("지문변형")) {
            prompt.append("- Passage Modification: Modify the original passage but keep the core meaning.\n");
        } else {
            prompt.append("- Passage Modification: Use the original passage EXACTLY as it is.\n");
        }
        prompt.append("\n");

        // 🔴 파일 유무와 텍스트 유무를 먼저 판단하여 변수에 저장한다. 🔴
        boolean hasFile = passageImage != null && !passageImage.isEmpty();
        boolean hasText = passageText != null && !passageText.trim().isEmpty();

        prompt.append("[PASSAGE SOURCE & EXTRACTION]\n");
        
        // 🔴 파일이 실제로 존재하고, 모의고사 모드일 때만 PDF를 스캔하라는 명령을 내린다. 🔴
        if (hasFile && "모의고사".equals(examType) && questionNos != null && !questionNos.isEmpty()) {
            String targetNums = String.join(", ", questionNos);
            prompt.append("Target Question Numbers: [").append(targetNums).append("]\n");
            prompt.append("TASK: Read the attached document carefully. Extract the English reading passage for EACH Target Question Number.\n");
            prompt.append("HINT: Locate the number, skip the Korean text, and EXTRACT ONLY THE ENGLISH PASSAGE.\n\n");
        } 
        // 🔴 파일이 없고 텍스트만 입력되었을 경우, 입력된 텍스트를 기반으로 문제를 만들도록 분기 처리한다. 🔴
        else if (hasText) {
            prompt.append("TASK: Use the text provided below as the base passage.\n");
            if ("외부지문".equals(examType)) {
                // 🔴 텍스트가 짧은데 여러 문제를 요구할 때 500 에러(서버 크래시)가 나는 것을 막기 위해, 분할 규칙을 아주 유연하게 수정한다. 🔴
                prompt.append("RULE FOR EXTERNAL TEXT: If the text is long enough, you may divide it into smaller paragraphs for different questions. However, if the text is short, just use the entire text for ALL questions. DO NOT force division if it causes a logical error.\n");
            }
            prompt.append("[TEXT]\n").append(passageText).append("\n\n");
        } 
        else {
            prompt.append("TASK: No specific passage provided. Please generate a random high school level English passage and base the questions on it.\n\n");
        }

        prompt.append("[QUESTION TYPES TO GENERATE]\n");
        prompt.append("CRITICAL: DO NOT reuse the original question from the text. Create completely NEW questions.\n");
        // 🔴 AI의 과부하를 줄이기 위해 선택지 규칙을 조금 더 간결하게 다듬는다. 🔴
        prompt.append("- Make 3 options clearly incorrect, and 2 options (including the answer) highly confusing.\n\n");
        
        if (questionTypes != null) {
            for (String type : questionTypes) {
                String count = allParams.get("count_" + type);
                prompt.append("- ").append(type).append(": ").append(count).append("개\n");
                
                // 🔴 각 유형별 공식도 AI가 소화하기 쉽도록 간결한 영어로 최적화한다. 🔴
                if (type.contains("빈칸")) {
                    prompt.append("  -> RULE: Replace a phrase with [ ________ ]. The answer MUST be a SYNONYM, not the exact original text.\n");
                } else if (type.contains("순서")) {
                    prompt.append("  -> RULE: Provide a starting sentence, then shuffle the rest into (A), (B), and (C).\n");
                } else if (type.contains("요약")) {
                    prompt.append("  -> RULE: At the bottom of [[PASSAGE]], put '↓' and a 1-2 sentence summary with blanks (A) and (B).\n");
                } else if (type.contains("주제") || type.contains("요지") || type.contains("제목")) {
                    prompt.append("  -> RULE: Options MUST be in English.\n");
                } else if (type.contains("어법")) {
                    prompt.append("  -> RULE: Underline 5 parts and label them (1) to (5). One is incorrect.\n");
                } else if (type.contains("어휘")) {
                    prompt.append("  -> RULE: Underline 5 words labeled (1) to (5). Replace one with an inappropriate word.\n");
                } else if (type.contains("문장삽입")) {
                    prompt.append("  -> RULE: Extract one sentence into [[QUESTION]]. Place spots (1) to (5) in the [[PASSAGE]].\n");
                } else if (type.contains("무관한")) {
                    prompt.append("  -> RULE: Insert ONE irrelevant sentence. Number 5 sentences (1) to (5).\n");
                } else if (type.contains("대명사")) {
                    prompt.append("  -> RULE: Underline 5 pronouns labeled (a) to (e). One refers to a different entity.\n");
                } else if (type.contains("내용일치")) {
                    prompt.append("  -> RULE: Write 5 factual statements in Korean. Exactly ONE MUST be FALSE.\n");
                }
            }
        }

        String aiResponse = geminiService.getGeminiResponse(prompt.toString(), passageImage);
        
        // 🔴 디버깅용: AI가 출력한 실제 결과를 콘솔창에 띄워 문제 원인을 쉽게 파악할 수 있도록 한다. 🔴
        System.out.println("================== [AI 응답 결과 확인] ==================");
        System.out.println(aiResponse);
        System.out.println("=========================================================");

        ModelAndView mav = new ModelAndView("result");
        mav.addObject("examResult", aiResponse);
        return mav;
    }
}