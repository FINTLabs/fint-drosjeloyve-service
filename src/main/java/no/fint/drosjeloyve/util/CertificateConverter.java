package no.fint.drosjeloyve.util;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfAConformanceLevel;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfOutputIntent;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.pdfa.PdfADocument;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;


@Slf4j
public class CertificateConverter {

    private static final String TAX_TITLE = "Skatteattest";
    private static final String BANKRUPT_TITLE = "Konkursattest";

    private String fontFile;

    public CertificateConverter() {
        this.fontFile = this.getClass().getResource("/times.ttf").getFile();
    }

    public byte[] convertTaxCertificate() {
        try {
            File pdfFile = File.createTempFile("tax", ".pdf");
            pdfFile.deleteOnExit();

            Document document = createDocument(pdfFile);
            Paragraph paragraph = new Paragraph(TAX_TITLE);
            document.add(paragraph);
            document.close();

            log.info("We'll now return an byte array representation of the tax certificate, temporary saved in {}. It will be deleted on exit.",
                    pdfFile.getPath());
            return Files.readAllBytes(pdfFile.toPath());
        } catch (IOException e) {
            log.error("Ups, it's trouble in the tower :-/", e);
        }

        return null;
    }

    public byte[] convertBankruptCertificate() {
        try {
            File pdfFile = File.createTempFile("bankrupt", ".pdf");
            pdfFile.deleteOnExit();

            Document document = createDocument(pdfFile);
            Paragraph paragraph = new Paragraph(BANKRUPT_TITLE);
            document.add(paragraph);
            document.close();

            log.info("We'll now return an byte array representation of the bankrupt certificate, temporary saved in {}. It will be deleted on exit.",
                    pdfFile.getPath());
            return Files.readAllBytes(pdfFile.toPath());
        } catch (IOException e) {
            log.error("Ups, it's trouble in the tower :-/", e);
        }

        return null;
    }

    private Document createDocument(File pdfFile) throws IOException {
        PdfADocument pdfADocument = new PdfADocument(new PdfWriter(pdfFile),
                PdfAConformanceLevel.PDF_A_1A,
                new PdfOutputIntent(new PdfDictionary()));

        Document document = new Document(pdfADocument);
        pdfADocument.setTagged();

        PdfFont pdfFont = PdfFontFactory.createFont(fontFile, PdfEncodings.WINANSI, true);
        document.setFont(pdfFont);

        return document;
    }
}
