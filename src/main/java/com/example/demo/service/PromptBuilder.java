package com.example.demo.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// 🔴 JSON 스키마 + Few-Shot 프롬프트로 Gemini 출력 안정성을 극대화한다. 🔴
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
            Map<String, String> counts,
            boolean isMixed,
            boolean isSetMode) {

        int totalCount = countTotal(questionTypes, counts);
        StringBuilder prompt = new StringBuilder();
        appendAbsoluteRule(prompt);
        appendJsonSchema(prompt, questionTypes);
        appendExamConditions(prompt, difficultyLevel, modifications);
        appendPassageSource(prompt, examType, passageText, questionNos, hasFile, hasText, isSetMode, totalCount);
        appendQuestionTypes(prompt, questionTypes, counts, isMixed);
        return prompt.toString();
    }

    // 🔴 혼합 유형 랜덤 시험 생성용 프롬프트를 조립한다. orderedTypes는 이미 셔플된 1문제당 1항목 시퀀스다. 🔴
    public String buildMixed(
            String examType,
            String passageText,
            List<String> questionNos,
            List<String> orderedTypes,
            String difficultyLevel,
            List<String> modifications,
            boolean hasFile,
            boolean hasText) {

        StringBuilder prompt = new StringBuilder();
        appendAbsoluteRule(prompt);
        appendJsonSchema(prompt, orderedTypes);
        appendExamConditions(prompt, difficultyLevel, modifications);
        appendPassageSource(prompt, examType, passageText, questionNos, hasFile, hasText, false, 0);
        appendMixedQuestionTypes(prompt, orderedTypes);
        return prompt.toString();
    }

    // 🔴 단일 지문 세트 문제 생성용 프롬프트를 조립한다. setTypes는 세트 내 문제 유형의 순서 목록이다. 🔴
    public String buildSet(
            String examType,
            String passageText,
            List<String> questionNos,
            List<String> setTypes,
            String difficultyLevel,
            List<String> modifications,
            boolean hasFile,
            boolean hasText) {

        StringBuilder prompt = new StringBuilder();
        appendAbsoluteRule(prompt);
        appendJsonSchema(prompt, setTypes);
        appendExamConditions(prompt, difficultyLevel, modifications);
        appendPassageSource(prompt, examType, passageText, questionNos, hasFile, hasText, true, setTypes.size());
        appendSetQuestionTypes(prompt, setTypes);
        return prompt.toString();
    }

    // 🔴 단일 문제 재생성용 프롬프트를 조립한다. 🔴
    public String buildSingle(String questionType, String passageText, String difficultyLevel) {
        StringBuilder prompt = new StringBuilder();
        appendAbsoluteRule(prompt);
        appendJsonSchema(prompt, List.of(questionType));
        if (difficultyLevel != null) {
            prompt.append("[EXAM CONDITIONS]\n");
            appendDifficultyDefinition(prompt, difficultyLevel);
            prompt.append("\n");
        }
        prompt.append("[PASSAGE SOURCE]\n");
        prompt.append("TASK: Use the passage below to create ONE brand new question.\n");
        prompt.append("[TEXT]\n").append(passageText).append("\n\n");
        prompt.append("[QUESTION TYPE]\n- ").append(questionType).append(": 1개\n");
        appendTypeRule(prompt, questionType);
        return prompt.toString();
    }

    // ── 공통 규칙 ──────────────────────────────────────────────────────

    // 🔴 원본 문제 재사용 금지 — 모든 프롬프트 최상단에 삽입한다. 🔴
    private void appendAbsoluteRule(StringBuilder p) {
        p.append("[ABSOLUTE RULE — READ FIRST]\n");
        p.append("The input may contain an ORIGINAL exam question (Korean stem, underlined phrases, answer choices, etc.).\n");
        p.append("You MUST completely IGNORE the original question. ONLY extract the English passage text.\n");
        p.append("Create BRAND NEW questions from the passage. NEVER copy or reuse any expression from the original.\n");
        p.append("Use only standard ASCII apostrophe (') for possessives — NEVER curly quotes.\n\n");
    }

    // 🔴 JSON 출력 스키마 + Few-Shot 예시를 삽입한다. 🔴
    // 🔴 Few-Shot은 백 마디 지시보다 효과적이며 요약문/어법 누락 현상을 원천 차단한다. 🔴
    private void appendJsonSchema(StringBuilder p, List<String> questionTypes) {
        p.append("[STRICT OUTPUT CONTROL]\n");
        p.append("CRITICAL: Output ONLY the requested tags and content. DO NOT print your internal thought process, apologies, or comments like '[REWRITE]'.\n\n");

        p.append("[JSON HYGIENE RULES — STRICTLY ENFORCED]\n");
        p.append("1. CRITICAL: NEVER use actual newlines (Enter key / ASCII code 10) inside any JSON string value.\n");
        p.append("2. For line breaks inside 'passage' or 'explanation', use ONLY the two-character sequence: \\n (backslash + n).\n");
        p.append("3. Correct: \"explanation\": \"First line.\\nSecond line.\"  ← two chars, not a real newline.\n");
        p.append("4. Before finalizing, scan every string field for raw newlines or tab characters and escape them.\n");
        p.append("5. Process each question as a fully INDEPENDENT task. Reset context between questions to maintain logical consistency.\n\n");
        p.append("[CRITICAL FORMATTING REMINDERS]\n");
        p.append("- 요약문: passage MUST end with ↓↓↓ on its own line, followed by the summary with (A) [ ________ ] (B) [ ________ ].\n");
        p.append("- 어법/어휘: passage MUST use <u>(1)word</u> through <u>(5)word</u> — all 5 required, no exceptions.\n\n");

        p.append("[OUTPUT FORMAT — STRICT JSON ARRAY]\n");
        p.append("Return ONLY a valid JSON array. No markdown, no explanation, no text before or after.\n");
        p.append("Each question is a JSON object with these EXACT fields:\n\n");
        p.append("{\n");
        p.append("  \"questionType\": \"<한국어 유형명 e.g. 빈칸추론>\",\n");
        p.append("  \"questionText\": \"<반드시 한국어 지시문(발문)만 — 아래 규칙 엄수>\",\n");
        p.append("  \"passage\": \"<영어 지문 — 유형별 규칙에 맞게 포맷팅>\",\n");
        p.append("  \"options\": [\"(1) ...\", \"(2) ...\", \"(3) ...\", \"(4) ...\", \"(5) ...\"],\n");
        p.append("  \"answer\": <정답 번호 정수 1~5>,\n");
        p.append("  \"explanation\": \"<한국어 상세 해설 — 아래 [EXPLANATION RULES] 엄수>\"\n");
        p.append("}\n\n");
        p.append("[EXPLANATION RULES — APPLY TO EVERY QUESTION]\n");
        p.append("  1. State the logical reason why the correct answer is right (핵심 근거 문장 인용 포함).\n");
        p.append("  2. [Objective types only] Distractor Analysis: Pick the most tempting wrong option and explain in one sentence why it is incorrect.\n");
        p.append("  3. [서술형 only] State the core grammatical structure used in the model answer (e.g., Passive Voice, It-to 구문, 분사구문).\n\n");
        p.append("⛔ questionText CRITICAL RULES:\n");
        p.append("  - questionText = ONLY the Korean instruction (발문). e.g. '1. 다음 글의 빈칸에 들어갈 말로 가장 적절한 것은?'\n");
        p.append("  - NEVER put English passage sentences inside questionText. English content belongs ONLY in passage.\n");
        p.append("  - If you catch yourself writing an English sentence in questionText — STOP and move it to passage.\n\n");

        p.append("[FEW-SHOT EXAMPLES — YOUR OUTPUT MUST MATCH THIS STRUCTURE EXACTLY]\n");
        // Few-Shot 1: 빈칸추론
        p.append("Example 1 (빈칸추론):\n");
        p.append("{\n");
        p.append("  \"questionType\": \"빈칸추론\",\n");
        p.append("  \"questionText\": \"1. 다음 글의 빈칸에 들어갈 말로 가장 적절한 것은?\",\n");
        p.append("  \"passage\": \"Commitment is the glue holding together human social life. Commitments make individuals' behavior predictable, thereby [ ________ ] the planning of joint actions. Without commitments, institutions like money could not function.\",\n");
        p.append("  \"options\": [\"(1) hindering\", \"(2) enabling\", \"(3) complicating\", \"(4) preventing\", \"(5) ignoring\"],\n");
        p.append("  \"answer\": 2,\n");
        p.append("  \"explanation\": \"약속이 행동을 예측 가능하게 만들어 공동 행동 계획을 가능하게(enabling) 한다는 흐름이 자연스럽다. 나머지는 문맥상 반대 의미다.\"\n");
        p.append("}\n\n");

        // Few-Shot 2: 요약문 (요약문은 passage 필드 안에 ↓ + 빈칸 문장이 포함되어야 한다)
        if (questionTypes != null && questionTypes.stream().anyMatch(t -> t.contains("요약"))) {
            p.append("Example 2 (요약문 — passage 필드 끝에 ↓와 빈칸 문장 MANDATORY):\n");
            p.append("{\n");
            p.append("  \"questionType\": \"요약문\",\n");
            p.append("  \"questionText\": \"2. 다음 글의 내용을 한 문장으로 요약하고자 한다. 빈칸 (A), (B)에 들어갈 말로 가장 적절한 것은?\",\n");
            p.append("  \"passage\": \"Commitment is the glue holding together human social life. Commitments make individuals' behavior predictable, thereby facilitating the planning of joint actions. Social institutions depend on the credibility of commitments.\\n\\n↓\\nCommitment acts as the (A) [ ________ ] of society by ensuring (B) [ ________ ] behavior among individuals.\",\n");
            p.append("  \"options\": [\"(1) foundation - predictable\", \"(2) barrier - unpredictable\", \"(3) result - random\", \"(4) challenge - stable\", \"(5) symbol - creative\"],\n");
            p.append("  \"answer\": 1,\n");
            p.append("  \"explanation\": \"(A): 약속은 사회의 기반(foundation)이며, (B): 예측 가능한(predictable) 행동을 보장한다는 내용이 글과 일치한다.\"\n");
            p.append("}\n\n");
            p.append("⛔ 요약문 CRITICAL: passage 필드는 반드시 '\\n\\n↓\\n(A) [ ________ ] ... (B) [ ________ ]' 로 끝나야 한다.\n");
            p.append("  '↓' 기호는 절대 생략 불가. 반드시 단독 줄에 ↓ 하나만 출력하라. 없으면 응답이 거부된다.\n\n");
        }

        // Few-Shot 3: 어법문제
        if (questionTypes != null && questionTypes.stream().anyMatch(t -> t.contains("어법"))) {
            p.append("Example 3 (어법문제 — passage에 <u>(1)word</u> 형태 5개 MANDATORY):\n");
            p.append("{\n");
            p.append("  \"questionType\": \"어법문제\",\n");
            p.append("  \"questionText\": \"3. 다음 글의 밑줄 친 부분 중, 어법상 틀린 것은?\",\n");
            p.append("  \"passage\": \"Commitment is the glue <u>(1)holding</u> together human social life. Commitments make individuals' behavior <u>(2)predictably</u> in the face of fluctuations. Moreover, commitments make people <u>(3)willing</u> to perform actions. A taxi driver <u>(4)picks</u> up clients because they are committed to paying. Social institutions <u>(5)depend</u> on the credibility of commitments.\",\n");
            p.append("  \"options\": [\"(1) holding\", \"(2) predictably\", \"(3) willing\", \"(4) picks\", \"(5) depend\"],\n");
            p.append("  \"answer\": 2,\n");
            p.append("  \"explanation\": \"(2) predictably는 부사로 형용사 자리에 쓰였으므로 predictable이 되어야 한다.\"\n");
            p.append("}\n\n");
        }

        p.append("[PASSAGE FORMATTING RULES BY TYPE]\n");
        p.append("- 빈칸추론: insert [ ________ ] where the blank goes.\n");
        p.append("- 요약문: passage must end with \\n\\n↓\\n<summary with (A) [ ________ ] and (B) [ ________ ]>. '↓' is MANDATORY — never omit it.\n");
        p.append("- 어법문제: 5 underlined items as <u>(1)word</u> ... <u>(5)word</u>. ALL 5 required.\n");
        p.append("- 어휘문제: 5 underlined words as <u>(1)word</u> ... <u>(5)word</u>. ALL 5 required.\n");
        p.append("- 문장삽입: given sentence in questionText; numbered spots ( 1 ) through ( 5 ) in passage.\n");
        p.append("- 순서배열: starting sentence in questionText; ONLY (A)(B)(C) blocks in passage — no full passage repeat.\n");
        p.append("- 대명사지칭: 5 pronouns labeled <u>(a)pronoun</u>; exactly 4 refer to same person, 1 to different entity.\n\n");
        p.append("⛔ options CRITICAL RULE: options array MUST NEVER be empty or contain blank entries.\n");
        p.append("  Even for 문장삽입/무관한문장: options = [\"(1) 1\", \"(2) 2\", \"(3) 3\", \"(4) 4\", \"(5) 5\"].\n");
        p.append("  Input JSON may show choices as null — IGNORE that and always generate concrete option values.\n\n");
        p.append("CRITICAL: NEVER summarize, truncate, or shorten the passage. You MUST print the FULL, intact passage for EVERY question type. Even for Grammar, Vocabulary, or Pronoun questions, the entire original text must be present.\n\n");
    }

    // 🔴 난이도와 지문 변형 여부를 프롬프트에 추가한다. 🔴
    private void appendExamConditions(StringBuilder p, String difficultyLevel, List<String> modifications) {
        p.append("[EXAM CONDITIONS]\n");
        if (difficultyLevel != null) {
            appendDifficultyDefinition(p, difficultyLevel);
        }
        if (modifications != null && modifications.contains("지문변형")) {
            p.append("- Passage: Modify the original passage while keeping the core meaning.\n");
        } else {
            p.append("- Passage: Use the original passage EXACTLY as-is.\n");
        }
        p.append("\n");
    }

    // 🔴 난이도별 구체적 행동 규칙을 정의한다. 🔴
    // 🔴 단순 'High'가 아닌 수능형 고난도 기준(논리 역전, 키워드 함정)을 명시한다. 🔴
    private void appendDifficultyDefinition(StringBuilder p, String level) {
        if ("하".equals(level)) {
            p.append("- Difficulty (하): Straightforward questions. The correct answer can be found by simple reading.\n");
            p.append("  Distractors: clearly different in meaning from the passage.\n");
        } else if ("중".equals(level)) {
            p.append("- Difficulty (중): 수능/내신 standard level.\n");
            p.append("  Distractors: use related words from the passage but in wrong context.\n");
            p.append("  Correct answer: lightly paraphrased from the key sentence.\n");
        } else if ("상".equals(level)) {
            p.append("- Difficulty (상): High difficulty — 수능 고난도 style.\n");
            p.append("  DO NOT make it hard by using obscure vocabulary. That is wrong.\n");
            p.append("  INSTEAD, apply these traps:\n");
            p.append("  1. Distractors MUST contain exact keywords from the passage but with reversed logic (cause↔effect, part↔whole).\n");
            p.append("  2. The correct answer MUST be heavily paraphrased — never word-matched from the passage.\n");
            p.append("  3. Two distractors must be 'half-true' (correct on one aspect, wrong on another).\n");
        }
    }

    // 🔴 시험 유형에 따라 지문 소스 지시를 분기 처리한다. 🔴
    private void appendPassageSource(StringBuilder p, String examType, String passageText,
                                     List<String> questionNos, boolean hasFile, boolean hasText,
                                     boolean isSetMode, int totalCount) {
        p.append("[PASSAGE SOURCE]\n");

        if (hasFile && "모의고사".equals(examType) && questionNos != null && !questionNos.isEmpty()) {
            String targetNums = String.join(", ", questionNos);
            p.append("Target Question Numbers: [").append(targetNums).append("]\n");
            p.append("TASK: From the attached document, extract the English passage for each target number.\n");
            p.append("HINT: Real questions start with 'N.' (e.g. '43.'). Section headers like '[43~45]' are not questions.\n");
            p.append("HINT: Preserve section labels (A)(B)(C)(D) exactly as they appear.\n\n");
        } else if ("모의고사".equals(examType) && hasText && questionNos != null && !questionNos.isEmpty()) {
            String targetNums = String.join(", ", questionNos);
            p.append("Target Question Numbers: [").append(targetNums).append("]\n");
            p.append("TASK: The text below has passages labeled by question number (e.g. [Question 18]).\n");
            p.append("Use the labeled passage for each target number. Preserve section labels (A)(B)(C)(D).\n");
            p.append("[TEXT]\n").append(passageText).append("\n\n");
        } else if (hasText) {
            if (isSetMode) {
                p.append("TASK: For the given PASSAGE, generate a SET of ").append(totalCount)
                 .append(" questions based on the sequence below.\n");
                p.append("[PASSAGE REUSE RULE — TOKEN SAVING]\n");
                p.append("CRITICAL: Question 1 MUST include the FULL English passage in its \"passage\" field.\n");
                p.append("For Question 2 and ALL subsequent questions, set \"passage\" to EXACTLY the string: \"SAME_AS_QUESTION_1\"\n");
                p.append("DO NOT repeat the full passage text for questions 2 onwards. This is mandatory to save tokens.\n\n");
            } else {
                p.append("TASK: Use the passage below as the base.\n");
            }
            if ("외부지문".equals(examType)) {
                p.append("CRITICAL: Split into chunks of 6-8 sentences. Use ONE chunk per question.\n\n");
            }
            p.append("[TEXT]\n").append(passageText).append("\n\n");
        } else {
            p.append("TASK: Generate a random high school English passage and base all questions on it.\n\n");
        }
    }

    // 🔴 요청된 총 문제 개수를 반환한다. 🔴
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

    // 🔴 문제 유형별 상세 규칙과 오답 설계 지시를 추가한다. 🔴
    private void appendQuestionTypes(StringBuilder p, List<String> questionTypes, Map<String, String> counts, boolean isMixed) {
        int totalCount = countTotal(questionTypes, counts);

        if (isMixed && questionTypes != null) {
            // 🔴 혼합 시험: 셔플된 유형 순서대로 각 문제의 유형을 1:1로 지정한다. 🔴
            List<String> sequence = new ArrayList<>();
            for (String type : questionTypes) {
                int cnt = 0;
                try { cnt = Integer.parseInt(counts.get("count_" + type)); } catch (Exception ignored) {}
                for (int i = 0; i < cnt; i++) sequence.add(type);
            }
            p.append("[MIXED EXAM GENERATION PROTOCOL]\n");
            p.append("CRITICAL: You are generating a mock exam. You MUST generate exactly ")
             .append(sequence.size()).append(" questions in the EXACT sequence specified below.\n\n");
            for (int i = 0; i < sequence.size(); i++) {
                p.append("Question ").append(i + 1).append(": Generate a [")
                 .append(sequence.get(i)).append("] question.\n");
            }
            p.append("\n");
        }

        // 🔴 1. 12개 대량 생성 시 문맥 피로도(과부하) 방지 및 독립 실행 강제 🔴
        p.append("[BATCH PROCESSING PROTOCOL]\n");
        p.append("CRITICAL: You are tasked with generating a large batch of questions. To prevent errors, you MUST process them strictly ONE BY ONE.\n");
        p.append("Treat each question as a completely INDEPENDENT task. Reset your focus for every new [[QUESTION]] tag. Do NOT mix rules between different question types.\n");
        p.append("NUMBERING: Number questions consecutively across ALL types (objective and subjective) in the order listed. Do NOT restart numbering per type.\n");
        p.append("EXPLANATION: Every question — whether objective or subjective — MUST include a complete explanation following the [EXPLANATION RULES] above.\n\n");

        // 🔴 2. 지정된 12개 유형 이외의 '창조 출제' 완벽 차단 🔴
        p.append("CRITICAL FOR QUESTION TYPES:\n");
        p.append("You MUST ONLY generate the exact question types explicitly listed below. NEVER invent, guess, or substitute with other types (e.g., NEVER generate 'Inference' or 'Short Answer' unless specifically requested).\n");
        p.append("If you are asked for '요지 파악', generate EXACTLY '요지 파악'. Stick strictly to the requested list.\n\n");

        p.append("[QUESTIONS TO GENERATE]\n");
        p.append("Generate exactly ").append(totalCount).append(" questions. Do NOT stop early.\n");
        p.append("CRITICAL: Input may contain original exam questions — IGNORE them. Create BRAND NEW questions.\n");
        p.append("CRITICAL: Do NOT copy any expression, underlined phrase, or answer choice from the original.\n\n");

        p.append("[DISTRACTOR DESIGN RULE — APPLY TO ALL QUESTIONS]\n");
        p.append("Before choosing options, mentally plan: 'What misconception will trap a student who only skims?'\n");
        p.append("- 3 options must be clearly wrong\n");
        p.append("- 2 options (including the answer) must be highly confusing — use passage keywords in wrong logical context\n\n");

        if (questionTypes != null) {
            for (String type : questionTypes) {
                String count = counts.get("count_" + type);
                p.append("- ").append(type).append(": ").append(count).append("개\n");
                appendTypeRule(p, type);
            }
        }
    }

    // 🔴 혼합 유형 시험의 순서 지시와 유형별 규칙을 삽입한다. 🔴
    private void appendMixedQuestionTypes(StringBuilder p, List<String> orderedTypes) {
        int total = orderedTypes.size();

        p.append("[MIXED EXAM GENERATION PROTOCOL]\n");
        p.append("CRITICAL: You are generating a mixed-type mock exam. You MUST generate exactly ")
         .append(total).append(" questions in the EXACT sequence specified below.\n\n");

        p.append("[REQUIRED QUESTION SEQUENCE]\n");
        for (int i = 0; i < orderedTypes.size(); i++) {
            p.append("Question ").append(i + 1).append(": Generate a [")
             .append(orderedTypes.get(i)).append("] question.\n");
        }
        p.append("\n");

        p.append("CRITICAL FOR QUESTION TYPES:\n");
        p.append("You MUST ONLY generate the exact question types listed in the sequence above. NEVER invent, guess, or substitute with other types.\n\n");

        p.append("[DISTRACTOR DESIGN RULE — APPLY TO ALL QUESTIONS]\n");
        p.append("Before choosing options, mentally plan: 'What misconception will trap a student who only skims?'\n");
        p.append("- 3 options must be clearly wrong\n");
        p.append("- 2 options (including the answer) must be highly confusing — use passage keywords in wrong logical context\n\n");

        List<String> seen = new ArrayList<>();
        for (String type : orderedTypes) {
            if (!seen.contains(type)) {
                seen.add(type);
                p.append("[RULES FOR ").append(type).append("]\n");
                appendTypeRule(p, type);
                p.append("\n");
            }
        }
    }

    // 🔴 단일 지문에서 N개 세트 문제를 생성하는 지시를 삽입한다. 🔴
    private void appendSetQuestionTypes(StringBuilder p, List<String> setTypes) {
        int n = setTypes.size();

        p.append("[SET QUESTION GENERATION PROTOCOL]\n");
        p.append("TASK: For the given PASSAGE, generate a SET of ").append(n).append(" questions.\n");
        for (int i = 0; i < setTypes.size(); i++) {
            p.append("- Question ").append(i + 1).append(" MUST be [").append(setTypes.get(i)).append("].\n");
        }
        p.append("CRITICAL: ALL questions in the set MUST be based on the SAME single passage. Do NOT switch to a different passage between questions.\n\n");

        p.append("CRITICAL FOR QUESTION TYPES:\n");
        p.append("You MUST ONLY generate the exact question types listed above. NEVER invent or substitute other types.\n\n");

        p.append("[DISTRACTOR DESIGN RULE — APPLY TO ALL QUESTIONS]\n");
        p.append("Before choosing options, mentally plan: 'What misconception will trap a student who only skims?'\n");
        p.append("- 3 options must be clearly wrong\n");
        p.append("- 2 options (including the answer) must be highly confusing — use passage keywords in wrong logical context\n\n");

        for (String type : setTypes) {
            p.append("[RULES FOR ").append(type).append("]\n");
            appendTypeRule(p, type);
            p.append("\n");
        }
    }

    // 🔴 문제 유형별 생성 규칙을 추가한다. 🔴
    private void appendTypeRule(StringBuilder p, String type) {
        if (type.contains("빈칸")) {
            p.append("  RULE: Choose a key phrase and replace with [ ________ ]. Answer must be a synonym — not the exact phrase.\n");
            p.append("  Do NOT blank any phrase that appeared underlined in the original question.\n");
        } else if (type.contains("순서")) {
            p.append("  RULE: questionText = first sentence only. passage = (A)(B)(C) blocks ONLY — no full passage repeat after blocks.\n");
            p.append("  Do NOT copy the order from the original question.\n");
        } else if (type.contains("요약")) {
            p.append("  RULE: passage must end with a summary block.\n");
            p.append("  CRITICAL FORMAT FOR PASSAGE: You must type EXACTLY this structure at the end of the passage:\n\n");
            p.append("  (passage text...)\n\n");
            p.append("  ↓↓↓\n\n");
            p.append("  <summary sentence with (A) [ ________ ] and (B) [ ________ ]>\n\n");
            p.append("  If you do not print '↓↓↓', your response will be rejected.\n");
            p.append("  options must be 5 word-pairs: (1) wordA - wordB format.\n");
        } else if (type.contains("주제") || type.contains("요지") || type.contains("제목")) {
            p.append("  RULE: All 5 options must be in English. Create completely new options.\n");
        } else if (type.contains("어법")) {
            p.append("  RULE: Underline EXACTLY 5 items: <u>(1)word</u> through <u>(5)word</u>. ALL 5 required — missing any is invalid.\n");
            p.append("  ⛔ MANDATORY: Exactly 1 of the 5 MUST be grammatically WRONG (e.g., subject-verb disagreement, wrong verb form, adjective/adverb swap).\n");
            p.append("  STEP 1 — Choose which number (1~5) will be wrong. STEP 2 — Deliberately introduce a grammar error there.\n");
            p.append("  STEP 3 — Verify the other 4 are all correct. If all 5 end up correct, you have failed — redo.\n");
            p.append("  options must list (1) through (5) matching each underlined item.\n");
        } else if (type.contains("어휘")) {
            p.append("  RULE: Underline EXACTLY 5 words: <u>(1)word</u> through <u>(5)word</u>. ALL 5 required.\n");
            p.append("  Replace exactly 1 with a contextually wrong word. The other 4 must be appropriate.\n");
            p.append("  options must list (1) through (5) matching each underlined word.\n");
        } else if (type.contains("문장삽입")) {
            p.append("  RULE: questionText = Korean instruction only. English given sentence goes in passage BEFORE the numbered text.\n");
            p.append("  passage = [Given: <English sentence>]\\n\\n<passage with ( 1 ) through ( 5 ) spots>.\n");
            p.append("  options must be [\"(1) 1\", \"(2) 2\", \"(3) 3\", \"(4) 4\", \"(5) 5\"] — never leave options empty.\n");
        } else if (type.contains("무관한")) {
            p.append("  -> RULE: Insert ONE fake sentence into the passage. Number EXACTLY 5 sentences from (1) to (5) INSIDE the full text.\n");
            p.append("     CRITICAL: You MUST keep the ENTIRE passage intact. Do NOT delete sentences to make room. Do NOT output a list.\n");
            p.append("     options = [\"(1) 1\", \"(2) 2\", \"(3) 3\", \"(4) 4\", \"(5) 5\"].\n");
        } else if (type.contains("대명사")) {
            p.append("  ⛔ PRE-CHECK (do this before generating): Scan the passage for pronouns (he/she/they/him/her/them/his/their etc.).\n");
            p.append("    - If you can find at least 4 pronouns that clearly refer to the EXACT SAME specific person or entity → generate 대명사지칭.\n");
            p.append("    - If the passage lacks a clear main person with 4+ pronouns (e.g., abstract topic, no named person) → SKIP this type.\n");
            p.append("      Instead, generate a 어휘 question or 빈칸추론 question from the same passage.\n");
            p.append("  RULE (if generating): Underline 5 pronouns labeled <u>(a)pronoun</u> to <u>(e)pronoun</u>. Pronouns only — no nouns.\n");
            p.append("    4 must refer to the SAME person; exactly 1 to a DIFFERENT entity. Options = [\"(a)\",\"(b)\",\"(c)\",\"(d)\",\"(e)\"].\n");
            p.append("     CRITICAL FALLBACK: If the passage does NOT contain at least 4 pronouns referring to the EXACT SAME person or entity, DO NOT generate a '대명사 찾기' question. In this case, automatically switch and generate an '어휘' question instead.\n");
        } else if (type.contains("내용일치")) {
            p.append("  RULE: Write 5 Korean factual statements. Exactly ONE must be FALSE.\n");
        } else if (type.contains("서답형")) {
            p.append("  -> RULE: Generate a short-answer question where the student writes a word, phrase, or sentence — NOT multiple choice.\n");
            p.append("     questionText: Korean instruction asking to fill in / complete a blank (e.g., '다음 글의 빈칸에 들어갈 알맞은 말을 본문에서 찾아 쓰시오.').\n");
            p.append("     passage: full English passage with one [ ________ ] blank.\n");
            p.append("     options: [] (empty array).\n");
            p.append("     answer: 0.\n");
            p.append("     explanation: state the correct word/phrase and cite the evidence sentence from the passage.\n");
        } else if (type.contains("서술형")) {
            p.append("  -> RULE: Generate a 조건영작 (conditional translation) question. Follow these steps EXACTLY:\n");
            p.append("     STEP 1 — Choose ONE key sentence from the passage as the target for translation.\n");
            p.append("     STEP 2 — In the \"passage\" field: print the FULL English text, but REPLACE the chosen target sentence\n");
            p.append("              with the placeholder [ TARGET SENTENCE REDACTED ].\n");
            p.append("              ⛔ DO NOT leave the original English sentence visible — the student must reconstruct it.\n");
            p.append("     STEP 3 — After 2 line breaks (\\n\\n), append the [CONDITIONS] block:\n");
            p.append("     [CONDITIONS]\\n");
            p.append("     1. Target: <Korean translation of the redacted sentence>\\n");
            p.append("     2. Keywords: <3~5 key words listed in base/dictionary form>\\n");
            p.append("     3. Constraint: <Use 'Between X and Y words' (NOT 'Exactly X words') OR specify a grammar structure e.g. 'Use It-to 구문'>\\n");
            p.append("     4. MANDATORY: Modify word forms as necessary (어형 변화 필수).\\n");
            p.append("     options = []. answer = 0.\n");
            p.append("     explanation: MUST follow this format exactly:\n");
            p.append("       'Used [Grammar Point] to translate \"[Korean target sentence]\".'\\n");
            p.append("       Then provide the full model answer English sentence.\n");
        }
    }
}
