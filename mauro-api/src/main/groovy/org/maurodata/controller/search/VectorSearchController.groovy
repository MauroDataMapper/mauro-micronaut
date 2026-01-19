package org.maurodata.controller.search

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Value
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import org.duckdb.DuckDBConnection
import org.maurodata.web.ListResponse

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet

@Slf4j
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class VectorSearchController {

    @Value('${EMBEDDINGS_DATABASE}')
    String embeddingsDatabasePath

    String ollamaEmbedUrl = 'http://localhost:11434/api/embed'

    String embeddingModel = 'embeddinggemma'

    @Get('/api/vectorSearch')
    ListResponse<Map> vectorSearch(@QueryValue String input) {
        String requestBody = JsonOutput.toJson([
            model: embeddingModel,
            input: input
        ])

        HttpClient httpClient = HttpClient.newHttpClient()

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(ollamaEmbedUrl)) // Ollama embeddings endpoint
            .header('Content-Type', 'application/json')
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()

        HttpResponse response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        def json = new JsonSlurper().parseText(response.body())

        String embedding = '[' + json.embeddings[0].join(',') + ']'

        DuckDBConnection conn = (DuckDBConnection) DriverManager.getConnection("jdbc:duckdb:${embeddingsDatabasePath}")

        PreparedStatement stmt = conn.prepareStatement('select *, 1-array_cosine_distance(embedding, ?::float[768]) similarity from embeddings order by array_cosine_distance(embedding, ?::float[768]) limit 1000')
        stmt.setObject(1, embedding)
        stmt.setObject(2, embedding)
        ResultSet resultSet = stmt.executeQuery()

        List<Map> items = []
        while (resultSet.next()) {
            items << [
                text: resultSet.getString(1),
                source_schema: resultSet.getString(2),
                source_table: resultSet.getString(3),
                source_column: resultSet.getString(4),
                similarity: resultSet.getFloat(6)
            ]
        }

        resultSet.close()
        stmt.close()
        conn.close()

        ListResponse.from(items)
    }
}
