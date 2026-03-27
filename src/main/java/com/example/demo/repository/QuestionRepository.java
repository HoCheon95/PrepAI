package com.example.demo.repository;

import com.example.demo.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// 🔴 questions 테이블에 대한 CRUD를 담당한다. 🔴
@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    // 🔴 생성 시각 역순으로 전체 문제 목록 조회 (ExamMaker 연동용) 🔴
    List<Question> findAllByOrderByCreatedAtDesc();
}
