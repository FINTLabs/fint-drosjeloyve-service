package no.fint.drosjeloyve.client

import com.fasterxml.jackson.databind.ObjectMapper
import no.fint.drosjeloyve.configuration.FintProperties
import no.fint.drosjeloyve.configuration.OrganisationProperties
import no.fint.model.resource.arkiv.noark.JournalpostResource
import no.fint.model.resource.arkiv.samferdsel.DrosjeloyveResource
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import spock.lang.Specification

class FintClientSpec extends Specification {
    MockWebServer mockWebServer = new MockWebServer()

    WebClient webClient = WebClient.builder()
            .baseUrl('http://localhost:' + mockWebServer.getPort())
            .build()

    OrganisationProperties organisationProperties = Stub(OrganisationProperties) {
        getOrganisations() >> [(_ as String): new OrganisationProperties.Organisation(
                name: 'name',
                registration: 'registration',
                username: 'username',
                password: 'password'
        )]
    }

    ReactiveOAuth2AuthorizedClientManager authorizedClientManager = Stub(ReactiveOAuth2AuthorizedClientManager) {
        authorize(_ as OAuth2AuthorizeRequest) >> Mono.just(Mock(OAuth2AuthorizedClient))
    }

    FintClient fintClient = new FintClient(organisationProperties, webClient, authorizedClientManager, Mock(Authentication))

    void cleanup() {
        mockWebServer.shutdown()
    }

    def "post resource returns location of status resource"() {
        given:
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.ACCEPTED.value())
                .addHeader(HttpHeaders.LOCATION, 'status-location'))

        when:
        def setup = fintClient.postResource(_ as String, new DrosjeloyveResource(), _ as String)

        then:
        StepVerifier
                .create(setup)
                .expectNextMatches({ responseEntity ->
                    responseEntity.statusCode == HttpStatus.ACCEPTED &&
                            responseEntity.headers.getLocation() == URI.create('status-location')
                })
                .verifyComplete()
    }

    def "put resource returns location of status resource"() {
        given:
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.ACCEPTED.value())
                .addHeader(HttpHeaders.LOCATION, 'status-location'))

        when:
        def setup = fintClient.putResource(_ as String, new DrosjeloyveResource(), _ as String, _ as String)

        then:
        StepVerifier
                .create(setup)
                .expectNextMatches({ responseEntity ->
                    responseEntity.statusCode == HttpStatus.ACCEPTED &&
                            responseEntity.headers.getLocation() == URI.create('status-location')
                })
                .verifyComplete()
    }

    def "get status returns location of and the updated resource"() {
        given:
        DrosjeloyveResource resource = new DrosjeloyveResource()

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.CREATED.value())
                .addHeader(HttpHeaders.LOCATION, 'resource-location')
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .setBody(new ObjectMapper().writeValueAsString(resource)))

        when:
        def setup = fintClient.getStatus(_ as String, _ as String)

        then:
        StepVerifier
                .create(setup)
                .expectNextMatches({ responseEntity ->
                    responseEntity.statusCode == HttpStatus.CREATED &&
                            responseEntity.headers.getLocation() == URI.create('resource-location')
                })
                .verifyComplete()
    }

    def "get resource returns resource"() {
        given:
        DrosjeloyveResource resource = new DrosjeloyveResource()

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.OK.value())
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .setBody(new ObjectMapper().writeValueAsString(resource)))

        when:
        def setup = fintClient.getResource(_ as String, DrosjeloyveResource.class, _ as String, _ as String)

        then:
        StepVerifier
                .create(setup)
                .expectNext(resource)
                .verifyComplete()
    }

    def "get resources returns resources"() {
        given:
        DrosjeloyveResource resource = new DrosjeloyveResource()

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.OK.value())
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .setBody(new ObjectMapper().writeValueAsString([resource, resource])))

        when:
        def setup = fintClient.getResources(_ as String, _ as String, _ as String)

        then:
        StepVerifier
                .create(setup)
                .expectNext([resource, resource])
                .verifyComplete()
    }

    def "get resource throws exception"() {
        given:
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.NOT_FOUND.value()))

        when:
        def setup = fintClient.getResource(_ as String, DrosjeloyveResource.class, _ as String, _ as String)

        then:
        StepVerifier
                .create(setup)
                .verifyError(WebClientResponseException.class)
    }
}
