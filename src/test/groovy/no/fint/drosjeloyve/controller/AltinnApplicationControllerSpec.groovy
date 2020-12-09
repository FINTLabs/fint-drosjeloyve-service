package no.fint.drosjeloyve.controller

import no.fint.altinn.model.AltinnApplication
import no.fint.altinn.model.AltinnApplicationStatus
import no.fint.drosjeloyve.repository.AltinnApplicationRepository
import org.spockframework.spring.SpringBean
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.test.web.reactive.server.WebTestClient
import spock.lang.Specification

import java.time.LocalDateTime

@WebFluxTest(value = AltinnApplicationController.class,
        properties = ['spring.security.oauth2.client.registration.test.client-id=client-id',
                'spring.security.oauth2.client.registration.test.client-secret=client-secret',
                'spring.security.oauth2.client.registration.test.authorization-grant-type=password',
                'spring.security.oauth2.client.registration.test.scope=fint-client',
                'spring.security.oauth2.client.registration.test.provider=fint',
                'spring.security.oauth2.client.provider.fint.token-uri=https://idp.felleskomponent.no/nidp/oauth/nam/token'])
class AltinnApplicationControllerSpec extends Specification {
    @SpringBean
    AltinnApplicationRepository repository = Mock()

    AltinnApplicationController controller = new AltinnApplicationController(repository)

    WebTestClient client

    void setup() {
        client = WebTestClient.bindToController(controller).build()
    }

    def "Get altinn applications returns list of applications with defined attributes"() {
        when:
        repository.findAllMinified() >> [newAltinnApplication()]

        then:
        client.get()
                .uri("/api/applications")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.[0].caseId').isEqualTo('case-id')
    }

    def newAltinnApplication() {
        def now = LocalDateTime.now()

        return new AltinnApplication(
                caseId: 'case-id',
                requestor: 'requestor',
                requestorName: 'requestor-name',
                subject: 'subject',
                subjectName: 'subject-name',
                status: AltinnApplicationStatus.NEW,
                archivedDate: now,
                updatedDate: now)
    }
}
