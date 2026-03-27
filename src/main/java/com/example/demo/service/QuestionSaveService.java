package com.example.demo.service;

import com.example.demo.entity.Answer;
import com.example.demo.entity.Question;
import com.example.demo.repository.AnswerRepository;
import com.example.demo.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// 🔴 프론트엔드에서 검토·편집이 끝난 문제 목록을 questions + answers 두 테이블에 저장한다. 🔴
@Service
public class QuestionSaveService {

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private AnswerRepository answerRepository;

    // 🔴 questions와 answers를 한 트랜잭션 안에서 저장하고 생성된 ID 목록을 반환한다. 🔴
    @Transactional
    public List<Long> saveAll(String examType, List<Map<String, String>> questions) {
        List<Long> savedIds = new ArrayList<>();

        for (int i = 0; i < questions.size(); i++) {
            Map<String, String> q = questions.get(i);

            Question entity = new Question();
            entity.setInputMode(examType);
            entity.setQuestionNumber(i + 1);
            entity.setQuestionText(q.getOrDefault("question", ""));
            entity.setPassage(q.getOrDefault("passage", ""));
            entity.setOptions(q.getOrDefault("options", ""));

            // 🔴 문제 텍스트 첫 줄에서 유형을 추출한다. 길면 50자로 잘라 저장한다. 🔴
            String firstLine = entity.getQuestionText().split("\n")[0].trim();
            entity.setQuestionType(firstLine.length() > 50 ? firstLine.substring(0, 50) : firstLine);

            Question saved = questionRepository.save(entity);

            Answer answer = new Answer();
            answer.setQuestionId(saved.getId());
            answer.setAnswer(q.getOrDefault("answer", ""));
            answer.setExplanation(q.getOrDefault("explanation", ""));
            answerRepository.save(answer);

            savedIds.add(saved.getId());
        }

        return savedIds;
    }
}
