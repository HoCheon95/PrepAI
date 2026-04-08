# TASK: Optimize PromptBuilder for Cost Reduction and Subjective Accuracy

Please update `PromptBuilder.java` to implement token-saving strategies and fix logical errors in subjective question generation.

## 1. Token Cost Optimization (Crucial)
To reduce API costs, we must stop redundant passage output when generating multiple questions for the same text.
- **Passage Reuse Rule:** - If `isSetMode` is true, the `passage` field for the **FIRST** question must contain the full English text.
  - For **Question 2 and onwards**, the `passage` field must ONLY contain the string: `"SAME_AS_QUESTION_1"`. 
  - (Note: The Java backend will handle re-mapping this, but we must stop the LLM from printing the same 300-word text 12 times.)

## 2. Subjective (서술형) Logic Fix
Prevent the "Answer Leak" where the target sentence remains visible in the passage.
- **Redaction Rule:** - When generating a '서술형 조건영작' question, the LLM MUST identify the target English sentence within the passage and **REPLACE** it with `[ TARGET SENTENCE REDACTED ]`.
  - This ensures the student cannot simply copy the answer from the text above.

## 3. Improved Explanation & Constraints
Refine the generation quality to match the 'Answer Key' style (Pages 63-75 of the PDF).
- **Flexible Constraints:** Change "Exactly X words" to "Between X and Y words" or "Approximately X words" to prevent the LLM from hallucinating or cutting off mid-generation.
- **Grammar Explanation:** The `explanation` field for subjective questions must explicitly state: "Used [Grammar Point] to translate '[Korean Sentence]'."

## 4. JSON Hygiene & Stability
- **No Raw Newlines:** Reiterate that actual Enter keys (code 10) are FORBIDDEN inside JSON strings. Use `\n` instead.
- **Batch Processing:** Remind the LLM to process each question independently to maintain high logical consistency.

## 5. Critical Formatting Rule
Ensure the `↓↓↓` marker for Summary (요약문) and the `<u>(1)word</u>` format for Grammar/Vocab remain strictly enforced.