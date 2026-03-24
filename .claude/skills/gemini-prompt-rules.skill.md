---
name: gemini-prompt-rules
description: Guidelines for modifying the Gemini prompt generation logic in GeminiController.java for English exam questions.
---

# Gemini Prompt Generation Rules

When modifying `GeminiController.java` or any related prompt generation logic for the high school English exam creator, you MUST strictly follow these rules to prevent API errors and parsing failures.

## 1. Strict Formatting Rules
- **No Markdown**: NEVER use markdown formatting like **bold** or *italic* in the prompt instructions. The output must be raw text.
- **Underlines**: If a word needs to be underlined, you MUST instruct the API to use HTML tags: `<u>underlined word</u>`.
- **Blanks**: For 'Fill in the blank' questions, you MUST instruct the API to use `[ ________ ]` to represent the blank.

## 2. Tag System (CRITICAL)
The frontend parsing logic depends on these exact tags. You MUST preserve this tag structure in the prompt builder:
- `[[QUESTION]]`
- `[[PASSAGE]]`
- `[[OPTIONS]]` (Formatted exactly as (1) to (5))
- `[[ANSWER]]`
- `[[EXPLANATION]]`
- `---SEP---` (Used to separate multiple questions)

## 3. Modifying Prompt Logic
- **Keep it concise**: When adding new question types (`questionTypes`), write the rule in a single, short English sentence. Long prompts cause 500 errors (server crashes).
- **Conditionals**: Do not alter the boolean checks (`hasFile`, `hasText`) that separate the PDF scanning logic ("모의고사" mode) from the raw text processing logic ("외부지문" mode).

## Example of Good Rule Addition
If adding a new question type for "Grammar":
`prompt.append("  -> RULE: Underline 5 parts and label them (1) to (5). One is incorrect.\n");`

### 📝 시험 문제 최종 출력 렌더링 규칙
- 시험 문제를 화면에 최종적으로 보여줄 때는 하나의 긴 페이지로 끝없이 늘어뜨리지 마세요.
- 실제 A4 용지 규격에 맞춰서 인쇄하기 좋도록, 적절한 문항 수 단위로 페이지를 명확히 나누어(Pagination) 주세요.
- 문제의 지문이나 보기가 페이지 경계에서 어색하게 잘리지 않도록 여백과 줄바꿈을 조절해 주세요.
- 사용자가 즉시 인쇄하거나 문서로 저장하기 편하도록, 각 페이지가 딱딱 나뉘어 떨어지는 깔끔한 레이아웃으로 구성해 주세요.