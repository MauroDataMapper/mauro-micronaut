package org.maurodata.plugin.chat.providers

import org.maurodata.service.chat.capabilities.*
import org.maurodata.service.chat.llm.*

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import org.maurodata.plugin.chat.api.chat.ModelDto
import org.maurodata.plugin.chat.api.chat.ProviderDto

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@CompileStatic
@Slf4j
@Singleton
final class OllamaCapabilitiesProvider implements CapabilitiesProvider {

    private final String baseUrl
    private final List<String> allowlist
    private final boolean traceCapabilities
    private final HttpClient client
    private final JsonSlurper slurper

    OllamaCapabilitiesProvider(
        @Value('${chat.providers.ollama.base-url:http://localhost:11434}') final String baseUrl,
        @Value('${chat.providers.ollama.model-allowlist:}') final List<String> allowlist,
        @Value('${chat.providers.ollama.trace-capabilities:false}') final boolean traceCapabilities
    ) {
        this.baseUrl = baseUrl
        this.allowlist = normalizeAllowlist(allowlist)
        this.traceCapabilities = traceCapabilities
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
        this.slurper = new JsonSlurper()
    }

    @Override
    String providerId() { 'ollama' }

    @Override
    List<ModelDto> listModels() {
        try {
            if (traceCapabilities) {
                log.info('OLLAMA_CAPS request url={} allowlist={}', baseUrl + '/api/tags', allowlist)
            }
            final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + '/api/tags'))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build()

            final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (traceCapabilities) {
                log.info('OLLAMA_CAPS response status={} body={}', Integer.valueOf(response.statusCode()), response.body())
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                if (traceCapabilities) {
                    log.info('OLLAMA_CAPS non-2xx response; returning empty model list')
                }
                return Collections.<ModelDto>emptyList()
            }

            final Object parsed = slurper.parseText(response.body())
            if (!(parsed instanceof Map)) {
                if (traceCapabilities) {
                    log.info('OLLAMA_CAPS parsed payload is not a map; returning empty model list')
                }
                return Collections.<ModelDto>emptyList()
            }

            final Map<?, ?> root = (Map<?, ?>) parsed
            final Object modelsObj = root.get('models')
            if (!(modelsObj instanceof List)) {
                if (traceCapabilities) {
                    log.info('OLLAMA_CAPS root.models is not a list; returning empty model list')
                }
                return Collections.<ModelDto>emptyList()
            }

            final List<?> models = (List<?>) modelsObj
            final List<ModelDto> out = new ArrayList<ModelDto>(models.size())

            for (int i = 0; i < models.size(); i++) {
                final Object modelObj = models.get(i)
                if (!(modelObj instanceof Map)) {
                    continue
                }
                final Map<?, ?> modelMap = (Map<?, ?>) modelObj
                final String modelName = asString(modelMap.get('model'))
                if (modelName == null || modelName.isEmpty()) {
                    continue
                }

                if (!allowlist.isEmpty() && !allowlist.contains(modelName)) {
                    if (traceCapabilities) {
                        log.info('OLLAMA_CAPS filtered out model={} due to allowlist', modelName)
                    }
                    continue
                }

                final ModelDto dto = new ModelDto()
                dto.id = modelName
                dto.provider = providerId()
                dto.streaming = Boolean.TRUE
                dto.tools = Boolean.TRUE
                dto.contextWindow = null
                out.add(dto)
            }
            if (traceCapabilities) {
                final List<String> ids = new ArrayList<String>(out.size())
                for (int i = 0; i < out.size(); i++) {
                    ids.add(out.get(i).id)
                }
                log.info('OLLAMA_CAPS discovered modelCount={} modelIds={}', Integer.valueOf(out.size()), ids)
            }

            return out
        } catch (Throwable t) {
            if (traceCapabilities) {
                log.warn('OLLAMA_CAPS discovery failed: {}', t.getMessage(), t)
            }
            return Collections.<ModelDto>emptyList()
        }
    }

    @Override
    ProviderDto providerStatus() {
        final ProviderDto dto = new ProviderDto()
        dto.id = providerId()
        try {
            final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + '/api/tags'))
                .timeout(Duration.ofSeconds(5))
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
