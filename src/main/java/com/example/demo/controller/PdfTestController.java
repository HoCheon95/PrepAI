package com.example.demo.controller;

import com.example.demo.geminiAI.GeminiService;
import com.example.demo.service.PdfPageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 🔴 PDF에서 지문을 제대로 추출하는지 확인하는 개발용 테스트 컨트롤러다. 🔴
// 🔴 PDFBox로 특정 페이지를 PNG 변환 후 Gemini 비전으로 추출 — 토큰 소모 최소화. 🔴
@RestController
@RequestMapping("/api/test")
public class PdfTestController {

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private PdfPageService pdfPageService;

    // 🔴 PDF 총 페이지 수를 반환한다 (파일 업로드 후 페이지 범위 확인용). 🔴
    @PostMapping(value = "/pdf-page-count", produces = "application/json; charset=UTF-8")
    public Map<String, Object> getPageCount(@RequestParam("passageImage") MultipartFile file) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            int count = pdfPageService.getPageCount(file.getBytes());
            result.put("totalPages", count);
        } catch (Exception e) {
            result.put("error", "페이지 수 확인 실패: " + e.getMessage());
        }
        return result;
    }

    // 🔴 지정한 시작 페이지(+다음 페이지)를 PNG로 변환해 Gemini 비전으로 지문을 추출한다. 🔴
    @PostMapping(value = "/pdf-extract", produces = "application/json; charset=UTF-8")
    public Map<String, Object> extractPassage(
            @RequestParam("passageImage") MultipartFile file,
            @RequestParam("questionNos") String questionNos,
            @RequestParam(value = "startPage", defaultValue = "1") int startPage) {

        Map<String, Object> result = new LinkedHashMap<>();

        if (file == null || file.isEmpty()) {
            result.put("error", "PDF 파일이 없습니다.");
            return result;
        }

        String[] nums = questionNos.split("[,\\s]+");
        String targetList = String.join(", ", nums);

        System.out.println("[PdfTest] 추출 요청 — 문제 번호: " + targetList + " | 시작 페이지: " + startPage);
        long start = System.currentTimeMillis();

        String raw;
        try {
            // 🔴 시작 페이지와 다음 페이지(2페이지 걸침 대응)를 PNG로 변환한다. 🔴
            byte[] pdfBytes = file.getBytes();
            int totalPages = pdfPageService.getPageCount(pdfBytes);
            int endPage = Math.min(startPage + 1, totalPages);

            List<byte[]> pageImages;
            if (startPage == endPage) {
                pageImages = pdfPageService.renderPages(pdfBytes, startPage);
            } else {
                pageImages = pdfPageService.renderPages(pdfBytes, startPage, endPage);
            }

            System.out.println("[PdfTest] 렌더링 완료 — " + pageImages.size() + "페이지 (총 " + totalPages + "p)");

            String prompt = buildExtractionPrompt(targetList);
            raw = geminiService.getGeminiResponse(prompt, pageImages);

        } catch (Exception e) {
            result.put("error", "PDF 처리 실패: " + e.getMessage());
            return result;
        }

        long elapsed = System.currentTimeMillis() - start;
        System.out.println("[PdfTest] 추출 완료 (" + elapsed + "ms)");
        System.out.println("[PdfTest] 응답:\n" + raw);

        result.put("requestedNos", targetList);
        result.put("startPage", startPage);
        result.put("elapsedMs", elapsed);
        result.put("rawResponse", raw);
        result.put("parsed", parseExtractionResult(raw, nums));
        return result;
    }

    // 🔴 전체 페이지를 스캔해 각 페이지에 어떤 문제 번호가 있는지 반환한다. 🔴
    @PostMapping(value = "/pdf-scan", produces = "application/json; charset=UTF-8")
    public Map<String, Object> scanPages(@RequestParam("passageImage") MultipartFile file) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (file == null || file.isEmpty()) {
            result.put("error", "PDF 파일이 없습니다.");
            return result;
        }
        try {
            byte[] pdfBytes = file.getBytes();
            int totalPages = pdfPageService.getPageCount(pdfBytes);
            Map<String, Object> pageMap = new LinkedHashMap<>();

            Pattern qPat = Pattern.compile("(?m)^\\s*(\\d{1,2})\\.");
            for (int p = 1; p <= totalPages; p++) {
                String text = pdfPageService.extractTextByColumns(pdfBytes, p, p);
                text = cleanText(text);
                Matcher m = qPat.matcher(text);
                List<Integer> nums = new ArrayList<>();
                while (m.find()) {
                    int n = Integer.parseInt(m.group(1));
                    if (n >= 18 && n <= 45 && !nums.contains(n)) nums.add(n);
                }
                pageMap.put("p" + p, nums);
            }
            result.put("totalPages", totalPages);
            result.put("pageMap", pageMap);
        } catch (Exception e) {
            result.put("error", "스캔 실패: " + e.getMessage());
        }
        return result;
    }

    // 🔴 PDFBox 컬럼 분리 추출 후 문제 번호 기준으로 파싱한다 — Gemini 호출 없이 API 절약. 🔴
    @PostMapping(value = "/pdf-text-extract", produces = "application/json; charset=UTF-8")
    public Map<String, Object> extractTextDirect(
            @RequestParam("passageImage") MultipartFile file,
            @RequestParam(value = "startPage", defaultValue = "1") int startPage) {

        Map<String, Object> result = new LinkedHashMap<>();
        if (file == null || file.isEmpty()) {
            result.put("error", "PDF 파일이 없습니다.");
            return result;
        }
        try {
            byte[] pdfBytes = file.getBytes();
            int totalPages = pdfPageService.getPageCount(pdfBytes);

            // 🔴 전체 PDF를 한 번에 파싱 — 섹션 헤더가 이전 페이지에 있어도 누락 없이 처리 🔴
            String text = pdfPageService.extractTextByColumns(pdfBytes, 1, totalPages);

            // 🔴 문제 번호 기준으로 파싱 🔴
            Map<String, String> parsed = parseByQuestionNumber(text);

            result.put("startPage", 1);
            result.put("endPage", totalPages);
            result.put("totalPages", totalPages);
            result.put("extractedText", text);
            result.put("parsed", parsed);
        } catch (Exception e) {
            result.put("error", "텍스트 추출 실패: " + e.getMessage());
        }
        return result;
    }

    // 🔴 페이지/시험지 푸터·헤더 잡음을 제거한다. 🔴
    private String cleanText(String text) {
        // "* 확인 사항 ◦ 답안지의..." 푸터 블록 제거
        text = text.replaceAll("\\*\\s*확인 사항[\\s\\S]*?했는지 확인하시오\\.?\\s*", "");
        // 컨텐츠 줄 끝에 붙은 페이지 번호 + 다음 줄 영역/영어 헤더 (예: "definitions. 6\n영역 고2\n")
        text = text.replaceAll(" \\d{1,2}\n(영역|영어)[^\n]*\n", "\n");
        // 페이지 번호 + "영역 고N" 또는 "영역 N" 헤더 (두 줄 형식: "8\n영역 고2")
        text = text.replaceAll("(?m)^\\d+\\s*\n영역[^\n]*\n", "");
        // 단독 "영역 N" 헤더 한 줄 형식 (예: "영역 7\n")
        text = text.replaceAll("(?m)^영역[^\n]*\n", "");
        // 한 줄에 번호와 과목명이 함께 있는 헤더 (예: "8 영어\n", "7 고2 영어\n")
        text = text.replaceAll("(?m)^\\d{1,2}\\s+(영어|영역)[^\n]*\n", "");
        // 2단 추출 시 좌우 페이지 번호가 합쳐진 행 제거 (예: "5 8\n", "6 7\n")
        text = text.replaceAll("(?m)^\\d{1,2}\\s+\\d{1,2}\\s*$\n", "");
        // "영어영역" 과목명 행 제거
        text = text.replaceAll("(?m)^영어[^\n]*\n", "");
        // "고2", "고 2 영어" 등 학년/과목 표시 행 제거 (공백 포함, 줄 끝 개행 없어도 처리)
        text = text.replaceAll("(?m)^고\\s*\\d[^\n]*\n?", "");
        // 홀로 남은 한 자리·두 자리 숫자 행 제거 (페이지 번호)
        text = text.replaceAll("(?m)^\\d{1,2}\\s*$\n", "");
        return text.trim();
    }

    // 🔴 PDF 빈칸(언더라인)을 ______ 으로 표시한다. 빈칸 채우기 문제 지원. 🔴
    // 패턴 1: 단어 사이 공백 2개 이상 → 인라인 빈칸  예) "of  in" → "of ______ in"
    // 패턴 2: 줄 첫 머리 마침표 → 앞줄 끝 빈칸    예) "\n. [3점]" → "\n______. [3점]"
    // 패턴 3: 소문자/괄호 뒤 공백+마침표 → 문장 끝 빈칸  예) "being ." → "being ______."
    // ※ * 로 시작하는 각주·어휘 주석 줄은 제외 (예: "* dissemination: 보급  ** non-virtuosic: ...")
    private String applyBlankMarker(String text) {
        String[] lines = text.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            if (!line.trim().startsWith("*")) {
                // 패턴 1: 단어 사이 연속 공백 2개 이상
                line = line.replaceAll("(?<=\\S) {2,}(?=\\S)", " ______ ");
                // 패턴 3: 영문 소문자/괄호/하이픈 뒤 공백+마침표
                line = line.replaceAll("([a-z)\\-]) \\.", "$1 ______.");
            }
            if (sb.length() > 0) sb.append("\n");
            sb.append(line);
        }
        // 패턴 2: 행 첫 머리 마침표 (앞줄 끝이 빈칸) — 각주와 무관하게 적용
        return sb.toString().replaceAll("(?m)^\\.", "______.");
    }

    // 🔴 섹션 헤더([N ~ M])를 기준으로 지문+문제를 묶어 문제 번호별 맵을 반환한다. 🔴
    private Map<String, String> parseByQuestionNumber(String text) {
        Map<String, String> map = new LinkedHashMap<>();

        text = cleanText(text);

        // 섹션 헤더: "[41 ~ 42] 다음 글을 읽고..." 패턴 (앞 공백 허용)
        Pattern sectionPat = Pattern.compile("(?m)^\\s*\\[(\\d{1,2})\\s*~\\s*(\\d{1,2})\\]");
        Matcher sm = sectionPat.matcher(text);

        List<int[]> sections = new ArrayList<>();  // [firstQ, lastQ, textPos]
        while (sm.find()) {
            int firstQ = Integer.parseInt(sm.group(1));
            int lastQ  = Integer.parseInt(sm.group(2));
            if (firstQ >= 18 && lastQ <= 45) {
                sections.add(new int[]{firstQ, lastQ, sm.start()});
            }
        }

        // 🔴 섹션별 공유 지문을 번호 범위로 저장 — 독립 탐색 폴백에서 재활용한다 🔴
        Map<String, String> sectionPassageByQNum = new LinkedHashMap<>();

        for (int i = 0; i < sections.size(); i++) {
            int firstQ    = sections.get(i)[0];
            int lastQ     = sections.get(i)[1];
            int secStart  = sections.get(i)[2];
            int secEnd    = (i + 1 < sections.size()) ? sections.get(i + 1)[2] : text.length();
            // 🔴 줄 끝 문자 정규화 — (?m)^ 가 \r\n 환경에서도 올바르게 작동하게 한다 🔴
            String secText = text.substring(secStart, secEnd).trim()
                    .replace("\r\n", "\n").replace("\r", "\n");

            // 섹션 내 지문 = 첫 번째 문제 번호 등장 이전 텍스트
            Pattern firstQPat = Pattern.compile("(?m)^\\s*" + firstQ + "\\.");
            Matcher fqm = firstQPat.matcher(secText);
            String passage = fqm.find() ? secText.substring(0, fqm.start()).trim() : "";
            // "[N ~ M]" 대괄호 태그만 제거하고 안내 지문은 유지
            passage = passage.replaceAll("^\\s*\\[\\d{1,2}\\s*~\\s*\\d{1,2}\\]\\s*", "").trim();

            // 🔴 섹션 내 모든 문제 번호에 대해 공유 지문을 매핑해 둔다 🔴
            for (int q = firstQ; q <= lastQ; q++) {
                sectionPassageByQNum.put(String.valueOf(q), passage);
            }

            // 각 문제별로 지문 + 해당 문제 텍스트 조합
            for (int q = firstQ; q <= lastQ; q++) {
                Pattern qPat = Pattern.compile("(?m)^\\s*" + q + "\\.");
                Matcher qm   = qPat.matcher(secText);
                if (!qm.find()) continue;

                int qTextStart = qm.start();
                // 다음 문제 시작 위치 (없으면 섹션 끝)
                Pattern nextPat = Pattern.compile("(?m)^\\s*" + (q + 1) + "\\.");
                Matcher nm      = nextPat.matcher(secText);
                int qTextEnd    = nm.find(qTextStart) ? nm.start() : secText.length();

                String qText   = secText.substring(qTextStart, qTextEnd).trim();
                String fullText = passage.isEmpty() ? qText : passage + "\n\n" + qText;
                map.put(String.valueOf(q), applyBlankMarker(fullText));
            }
        }

        // 🔴 섹션 헤더 없는 독립 문제(예: 37, 40번)를 번호 기반으로 추가 파싱한다. 🔴
        // 섹션 파싱에서 누락된 번호만 추가 (이미 있는 번호는 덮어쓰지 않음)
        {
            Pattern boundary = Pattern.compile("(?m)^\\s*(\\d{1,2})\\.");
            Matcher m = boundary.matcher(text);
            List<int[]> positions = new ArrayList<>();
            while (m.find()) {
                int num = Integer.parseInt(m.group(1));
                if (num >= 18 && num <= 45) positions.add(new int[]{num, m.start()});
            }
            for (int i = 0; i < positions.size(); i++) {
                int qNum = positions.get(i)[0];
                if (map.containsKey(String.valueOf(qNum))) continue; // 섹션 파싱에서 이미 처리됨
                int qStart = positions.get(i)[1];
                int qEnd   = (i + 1 < positions.size()) ? positions.get(i + 1)[1] : text.length();
                String qText = text.substring(qStart, qEnd).trim();
                // 독립 문제 끝에 다음 섹션 헤더가 붙는 경우 제거
                // 예: Q37 끝에 "[38 ~ 39] 글의 흐름으로...\n적절한 곳을 고르시오." 잔재
                qText = qText.replaceAll("(?s)\\n\\[\\d{1,2}\\s*~\\s*\\d{1,2}\\][^.]*?\\.", "").trim();
                // 🔴 섹션에 속한 문제(예: 42~44)는 섹션 공유 지문을 앞에 붙인다 🔴
                String secPassage = sectionPassageByQNum.getOrDefault(String.valueOf(qNum), "");
                String fullText = secPassage.isEmpty() ? qText : secPassage + "\n\n" + qText;
                map.put(String.valueOf(qNum), applyBlankMarker(fullText));
            }
        }

        System.out.println("[PdfTest] 문제 파싱 완료 — " + map.size() + "개 추출");
        return map;
    }

    // 🔴 이미지로 전달된 시험지 페이지에서 지문만 추출하는 프롬프트를 만든다. 🔴
    private String buildExtractionPrompt(String targetList) {
        return "You are a text extraction tool for Korean high school English exams. Do NOT generate questions.\n\n"
             + "The attached image(s) are scanned pages from a Korean CSAT (수능) or mock exam.\n\n"
             + "Target question numbers: [" + targetList + "]\n\n"
             + "HINT: A real question ALWAYS starts with 'N.' (e.g., '43.'). Do NOT treat section headers like '[43~45]' as question numbers.\n\n"
             + "For each target question number, extract:\n"
             + "1. The full English reading passage — including ALL paragraph labels (A), (B), (C), (D) exactly as they appear\n"
             + "2. The Korean question text — the line starting with 'N.' (e.g., '43.주어진 글...')\n"
             + "3. The answer choices — ①②③④⑤ lines exactly as they appear\n\n"
             + "CRITICAL — Underlined text: If any word or phrase is underlined in the PDF, wrap it with <u> and </u> tags. Example: (a)<u>she</u> still seemed reluctant.\n\n"
             + "Skip ONLY: Korean section headers like '[43~45] 다음 글을 읽고...'.\n\n"
             + "Output format (strictly follow — one block per question):\n"
             + "[Q{number}]\n"
             + "PASSAGE:\n"
             + "(full English passage with paragraph labels preserved)\n"
             + "QUESTION: (Korean question text starting with 'N.' followed by answer choices ①②③④⑤)\n"
             + "[END_Q{number}]\n\n"
             + "If a question number is not visible in the provided pages, write:\n"
             + "[Q{number}]\n"
             + "NOT_FOUND\n"
             + "[END_Q{number}]\n\n"
             + "Begin extraction now:";
    }

    // 🔴 응답에서 [Q번호]...[END_Q번호] 블록을 파싱해 번호별 지문 맵을 반환한다. 🔴
    private Map<String, String> parseExtractionResult(String raw, String[] nums) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String num : nums) {
            num = num.trim();
            String startTag = "[Q" + num + "]";
            String endTag   = "[END_Q" + num + "]";
            int s = raw.indexOf(startTag);
            int e = raw.indexOf(endTag);
            if (s != -1 && e != -1 && e > s) {
                String block = raw.substring(s + startTag.length(), e).trim();
                if (block.equals("NOT_FOUND")) {
                    map.put(num, "❌ 해당 번호가 제공된 페이지에 없습니다.");
                } else {
                    // 🔴 PASSAGE: 먼저, QUESTION: 마지막 순서로 파싱 (원본 시험지 구조와 동일) 🔴
                    String questionLine = "";
                    String passageText  = block;
                    int pIdx = block.indexOf("PASSAGE:");
                    int qIdx = block.indexOf("QUESTION:");
                    if (pIdx != -1 && qIdx != -1 && qIdx > pIdx) {
                        passageText  = block.substring(pIdx + "PASSAGE:".length(), qIdx).trim();
                        questionLine = block.substring(qIdx + "QUESTION:".length()).trim();
                    }
                    String display = questionLine.isEmpty()
                            ? passageText
                            : passageText + "\n\n" + questionLine;
                    map.put(num, display);
                }
            } else {
                map.put(num, "⚠️ 파싱 실패 — 태그를 찾지 못했습니다. rawResponse를 확인하세요.");
            }
        }
        return map;
    }

    // 🔴 이미지 한 장으로 5개 유형 문제를 한 번에 생성한다. 🔴
    @PostMapping(value = "/image-question", produces = "application/json; charset=UTF-8")
    public Map<String, Object> generateImageQuestion(
            @RequestParam("image") MultipartFile image) {

        Map<String, Object> result = new LinkedHashMap<>();
        if (image == null || image.isEmpty()) {
            result.put("error", "이미지 파일이 없습니다.");
            return result;
        }

        long start = System.currentTimeMillis();
        System.out.println("[ImageQuestion] 이미지 수신: " + image.getOriginalFilename()
                + " (" + image.getSize() / 1024 + "KB)");

        String prompt = buildImageQuestionPrompt();
        String raw = geminiService.getGeminiResponse(prompt, image);

        long elapsed = System.currentTimeMillis() - start;
        System.out.println("[ImageQuestion] 완료 (" + elapsed + "ms)");

        result.put("elapsedMs", elapsed);
        result.put("rawResponse", raw);
        result.put("questions", parseAllImageQuestions(raw));
        return result;
    }

    // 🔴 이미지 한 장 → 5개 유형 문제 + 각 문제마다 새로운 차트 데이터 동시 생성 🔴
    // Q1: 도표 불일치  Q2: 도표 일치  Q3: 빈칸 추론  Q4: 제목 파악  Q5: 요약 완성
    // CHART_DATA_N: Chart.js가 그릴 새로운 차트 JSON (원본과 유사한 구조, 다른 데이터)
    private String buildImageQuestionPrompt() {
        return "STEP 1 — Analyze the original chart image carefully:\n"
             + "- Identify the chart TYPE (bar / grouped bar / line / pie / etc.)\n"
             + "- Identify the TOPIC (what is being measured)\n"
             + "- Identify the CATEGORIES (x-axis labels, legend items)\n"
             + "- Identify the UNIT (%, points, count, etc.) and VALUE RANGE\n"
             + "- Identify how many DATASETS exist (single or grouped bars)\n\n"

             + "STEP 2 — Generate 5 questions. For each question, create NEW chart data that:\n"
             + "  - Uses the SAME chart type and structure as the original\n"
             + "  - Uses a SIMILAR topic or a closely related real-world topic\n"
             + "  - Uses REALISTIC values (similar scale/range as original, plausible statistics)\n"
             + "  - Has DIFFERENT specific numbers from the original image\n\n"

             + "[STRICT OUTPUT RULES]\n"
             + "1. Output ONLY the tagged blocks below. NO markdown fences (no ```). NO extra commentary.\n"
             + "2. CHART_DATA_N must be a single-line JSON — absolutely no newlines inside the JSON.\n"
             + "3. All passage sentences must use exact values from CHART_DATA_N only.\n"
             + "4. For 도표 불일치/일치: write 5 complete declarative sentences (not bullet points).\n"
             + "5. Answer choices ①~⑤ must each be a full English sentence about the chart.\n\n"

             + "CHART_DATA JSON FORMAT (single line):\n"
             + "{\"type\":\"bar\",\"title\":\"...\",\"labels\":[\"A\",\"B\"],\"datasets\":[{\"label\":\"Series1\",\"data\":[30,45]}],\"yAxisLabel\":\"(%)\",\"yMax\":100}\n"
             + "Supported types: bar | horizontalBar | line | pie | doughnut\n"
             + "For grouped bars: datasets array with multiple objects, each with label+data.\n\n"

             + "=== QUESTION 1 (도표 불일치) ===\n"
             + "[[CHART_DATA_1]]\n"
             + "New chart JSON — same type as original, different topic & realistic values.\n"
             + "[[INSTRUCTION_1]]\n"
             + "다음 도표의 내용과 일치하지 않는 것은?\n"
             + "[[PASSAGE_1]]\n"
             + "The graph above shows [topic of CHART_DATA_1 with year/source if realistic].\n"
             + "① [true statement — exact value from chart]\n"
             + "② [true statement — exact value from chart]\n"
             + "③ [true statement — exact value from chart]\n"
             + "④ [FALSE — one number is subtly wrong, e.g. 34% vs actual 43%]\n"
             + "⑤ [true statement — exact value from chart]\n"
             + "[[ANSWER_1]]\n"
             + "4\n"
             + "[[EXPLANATION_1]]\n"
             + "④번: 도표에서 실제 값은 [correct value]인데 지문은 [wrong value]로 서술했으므로 불일치.\n\n"

             + "=== QUESTION 2 (도표 일치) ===\n"
             + "[[CHART_DATA_2]]\n"
             + "New chart JSON — different topic from Q1, same chart type family.\n"
             + "[[INSTRUCTION_2]]\n"
             + "다음 도표의 내용과 일치하는 것은?\n"
             + "[[PASSAGE_2]]\n"
             + "The graph above shows [topic of CHART_DATA_2].\n"
             + "① [FALSE — wrong value or wrong comparison]\n"
             + "② [FALSE — wrong ranking]\n"
             + "③ [TRUE — exactly matches chart data]\n"
             + "④ [FALSE — wrong trend or wrong percentage point difference]\n"
             + "⑤ [FALSE — wrong number]\n"
             + "[[ANSWER_2]]\n"
             + "3\n"
             + "[[EXPLANATION_2]]\n"
             + "③번: 도표의 [value]와 정확히 일치. 나머지는 각각 [brief reason each is wrong].\n\n"

             + "=== QUESTION 3 (빈칸 추론) ===\n"
             + "[[CHART_DATA_3]]\n"
             + "New line or bar chart JSON showing a clear trend (increase/decrease/comparison).\n"
             + "[[INSTRUCTION_3]]\n"
             + "다음 빈칸에 들어갈 말로 가장 적절한 것을 고르시오.\n"
             + "[[PASSAGE_3]]\n"
             + "[3 sentences describing the CHART_DATA_3 trend. The KEY descriptive word/phrase is replaced with [ ________ ] in the final sentence.]\n"
             + "[[CHOICES_3]]\n"
             + "① [plausible but wrong word]\n"
             + "② [plausible but wrong word]\n"
             + "③ [correct word — matches the trend]\n"
             + "④ [plausible but wrong word]\n"
             + "⑤ [plausible but wrong word]\n"
             + "[[ANSWER_3]]\n"
             + "3\n"
             + "[[EXPLANATION_3]]\n"
             + "[Korean: 차트에서 [trend description]이므로 빈칸에는 '③ [word]'가 가장 적절하다.]\n\n"

             + "=== QUESTION 4 (제목 파악) ===\n"
             + "[[CHART_DATA_4]]\n"
             + "New chart JSON — can be pie or doughnut to add variety. Realistic composition data.\n"
             + "[[INSTRUCTION_4]]\n"
             + "다음 그래프의 제목으로 가장 적절한 것은?\n"
             + "[[PASSAGE_4]]\n"
             + "[2~3 sentences summarizing the key insight of CHART_DATA_4.]\n"
             + "[[CHOICES_4]]\n"
             + "① [English title — too narrow]\n"
             + "② [English title — correct and most fitting]\n"
             + "③ [English title — too broad]\n"
             + "④ [English title — slightly off topic]\n"
             + "⑤ [English title — wrong aspect emphasized]\n"
             + "[[ANSWER_4]]\n"
             + "2\n"
             + "[[EXPLANATION_4]]\n"
             + "[Korean: 그래프의 핵심 내용은 [key message]이므로 ②가 가장 적절한 제목이다.]\n\n"

             + "=== QUESTION 5 (요약 완성) ===\n"
             + "[[CHART_DATA_5]]\n"
             + "New grouped bar chart JSON with 2 datasets and 3~4 labels. Realistic survey/statistics data.\n"
             + "[[INSTRUCTION_5]]\n"
             + "다음 그래프의 내용을 한 문장으로 요약하고자 한다. 빈칸 (A), (B)에 들어갈 말로 가장 적절한 것은?\n"
             + "[[PASSAGE_5]]\n"
             + "[3~4 sentences describing CHART_DATA_5 data in detail.]\n"
             + "↓\n"
             + "[Summary sentence with (A) ________ and (B) ________ blanks, referring to key concepts from the chart.]\n"
             + "[[CHOICES_5]]\n"
             + "① [A option] — [B option]\n"
             + "② [A option] — [B option]\n"
             + "③ [correct A] — [correct B]\n"
             + "④ [A option] — [B option]\n"
             + "⑤ [A option] — [B option]\n"
             + "[[ANSWER_5]]\n"
             + "3\n"
             + "[[EXPLANATION_5]]\n"
             + "[Korean: (A)는 [reason], (B)는 [reason]이므로 ③이 정답이다.]\n";
    }

    // 🔴 Gemini 응답에서 CHART_DATA + Q1~Q5 필드를 파싱해 리스트로 반환한다. 🔴
    private List<Map<String, String>> parseAllImageQuestions(String raw) {
        List<Map<String, String>> questions = new ArrayList<>();
        if (raw == null || raw.isEmpty()) return questions;

        String[] fields = {"CHART_DATA", "INSTRUCTION", "PASSAGE", "CHOICES", "ANSWER", "EXPLANATION"};
        for (int n = 1; n <= 5; n++) {
            Map<String, String> q = new LinkedHashMap<>();
            for (int i = 0; i < fields.length; i++) {
                String open = "[[" + fields[i] + "_" + n + "]]";
                String close = null;
                if (i + 1 < fields.length) {
                    close = "[[" + fields[i + 1] + "_" + n + "]]";
                } else if (n + 1 <= 5) {
                    close = "[[CHART_DATA_" + (n + 1) + "]]";
                }
                int s = raw.indexOf(open);
                if (s < 0) continue;
                s += open.length();
                int e = (close != null) ? raw.indexOf(close, s) : raw.length();
                if (e < 0) e = raw.length();
                String value = raw.substring(s, e).trim();
                if (!value.isEmpty()) q.put(fields[i].toLowerCase().replace("_", ""), value);
            }
            if (!q.isEmpty()) questions.add(q);
        }
        return questions;
    }

}
