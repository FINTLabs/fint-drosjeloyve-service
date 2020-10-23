package no.fint.drosjeloyve.client

import com.fasterxml.jackson.databind.ObjectMapper
import no.fint.drosjeloyve.model.ebevis.Evidence
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import reactor.test.StepVerifier
import spock.lang.Specification

class FintAltinnClientSpec extends Specification {
    MockWebServer mockWebServer = new MockWebServer()

    FintAltinnClient client = new FintAltinnClient(WebClient.builder())

    void setup() {
        mockWebServer.start()
    }

    void cleanup() {
        mockWebServer.shutdown()
    }

    def "get attachment returns byte array"() {
        given:
        def attachment = [0, 1, 2] as byte[]
        def uri = 'http://localhost:' + mockWebServer.getPort()

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.OK.value())
                .setBody(new Buffer().write(attachment)))

        when:
        def setup = client.getAttachment(uri, 1044)

        then:
        StepVerifier
                .create(setup)
                .expectNextMatches({ byteArray ->
                    byteArray == attachment
                })
                .verifyComplete()
    }

    def "get application returns byte array"() {
        given:
        def attachment = [0, 1, 2] as byte[]
        def uri = 'http://localhost:' + mockWebServer.getPort()

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.OK.value())
                .setBody(new Buffer().write(attachment)))

        when:
        def setup = client.getApplication(uri, _ as String, 1044)

        then:
        StepVerifier
                .create(setup)
                .expectNextMatches({ byteArray ->
                    byteArray == attachment
                })
                .verifyComplete()
    }

    def "get evidence returns evidence"() {
        given:
        def uri = 'http://localhost:' + mockWebServer.getPort()

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.OK.value())
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .setBody(new ObjectMapper().writeValueAsString(new Evidence())))

        when:
        def setup = client.getEvidence(uri, _ as String, _ as String)

        then:
        StepVerifier
                .create(setup)
                .expectNext(new Evidence())
                .verifyComplete()
    }
}
