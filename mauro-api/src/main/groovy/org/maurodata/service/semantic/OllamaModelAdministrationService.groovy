package org.maurodata.service.semantic

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@CompileStatic
@Singleton
class OllamaModelAdministrationService {

    private final String baseUrl
    private final Duration requestTimeout
    private final HttpClient client
    private final JsonSlurper slurper = new JsonSlurper()

    OllamaModelAdministrationService(
        @Value('${chat.providers.ollama.base-url:http://localhost:11434}') String baseUrl,
        @Value('${chat.semantic.embeddings.ollama.pull-timeout-seconds:600}') Integer timeoutSeconds
    ) {
        this.baseUrl = baseUrl
        this.requestTimeout = Duration.ofSeconds(Math.max(timeoutSeconds ?: 600, 1))
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
    }

    Map<String, Object> pull(String model) {
        if (model == null || model.trim().isEmpty()) {
            throw new IllegalArgumentException('Ollama model pull requires model')
        }
        String cleanModel = model.trim()
        String body = JsonOutput.toJson([
            model : cleanModel,
            stream: false
        ])
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + '/api/pull'))
            .header('Content-Type', 'application/json')
            .timeout(requestTimeout)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Ollama model pull failed with HTTP ${response.statusCode()}: ${response.body()}")
        }
        Object parsed = response.body() == null || response.body().trim().isEmpty() ? Collections.<String, Object>emptyMap() : slurper.parseText(response.body())
        Map<String, Object> result = parsed instanceof Map ? new LinkedHashMap<String, Object>((Map<String, Object>) parsed) : new LinkedHashMap<String, Object>()
        result.put('model', cleanModel)
        result.put('provider', 'ollama')
        result.put('baseUrl', baseUrl)
        result
    }
}
