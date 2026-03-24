package com.example.demo.controller;

import com.example.demo.geminiAI.GeminiService;
import com.example.demo.service.PromptBuilder;
import com.example.demo.service.ResponseValidator;
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

    // 🔴 프롬프트 조립은 PromptBuilder에, 검증/재시도는 ResponseValidator에 위임한다. 🔴
    @Autowired
    private PromptBuilder promptBuilder;

    @Autowired
    private ResponseValidator responseValidator;

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
            @RequestParam(value = "passageImage", required = false) MultipartFile passageImage,
            @RequestParam Map<String, String> allParams) {

        boolean hasFile = passageImage != null && !passageImage.isEmpty();
        boolean hasText = passageText != null && !passageText.trim().isEmpty();

        // 🔴 프롬프트 조립을 PromptBuilder에 위임한다. 🔴
        String prompt = promptBuilder.build(
                examType, passageText, questionNos, questionTypes,
                difficultyLevel, modifications, hasFile, hasText, allParams);

        // 🔴 Gemini 호출 후 ResponseValidator로 형식 검증 + 최대 3회 재시도한다. 🔴
        String initialResponse = geminiService.getGeminiResponse(prompt, passageImage);
        String aiResponse = responseValidator.validateWithRetry(initialResponse, prompt, passageImage, geminiService);

        System.out.println("================== [AI 응답 결과 확인] ==================");
        System.out.println(aiResponse);
        System.out.println("=========================================================");

        ModelAndView mav = new ModelAndView("result");
        mav.addObject("examResult", aiResponse);
        return mav;
    }
}
