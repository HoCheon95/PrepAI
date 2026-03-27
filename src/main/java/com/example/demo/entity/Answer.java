package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// 🔴 문제의 정답과 해설을 저장하는 JPA 엔티티다. questions 테이블과 1:1 관계다. 🔴
// 🔴 ExamMaker는 이 테이블을 읽어 답안지를 구성한다. 🔴
@Entity
@Table(name = "answers")
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔴 연결된 문제의 ID (questions.id 참조) — 두 테이블이 별도이므로 FK 어노테이션 없이 Long으로 관리 🔴
    @Column(name = "question_id", nullable = false)
    private Long questionId;

    // 🔴 정답 번호 ([[ANSWER]] 내용, 예: "3") 🔴
    @Column
    private String answer;

    // 🔴 해설 ([[EXPLANATION]] 내용, 한국어) 🔴
    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // 🔴 저장 전 자동으로 현재 시각을 기록한다. 🔴
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
