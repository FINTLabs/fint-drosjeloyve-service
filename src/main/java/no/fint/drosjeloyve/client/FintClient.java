package no.fint.drosjeloyve.client;

import no.fint.drosjeloyve.configuration.OrganisationProperties;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.arkiv.samferdsel.DrosjeloyveResource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient;

@Component
public class FintClient {
    private final OrganisationProperties organisationProperties;
    private final WebClient webClient;
    private final ReactiveOAuth2AuthorizedClientManager authorizedClientManager;
    private final Authentication principal;

    public FintClient(OrganisationProperties organisationProperties, WebClient webClient, ReactiveOAuth2AuthorizedClientManager authorizedClientManager, Authentication principal) {
        this.organisationProperties = organisationProperties;
        this.webClient = webClient;
        this.authorizedClientManager = authorizedClientManager;
        this.principal = principal;
    }

    public <T extends FintLinks> Mono<ResponseEntity<Void>> postResource(String requestor, T resource, String uri) {
        return authorizedClient(requestor).flatMap(client ->
                webClient.post()
                        .uri(uri)
                        .attributes(oauth2AuthorizedClient(client))
                        .bodyValue(resource)
                        .retrieve()
                        .toBodilessEntity()
        );
    }

    public <T extends FintLinks> Mono<ResponseEntity<Void>> putResource(String requestor, T resource, String uri, String id) {
        return authorizedClient(requestor).flatMap(client ->
                webClient.put()
                        .uri(uri, id)
                        .attributes(oauth2AuthorizedClient(client))
                        .bodyValue(resource)
                        .retrieve()
                        .toBodilessEntity()
        );
    }

    public <T extends FintLinks> Mono<ResponseEntity<Void>> getStatus(String organisationNumber, String uri) {
        return authorizedClient(organisationNumber).flatMap(client ->
                webClient.head()
                        .uri(uri)
                        .attributes(oauth2AuthorizedClient(client))
                        .retrieve()
                        .toBodilessEntity()
        );
    }

    public <T extends FintLinks> Mono<T> getResource(String requestor, Class<T> clazz, String uri, String id) {
        return authorizedClient(requestor).flatMap(client ->
                webClient.get()
                        .uri(uri, id)
                        .attributes(oauth2AuthorizedClient(client))
                        .retrieve()
                        .bodyToMono(clazz)
        );
    }

    public Mono<List<DrosjeloyveResource>> getResources(String requestor, String uri, String subject) {
        return authorizedClient(requestor).flatMap(client ->
                webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path(uri)
                                .queryParam("organisasjonsnummer", subject)
                                .build())
                        .attributes(oauth2AuthorizedClient(client))
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<List<DrosjeloyveResource>>() {
                        }));
    }

    private Mono<OAuth2AuthorizedClient> authorizedClient(String organisationNumber) {
        OrganisationProperties.Organisation organisation = organisationProperties.getOrganisations().get(organisationNumber);

        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId(organisation.getRegistration())
                .principal(principal)
                .attributes(attributes -> {
                    attributes.put(OAuth2ParameterNames.USERNAME, organisation.getUsername());
                    attributes.put(OAuth2ParameterNames.PASSWORD, organisation.getPassword());
                }).build();

        return authorizedClientManager.authorize(authorizeRequest);
    }
}
