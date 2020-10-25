package no.fint.drosjeloyve.client;

import no.fint.drosjeloyve.configuration.OrganisationProperties;
import no.fint.drosjeloyve.model.AltinnApplication;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.arkiv.noark.DokumentfilResource;
import no.fint.model.resource.arkiv.samferdsel.DrosjeloyveResource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient;

@Component
public class FintClient {
    private final WebClient webClient;
    private final ReactiveOAuth2AuthorizedClientManager authorizedClientManager;
    private final Authentication principal;

    public FintClient(WebClient webClient, ReactiveOAuth2AuthorizedClientManager authorizedClientManager, Authentication principal) {
        this.webClient = webClient;
        this.authorizedClientManager = authorizedClientManager;
        this.principal = principal;
    }

    public <T extends FintLinks> Mono<ResponseEntity<Void>> postResource(OrganisationProperties.Organisation organisation, T resource, String uri) {
        return authorizedClient(organisation).flatMap(client ->
                webClient.post()
                        .uri(uri)
                        .attributes(oauth2AuthorizedClient(client))
                        .bodyValue(resource)
                        .retrieve()
                        .toBodilessEntity()
        );
    }

    public Mono<ResponseEntity<Void>> postFile(OrganisationProperties.Organisation organisation, byte[] bytes, AltinnApplication.Attachment attachment, String uri) {
        return authorizedClient(organisation).flatMap(client ->
                webClient.post()
                        .uri(uri)
                        .contentType(MediaType.parseMediaType(attachment.getAttachmentType().replace("_", "/")))
                        .headers(httpHeaders -> {
                            httpHeaders.add(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.builder("inline").filename("te.pdf").build().toString());
                        })
                        .attributes(oauth2AuthorizedClient(client))
                        .bodyValue(bytes)
                        .retrieve()
                        .toBodilessEntity()
        );
    }

    public <T extends FintLinks> Mono<ResponseEntity<Void>> putResource(OrganisationProperties.Organisation organisation, T resource, String uri, String id) {
        return authorizedClient(organisation).flatMap(client ->
                webClient.put()
                        .uri(uri, id)
                        .attributes(oauth2AuthorizedClient(client))
                        .bodyValue(resource)
                        .retrieve()
                        .toBodilessEntity()
        );
    }

    public <T extends FintLinks> Mono<ResponseEntity<T>> getStatus(OrganisationProperties.Organisation organisation, Class<T> clazz, URI location) {
        return authorizedClient(organisation).flatMap(client ->
                webClient.get()
                        .uri(location)
                        .attributes(oauth2AuthorizedClient(client))
                        .retrieve()
                        .toEntity(clazz)
        );
    }

    public <T extends FintLinks> Mono<T> getResource(OrganisationProperties.Organisation organisation, Class<T> clazz, String uri, String id) {
        return authorizedClient(organisation).flatMap(client ->
                webClient.get()
                        .uri(uri, id)
                        .attributes(oauth2AuthorizedClient(client))
                        .retrieve()
                        .bodyToMono(clazz)
        );
    }

    private Mono<OAuth2AuthorizedClient> authorizedClient(OrganisationProperties.Organisation organisation) {
        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId(organisation.getRegistration())
                .principal(principal)
                .attributes(attributes -> {
                    attributes.put(OAuth2ParameterNames.USERNAME, organisation.getUsername());
                    attributes.put(OAuth2ParameterNames.PASSWORD, organisation.getPassword());
                }).build();

        return authorizedClientManager.authorize(authorizeRequest);
    }
}
