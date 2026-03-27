package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/question-form")
    public String showQuestionForm() {
        return "questionForm";
    }

    // 🔴 PDF 지문 추출 테스트 페이지 🔴
    @GetMapping("/test/pdf")
    public String showPdfTest() {
        return "pdfTest";
    }

    // 🔴 ResponseValidator에서 던진 예외를 잡아 error.jsp로 전달한다. 🔴
    @ControllerAdvice
    public static class GlobalExceptionHandler {
        @ExceptionHandler(Exception.class)
        public String handleException(Exception e, Model model) {
            model.addAttribute("errorMessage", e.getMessage());
            return "error";
        }
    }
}
