package org.maurodata.plugin.chat.semantic

import org.maurodata.service.chat.semantic.*
import org.maurodata.service.search.*
import org.maurodata.service.semantic.*

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
class OllamaEmbeddingProvider implements EmbeddingProvider {

    private final String baseUrl
    private final Duration requestTimeout
    private final HttpClient client
    private final JsonSlurper slurper = new JsonSlurper()

    OllamaEmbeddingProvider(
        @Value('${chat.providers.ollama.base-url:http://localhost:11434}') String baseUrl,
        @Value('${chat.semantic.embeddings.ollama.timeout-seconds:60}') Integer timeoutSeconds
    ) {
        this.baseUrl = baseUrl
        this.requestTimeout = Duration.ofSeconds(Math.max(timeoutSeconds ?: 60, 1))
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
    }

    @Override
    String id() {
        'ollama'
    }

    @Override
    boolean supports(EmbeddingProfile profile) {
        profile?.provider == id() && profile.embeddingModel
    }

    @Override
    List<float[]> embed(EmbeddingProfile profile, List<String> texts) {
        if (profile == null || profile.embeddingModel == null || profile.embeddingModel.trim().isEmpty()) {
            throw new IllegalArgumentException('Ollama embeddings require an embedding model')
        }
        List<String> safeTexts = (texts ?: []).collect {String text -> text ?: ''} as List<String>
        if (safeTexts.isEmpty()) {
            return Collections.<float[]>emptyList()
        }

        String body = JsonOutput.toJson([
            model: profile.embeddingModel,
            input: safeTexts
        ])
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + '/api/embed'))
            .header('Content-Type', 'application/json')
            .timeout(requestTimeout)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Ollama embeddings failed with HTTP ${response.statusCode()}: ${response.body()}")
        }

        Object parsed = slurper.parseText(response.body())
        if (!(parsed instanceof Map)) {
            throw new IllegalStateException('Ollama embeddings response was not a JSON object')
        }
        Object embeddingsObj = ((Map<?, ?>) parsed).get('embeddings')
        if (!(embeddingsObj instanceof Collection)) {
            throw new IllegalStateException('Ollama embeddings response did not contain embeddings')
        }

        List<float[]> embeddings = new ArrayList<float[]>()
        for (Object embeddingObj : (Collection<?>) embeddingsObj) {
            embeddings.add(toVector(embeddingObj, profile))
        }
        if (embeddings.size() != safeTexts.size()) {
            throw new IllegalStateException("Ollama returned ${embeddings.size()} embeddings for ${safeTexts.size()} input texts")
        }
        embeddings
    }

    private static float[] toVector(Object embeddingObj, EmbeddingProfile profile) {
        if (!(embeddingObj instanceof Collection)) {
            throw new IllegalStateException('Ollama embedding entry was not an array')
        }
        Collection<?> values = (Collection<?>) embeddingObj
        if (profile.dimension != null && values.size() != profile.dimension) {
            throw new IllegalStateException("Ollama embedding dimension ${values.size()} did not match profile dimension ${profile.dimension} for ${profile.name}")
        }
        float[] vector = new float[values.size()]
        int index = 0
        for (Object value : values) {
            if (!(value instanceof Number)) {
                throw new IllegalStateException('Ollama embedding value was not numeric')
            }
            vector[index++] = ((Number) value).floatValue()
        }
        vector
    }
}
