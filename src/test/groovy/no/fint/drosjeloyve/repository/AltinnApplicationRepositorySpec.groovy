package no.fint.drosjeloyve.repository

import no.fint.drosjeloyve.model.AltinnApplication
import no.fint.drosjeloyve.model.AltinnApplicationStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.mongodb.repository.Query
import spock.lang.Specification

import java.time.LocalDateTime

@DataMongoTest
class AltinnApplicationRepositorySpec extends Specification {

    @Autowired
    AltinnApplicationRepository repository

    LocalDateTime now = LocalDateTime.of(1970,01,01,00,00,00)

    void setup() {
        repository.saveAll(Arrays.asList(
                new AltinnApplication(archiveReference: 'archive-reference-1', caseId: 'case-id', requestor: 'requestor', requestorName: 'requestor-name', subject: 'subject', subjectName: 'subject-name', status: AltinnApplicationStatus.NEW, archivedDate: now, updatedDate: now, phone: 'phone'),
                new AltinnApplication(archiveReference: 'archive-reference-2', caseId: 'case-id', requestor: 'requestor', requestorName: 'requestor-name', subject: 'subject', subjectName: 'subject-name', status: AltinnApplicationStatus.CONSENTS_REQUESTED, archivedDate: now, updatedDate: now, phone: 'phone')))
    }

    void cleanup() {
        repository.deleteAll()
    }

    def "findAllByStatus() returns documents given status"() {
        when:
        def documents = repository.findAllByStatus(AltinnApplicationStatus.NEW)

        then:
        documents.size() == 1
    }

    def "findAllByRequestorAndSubject returns document"() {
        when:
        def documents = repository.findAllByRequestorAndSubject('requestor', 'subject')

        then:
        documents.size() == 2
    }

    def "findAllMinified returns only selected attributes"() {
        when:
        def documents = repository.findAllMinified()

        then:
        documents.size() == 2
        documents.first().archiveReference == 'archive-reference-1'
        documents.first().caseId == 'case-id'
        documents.first().requestor == 'requestor'
        documents.first().requestorName == 'requestor-name'
        documents.first().subject == 'subject'
        documents.first().subjectName == 'subject-name'
        documents.first().status == AltinnApplicationStatus.NEW
        documents.first().archivedDate == now
        documents.first().updatedDate == now
        documents.first().phone == null
    }
}