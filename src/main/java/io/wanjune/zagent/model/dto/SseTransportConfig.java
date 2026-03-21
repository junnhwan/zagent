package io.wanjune.zagent.model.dto;

public record SseTransportConfig(
        String baseUri,
        String sseEndpoint
) {
}
