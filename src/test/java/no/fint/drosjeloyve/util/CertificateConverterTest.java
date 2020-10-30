package no.fint.drosjeloyve.util;

import no.fint.drosjeloyve.model.AltinnApplication;
import no.fint.drosjeloyve.model.ebevis.Evidence;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

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

        assertNotNull("The byte array with the converted tax certificate should not be null!", pdf);
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

        assertNotNull("The byte array with the converted bankrupt certificate should not be null!", pdf);
    }
}
