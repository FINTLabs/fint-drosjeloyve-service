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

import java.util.*;

public class DrosjeloyveResourceFactory {

    public static DrosjeloyveResource ofBasic(AltinnApplication application) {
        DrosjeloyveResource resource = new DrosjeloyveResource();
        resource.setOrganisasjonsnummer(application.getSubject());
        resource.setTittel(application.getSubjectName());

        return resource;
    }

    public static DrosjeloyveResource ofComplete(DrosjeloyveResource resource, AltinnApplication application, OrganisationProperties.Organisation organisation) {
        resource.setJournalpost(Arrays.asList(ofSensitive(application, organisation), ofLessSensitive(application, organisation)));

        return resource;
    }

    public static JournalpostResource ofSensitive(AltinnApplication application, OrganisationProperties.Organisation organisation) {
        JournalpostResource journalpostResource = new JournalpostResource();

        journalpostResource.addJournalposttype(Link.with(JournalpostType.class, "systemid", "I"));

        String title = String.format("Politiattest - %s", application.getArchivedDate().toLocalDate());

        journalpostResource.setTittel(title);
        journalpostResource.setOffentligTittel(title);

        SkjermingResource skjermingResource = new SkjermingResource();

        skjermingResource.addSkjermingshjemmel(Link.with(Skjerming.class, "systemid", organisation.getSkjermingshjemmel()));
        skjermingResource.addTilgangsrestriksjon(Link.with(Tilgang.class, "systemid", organisation.getTilgangsrestriksjon()));

        journalpostResource.setSkjerming(skjermingResource);

        KorrespondansepartResource korrespondansepartResource = new KorrespondansepartResource();

        korrespondansepartResource.setKorrespondansepartNavn(application.getSubjectName());
        korrespondansepartResource.setOrganisasjonsnummer(application.getSubject());
        korrespondansepartResource.addKorrespondanseparttype(Link.with(KorrespondansepartType.class, "systemid", "EA"));

        journalpostResource.setKorrespondansepart(Collections.singletonList(korrespondansepartResource));

        journalpostResource.setDokumentbeskrivelse(new ArrayList<>());

        application.getAttachments().values().stream()
                .filter(attachment -> attachment.getAttachmentTypeName().startsWith("Kopi") || attachment.getAttachmentType().startsWith("Politiattest"))
                .map(attachment -> {
                    DokumentbeskrivelseResource dokumentbeskrivelseResource = new DokumentbeskrivelseResource();

                    dokumentbeskrivelseResource.setTittel(attachment.getAttachmentTypeNameLanguage());

                    DokumentobjektResource dokumentobjektResource = new DokumentobjektResource();

                    dokumentobjektResource.setFormat(attachment.getAttachmentType());
                    dokumentobjektResource.addVariantFormat(Link.with(Variantformat.class, "systemid", "A"));
                    dokumentobjektResource.addReferanseDokumentfil(Link.with(Dokumentfil.class, "systemid", attachment.getDocumentId()));

                    dokumentbeskrivelseResource.setDokumentobjekt(Collections.singletonList(dokumentobjektResource));

                    return dokumentbeskrivelseResource;
                })
                .forEach(journalpostResource.getDokumentbeskrivelse()::add);

        return journalpostResource;
    }

    public static JournalpostResource ofLessSensitive(AltinnApplication application, OrganisationProperties.Organisation organisation) {
        JournalpostResource journalpostResource = new JournalpostResource();

        journalpostResource.addJournalposttype(Link.with(JournalpostType.class, "systemid", "I"));

        String title = String.format("Drosjeløyvesøknad - %s", application.getArchivedDate().toLocalDate());

        journalpostResource.setTittel(title);
        journalpostResource.setOffentligTittel(title);

        SkjermingResource skjermingResource = new SkjermingResource();

        skjermingResource.addSkjermingshjemmel(Link.with(Skjerming.class, "systemid", organisation.getSkjermingshjemmel()));
        skjermingResource.addTilgangsrestriksjon(Link.with(Tilgang.class, "systemid", organisation.getTilgangsrestriksjon()));

        journalpostResource.setSkjerming(skjermingResource);

        KorrespondansepartResource korrespondansepartResource = new KorrespondansepartResource();

        korrespondansepartResource.setKorrespondansepartNavn(application.getSubjectName());
        korrespondansepartResource.setOrganisasjonsnummer(application.getSubject());
        korrespondansepartResource.addKorrespondanseparttype(Link.with(KorrespondansepartType.class, "systemid", "EA"));

        journalpostResource.setKorrespondansepart(Collections.singletonList(korrespondansepartResource));

        journalpostResource.setDokumentbeskrivelse(new ArrayList<>());

        application.getAttachments().values().stream()
                .filter(attachment -> attachment.getAttachmentTypeName().startsWith("Konkursattest") || attachment.getAttachmentType().startsWith("Skatteattest"))
                .map(attachment -> {
                    DokumentbeskrivelseResource dokumentbeskrivelseResource = new DokumentbeskrivelseResource();

                    dokumentbeskrivelseResource.setTittel(attachment.getAttachmentTypeNameLanguage());

                    DokumentobjektResource dokumentobjektResource = new DokumentobjektResource();

                    dokumentobjektResource.setFormat(attachment.getAttachmentType());
                    dokumentobjektResource.addVariantFormat(Link.with(Variantformat.class, "systemid", "A"));
                    dokumentobjektResource.addReferanseDokumentfil(Link.with(Dokumentfil.class, "systemid", attachment.getDocumentId()));

                    dokumentbeskrivelseResource.setDokumentobjekt(Collections.singletonList(dokumentobjektResource));

                    return dokumentbeskrivelseResource;
                }).forEach(journalpostResource.getDokumentbeskrivelse()::add);

        application.getConsents().values().stream()
                .map(consent -> {
                    DokumentbeskrivelseResource dokumentbeskrivelseResource = new DokumentbeskrivelseResource();

                    if (consent.getEvidenceCodeName().equals("RestanserDrosje")) {
                        dokumentbeskrivelseResource.setTittel("Skatteattest for foretak");
                    } else {
                        dokumentbeskrivelseResource.setTittel("Konkursattest for foretak");
                    }

                    DokumentobjektResource dokumentobjektResource = new DokumentobjektResource();

                    dokumentobjektResource.setFormat("application/pdf");
                    dokumentobjektResource.addVariantFormat(Link.with(Variantformat.class, "systemid", "A"));
                    dokumentobjektResource.addReferanseDokumentfil(Link.with(Dokumentfil.class, "systemid", consent.getDocumentId()));

                    dokumentbeskrivelseResource.setDokumentobjekt(Collections.singletonList(dokumentobjektResource));

                    return dokumentbeskrivelseResource;
                }).forEach(journalpostResource.getDokumentbeskrivelse()::add);


        DokumentbeskrivelseResource dokumentbeskrivelseResource = new DokumentbeskrivelseResource();

        dokumentbeskrivelseResource.setTittel("Altinn søknadsskjema");

        DokumentobjektResource dokumentobjektResource = new DokumentobjektResource();

        dokumentobjektResource.setFormat("application/pdf");
        dokumentobjektResource.addVariantFormat(Link.with(Variantformat.class, "systemid", "A"));
        dokumentobjektResource.addReferanseDokumentfil(Link.with(Dokumentfil.class, "systemid", application.getForm().getDocumentId()));

        journalpostResource.getDokumentbeskrivelse().add(dokumentbeskrivelseResource);

        return journalpostResource;
    }
}
