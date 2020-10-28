package no.fint.drosjeloyve.factory;

import no.fint.drosjeloyve.configuration.OrganisationProperties;
import no.fint.drosjeloyve.model.AltinnApplication;
import no.fint.model.arkiv.kodeverk.JournalpostType;
import no.fint.model.arkiv.kodeverk.KorrespondansepartType;
import no.fint.model.arkiv.kodeverk.Variantformat;
import no.fint.model.arkiv.noark.Dokumentfil;
import no.fint.model.arkiv.noark.Skjerming;
import no.fint.model.arkiv.noark.Tilgang;
import no.fint.model.resource.Link;
import no.fint.model.resource.arkiv.noark.*;
import no.fint.model.resource.arkiv.samferdsel.DrosjeloyveResource;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class DrosjeloyveResourceFactory {
    private static final Set<String> POLICE_CERTIFICATES = new HashSet<>(Arrays.asList("PolitiattestForForetaket",
            "PolitiattestInnehaverDagligLeder", "PolitiattestTransportleder", "KopiAvDomForelegg"));

    private static final Set<String> BANKRUPTCY_ARREARS_MANAGER = new HashSet<>(Arrays.asList("KonkursattestInnehaverDagligLeder",
            "SkatteattestInnehaverDagligLeder"));

    private static final Set<String> BANKRUPTCY_ARREARS_COMPANY = new HashSet<>(Arrays.asList("RestanserDrosje", "KonkursDrosje"));

    public static DrosjeloyveResource ofBasic(AltinnApplication application) {
        DrosjeloyveResource resource = new DrosjeloyveResource();

        resource.setOrganisasjonsnummer(application.getSubject());
        resource.setTittel(application.getSubjectName());

        return resource;
    }

    public static DrosjeloyveResource ofComplete(DrosjeloyveResource resource, AltinnApplication application, OrganisationProperties.Organisation organisation) {
        if (resource.getJournalpost() == null) {
            resource.setJournalpost(new ArrayList<>());
        }

        resource.getJournalpost().addAll(Arrays.asList(application(application, organisation), policeCertificates(application, organisation)));

        return resource;
    }

    private static JournalpostResource application(AltinnApplication application, OrganisationProperties.Organisation organisation) {
        JournalpostResource resource = new JournalpostResource();

        String title = String.format("Drosjeløyvesøknad - %s - %s", application.getSubjectName(), application.getArchivedDate().toLocalDate());

        setDefaults(application, organisation, resource, title);

        DokumentbeskrivelseResource dokumentbeskrivelseResource = toDokumentbeskrivelseResource(application.getForm());

        resource.getDokumentbeskrivelse().add(dokumentbeskrivelseResource);

        application.getAttachments().values().stream()
                .filter(attachment -> BANKRUPTCY_ARREARS_MANAGER.contains(attachment.getAttachmentTypeName()))
                .map(DrosjeloyveResourceFactory::toDokumentbeskrivelseResource)
                .forEach(resource.getDokumentbeskrivelse()::add);

        application.getConsents().values().stream()
                .filter(consent -> BANKRUPTCY_ARREARS_COMPANY.contains(consent.getEvidenceCodeName()))
                .map(DrosjeloyveResourceFactory::toDokumentbeskrivelseResource)
                .forEach(resource.getDokumentbeskrivelse()::add);

        return resource;
    }

    private static JournalpostResource policeCertificates(AltinnApplication application, OrganisationProperties.Organisation organisation) {
        JournalpostResource resource = new JournalpostResource();

        String title = String.format("Politiattest - %s - %s", application.getSubjectName(), application.getArchivedDate().toLocalDate());

        setDefaults(application, organisation, resource, title);

        application.getAttachments().values().stream()
                .filter(attachment -> POLICE_CERTIFICATES.contains(attachment.getAttachmentTypeName()))
                .map(DrosjeloyveResourceFactory::toDokumentbeskrivelseResource)
                .forEach(resource.getDokumentbeskrivelse()::add);

        return resource;
    }

    private static void setDefaults(AltinnApplication application, OrganisationProperties.Organisation organisation, JournalpostResource resource, String title) {
        resource.addJournalposttype(Link.with(JournalpostType.class, "systemid", "I"));

        resource.setTittel(title);
        resource.setOffentligTittel(title);

        SkjermingResource skjermingResource = new SkjermingResource();
        skjermingResource.addSkjermingshjemmel(Link.with(Skjerming.class, "systemid", organisation.getSkjermingshjemmel()));
        skjermingResource.addTilgangsrestriksjon(Link.with(Tilgang.class, "systemid", organisation.getTilgangsrestriksjon()));

        resource.setSkjerming(skjermingResource);

        KorrespondansepartResource korrespondansepartResource = new KorrespondansepartResource();
        korrespondansepartResource.setKorrespondansepartNavn(application.getSubjectName());
        korrespondansepartResource.setOrganisasjonsnummer(application.getSubject());
        korrespondansepartResource.addKorrespondanseparttype(Link.with(KorrespondansepartType.class, "systemid", "EA"));

        resource.setKorrespondansepart(Collections.singletonList(korrespondansepartResource));

        resource.setDokumentbeskrivelse(new ArrayList<>());
    }

    private static DokumentbeskrivelseResource toDokumentbeskrivelseResource(AltinnApplication.Attachment attachment) {
        DokumentbeskrivelseResource resource = new DokumentbeskrivelseResource();

        resource.setTittel(attachment.getAttachmentTypeNameLanguage());

        String format = StringUtils.substringAfter(attachment.getFileName(), ".").toUpperCase();

        DokumentobjektResource dokumentobjektResource = getDokumentobjektResource(format, attachment.getDocumentId());

        resource.setDokumentobjekt(Collections.singletonList(dokumentobjektResource));

        return resource;
    }

    private static DokumentbeskrivelseResource toDokumentbeskrivelseResource(AltinnApplication.Consent consent) {
        DokumentbeskrivelseResource resource = new DokumentbeskrivelseResource();

        if (consent.getEvidenceCodeName().equals("RestanserDrosje")) {
            resource.setTittel("Skatteattest for foretak");
        } else {
            resource.setTittel("Konkursattest for foretak");
        }

        DokumentobjektResource dokumentobjektResource = getDokumentobjektResource("PDF", consent.getDocumentId());

        resource.setDokumentobjekt(Collections.singletonList(dokumentobjektResource));

        return resource;
    }

    private static DokumentbeskrivelseResource toDokumentbeskrivelseResource(AltinnApplication.Form form) {
        DokumentbeskrivelseResource resource = new DokumentbeskrivelseResource();

        resource.setTittel("Altinn søknadsskjema");

        DokumentobjektResource dokumentobjektResource = getDokumentobjektResource("PDF", form.getDocumentId());

        resource.setDokumentobjekt(Collections.singletonList(dokumentobjektResource));

        return resource;
    }

    private static DokumentobjektResource getDokumentobjektResource(String format, String id) {
        DokumentobjektResource resource = new DokumentobjektResource();

        resource.setFormat(format);
        resource.addVariantFormat(Link.with(Variantformat.class, "systemid", "A"));
        resource.addReferanseDokumentfil(Link.with(Dokumentfil.class, "systemid", id));

        return resource;
    }
}
