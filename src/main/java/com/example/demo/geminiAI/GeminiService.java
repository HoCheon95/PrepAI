package com.example.demo.geminiAI;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import com.google.genai.types.Blob; // 🔴 파일 처리를 위한 Blob 클래스를 추가한다. 🔴
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile; // 🔴 첨부파일 처리를 위한 클래스를 추가한다. 🔴

import java.util.ArrayList; // 🔴 리스트 생성을 위한 유틸 클래스를 추가한다. 🔴
import java.util.Base64;    // 🔴 파일 데이터를 문자열로 변환할 Base64 클래스를 추가한다. 🔴
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    // 기존 텍스트 전용 메서드 호환성을 위해 남겨둡니다.
    public String getGeminiResponse(String userPrompt) {
        return getGeminiResponse(userPrompt, (MultipartFile) null);
    }

    // 🔴 PNG 바이트 배열 목록(여러 페이지 이미지)과 텍스트를 함께 전송한다. 🔴
    public String getGeminiResponse(String userPrompt, List<byte[]> imageBytesList) {
        Client client = Client.builder().apiKey(apiKey).build();
        try {
            Content systemInstruction = Content.builder()
                    .parts(Collections.singletonList(Part.builder()
                            .text("당신은 한국 수능 영어 시험 PDF에서 영어 지문을 추출하는 도구입니다. 지시에 따라 정확히 추출하십시오.").build()))
                    .build();

            GenerateContentConfig config = GenerateContentConfig.builder()
                    .temperature(0.0f)
                    .maxOutputTokens(8192)
                    .systemInstruction(systemInstruction)
                    .build();

            List<Part> parts = new ArrayList<>();
            parts.add(Part.builder().text(userPrompt).build());
            if (imageBytesList != null) {
                for (byte[] imgBytes : imageBytesList) {
                    parts.add(Part.builder()
                            .inlineData(Blob.builder()
                                    .data(Base64.getEncoder().encodeToString(imgBytes))
                                    .mimeType("image/png")
                                    .build())
                            .build());
                }
            }

            GenerateContentResponse response = client.models.generateContent(
                    "gemini-1.5-flash",
                    Content.builder().parts(parts).build(),
                    config);
            return response.text();
        } catch (Exception e) {
            return "오류 발생: " + e.getMessage();
        }
    }

    // 🔴 텍스트와 파일을 동시에 받아 처리할 수 있는 오버로딩 메서드를 구성한다. 🔴
    public String getGeminiResponse(String userPrompt, MultipartFile file) {
        return getGeminiResponse(userPrompt, file, 16384);
    }

    // 🔴 maxOutputTokens를 요청 문제 수 기반으로 동적 지정할 수 있는 메서드다. 🔴
    public String getGeminiResponse(String userPrompt, MultipartFile file, int maxOutputTokens) {
        
        Client client = Client.builder()
                .apiKey(apiKey)
                .build();

        try {
            Content systemInstructionContent = Content.builder()
                    .parts(Collections.singletonList(Part.builder().text("당신은 주어진 양식에 맞춰 영어 문제(객관식·주관식 모두 포함)를 생성하는 데이터 변환 기계다. 절대 사용자와 대화하거나 설명하지 마라.").build()))
                    .build();

            GenerateContentConfig config = GenerateContentConfig.builder()
                    .temperature(0.0f)
                    .maxOutputTokens(maxOutputTokens)  // 🔴 동적으로 지정 — 문제 수에 비례해 호출 속도가 향상된다. 🔴
                    .systemInstruction(systemInstructionContent)
                    .build();

            // 🔴 AI에게 보낼 여러 조각(텍스트 + 파일)을 담을 리스트를 생성한다. 🔴
            List<Part> parts = new ArrayList<>();
            parts.add(Part.builder().text(userPrompt).build());

            // 🔴 사용자가 PDF나 이미지를 업로드했다면, AI가 읽을 수 있도록 변환해서 추가한다. 🔴
            if (file != null && !file.isEmpty()) {
                parts.add(Part.builder()
                        .inlineData(Blob.builder()
                                // 🔴 byte 배열을 바로 넣지 않고, SDK가 요구하는 Base64 인코딩 문자열(String)로 변환하여 주입한다. 🔴
                                .data(Base64.getEncoder().encodeToString(file.getBytes()))
                                .mimeType(file.getContentType()) // 파일 형식(PDF 등)을 알려준다.
                                .build())
                        .build());
            }

            // 🔴 텍스트와 파일이 하나로 결합된 Content 객체를 조립한다. 🔴
            Content userContent = Content.builder().parts(parts).build();

            // 🔴 조립된 userContent를 전송한다. 타임아웃 120초 설정으로 무한 블로킹을 방지한다. 🔴
            final Content finalContent = userContent;
            GenerateContentResponse response = CompletableFuture
                    .supplyAsync(() -> {
                        try {
                            return client.models.generateContent("gemini-1.5-flash", finalContent, config);
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                    })
                    .get(120, TimeUnit.SECONDS);

            return postProcess(response.text());

        } catch (TimeoutException e) {
            return "최신 SDK 호출 중 오류 발생: 응답 시간 초과 (120초). 문제 수를 줄이거나 다시 시도해 주세요.";
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            return "최신 SDK 호출 중 오류 발생: " + cause.getMessage();
        }
    }

    // 🔴 Gemini 응답의 공통 인코딩 오류를 후처리로 자동 수정한다. 🔴
    // 🔴 어포스트로피(')가 ?로 깨지는 현상: don?t → don't, individuals? behavior → individuals' behavior 🔴
    private String postProcess(String text) {
        if (text == null) return null;
        return text
            // 축약형: don?t, it?s, won?t 등 — 글자 사이에 낀 ?를 '로 변환
            .replaceAll("([a-zA-Z])\\?([a-zA-Z])", "$1'$2")
            // 소유격: individuals? behavior 등 — 글자 뒤 ? + 공백 + 소문자
            .replaceAll("([a-zA-Z])\\?(\\s+[a-z])", "$1'$2");
    }
}