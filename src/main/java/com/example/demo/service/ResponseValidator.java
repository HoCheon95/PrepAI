package com.example.demo.service;

import com.example.demo.geminiAI.GeminiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

// 🔴 Gemini JSON 배열 응답의 형식과 문제 수를 검증하고, 오류 시 자동으로 재시도하는 책임을 담당한다. 🔴
@Component
public class ResponseValidator {

    private static final int MAX_RETRY = 3;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 🔴 응답을 검증하고, 실패하면 최대 3회 재시도한다. 🔴
    // 🔴 형식 오류는 예외를 던진다. 문제 수 부족은 최선의 결과를 반환한다. 🔴
    public String validateWithRetry(String initialResponse, String prompt,
                                    MultipartFile file, GeminiService geminiService,
                                    int expectedCount) {
        String response     = initialResponse;
        String bestResponse = initialResponse;
        int    bestCount    = countCompleteBlocks(initialResponse);

        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {

            if (!isStructurallyValid(response)) {
                System.out.println("[PrepAI] ⚠️ 응답 형식 오류 (" + attempt + "/" + MAX_RETRY + "회) — 재시도");
                if (attempt < MAX_RETRY) {
                    response = geminiService.getGeminiResponse(prompt, file);
                }
                continue;
            }
            // 🔴 잘린 JSON 복구 성공 시 복구된 응답으로 교체한다. 🔴
            if (repairedResponse != null) {
                response = repairedResponse;
                repairedResponse = null;
            }

            int actual = countCompleteBlocks(response);

            // 🔴 컨텐츠 검증 전에 bestResponse를 먼저 업데이트해야 최선의 결과가 보존된다. 🔴
            if (actual > bestCount) {
                bestCount    = actual;
                bestResponse = response;
            }

            if (!isContentValid(response)) {
                System.out.println("[PrepAI] ⚠️ 컨텐츠 누락 (" + attempt + "/" + MAX_RETRY + "회) — 재시도");
                if (attempt < MAX_RETRY) {
                    response = geminiService.getGeminiResponse(prompt, file);
                }
                continue;
            }

            if (expectedCount > 0 && actual < expectedCount) {
                System.out.println("[PrepAI] ⚠️ 문제 수 부족 (" + attempt + "/" + MAX_RETRY + "회)"
                        + " — 요청 " + expectedCount + "개 / 생성 " + actual + "개 → 재시도");
                if (attempt < MAX_RETRY) {
                    response = geminiService.getGeminiResponse(prompt, file);
                }
                continue;
            }

            // 🔴 형식 정상 + 컨텐츠 정상 + 문제 수 충족 🔴
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

    // 🔴 JSON 배열 형식인지, 각 객체에 필수 필드가 있는지 검증한다. 🔴
    // 🔴 잘린 JSON(end-of-input)은 자동 복구를 시도하고, 복구된 응답으로 교체한다. 🔴
    private String repairedResponse = null;  // 복구된 응답을 validateWithRetry에 전달하기 위한 필드

    private boolean isStructurallyValid(String response) {
        repairedResponse = null;
        if (response == null || response.isBlank()) return false;
        if (response.startsWith("최신 SDK 호출 중 오류 발생")) return false;
        try {
            List<Map<String, Object>> questions = parseJsonArray(response);
            if (questions == null || questions.isEmpty()) return false;
            for (Map<String, Object> q : questions) {
                if (!q.containsKey("questionText") || !q.containsKey("options") || !q.containsKey("answer")) {
                    System.out.println("[PrepAI] 불완전한 JSON 객체 감지 (fields): " + q.keySet());
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            // 🔴 "end-of-input" 오류 = 응답이 중간에 잘린 것 → 완성된 객체만 살려낸다. 🔴
            if (msg.contains("end-of-input") || msg.contains("Unexpected end")) {
                System.out.println("[PrepAI] ⚠️ 응답 잘림 감지 — 완성된 객체 복구 시도");
                String recovered = tryRepairTruncatedJson(response);
                if (recovered != null) {
                    repairedResponse = recovered;
                    System.out.println("[PrepAI] 복구 성공 — " + countCompleteBlocks(recovered) + "개 문제 살림");
                    return true;
                }
                System.out.println("[PrepAI] 복구 실패 — 문제 수를 줄여 재시도하세요");
            } else {
                System.out.println("[PrepAI] JSON 파싱 실패: " + msg.substring(0, Math.min(100, msg.length())));
            }
            return false;
        }
    }

    // 🔴 잘린 JSON 배열에서 완성된 객체만 추출해 유효한 JSON 배열로 재조립한다. 🔴
    private String tryRepairTruncatedJson(String response) {
        try {
            String clean = response.trim();
            if (clean.startsWith("```json")) clean = clean.substring(7);
            else if (clean.startsWith("```")) clean = clean.substring(3);
            clean = clean.trim();

            // 마지막으로 완전히 닫힌 } 위치를 찾아 그 뒤에 ] 를 붙인다
            int lastClose = clean.lastIndexOf('}');
            if (lastClose == -1) return null;
            String truncated = clean.substring(0, lastClose + 1);
            // 배열 시작 [ 가 없으면 붙인다
            if (!truncated.trim().startsWith("[")) truncated = "[" + truncated;
            String candidate = truncated + "]";
            // 파싱 가능한지 확인
            List<Map<String, Object>> list = objectMapper.readValue(candidate,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
            if (list.isEmpty()) return null;
            return candidate;
        } catch (Exception e) {
            return null;
        }
    }

    // 🔴 문제 유형별 필수 컨텐츠(요약문 본체, 어법 밑줄 등)가 올바르게 생성됐는지 검증한다. 🔴
    boolean isContentValid(String response) {
        if (response == null || response.isBlank()) return true;
        try {
            List<Map<String, Object>> questions = parseJsonArray(response);
            for (Map<String, Object> q : questions) {
                String type    = String.valueOf(q.getOrDefault("questionType", "")).toLowerCase();
                String passage = String.valueOf(q.getOrDefault("passage", ""));
                String qText   = String.valueOf(q.getOrDefault("questionText", "")).toLowerCase();

                // 요약문: passage 안에 (A) [ 빈칸 문장이 반드시 있어야 한다.
                if (type.contains("요약") || qText.contains("요약")) {
                    boolean hasSummaryBlank = passage.contains("(A) [") || passage.contains("(A)[")
                            || passage.contains("(A) _") || passage.contains("(A)_");
                    if (!hasSummaryBlank) {
                        System.out.println("[PrepAI] ⚠️ 요약문 본체 누락 감지 — 재시도");
                        return false;
                    }
                }

                // 어법문제: <u>(1)~<u>(5) 밑줄이 모두 있어야 한다.
                if (type.contains("어법") || qText.contains("어법")) {
                    if (!hasAllFiveUnderlines(passage)) {
                        System.out.println("[PrepAI] ⚠️ 어법 밑줄 5개 미완성 — 재시도");
                        return false;
                    }
                }

                // 어휘문제: <u>(1)~<u>(5) 밑줄이 모두 있어야 한다.
                if (type.contains("어휘") || type.contains("낱말") || qText.contains("어휘") || qText.contains("낱말")) {
                    if (!hasAllFiveUnderlines(passage)) {
                        System.out.println("[PrepAI] ⚠️ 어휘 밑줄 5개 미완성 — 재시도");
                        return false;
                    }
                }

                // 문장삽입: passage 안에 ( 1 ) 형태의 삽입 위치가 있어야 한다.
                if (type.contains("삽입") || qText.contains("들어가기") || qText.contains("삽입")) {
                    if (!passage.contains("( 1 )") && !passage.contains("(1)")) {
                        System.out.println("[PrepAI] ⚠️ 문장삽입 위치 번호 누락 감지 — 재시도");
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            // JSON 파싱 실패는 isStructurallyValid에서 잡히므로 여기서는 통과
        }
        return true;
    }

    // 🔴 어법/어휘 문제에서 (1)~(5) 밑줄이 모두 존재하는지 확인한다. 🔴
    private boolean hasAllFiveUnderlines(String passage) {
        return passage.contains("<u>(1)") && passage.contains("<u>(2)")
            && passage.contains("<u>(3)") && passage.contains("<u>(4)")
            && passage.contains("<u>(5)");
    }

    // 🔴 필수 필드를 모두 갖춘 완전한 JSON 객체의 수를 반환한다. 🔴
    private int countCompleteBlocks(String response) {
        if (response == null || response.isBlank()) return 0;
        try {
            List<Map<String, Object>> questions = parseJsonArray(response);
            int count = 0;
            for (Map<String, Object> q : questions) {
                if (q.containsKey("questionText") && q.containsKey("options") && q.containsKey("answer")) {
                    count++;
                }
            }
            return count;
        } catch (Exception e) {
            return 0;
        }
    }

    // 🔴 Gemini가 ```json 마크다운으로 감쌀 수 있으므로 벗겨내고 파싱한다. 🔴
    public List<Map<String, Object>> parseJsonArray(String response) throws Exception {
        String clean = response.trim();
        if (clean.startsWith("```json")) clean = clean.substring(7);
        else if (clean.startsWith("```"))  clean = clean.substring(3);
        if (clean.endsWith("```"))         clean = clean.substring(0, clean.length() - 3);
        clean = clean.trim();
        return objectMapper.readValue(clean,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
    }

    // 🔴 외부에서 단순 유효성 확인이 필요할 때 사용하는 public 메서드다. 🔴
    public boolean isValid(String response) {
        return isStructurallyValid(response);
    }
}
