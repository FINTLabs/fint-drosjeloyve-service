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
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.pdfa.PdfADocument;
import lombok.extern.slf4j.Slf4j;
import no.fint.drosjeloyve.model.AltinnApplication;
import no.fint.drosjeloyve.model.ebevis.Evidence;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


@Slf4j
public class CertificateConverter {

    private static final String TAX_TITLE = "Skatteattest";
    private static final String BANKRUPT_TITLE = "Bekreftelse fra Konkursregisteret";

    private String fontFile;

    public CertificateConverter() {
        this.fontFile = this.getClass().getResource("/times.ttf").getFile();
    }

    public byte[] convertTaxCertificate(Evidence evidence, AltinnApplication application) {
        try {
            File pdfFile = File.createTempFile("tax", ".pdf");
            pdfFile.deleteOnExit();

            Document document = createDocument(pdfFile);
            Paragraph paragraph = new Paragraph(TAX_TITLE).setFontSize(36);
            document.add(paragraph);

            document.add(new Paragraph("U.off. offl. § 13, sktbl. § 3-2").setTextAlignment(TextAlignment.RIGHT));

            document.add(new Paragraph().add(application.getSubject()).add(new Text("\n")).add(application.getSubjectName()));

            document.add(new Paragraph("Attest for skatt og merverdiavgift").setFontSize(18).setBold());
            String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.YYYY"));
            document.add(new Paragraph("Attesten er produsert på bakgrunn av registrerte opplysninger i skatte- og avgiftssystemene per " + currentDate +
                    ". For spørsmål om merverdiavgift kontakt Skatteetaten. For spørsmål om øvrige skatte- og avgiftskrav kontakt skatteoppkrever"));

            document.add(new Paragraph("Gjelder:").setBold());
            document.add(new Paragraph("Organisasjonsnummer " + application.getSubject()));

            document.add(new Paragraph("Følgende forfalte ikke betalte restanser er registrert på ovennevnte foretak per dags dato:").setBold());

            document.add(new Paragraph("Snart vil du se restanser her...").setItalic());

            document.add(new Paragraph("Ved offentlige anskaffelser skal attesten ikke være eldre enn 6 måneder.").setBold());
            document.add(new Paragraph("Dokumentet er elektronisk godkjent og er derfor ikke signert."));
            document.add(new Paragraph().add("Skatteetaten.no | FINTLabs.no | VigoIKS.no").setTextAlignment(TextAlignment.RIGHT));
            document.close();

            log.info("We'll now return an byte array representation of the tax certificate, temporary saved in {}. It will be deleted on exit.",
                    pdfFile.getPath());
            return Files.readAllBytes(pdfFile.toPath());
        } catch (IOException e) {
            log.error("Ups, it's trouble in the tower :-/", e);
        }

        return null;
    }

    public byte[] convertBankruptCertificate(Evidence evidence, AltinnApplication application) {
        try {
            File pdfFile = File.createTempFile("bankrupt", ".pdf");
            pdfFile.deleteOnExit();

            Document document = createDocument(pdfFile);
            document.add(new Paragraph("Brønnøysundregistrene").setFontSize(18).setItalic());
            document.add(new Paragraph(BANKRUPT_TITLE).setFontSize(36));

            document.add(new Paragraph("Vi viser til din bestilling vedrørende:"));
            document.add(new Paragraph("Organisasjonsnummer: ").add(application.getSubject())
            .add(new Text("\n")).add("Navn: ").add(application.getSubjectName()));

            document.add(new Paragraph("Konkursregisteret har følgende registrerte opplysnigner per dags dato:").setBold());

            document.add(new Paragraph("Snart vil du se om foretaket er under tvangsavvikling her...").setItalic());

            document.add(new Paragraph("Ved offentlige anskaffelser skal attesten ikke være eldre enn 6 måneder.").setBold());
            document.add(new Paragraph("Dokumentet er elektronisk godkjent og er derfor ikke signert."));
            document.add(new Paragraph().add("brreg.no | FINTLabs.no | VigoIKS.no").setTextAlignment(TextAlignment.RIGHT));

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
