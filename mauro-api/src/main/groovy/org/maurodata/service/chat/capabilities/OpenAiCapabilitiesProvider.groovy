package org.maurodata.service.chat.capabilities

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import org.maurodata.api.chat.ModelDto
import org.maurodata.api.chat.ProviderDto

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@CompileStatic
@Singleton
final class OpenAiCapabilitiesProvider implements CapabilitiesProvider {

    private final String baseUrl
    private final String apiKey
    private final List<String> allowlist
    private final HttpClient client
    private final JsonSlurper slurper

    OpenAiCapabilitiesProvider(
        @Value('${chat.providers.openai.base-url:https://api.openai.com}') final String baseUrl,
        @Value('${chat.providers.openai.api-key:}') final String apiKey,
        @Value('${chat.providers.openai.model-allowlist:}') final List<String> allowlist
    ) {
        this.baseUrl = baseUrl
        this.apiKey = apiKey
        this.allowlist = normalizeAllowlist(allowlist)
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
        this.slurper = new JsonSlurper()
    }

    @Override
    String providerId() { 'openai' }

    @Override
    List<ModelDto> listModels() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return Collections.<ModelDto>emptyList()
        }

        try {
            final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + '/v1/models'))
                .timeout(Duration.ofSeconds(10))
                .header('Authorization', 'Bearer ' + apiKey)
                .GET()
                .build()

            final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return Collections.<ModelDto>emptyList()
            }

            final Object parsed = slurper.parseText(response.body())
            if (!(parsed instanceof Map)) {
                return Collections.<ModelDto>emptyList()
            }

            final Map<?, ?> root = (Map<?, ?>) parsed
            final Object dataObj = root.get('data')
            if (!(dataObj instanceof List)) {
                return Collections.<ModelDto>emptyList()
            }

            final List<?> data = (List<?>) dataObj
            final List<ModelDto> out = new ArrayList<ModelDto>(data.size())

            for (int i = 0; i < data.size(); i++) {
                final Object itemObj = data.get(i)
                if (!(itemObj instanceof Map)) {
                    continue
                }
                final Map<?, ?> item = (Map<?, ?>) itemObj
                final String modelId = asString(item.get('id'))
                if (modelId == null || modelId.isEmpty()) {
                    continue
                }

                if (!allowlist.isEmpty()) {
                    if (!allowlist.contains(modelId)) {
                        continue
                    }
                } else if (!modelId.startsWith('gpt-') && !modelId.startsWith('o')) {
                    continue
                }

                final ModelDto dto = new ModelDto()
                dto.id = modelId
                dto.provider = providerId()
                dto.streaming = Boolean.TRUE
                dto.tools = Boolean.TRUE
                dto.contextWindow = null
                out.add(dto)
            }

            return out
        } catch (Throwable ignored) {
            return Collections.<ModelDto>emptyList()
        }
    }

    @Override
    ProviderDto providerStatus() {
        final ProviderDto dto = new ProviderDto()
        dto.id = providerId()

        if (apiKey == null || apiKey.trim().isEmpty()) {
            dto.status = 'NOT_SET'
            dto.message = 'Missing API key'
            return dto
        }

        try {
            final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + '/v1/models'))
                .timeout(Duration.ofSeconds(10))
                .header('Authorization', 'Bearer ' + apiKey)
                .GET()
                .build()

            final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString())
            dto.status = (response.statusCode() >= 200 && response.statusCode() < 300) ? 'SET' : 'INVALID'
            dto.message = (dto.status == 'SET') ? null : ('HTTP ' + response.statusCode())
        } catch (Throwable t) {
            dto.status = 'INVALID'
            dto.message = t.getMessage()
        }
        return dto
    }

    private static String asString(final Object value) {
        return value == null ? null : String.valueOf(value)
    }

    private static List<String> normalizeAllowlist(final List<String> rawAllowlist) {
        if (rawAllowlist == null || rawAllowlist.isEmpty()) {
            return Collections.<String>emptyList()
        }
        final List<String> cleaned = new ArrayList<String>(rawAllowlist.size())
        for (int i = 0; i < rawAllowlist.size(); i++) {
            final String value = rawAllowlist.get(i)
            if (value != null && !value.trim().isEmpty()) {
                cleaned.add(value)
            }
        }
        return cleaned
    }
}
