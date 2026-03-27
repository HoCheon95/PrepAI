package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// 🔴 생성된 문제를 저장하는 JPA 엔티티다. ExamMaker가 이 테이블을 읽어 시험지를 구성한다. 🔴
@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔴 문제 유형 (빈칸추론, 주제파악, 요지파악 등 13개 중 하나) 🔴
    @Column(name = "question_type")
    private String questionType;

    // 🔴 생성 세트 내 문제 순서 (1, 2, 3...) 🔴
    @Column(name = "question_number")
    private Integer questionNumber;

    // 🔴 영어 지문 ([[PASSAGE]] 내용) 🔴
    @Column(columnDefinition = "TEXT")
    private String passage;

    // 🔴 입력 모드 — 모의고사 / 외부지문 / 교과서 🔴
    @Column(name = "input_mode")
    private String inputMode;

    // 🔴 문제 본문 ([[QUESTION]] 내용, 한국어) 🔴
    @Column(name = "question_text", columnDefinition = "TEXT")
    private String questionText;

    // 🔴 선택지 원문 ([[OPTIONS]] 내용, 줄바꿈 포함 텍스트) 🔴
    @Column(columnDefinition = "TEXT")
    private String options;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // 🔴 저장 전 자동으로 현재 시각을 기록한다. 🔴
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }

    public Integer getQuestionNumber() { return questionNumber; }
    public void setQuestionNumber(Integer questionNumber) { this.questionNumber = questionNumber; }

    public String getPassage() { return passage; }
    public void setPassage(String passage) { this.passage = passage; }

    public String getInputMode() { return inputMode; }
    public void setInputMode(String inputMode) { this.inputMode = inputMode; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public String getOptions() { return options; }
    public void setOptions(String options) { this.options = options; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
