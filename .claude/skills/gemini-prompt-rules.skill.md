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