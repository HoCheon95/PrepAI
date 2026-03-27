package com.example.demo.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.springframework.stereotype.Service;

import java.awt.Rectangle;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

// 🔴 스캔 이미지 PDF의 특정 페이지들을 PNG 바이트 배열로 변환한다. 🔴
// 🔴 Gemini에 전체 PDF 대신 필요한 페이지만 전송해 추출 정확도를 높인다. 🔴
@Service
public class PdfPageService {

    private static final float RENDER_DPI = 100f;  // 해상도: 품질/용량 균형점

    // 🔴 PDF 바이트 배열에서 지정된 페이지 번호들(1-indexed)을 PNG로 변환해 반환한다. 🔴
    public List<byte[]> renderPages(byte[] pdfBytes, int... pageNumbers) throws Exception {
        List<byte[]> result = new ArrayList<>();

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(doc);
            int totalPages = doc.getNumberOfPages();

            for (int pageNo : pageNumbers) {
                int idx = pageNo - 1;  // 1-indexed → 0-indexed
                if (idx < 0 || idx >= totalPages) {
                    System.out.println("[PdfPage] 페이지 " + pageNo + "는 범위 초과 (총 " + totalPages + "페이지)");
                    continue;
                }
                BufferedImage img = renderer.renderImageWithDPI(idx, RENDER_DPI);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(img, "PNG", baos);
                result.add(baos.toByteArray());
                System.out.println("[PdfPage] 페이지 " + pageNo + " 렌더링 완료 (" + baos.size() / 1024 + "KB)");
            }
        }
        return result;
    }

    // 🔴 PDF 총 페이지 수를 반환한다. 🔴
    public int getPageCount(byte[] pdfBytes) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            return doc.getNumberOfPages();
        }
    }

    // 🔴 PDF에서 지정된 페이지 범위의 텍스트를 직접 추출한다 (Gemini 호출 없음). 🔴
    public String extractText(byte[] pdfBytes, int startPage, int endPage) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(startPage);
            stripper.setEndPage(endPage);
            stripper.setSortByPosition(true);
            return stripper.getText(doc);
        }
    }

    // 🔴 2단 레이아웃 PDF에서 왼쪽/오른쪽 컬럼을 분리해 추출하고 밑줄을 <u>태그로 표시한다. 🔴
    public String extractTextByColumns(byte[] pdfBytes, int startPage, int endPage) throws Exception {
        StringBuilder result = new StringBuilder();

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            int totalPages = doc.getNumberOfPages();

            for (int pageNo = startPage; pageNo <= Math.min(endPage, totalPages); pageNo++) {
                PDPage page = doc.getPage(pageNo - 1);  // 0-indexed

                float pageWidth  = page.getMediaBox().getWidth();
                float pageHeight = page.getMediaBox().getHeight();
                int midX   = (int) (pageWidth / 2);
                int height = (int) pageHeight;
                int width  = (int) pageWidth;

                PDFTextStripperByArea stripper = new PDFTextStripperByArea();
                stripper.setSortByPosition(true);
                stripper.addRegion("left",  new Rectangle(0,    0, midX,        height));
                stripper.addRegion("right", new Rectangle(midX, 0, width - midX, height));
                stripper.extractRegions(page);

                String pageText = stripper.getTextForRegion("left") + stripper.getTextForRegion("right");

                // 🔴 밑줄 어노테이션을 찾아 해당 텍스트를 <u> 태그로 감싼다. 🔴
                pageText = applyUnderlineAnnotations(page, pageText, pageHeight);

                if (result.length() > 0) result.append("\n");
                result.append(pageText);

                System.out.println("[PdfPage] 컬럼 추출 완료 — 페이지 " + pageNo);
            }
        }
        return result.toString();
    }

    // 🔴 PDF 어노테이션 또는 한국 수능 패턴으로 밑줄 텍스트를 <u> 태그로 표시한다. 🔴
    private String applyUnderlineAnnotations(PDPage page, String pageText, float pageHeight) {
        try {
            java.util.List<String> underlinedWords = new ArrayList<>();

            for (PDAnnotation annot : page.getAnnotations()) {
                if (!(annot instanceof PDAnnotationTextMarkup)) continue;
                PDAnnotationTextMarkup markup = (PDAnnotationTextMarkup) annot;
                if (!"Underline".equals(markup.getSubtype())) continue;

                PDRectangle rect = markup.getRectangle();
                if (rect == null) continue;

                int rx = Math.max(0, (int) rect.getLowerLeftX() - 1);
                int ry = Math.max(0, (int) (pageHeight - rect.getUpperRightY()) - 1);
                int rw = (int) (rect.getUpperRightX() - rect.getLowerLeftX()) + 4;
                int rh = (int) (rect.getUpperRightY() - rect.getLowerLeftY()) + 4;

                PDFTextStripperByArea regionStripper = new PDFTextStripperByArea();
                regionStripper.setSortByPosition(true);
                regionStripper.addRegion("u", new Rectangle(rx, ry, rw, rh));
                regionStripper.extractRegions(page);
                String uText = regionStripper.getTextForRegion("u").trim();
                if (!uText.isEmpty()) underlinedWords.add(uText);
            }

            if (!underlinedWords.isEmpty()) {
                // PDF 어노테이션 기반 밑줄 적용
                underlinedWords.sort((a, b) -> b.length() - a.length());
                for (String word : underlinedWords) {
                    pageText = pageText.replace(word, "<u>" + word + "</u>");
                }
                System.out.println("[PdfPage] 어노테이션 밑줄 " + underlinedWords.size() + "개 적용");
            }
            // 🔴 어노테이션 유무와 관계없이 수능 (a)~(e) 라벨 패턴도 항상 적용 🔴
            // "(a) sped", "(b) expansion" → "(a) <u>sped</u>" 형태로 변환
            pageText = pageText.replaceAll(
                    "\\(([a-e])\\) ([A-Za-z'\\-]+)",
                    "($1) <u>$2</u>"
            );
            System.out.println("[PdfPage] 수능 패턴 밑줄 적용");
        } catch (Exception e) {
            System.out.println("[PdfPage] 밑줄 처리 실패 (무시): " + e.getMessage());
        }
        return pageText;
    }
}
