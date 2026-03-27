package com.example.demo.service;

import com.example.demo.geminiAI.GeminiService;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

// 🔴 Gemini 응답의 형식과 문제 수를 검증하고, 오류 시 자동으로 재시도하는 책임을 담당한다. 🔴
@Component
public class ResponseValidator {

    private static final int MAX_RETRY = 3;

    // 🔴 응답을 검증하고, 실패하면 최대 3회 재시도한다. 🔴
    // 🔴 형식 오류는 예외를 던진다. 문제 수 부족은 최선의 결과를 반환한다. 🔴
    public String validateWithRetry(String initialResponse, String prompt,
                                    MultipartFile file, GeminiService geminiService,
                                    int expectedCount) {
        String response      = initialResponse;
        String bestResponse  = initialResponse;
        int    bestCount     = countCompleteBlocks(initialResponse);

        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {

            if (!isStructurallyValid(response)) {
                System.out.println("[PrepAI] ⚠️ 응답 형식 오류 (" + attempt + "/" + MAX_RETRY + "회) — 재시도");
                if (attempt < MAX_RETRY) {
                    response = geminiService.getGeminiResponse(prompt, file);
                }
                continue;
            }

            int actual = countCompleteBlocks(response);

            if (actual > bestCount) {
                bestCount    = actual;
                bestResponse = response;
            }

            if (expectedCount > 0 && actual < expectedCount) {
                // 🔴 완전한 문제 수가 요청 수보다 적으면 재시도한다. 🔴
                System.out.println("[PrepAI] ⚠️ 문제 수 부족 (" + attempt + "/" + MAX_RETRY + "회)"
                        + " — 요청 " + expectedCount + "개 / 생성 " + actual + "개 → 재시도");
                if (attempt < MAX_RETRY) {
                    response = geminiService.getGeminiResponse(prompt, file);
                }
                continue;
            }

            // 🔴 형식 정상 + 문제 수 충족 🔴
            if (attempt > 1) {
                System.out.println("[PrepAI] " + attempt + "회 시도 후 정상 완료 — " + actual + "개 생성");
            }
            return response;
        }

        // 🔴 3회 모두 재시도 후에도 최선의 결과를 반환한다. 형식 자체가 깨진 경우만 예외를 던진다. 🔴
        if (isStructurallyValid(bestResponse) && bestCount > 0) {
            int missing = Math.max(0, expectedCount - bestCount);
            if (missing > 0) {
                System.out.println("[PrepAI] ⚠️ 최종 결과 — 요청 " + expectedCount + "개 / 생성 " + bestCount + "개 / 미생성 " + missing + "개");
            }
            return bestResponse;
        }

        throw new RuntimeException("AI 응답 형식 오류: " + MAX_RETRY + "회 재시도 모두 실패. 문제 수를 줄이거나 다시 시도해 주세요.");
    }

    // 🔴 하위 호환용 — expectedCount 없이 호출할 때 사용한다. 🔴
    public String validateWithRetry(String initialResponse, String prompt,
                                    MultipartFile file, GeminiService geminiService) {
        return validateWithRetry(initialResponse, prompt, file, geminiService, 0);
    }

    // 🔴 응답의 구조적 형식(필수 태그 존재 여부 + 각 블록 완전성)을 확인한다. 🔴
    private boolean isStructurallyValid(String response) {
        if (response == null || response.isBlank()) return false;
        if (response.startsWith("최신 SDK 호출 중 오류 발생")) return false;
        if (!response.contains("[[QUESTION]]")
                || !response.contains("[[OPTIONS]]")
                || !response.contains("[[ANSWER]]")
                || !response.contains("---SEP---")) return false;

        // 🔴 각 블록이 필수 태그를 모두 갖추었는지 개별 검증한다. 🔴
        for (String block : response.split("---SEP---")) {
            String t = block.trim();
            if (t.isEmpty()) continue;
            if (!t.contains("[[QUESTION]]") || !t.contains("[[OPTIONS]]") || !t.contains("[[ANSWER]]")) {
                System.out.println("[PrepAI] 불완전한 블록 감지 (앞 50자): "
                        + t.substring(0, Math.min(50, t.length())));
                return false;
            }
        }
        return true;
    }

    // 🔴 필수 태그를 모두 포함한 완전한 블록의 수를 반환한다. 🔴
    private int countCompleteBlocks(String response) {
        if (response == null || response.isBlank()) return 0;
        int count = 0;
        for (String block : response.split("---SEP---")) {
            String t = block.trim();
            if (!t.isEmpty()
                    && t.contains("[[QUESTION]]")
                    && t.contains("[[OPTIONS]]")
                    && t.contains("[[ANSWER]]")) {
                count++;
            }
        }
        return count;
    }

    // 🔴 외부에서 단순 유효성 확인이 필요할 때 사용하는 public 메서드다. 🔴
    public boolean isValid(String response) {
        return isStructurallyValid(response);
    }
}
