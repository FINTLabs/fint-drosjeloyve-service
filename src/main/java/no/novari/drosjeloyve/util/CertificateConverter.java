package no.novari.drosjeloyve.util;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.pdfa.PdfADocument;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import no.fint.altinn.model.AltinnApplication;
import no.fint.altinn.model.ebevis.Evidence;
import no.fint.altinn.model.ebevis.EvidenceStatus;
import no.fint.altinn.model.ebevis.EvidenceValue;
import no.fint.altinn.model.ebevis.vocab.ValueType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class CertificateConverter {
    private static final String TAX_TITLE = "Skatteattest";
    private static final String BANKRUPT_TITLE = "Bekreftelse fra Konkursregisteret";
    private static final String MISSING_OR_FAULTY_DATA = "<Feil eller mangler i mottatte data>";

    @Value("${fint.font:/data/times.ttf}")
    @Setter
    @Getter
    private String fontFile;

    public CertificateConverter() {
        log.info("Congratulation with your brand new instance of {}. The font file seem right now to be: {}",
                CertificateConverter.class.getCanonicalName(), fontFile);
    }

    @PostConstruct
    public void init() {
        if (this.fontFile == null) {
            log.warn("The font file seems to be null? It can cause trouble. You're hereby warned.");
        } else {
            log.info("All good. The font file value is: {}. Hopefully you'll be happy with that.", this.fontFile);
        }
    }

    public byte[] convertTaxCertificate(Evidence evidence, AltinnApplication application) {
        try {
            File pdfFile = File.createTempFile("tax", ".pdf");
            pdfFile.deleteOnExit();

            Document document = createDocument(pdfFile);
            Paragraph paragraph = new Paragraph(TAX_TITLE).setFontSize(36);
            document.add(paragraph);

            document.add(new Paragraph("U.off. offl. § 13, sktbl. § 3-2").setTextAlignment(TextAlignment.RIGHT));

            document.add(new Paragraph("Attest for skatt og merverdiavgift").setFontSize(18).simulateBold());

            document.add(new Paragraph("Attesten er produsert på bakgrunn av registrerte opplysninger i skatte- og avgiftssystemene." +
                    " For spørsmål om merverdiavgift kontakt Skatteetaten. For spørsmål om øvrige skatte- og avgiftskrav kontakt skatteoppkrever."));

            document.add(new Paragraph("Gjelder:").simulateBold());

            document.add(new Paragraph(String.format("%s (%s)", application.getSubjectName(), application.getSubject())));

            document.add(new Paragraph("Følgende forfalte ikke betalte restanser er registrert på ovennevnte foretak:").simulateBold());

            addEvidence(evidence, document);

            document.add(new Paragraph("Ved offentlige anskaffelser skal attesten ikke være eldre enn 6 måneder.").simulateBold());
            document.add(new Paragraph("Dokumentet er elektronisk godkjent og er derfor ikke signert."));
            document.add(new Paragraph().add("Skatteetaten.no | FINTLabs.no | Novari.no").setTextAlignment(TextAlignment.RIGHT));

            document.close();

            return Files.readAllBytes(pdfFile.toPath());
        } catch (IOException e) {
            log.error("Ups, it's PDF trouble in the tower :-/", e);
        }

        return null;
    }

    public byte[] convertBankruptCertificate(Evidence evidence, AltinnApplication application) {
        try {
            File pdfFile = File.createTempFile("bankrupt", ".pdf");
            pdfFile.deleteOnExit();

            Document document = createDocument(pdfFile);
            document.add(new Paragraph("Brønnøysundregistrene").setFontSize(18).simulateItalic());
            document.add(new Paragraph(BANKRUPT_TITLE).setFontSize(36));

            document.add(new Paragraph("Vi viser til din bestilling vedrørende:"));

            document.add(new Paragraph(String.format("%s (%s)", application.getSubjectName(), application.getSubject())));

            document.add(new Paragraph("Konkursregisteret har følgende registrerte opplysninger:").simulateBold());

            addEvidence(evidence, document);

            document.add(new Paragraph("Ved offentlige anskaffelser skal attesten ikke være eldre enn 6 måneder.").simulateBold());
            document.add(new Paragraph("Dokumentet er elektronisk godkjent og er derfor ikke signert."));
            document.add(new Paragraph().add("brreg.no | FINTLabs.no | Novari.no").setTextAlignment(TextAlignment.RIGHT));

            document.close();

            return Files.readAllBytes(pdfFile.toPath());
        } catch (IOException e) {
            log.error("Ups, it's PDF trouble in the tower :-/", e);
        }

        return null;
    }

    private Document createDocument(File pdfFile) throws IOException {
        PdfADocument pdfADocument = new PdfADocument(new PdfWriter(pdfFile),
                PdfAConformance.PDF_A_1A,
                new PdfOutputIntent(new PdfDictionary()));

        Document document = new Document(pdfADocument);
        pdfADocument.setTagged();
        pdfADocument.getCatalog().setLang(new PdfString("no"));

        PdfFont pdfFont = PdfFontFactory.createFont(fontFile, PdfEncodings.WINANSI);
        document.setFont(pdfFont);

        return document;
    }

    private void addEvidence(Evidence evidence, Document document) {
        if (evidence == null) {
            document.add(new Paragraph(MISSING_OR_FAULTY_DATA));
            return;
        }

        List<EvidenceValue> evidenceValues = evidence.getEvidenceValues();
        EvidenceStatus evidenceStatus = evidence.getEvidenceStatus();

        if (evidenceValues == null || evidenceStatus == null) {
            document.add(new Paragraph(MISSING_OR_FAULTY_DATA));
            return;
        }

        Map<String, EvidenceValue> values = evidenceValues.stream()
                .collect(Collectors.toMap(EvidenceValue::getEvidenceValueName, value -> value));

        String evidenceCodeName = evidenceStatus.getEvidenceCodeName();

        if (evidenceCodeName == null) {
            document.add(new Paragraph(String.format("Kilde: %s", MISSING_OR_FAULTY_DATA)));
        } else if (evidenceCodeName.equals("KonkursDrosje")) {
            document.add(new Paragraph(String.format("Kilde: %s %s", getSource(values.get("Organisasjonsnavn")), getDate(values.get("Organisasjonsnavn")))));
        }else if (evidenceCodeName.equals("RestanserDrosje")) {
            document.add(new Paragraph(String.format("Kilde: %s %s", getSource(values.get("skattForfaltOgUbetalt")), getDate(values.get("skattForfaltOgUbetalt")))));
        } else if (evidenceCodeName.equals("RestanserV2")) {
            document.add(new Paragraph(String.format("Kilde: %s %s", getSource(values.get("levert")), getDate(values.get("levert")))));
        }

        values.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .filter(entry -> evidenceValueMapping.containsKey(entry.getKey()))
                .forEach(entry -> document.add(new Paragraph(String.format("%s: %s", evidenceValueMapping.get(entry.getKey()), getValue(entry.getValue())))));
    }

    private String getValue(EvidenceValue evidenceValue) {
        return Optional.ofNullable(evidenceValue)
                .map(value -> {
                    if (value.getValue() == null || value.getValueType() == null) {
                        return MISSING_OR_FAULTY_DATA;
                    }

                    if (evidenceValue.getValueType().equals(ValueType.BOOLEAN)) {
                        return Boolean.parseBoolean(value.getValue().toString()) ? "ja" : "nei";
                    }

                    return evidenceValue.getValue().toString();
                })
                .orElse(MISSING_OR_FAULTY_DATA);
    }

    private String getSource(EvidenceValue evidenceValue) {
        return Optional.ofNullable(evidenceValue)
                .map(EvidenceValue::getSource)
                .orElse(MISSING_OR_FAULTY_DATA);
    }

    private String getDate(EvidenceValue evidenceValue) {
        return Optional.ofNullable(evidenceValue)
                .map(EvidenceValue::getTimestamp)
                .map(OffsetDateTime::toLocalDate)
                .map(LocalDate::toString)
                .orElse(MISSING_OR_FAULTY_DATA);
    }

    private static final Map<String, String> evidenceValueMapping = Stream.of(
                    // KonkursDrosje
                    new AbstractMap.SimpleImmutableEntry<>("UnderAvvikling", "Under avvikling"),
                    new AbstractMap.SimpleImmutableEntry<>("Konkurs", "Konkurs"),
                    new AbstractMap.SimpleImmutableEntry<>("UnderTvangsavviklingEllerTvangsopplosning", "Under tvangsavvikling eller tvangsoppløsning"),
                    // RestanserDrosje
                    new AbstractMap.SimpleImmutableEntry<>("skattForfaltOgUbetalt", "Skatt forfalt og ubetalt"),
                    new AbstractMap.SimpleImmutableEntry<>("skattRenterOgGebyrer", "Skatt renter og gebyr"),
                    new AbstractMap.SimpleImmutableEntry<>("arbeidsgiveravgiftForfaltOgUbetalt", "Arbeidsgiveravgift forfalt og ubetalt"),
                    new AbstractMap.SimpleImmutableEntry<>("arbeidsgiveravgiftRenterOgGebyrer", "Arbeidsgiveravgift renter og gebyr"),
                    new AbstractMap.SimpleImmutableEntry<>("merverdiavgiftForfaltOgUbetalt", "Merverdiavgift forfalt og ubetalt"),
                    new AbstractMap.SimpleImmutableEntry<>("merverdiavgiftRenterOgGebyrer", "Merverdiavgift renter og gebyr"),
                    new AbstractMap.SimpleImmutableEntry<>("forskuddstrekkForfaltOgUbetalt", "Forskuddstrekk forfalt og ubetalt"),
                    new AbstractMap.SimpleImmutableEntry<>("forskuddstrekkRenterOgGebyrer", "Forskuddstrekk renter og gebyr"),
                    new AbstractMap.SimpleImmutableEntry<>("ansvarskravSkattForfaltOgUbetalt", "Ansvarskrav skatt forfalt og ubetalt"),
                    new AbstractMap.SimpleImmutableEntry<>("ansvarskravSkattRenterOgGebyrer", "Ansvarskrav skatt renter og gebyr"),
                    new AbstractMap.SimpleImmutableEntry<>("ansvarskravMvaForfaltOgUbetalt", "Ansvarskrav mva forfalt og ubetalt"),
                    new AbstractMap.SimpleImmutableEntry<>("ansvarskravMvaRenterOgGebyrer", "Ansvarskrav mva renter og gebyr"),
                    // RestanserV2
                    new AbstractMap.SimpleImmutableEntry<>("arbeidsgiveravgiftForfaltOgUbetalt", "Arbeidsgiveravgift forfalt og ubetalt"),
                    new AbstractMap.SimpleImmutableEntry<>("forskuddstrekkForfaltOgUbetalt", "Forskuddstrekk forfalt og ubetalt"),
                    new AbstractMap.SimpleImmutableEntry<>("forskuddsskattForfaltOgUbetalt", "Forskuddsskatt forfalt og ubetalt"),
                    new AbstractMap.SimpleImmutableEntry<>("restskattForfaltOgUbetalt", "Restskatt forfalt og ubetalt"),
                    new AbstractMap.SimpleImmutableEntry<>("gebyrForfaltOgUbetalt", "Gebyr forfalt og ubetalt"),
                    new AbstractMap.SimpleImmutableEntry<>("merverdiavgiftForfaltOgUbetalt", "Merverdiavgift forfalt og ubetalt"))
            .distinct()
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
}
