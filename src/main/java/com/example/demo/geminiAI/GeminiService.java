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

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    // 기존 텍스트 전용 메서드 호환성을 위해 남겨둡니다.
    public String getGeminiResponse(String userPrompt) {
        return getGeminiResponse(userPrompt, null);
    }

    // 🔴 텍스트와 파일을 동시에 받아 처리할 수 있는 오버로딩 메서드를 구성한다. 🔴
    public String getGeminiResponse(String userPrompt, MultipartFile file) {
        
        Client client = Client.builder()
                .apiKey(apiKey)
                .build();

        try {
            Content systemInstructionContent = Content.builder()
                    .parts(Collections.singletonList(Part.builder().text("당신은 주어진 양식에 맞춰 영어 객관식 문제만 생성하는 데이터 변환 기계다. 절대 사용자와 대화하거나 설명하지 마라.").build()))
                    .build();

            GenerateContentConfig config = GenerateContentConfig.builder()
                    .temperature(0.0f)
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

            // 🔴 조립된 userContent를 전송한다. 🔴
            GenerateContentResponse response = client.models.generateContent(
                    "gemini-2.5-flash",
                    userContent,
                    config); 

            return response.text();

        } catch (Exception e) {
            return "최신 SDK 호출 중 오류 발생: " + e.getMessage();
        }
    }
}