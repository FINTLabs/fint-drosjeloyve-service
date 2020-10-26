package no.fint.drosjeloyve.util;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class CertificateConverterTest {

    @Test
    public void testThatWeCanDoSimpleTaxCertificateConversion() {
        CertificateConverter converter = new CertificateConverter();
        byte[] pdf = converter.convertTaxCertificate();

        assertNotNull("The byte array with the converted tax certificate should not be null!", pdf);
    }

    @Test
    public void testThatWeCanDoSimpleBankruptCertificateConversion() {
        CertificateConverter converter = new CertificateConverter();
        byte[] pdf = converter.convertBankruptCertificate();

        assertNotNull("The byte array with the converted bankrupt certificate should not be null!", pdf);
    }
}
