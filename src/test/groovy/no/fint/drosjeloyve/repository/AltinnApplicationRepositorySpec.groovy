package no.fint.drosjeloyve.repository

import no.fint.drosjeloyve.model.AltinnApplication
import no.fint.drosjeloyve.model.AltinnApplicationStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import spock.lang.Specification

@DataMongoTest
class AltinnApplicationRepositorySpec extends Specification {

    @Autowired
    AltinnApplicationRepository repository

    def "findAllByStatus() returns documents given status"() {
        given:
        repository.saveAll(Arrays.asList(new AltinnApplication(status: AltinnApplicationStatus.CONSENTS_ACCEPTED),
                new AltinnApplication(status: AltinnApplicationStatus.CONSENTS_REQUESTED),
                new AltinnApplication(status: AltinnApplicationStatus.CONSENTS_REQUESTED)))

        when:
        def documents = repository.findAllByStatus(AltinnApplicationStatus.CONSENTS_ACCEPTED)

        then:
        documents.size() == 1
    }
}