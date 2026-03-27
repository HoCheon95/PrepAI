package com.example.demo.repository;

import com.example.demo.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// 🔴 answers 테이블에 대한 CRUD를 담당한다. 🔴
@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {

    // 🔴 문제 ID로 해당 문제의 답안 조회 (ExamMaker 연동 API에서 사용) 🔴
    Optional<Answer> findByQuestionId(Long questionId);
}
