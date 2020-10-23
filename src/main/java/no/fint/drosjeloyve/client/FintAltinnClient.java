package no.fint.drosjeloyve.client;

import no.fint.drosjeloyve.model.ebevis.Evidence;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class FintAltinnClient {
    private final WebClient webClient;

    public FintAltinnClient(WebClient.Builder builder) {
        this.webClient = builder
                .exchangeStrategies(ExchangeStrategies.builder().codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(-1)).build())
                .build();
    }

    public Mono<byte[]> getAttachment(String uri, Integer attachmentId) {
        return webClient.get()
                .uri(uri, attachmentId)
                .retrieve()
                .bodyToMono(byte[].class);
    }

    public Mono<byte[]> getApplication(String uri, String archiveReference, Integer languageCode) {
        return webClient.get()
                .uri(uri, uriBuilder -> uriBuilder
                        .queryParam("languageCode", languageCode)
                        .build(archiveReference))
                .retrieve()
                .bodyToMono(byte[].class);
    }

    public Mono<Evidence> getEvidence(String uri, String accreditationId, String evidenceCodeName) {
        return webClient.get()
                .uri(uri, uriBuilder -> uriBuilder
                        .queryParam("evidenceCodeName", evidenceCodeName)
                        .build(accreditationId))
                .retrieve()
                .bodyToMono(Evidence.class);
    }
}
