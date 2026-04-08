package com.example.demo.controller;

import com.example.demo.geminiAI.GeminiService;
import com.example.demo.service.PromptBuilder;
import com.example.demo.service.QuestionSaveService;
import com.example.demo.service.ResponseValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
public class GeminiController {

    @Autowired
    private GeminiService geminiService;

    // 🔴 프롬프트 조립은 PromptBuilder에, 검증/재시도는 ResponseValidator에 위임한다. 🔴
    @Autowired
    private PromptBuilder promptBuilder;

    @Autowired
    private ResponseValidator responseValidator;

    @Autowired
    private QuestionSaveService questionSaveService;

    @GetMapping("/api/chat")
    public String chatWithGemini(@RequestParam String prompt) {
        return geminiService.getGeminiResponse(prompt);
    }

    @PostMapping(value = "/api/generate-questions", produces = "text/html; charset=UTF-8")
    public ModelAndView generateQuestions(
            @RequestParam(value = "examType", defaultValue = "모의고사") String examType,
            @RequestParam(value = "passageText", required = false) String passageText,
            @RequestParam(value = "questionNos", required = false) List<String> questionNos,
            @RequestParam(value = "questionTypes", required = false) List<String> questionTypes,
            @RequestParam(value = "difficultyLevel", required = false) String difficultyLevel,
            @RequestParam(value = "modification", required = false) List<String> modifications,
            @RequestParam(value = "outputMode", required = false, defaultValue = "") String outputMode,
            @RequestParam(value = "isSetMode", required = false, defaultValue = "false") boolean isSetMode,
            @RequestParam(value = "passageImage", required = false) MultipartFile passageImage,
            @RequestParam Map<String, String> allParams) {

        boolean hasFile = passageImage != null && !passageImage.isEmpty();
        boolean hasText = passageText != null && !passageText.trim().isEmpty();

        // 🔴 outputMode가 "mixed"이면 문제 유형 순서를 셔플한다. 🔴
        boolean isMixed = "mixed".equalsIgnoreCase(outputMode);
        if (isMixed && questionTypes != null) {
            questionTypes = new ArrayList<>(questionTypes);
            Collections.shuffle(questionTypes);
        }

        // 🔴 프롬프트 조립 + 요청 문제 수 산출을 PromptBuilder에 위임한다. 🔴
        String prompt        = promptBuilder.build(
                examType, passageText, questionNos, questionTypes,
                difficultyLevel, modifications, hasFile, hasText, allParams,
                isMixed, isSetMode);
        int    expectedCount = promptBuilder.countTotal(questionTypes, allParams);

        // 🔴 문제 수 × 900토큰(문제당 평균 출력량) + 2048 버퍼. 최소 4096, 최대 32768로 제한한다. 🔴
        int maxTokens = Math.min(32768, Math.max(4096, expectedCount * 900 + 2048));
        System.out.println("[PrepAI] 문제 생성 요청: " + expectedCount + "개 / maxTokens: " + maxTokens);

        // 🔴 Gemini 호출 후 ResponseValidator로 형식 + 문제 수 검증, 최대 3회 재시도한다. 🔴
        String initialResponse = geminiService.getGeminiResponse(prompt, passageImage, maxTokens);
        String aiResponse      = responseValidator.validateWithRetry(
                initialResponse, prompt, passageImage, geminiService, expectedCount, maxTokens);

        // 🔴 실제 생성된 완전한 JSON 객체 수를 세어 터미널에 출력한다. 🔴
        long actualCount = 0;
        try {
            actualCount = responseValidator.parseJsonArray(aiResponse).size();
        } catch (Exception ignored) {}
        if (actualCount < expectedCount) {
            System.out.println("[PrepAI] ⚠️ 문제 생성 완료 — 요청 " + expectedCount + "개 / 생성 " + actualCount + "개 / 미생성 " + (expectedCount - actualCount) + "개");
        } else {
            System.out.println("[PrepAI] 문제 생성 완료 — " + actualCount + "개");
        }

        // 🔴 생성 완료 후 시험지 직접 렌더링 대신 중간 검토 뷰로 이동한다. 🔴
        ModelAndView mav = new ModelAndView("reviewResult");
        mav.addObject("examResult", aiResponse);
        mav.addObject("examType", examType);
        return mav;
    }

    // 🔴 검토 화면에서 개별 문제 "재생성" 버튼을 누르면 호출된다. 🔴
    // 🔴 단일 문제용 프롬프트를 조립해 JSON 배열(1개짜리)로 반환한다. 🔴
    @PostMapping(value = "/api/regenerate-question", produces = "text/plain; charset=UTF-8")
    @ResponseBody
    public String regenerateQuestion(
            @RequestParam String questionType,
            @RequestParam String passageText,
            @RequestParam(required = false) String difficultyLevel) {

        String prompt          = promptBuilder.buildSingle(questionType, passageText, difficultyLevel);
        String initialResponse = geminiService.getGeminiResponse(prompt);
        return responseValidator.validateWithRetry(initialResponse, prompt, null, geminiService, 1);
    }

    // 🔴 프론트엔드 검토 화면에서 "DB에 저장" 버튼을 누르면 호출된다. 🔴
    // 🔴 사용자가 편집한 문제 목록을 JSON으로 받아 questions + answers 테이블에 저장한다. 🔴
    @PostMapping(value = "/api/save-questions", produces = "application/json")
    @ResponseBody
    public Map<String, Object> saveQuestions(@RequestBody Map<String, Object> body) {
        String examType = (String) body.getOrDefault("examType", "모의고사");

        @SuppressWarnings("unchecked")
        List<Map<String, String>> questions = (List<Map<String, String>>) body.get("questions");

        List<Long> ids = questionSaveService.saveAll(examType, questions);
        System.out.println("[PrepAI] 저장된 문제 ID: " + ids);
        return Map.of("savedCount", ids.size(), "ids", ids);
    }
}
