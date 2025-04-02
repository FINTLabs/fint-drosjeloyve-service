package no.fint.drosjeloyve.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.fint.altinn.model.AltinnApplication;
import no.fint.altinn.model.ebevis.Evidence;
import no.novari.drosjeloyve.util.CertificateConverter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;


public class CertificateConverterTest {

    @Test
    public void testThatWeCanDoSimpleTaxCertificateConversion() {
        Evidence evidence = new Evidence();
        AltinnApplication application = new AltinnApplication();
        application.setSubject("123456789");
        application.setSubjectName("Radiobilene AS");
        CertificateConverter converter = new CertificateConverter();
        converter.setFontFile("times.ttf");

        byte[] pdf = converter.convertTaxCertificate(evidence, application);

        Assertions.assertNotNull(pdf, "The byte array with the converted tax certificate should not be null!");
    }

    @Test
    public void testThatWeCanDoSimpleBankruptCertificateConversion() {
        Evidence evidence = new Evidence();
        AltinnApplication application = new AltinnApplication();
        application.setSubject("123456789");
        application.setSubjectName("Radiobilene AS");
        CertificateConverter converter = new CertificateConverter();
        converter.setFontFile("times.ttf");

        byte[] pdf = converter.convertBankruptCertificate(evidence, application);

        Assertions.assertNotNull(pdf, "The byte array with the converted bankrupt certificate should not be null!");
    }

    @Test
    public void testThatWeCanDoTaxCertificateConversionWithRestanserDrosje() throws IOException {
        URL file = getClass().getClassLoader().getResource("restanserDrosje.json");
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModules(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Evidence evidence = objectMapper.readValue(file, Evidence.class);

        AltinnApplication application = new AltinnApplication();
        application.setSubject("123456789");
        application.setSubjectName("Radiobilene AS");
        CertificateConverter converter = new CertificateConverter();
        converter.setFontFile("times.ttf");

        byte[] pdf = converter.convertTaxCertificate(evidence, application);
        //Files.write(new File("./RestanserDrosjeTest.pdf").toPath(), pdf);

        Assertions.assertNotNull(pdf, "The byte array with the converted tax certificate should not be null!");
    }

    @Test
    public void testThatWeCanDoTaxCertificateConversionWithRestanserV2() throws IOException {
        URL file = getClass().getClassLoader().getResource("restanserV2.json");
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModules(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Evidence evidence = objectMapper.readValue(file, Evidence.class);

        AltinnApplication application = new AltinnApplication();
        application.setSubject("123456789");
        application.setSubjectName("Radiobilene AS");
        CertificateConverter converter = new CertificateConverter();
        converter.setFontFile("times.ttf");

        byte[] pdf = converter.convertTaxCertificate(evidence, application);
        //Files.write(new File("./RestanserV2Test.pdf").toPath(), pdf);

        Assertions.assertNotNull(pdf, "The byte array with the converted tax certificate should not be null!");
    }

}
