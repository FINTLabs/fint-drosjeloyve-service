package no.fint.drosjeloyve.factory

import no.fint.drosjeloyve.configuration.OrganisationProperties
import no.fint.drosjeloyve.model.AltinnApplication
import no.fint.model.felles.kompleksedatatyper.Identifikator
import no.fint.model.resource.arkiv.samferdsel.DrosjeloyveResource
import spock.lang.Specification

import java.time.LocalDateTime

class DrosjeloyveResourceFactorySpec extends Specification {

    def "ofBasic returns DrosjeloyveResource"() {
        given:
        def application = new AltinnApplication(subject: 'subject', subjectName: 'subject-name')

        when:
        def resource = DrosjeloyveResourceFactory.ofBasic(application)

        then:
        resource.getOrganisasjonsnummer() == 'subject'
        resource.getTittel() == 'subject-name'
    }

    def "ofComplete returns DrosjeloyveResource"() {
        given:
        def resource = newDrosjeloyveResource()
        def application = newApplication()
        def organisation = newOrganisation()

        when:
        def updatedResource = DrosjeloyveResourceFactory.ofComplete(resource, application, organisation)

        then:
        resource.mappeId.identifikatorverdi == 'mappe-id'
        resource.tittel == 'subject-name'
        resource.organisasjonsnummer == 'subject'

        updatedResource.getJournalpost().size() == 2

        resource.journalpost.first().tittel == 'Drosjeløyvesøknad - subject-name - subject'
        resource.journalpost.first().offentligTittel == 'Drosjeløyvesøknad - subject-name - subject'
        resource.journalpost.first().journalposttype.any { it.href == '${arkiv.kodeverk.journalposttype}/systemid/I' }
        resource.journalpost.first().skjerming.skjermingshjemmel.any { it.href == '${arkiv.noark.skjerming}/systemid/skjermingshjemmel' }
        resource.journalpost.first().skjerming.tilgangsrestriksjon.any { it.href == '${arkiv.noark.tilgang}/systemid/tilgangsrestriksjon' }
        resource.journalpost.first().korrespondansepart.any { it ->
            it.korrespondansepartNavn == 'subject-name' &&
                    it.organisasjonsnummer == 'subject' &&
                    it.adresse.adresselinje == ['address'] &&
                    it.adresse.postnummer == 'post-code' &&
                    it.adresse.poststed == 'postal-area' &&
                    it.kontaktinformasjon.epostadresse == 'email' &&
                    it.kontaktinformasjon.telefonnummer == 'phone' &&
                    it.korrespondanseparttype.any { it.href == '${arkiv.kodeverk.korrespondanseparttype}/systemid/EA' }
        }

        resource.journalpost.first().dokumentbeskrivelse.size() == 2
        resource.journalpost.first().dokumentbeskrivelse.first().tittel == 'Drosjeløyvesøknad - subject-name'
        resource.journalpost.first().dokumentbeskrivelse.first().dokumentstatus.first().href == '${arkiv.kodeverk.dokumentstatus}/systemid/F'
        resource.journalpost.first().dokumentbeskrivelse.first().tilknyttetRegistreringSom.first().href == '${arkiv.kodeverk.tilknyttetregistreringsom}/systemid/H'
        resource.journalpost.first().dokumentbeskrivelse.first().skjerming.skjermingshjemmel.any { it.href == '${arkiv.noark.skjerming}/systemid/soknadsskjema' }
        resource.journalpost.first().dokumentbeskrivelse.first().skjerming.tilgangsrestriksjon.any { it.href == '${arkiv.noark.tilgang}/systemid/soknadsskjema-tilgangsrestriksjon' }
        resource.journalpost.first().dokumentbeskrivelse.first().dokumentobjekt.any { it ->
            it.format == 'PDF' &&
                    it.variantFormat.any { it.href == '${arkiv.kodeverk.variantformat}/systemid/A' } &&
                    it.referanseDokumentfil.any { it.href == '${arkiv.noark.dokumentfil}/systemid/document-id' }
        }
        resource.journalpost.first().dokumentbeskrivelse.last().tittel == 'Konkursattest for foretak'
        resource.journalpost.first().dokumentbeskrivelse.last().dokumentstatus.first().href == '${arkiv.kodeverk.dokumentstatus}/systemid/F'
        resource.journalpost.first().dokumentbeskrivelse.last().tilknyttetRegistreringSom.first().href == '${arkiv.kodeverk.tilknyttetregistreringsom}/systemid/V'
        resource.journalpost.first().dokumentbeskrivelse.last().skjerming.skjermingshjemmel.any { it.href == '${arkiv.noark.skjerming}/systemid/konkursattest' }
        resource.journalpost.first().dokumentbeskrivelse.last().skjerming.tilgangsrestriksjon.any { it.href == '${arkiv.noark.tilgang}/systemid/konkursattest-tilgangsrestriksjon' }
        resource.journalpost.first().dokumentbeskrivelse.last().dokumentobjekt.any { it ->
            it.format == 'PDF' &&
                    it.variantFormat.any { it.href == '${arkiv.kodeverk.variantformat}/systemid/A' } &&
                    it.referanseDokumentfil.any { it.href == '${arkiv.noark.dokumentfil}/systemid/document-id' }
        }

        resource.journalpost.last().tittel == 'Politiattest - subject-name - subject'
        resource.journalpost.last().offentligTittel == 'Politiattest - subject-name - subject'
        resource.journalpost.last().journalposttype.any { it.href == '${arkiv.kodeverk.journalposttype}/systemid/I' }
        resource.journalpost.last().skjerming.skjermingshjemmel.any { it.href == '${arkiv.noark.skjerming}/systemid/skjermingshjemmel' }
        resource.journalpost.last().skjerming.tilgangsrestriksjon.any { it.href == '${arkiv.noark.tilgang}/systemid/tilgangsrestriksjon' }
        resource.journalpost.last().korrespondansepart.any { it ->
            it.korrespondansepartNavn == 'subject-name' &&
                    it.organisasjonsnummer == 'subject' &&
                    it.adresse.adresselinje == ['address'] &&
                    it.adresse.postnummer == 'post-code' &&
                    it.adresse.poststed == 'postal-area' &&
                    it.kontaktinformasjon.epostadresse == 'email' &&
                    it.kontaktinformasjon.telefonnummer == 'phone' &&
                    it.korrespondanseparttype.any { it.href == '${arkiv.kodeverk.korrespondanseparttype}/systemid/EA' }
        }

        resource.journalpost.last().dokumentbeskrivelse.size() == 2
        resource.journalpost.last().dokumentbeskrivelse.first().tittel == 'Politiattest for foretaket'
        resource.journalpost.last().dokumentbeskrivelse.first().dokumentstatus.first().href == '${arkiv.kodeverk.dokumentstatus}/systemid/F'
        resource.journalpost.last().dokumentbeskrivelse.first().tilknyttetRegistreringSom.first().href == '${arkiv.kodeverk.tilknyttetregistreringsom}/systemid/H'
        resource.journalpost.last().dokumentbeskrivelse.first().skjerming.skjermingshjemmel.any { it.href == '${arkiv.noark.skjerming}/systemid/politiattest' }
        resource.journalpost.last().dokumentbeskrivelse.first().skjerming.tilgangsrestriksjon.any { it.href == '${arkiv.noark.tilgang}/systemid/politiattest-tilgangsrestriksjon' }
        resource.journalpost.last().dokumentbeskrivelse.first().dokumentobjekt.any { it ->
            it.format == 'PDF' &&
                    it.variantFormat.any { it.href == '${arkiv.kodeverk.variantformat}/systemid/A' } &&
                    it.referanseDokumentfil.any { it.href == '${arkiv.noark.dokumentfil}/systemid/document-id' }
        }

        resource.journalpost.last().dokumentbeskrivelse.last().tittel == 'Politiattest for innehaver/daglig leder'
        resource.journalpost.last().dokumentbeskrivelse.last().dokumentstatus.first().href == '${arkiv.kodeverk.dokumentstatus}/systemid/F'
        resource.journalpost.last().dokumentbeskrivelse.last().tilknyttetRegistreringSom.first().href == '${arkiv.kodeverk.tilknyttetregistreringsom}/systemid/V'
        resource.journalpost.last().dokumentbeskrivelse.last().skjerming.skjermingshjemmel.any { it.href == '${arkiv.noark.skjerming}/systemid/politiattest' }
        resource.journalpost.last().dokumentbeskrivelse.last().skjerming.tilgangsrestriksjon.any { it.href == '${arkiv.noark.tilgang}/systemid/politiattest-tilgangsrestriksjon' }
        resource.journalpost.last().dokumentbeskrivelse.last().dokumentobjekt.any { it ->
            it.format == 'PDF' &&
                    it.variantFormat.any { it.href == '${arkiv.kodeverk.variantformat}/systemid/A' } &&
                    it.referanseDokumentfil.any { it.href == '${arkiv.noark.dokumentfil}/systemid/document-id' }
        }
    }

    def newApplication() {
        return new AltinnApplication(
                archivedDate: LocalDateTime.parse('2020-01-01T00:00:00'),
                subject: 'subject',
                subjectName: 'subject-name',
                businessAddress: new AltinnApplication.Address(
                        address: 'address',
                        postCode: 'post-code',
                        postalArea: 'postal-area'
                ),
                phone: 'phone',
                email: 'email',
                form: new AltinnApplication.Form(documentId: 'document-id'),
                attachments: [(1): new AltinnApplication.Attachment(attachmentTypeName: 'PolitiattestForForetaket', attachmentTypeNameLanguage: 'Politiattest for foretaket', fileName: 'filename.pdf', documentId: 'document-id'),
                              (2): new AltinnApplication.Attachment(attachmentTypeName: 'PolitiattestInnehaverDagligLeder', attachmentTypeNameLanguage: 'Politiattest for innehaver/daglig leder', fileName: 'filename.pdf', documentId: 'document-id')],
                consents: [('konkurs-drosje'): new AltinnApplication.Consent(documentId: 'document-id', evidenceCodeName: 'KonkursDrosje')]
        )
    }

    def newOrganisation() {
        return new OrganisationProperties.Organisation(
                skjermingshjemmel: 'skjermingshjemmel',
                tilgangsrestriksjon: 'tilgangsrestriksjon',
                variantformat: 'A',
                politiattest: new OrganisationProperties.LegalBasis(skjermingshjemmel: 'politiattest', tilgangsrestriksjon: 'politiattest-tilgangsrestriksjon'),
                skatteattest: new OrganisationProperties.LegalBasis(skjermingshjemmel: 'skatteattest', tilgangsrestriksjon: 'skatteattest-tilgangsrestriksjon'),
                konkursattest: new OrganisationProperties.LegalBasis(skjermingshjemmel: 'konkursattest', tilgangsrestriksjon: 'konkursattest-tilgangsrestriksjon'),
                domForelegg: new OrganisationProperties.LegalBasis(skjermingshjemmel: 'dom-forelegg', tilgangsrestriksjon: 'dom-forelegg-tilgangsrestriksjon'),
                soknadsskjema: new OrganisationProperties.LegalBasis(skjermingshjemmel: 'soknadsskjema', tilgangsrestriksjon: 'soknadsskjema-tilgangsrestriksjon')
        )
    }

    def newDrosjeloyveResource() {
        return new DrosjeloyveResource(
                mappeId: new Identifikator(identifikatorverdi: 'mappe-id'),
                tittel: 'subject-name',
                organisasjonsnummer: 'subject'
        )
    }
}