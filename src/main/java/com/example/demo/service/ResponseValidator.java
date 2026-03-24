package com.example.demo.service;

import com.example.demo.geminiAI.GeminiService;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

// 🔴 Gemini 응답의 형식을 검증하고, 오류 시 자동으로 재시도하는 책임을 담당한다. 🔴
@Component
public class ResponseValidator {

    private static final int MAX_RETRY = 3;

    // 🔴 응답을 검증하고, 형식이 잘못되면 최대 3회까지 재시도한다. 🔴
    public String validateWithRetry(String initialResponse, String prompt,
                                    MultipartFile file, GeminiService geminiService) {
        String response = initialResponse;

        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            if (isValid(response)) {
                if (attempt > 1) {
                    System.out.println("[ResponseValidator] " + attempt + "회 시도에서 정상 응답 수신");
                }
                return response;
            }

            System.out.println("[ResponseValidator] 응답 형식 오류 감지 (" + attempt + "/" + MAX_RETRY + "회). 재시도...");
            System.out.println("[ResponseValidator] 오류 응답 일부: " + response.substring(0, Math.min(200, response.length())));

            if (attempt < MAX_RETRY) {
                response = geminiService.getGeminiResponse(prompt, file);
            }
        }

        // 🔴 3회 모두 실패하면 예외를 던져 에러 페이지로 이동시킨다. 🔴
        throw new RuntimeException("AI 응답 형식 오류: " + MAX_RETRY + "회 재시도 모두 실패. 문제 수를 줄이거나 다시 시도해 주세요.");
    }

    // 🔴 응답에 필수 태그가 모두 존재하는지 확인한다. (gemini-prompt-rules.skill.md 태그 시스템 기준) 🔴
    public boolean isValid(String response) {
        if (response == null || response.isBlank()) return false;

        // 🔴 오류 응답 문자열이 반환된 경우 (GeminiService catch 블록) 🔴
        if (response.startsWith("최신 SDK 호출 중 오류 발생")) return false;

        return response.contains("[[QUESTION]]")
                && response.contains("[[OPTIONS]]")
                && response.contains("[[ANSWER]]")
                && response.contains("---SEP---");
    }
}
