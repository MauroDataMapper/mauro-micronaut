package org.maurodata.service.chat.semantic

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
class OllamaTextGenerationService implements TextGenerationService {

    private final String baseUrl
    private final Duration requestTimeout
    private final HttpClient client
    private final JsonSlurper slurper = new JsonSlurper()

    OllamaTextGenerationService(
        @Value('${chat.providers.ollama.base-url:http://localhost:11434}') String baseUrl,
        @Value('${chat.semantic.related-terms.generation-timeout-seconds:12}') Integer timeoutSeconds
    ) {
        this.baseUrl = baseUrl
        this.requestTimeout = Duration.ofSeconds(Math.max(timeoutSeconds ?: 12, 1))
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
    }

    @Override
    String generate(String model, String prompt) {
        if (model == null || model.trim().isEmpty()) {
            throw new IllegalArgumentException('Text generation requires a model')
        }
        String body = JsonOutput.toJson([
            model : model,
            prompt: prompt,
            stream: false
        ])
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + '/api/generate'))
            .header('Content-Type', 'application/json')
            .timeout(requestTimeout)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Ollama text generation failed with HTTP ${response.statusCode()}")
        }
        Object parsed = slurper.parseText(response.body())
        if (!(parsed instanceof Map)) {
            return ''
        }
        Object text = ((Map<?, ?>) parsed).get('response')
        text == null ? '' : String.valueOf(text)
    }
}
