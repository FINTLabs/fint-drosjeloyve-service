package no.fint.drosjeloyve.factory;

import lombok.extern.slf4j.Slf4j;
import no.fint.altinn.model.AltinnApplication;
import no.fint.drosjeloyve.configuration.OrganisationProperties;
import no.fint.model.arkiv.kodeverk.*;
import no.fint.model.arkiv.noark.Dokumentfil;
import no.fint.model.arkiv.noark.Skjerming;
import no.fint.model.arkiv.noark.Tilgang;
import no.fint.model.felles.kompleksedatatyper.Kontaktinformasjon;
import no.fint.model.resource.Link;
import no.fint.model.resource.arkiv.noark.*;
import no.fint.model.resource.arkiv.samferdsel.SoknadDrosjeloyveResource;
import no.fint.model.resource.felles.kompleksedatatyper.AdresseResource;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

@Slf4j
public class DrosjeloyveResourceFactory {
    private static final Set<String> POLICE_CERTIFICATES = new HashSet<>(Arrays.asList("PolitiattestForForetaket",
            "PolitiattestInnehaverDagligLeder", "PolitiattestInnehaver", "PolitiattestTransportleder", "KopiAvDomForelegg"));

    private static final Set<String> BANKRUPTCY_ARREARS_MANAGER = new HashSet<>(Arrays.asList("KonkursattestInnehaverDagligLeder",
            "SkatteattestInnehaverDagligLeder"));

    private static final Set<String> BANKRUPTCY_ARREARS_COMPANY = new HashSet<>(Arrays.asList("RestanserDrosje", "RestanserV2", "KonkursDrosje"));

    public static final String BANKRUPTCY = "KonkursDrosje";
    public static final Set<String> ARREARS = new HashSet<>(Arrays.asList("RestanserDrosje", "RestanserV2"));

    public static final String DOCUMENTATION_PROFESSIONAL_COMPETENCE = "DokumentasjonFagkompetanse";

    public static SoknadDrosjeloyveResource ofBasic(AltinnApplication application) {
        SoknadDrosjeloyveResource resource = new SoknadDrosjeloyveResource();

        resource.setOrganisasjonsnummer(application.getSubject());
        resource.setOrganisasjonsnavn(application.getSubjectName());
        resource.setTittel(application.getSubjectName());

        return resource;
    }

    public static SoknadDrosjeloyveResource ofComplete(SoknadDrosjeloyveResource resource, AltinnApplication application, OrganisationProperties.Organisation organisation) {
        resource.setJournalpost(Arrays.asList(application(application, organisation), policeCertificates(application, organisation)));

        return resource;
    }

    private static JournalpostResource application(AltinnApplication application, OrganisationProperties.Organisation organisation) {
        JournalpostResource resource = new JournalpostResource();

        String title = String.format("Drosjeløyvesøknad - %s - %s", application.getSubjectName(), application.getSubject());
        log.debug("Journalpost title: {}", title);

        setJournalPostDefaults(application, organisation, resource, title);

        DokumentbeskrivelseResource dokumentbeskrivelseResource = toDokumentbeskrivelseResource(application, organisation);

        resource.getDokumentbeskrivelse().add(dokumentbeskrivelseResource);

        application.getAttachments().values().stream()
                .filter(attachment -> BANKRUPTCY_ARREARS_MANAGER.contains(attachment.getAttachmentTypeName()) || attachment.getAttachmentTypeName().equals(DOCUMENTATION_PROFESSIONAL_COMPETENCE))
                .map(attachment -> DrosjeloyveResourceFactory.toDokumentbeskrivelseResource(attachment, organisation))
                .forEach(resource.getDokumentbeskrivelse()::add);

        application.getConsents().values().stream()
                .filter(consent -> BANKRUPTCY_ARREARS_COMPANY.contains(consent.getEvidenceCodeName()))
                .map(consent -> DrosjeloyveResourceFactory.toDokumentbeskrivelseResource(consent, organisation))
                .forEach(resource.getDokumentbeskrivelse()::add);

//        application.getAttachments().values().stream()
//                .filter(attachment -> attachment.getAttachmentTypeName().equals(DOCUMENTATION_PROFESSIONAL_COMPETENCE))
//                .map(attachment -> DrosjeloyveResourceFactory.toDokumentbeskrivelseResource(attachment, organisation))
//                .forEach(resource.getDokumentbeskrivelse()::add);

        resource.getDokumentbeskrivelse().stream()
                .map(DokumentbeskrivelseResource::getTilknyttetRegistreringSom)
                .flatMap(List::stream)
                .findFirst()
                .ifPresent(link -> link.setVerdi(Link.with(TilknyttetRegistreringSom.class, "systemid", "H").getHref()));


        return resource;
    }

    private static JournalpostResource policeCertificates(AltinnApplication application, OrganisationProperties.Organisation organisation) {
        JournalpostResource resource = new JournalpostResource();

        String title = String.format("Politiattest - %s - %s", application.getSubjectName(), application.getSubject());

        setJournalPostDefaults(application, organisation, resource, title);

        application.getAttachments().values().stream()
                .filter(attachment -> POLICE_CERTIFICATES.contains(attachment.getAttachmentTypeName()))
                .map(attachment -> DrosjeloyveResourceFactory.toDokumentbeskrivelseResource(attachment, organisation))
                .forEach(resource.getDokumentbeskrivelse()::add);

        resource.getDokumentbeskrivelse().stream()
                .map(DokumentbeskrivelseResource::getTilknyttetRegistreringSom)
                .flatMap(List::stream)
                .findFirst()
                .ifPresent(link -> link.setVerdi(Link.with(TilknyttetRegistreringSom.class, "systemid", "H").getHref()));

        return resource;
    }

    private static void setJournalPostDefaults(AltinnApplication application, OrganisationProperties.Organisation organisation, JournalpostResource resource, String title) {
        resource.addJournalposttype(Link.with(JournalpostType.class, "systemid", "I"));

        resource.setTittel(title);
        resource.setOffentligTittel(title);

        SkjermingResource skjermingResource = new SkjermingResource();

        if (!organisation.getSkjermingshjemmel().equals("none")) {
            skjermingResource.addSkjermingshjemmel(Link.with(Skjerming.class, "systemid", organisation.getSkjermingshjemmel()));
        }

        if (!organisation.getTilgangsrestriksjon().equals("none")) {
            skjermingResource.addTilgangsrestriksjon(Link.with(Tilgang.class, "systemid", organisation.getTilgangsrestriksjon()));
        }

        if (!skjermingResource.getLinks().isEmpty()) {
            resource.setSkjerming(skjermingResource);
        }

        KorrespondansepartResource korrespondansepartResource = new KorrespondansepartResource();
        korrespondansepartResource.setKorrespondansepartNavn(application.getSubjectName());
        korrespondansepartResource.setOrganisasjonsnummer(application.getSubject());

        Optional.ofNullable(application.getBusinessAddress())
                .ifPresent(address -> {
                    AdresseResource adresseResource = new AdresseResource();
                    adresseResource.setAdresselinje(Collections.singletonList(address.getAddress()));
                    adresseResource.setPostnummer(address.getPostCode());
                    adresseResource.setPoststed(address.getPostalArea());
                    korrespondansepartResource.setAdresse(adresseResource);
                });

        Kontaktinformasjon kontaktinformasjon = new Kontaktinformasjon();
        kontaktinformasjon.setEpostadresse(application.getEmail());
        kontaktinformasjon.setTelefonnummer(application.getPhone());
        korrespondansepartResource.setKontaktinformasjon(kontaktinformasjon);

        korrespondansepartResource.addKorrespondanseparttype(Link.with(KorrespondansepartType.class, "systemid", "EA"));

        resource.setKorrespondansepart(Collections.singletonList(korrespondansepartResource));

        resource.setDokumentbeskrivelse(new ArrayList<>());
    }

    private static DokumentbeskrivelseResource toDokumentbeskrivelseResource(AltinnApplication application, OrganisationProperties.Organisation organisation) {
        DokumentbeskrivelseResource resource = new DokumentbeskrivelseResource();
        SkjermingResource skjermingResource = new SkjermingResource();

        if (!organisation.getSoknadsskjema().getSkjermingshjemmel().equals("none")) {
            skjermingResource.addSkjermingshjemmel(Link.with(Skjerming.class, "systemid", organisation.getSoknadsskjema().getSkjermingshjemmel()));
        }

        if (!organisation.getSoknadsskjema().getTilgangsrestriksjon().equals("none")) {
            skjermingResource.addTilgangsrestriksjon(Link.with(Tilgang.class, "systemid", organisation.getSoknadsskjema().getTilgangsrestriksjon()));
        }

        if (!skjermingResource.getLinks().isEmpty()) {
            resource.setSkjerming(skjermingResource);
        }

        resource.setTittel(String.format("Drosjeløyvesøknad - %s", application.getSubjectName()));
        log.debug("Dokumentbeskrivelse, tittel: {}", resource.getTittel());

        resource.addTilknyttetRegistreringSom(Link.with(TilknyttetRegistreringSom.class, "systemid", "H"));

        resource.addDokumentstatus(Link.with(DokumentStatus.class, "systemid", "F"));

        DokumentobjektResource dokumentobjektResource = getDokumentobjektResource("PDF", application.getForm().getDocumentId(), organisation.getVariantformat());
        resource.setDokumentobjekt(Collections.singletonList(dokumentobjektResource));

        return resource;
    }

    private static DokumentbeskrivelseResource toDokumentbeskrivelseResource(AltinnApplication.Attachment attachment, OrganisationProperties.Organisation organisation) {
        DokumentbeskrivelseResource resource = new DokumentbeskrivelseResource();
        SkjermingResource skjermingResource = new SkjermingResource();

        if (attachment.getAttachmentTypeName().startsWith("Politiattest")) {
            if (!organisation.getPolitiattest().getSkjermingshjemmel().equals("none")) {
                skjermingResource.addSkjermingshjemmel(Link.with(Skjerming.class, "systemid", organisation.getPolitiattest().getSkjermingshjemmel()));
            }

            if (!organisation.getPolitiattest().getTilgangsrestriksjon().equals("none")) {
                skjermingResource.addTilgangsrestriksjon(Link.with(Tilgang.class, "systemid", organisation.getPolitiattest().getTilgangsrestriksjon()));
            }
        } else if (attachment.getAttachmentTypeName().equals("SkatteattestInnehaverDagligLeder")) {
            if (!organisation.getSkatteattest().getSkjermingshjemmel().equals("none")) {
                skjermingResource.addSkjermingshjemmel(Link.with(Skjerming.class, "systemid", organisation.getSkatteattest().getSkjermingshjemmel()));
            }

            if (!organisation.getSkatteattest().getTilgangsrestriksjon().equals("none")) {
                skjermingResource.addTilgangsrestriksjon(Link.with(Tilgang.class, "systemid", organisation.getSkatteattest().getTilgangsrestriksjon()));
            }
        } else if (attachment.getAttachmentTypeName().equals("KonkursattestInnehaverDagligLeder")) {
            if (!organisation.getKonkursattest().getSkjermingshjemmel().equals("none")) {
                skjermingResource.addSkjermingshjemmel(Link.with(Skjerming.class, "systemid", organisation.getKonkursattest().getSkjermingshjemmel()));
            }

            if (!organisation.getKonkursattest().getTilgangsrestriksjon().equals("none")) {
                skjermingResource.addTilgangsrestriksjon(Link.with(Tilgang.class, "systemid", organisation.getKonkursattest().getTilgangsrestriksjon()));
            }
        } else if (attachment.getAttachmentTypeName().equals("DokumentasjonFagkompetanse")) {
            log.debug("Woohoo, we found some documentation of the drivers professional competence!");
            if (!organisation.getKonkursattest().getSkjermingshjemmel().equals("none")) {
                skjermingResource.addSkjermingshjemmel(Link.with(Skjerming.class, "systemid", organisation.getFagkompetanse().getSkjermingshjemmel()));
            }

            if (!organisation.getKonkursattest().getTilgangsrestriksjon().equals("none")) {
                skjermingResource.addTilgangsrestriksjon(Link.with(Tilgang.class, "systemid", organisation.getFagkompetanse().getTilgangsrestriksjon()));
            }
        } else if (attachment.getAttachmentTypeName().equals("KopiAvDomForelegg")) {
            if (!organisation.getDomForelegg().getSkjermingshjemmel().equals("none")) {
                skjermingResource.addSkjermingshjemmel(Link.with(Skjerming.class, "systemid", organisation.getDomForelegg().getSkjermingshjemmel()));
            }

            if (!organisation.getDomForelegg().getTilgangsrestriksjon().equals("none")) {
                skjermingResource.addTilgangsrestriksjon(Link.with(Tilgang.class, "systemid", organisation.getDomForelegg().getTilgangsrestriksjon()));
            }
        }

        if (!skjermingResource.getLinks().isEmpty()) {
            resource.setSkjerming(skjermingResource);
        }

        resource.setTittel(attachment.getAttachmentTypeNameLanguage());
        log.debug("Dokumentbeskrivelse, tittel: {}", resource.getTittel());

        resource.addDokumentstatus(Link.with(DokumentStatus.class, "systemid", "F"));

        resource.addTilknyttetRegistreringSom(Link.with(TilknyttetRegistreringSom.class, "systemid", "V"));

        String format = StringUtils.substringAfter(attachment.getFileName(), ".").toUpperCase();

        DokumentobjektResource dokumentobjektResource = getDokumentobjektResource(format, attachment.getDocumentId(), organisation.getVariantformat());
        resource.setDokumentobjekt(Collections.singletonList(dokumentobjektResource));

        return resource;
    }

    private static DokumentbeskrivelseResource toDokumentbeskrivelseResource(AltinnApplication.Consent consent, OrganisationProperties.Organisation organisation) {
        DokumentbeskrivelseResource resource = new DokumentbeskrivelseResource();
        SkjermingResource skjermingResource = new SkjermingResource();

        if (ARREARS.contains(consent.getEvidenceCodeName())) {
            resource.setTittel("Skatteattest for foretak");

            if (!organisation.getSkatteattest().getSkjermingshjemmel().equals("none")) {
                skjermingResource.addSkjermingshjemmel(Link.with(Skjerming.class, "systemid", organisation.getSkatteattest().getSkjermingshjemmel()));
            }

            if (!organisation.getSkatteattest().getSkjermingshjemmel().equals("none")) {
                skjermingResource.addTilgangsrestriksjon(Link.with(Tilgang.class, "systemid", organisation.getSkatteattest().getTilgangsrestriksjon()));
            }
        } else if (consent.getEvidenceCodeName().equals(BANKRUPTCY)){
            resource.setTittel("Konkursattest for foretak");

            if (!organisation.getKonkursattest().getSkjermingshjemmel().equals("none")) {
                skjermingResource.addSkjermingshjemmel(Link.with(Skjerming.class, "systemid", organisation.getKonkursattest().getSkjermingshjemmel()));
            }

            if (!organisation.getKonkursattest().getSkjermingshjemmel().equals("none")) {
                skjermingResource.addTilgangsrestriksjon(Link.with(Tilgang.class, "systemid", organisation.getKonkursattest().getTilgangsrestriksjon()));
            }
        }

        if (!skjermingResource.getLinks().isEmpty()) {
            resource.setSkjerming(skjermingResource);
        }

        resource.addDokumentstatus(Link.with(DokumentStatus.class, "systemid", "F"));

        resource.addTilknyttetRegistreringSom(Link.with(TilknyttetRegistreringSom.class, "systemid", "V"));

        DokumentobjektResource dokumentobjektResource = getDokumentobjektResource("PDF", consent.getDocumentId(), organisation.getVariantformat());
        resource.setDokumentobjekt(Collections.singletonList(dokumentobjektResource));

        return resource;
    }

    private static DokumentobjektResource getDokumentobjektResource(String format, String id, String variantFormat) {
        DokumentobjektResource resource = new DokumentobjektResource();

        resource.setFormat(format);
        resource.addVariantFormat(Link.with(Variantformat.class, "systemid", variantFormat));
        resource.addReferanseDokumentfil(Link.with(Dokumentfil.class, "systemid", id));

        return resource;
    }
}
