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
import jakarta.inject.Inject
import org.duckdb.DuckDBConnection
import org.maurodata.controller.datamodel.DataElementController
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.persistence.datamodel.DataClassRepository
import org.maurodata.persistence.datamodel.DataElementRepository
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

    @Inject
    DataElementController dataElementController

    @Inject
    DataClassRepository dataClassRepository

    @Inject
    DataElementRepository dataElementRepository

    @Value('${EMBEDDINGS_DATABASE}')
    String embeddingsDatabasePath

    String ollamaEmbedUrl = 'http://localhost:11434/api/embed'

    String embeddingModel = 'embeddinggemma'

    UUID goldModelId = UUID.fromString('4a6216ad-134d-4649-a39f-e3664ed9d960')

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

        Properties readOnlyProperty = new Properties()
        readOnlyProperty.setProperty("duckdb.read_only", "true")
        DuckDBConnection conn = (DuckDBConnection) DriverManager.getConnection("jdbc:duckdb:${embeddingsDatabasePath}", readOnlyProperty)

        PreparedStatement stmt = conn.prepareStatement('select *, 1-array_cosine_distance(embedding, ?::float[768]) similarity from embeddings order by array_cosine_distance(embedding, ?::float[768]), count desc, 1, 2, 3, 4 limit 1000')
        stmt.setObject(1, embedding)
        stmt.setObject(2, embedding)
        ResultSet resultSet = stmt.executeQuery()

        List<Map> items = []
        while (resultSet.next()) {
            String text = resultSet.getString(1)

            if (text) {
                items << [
                    text: text,
                    source_schema: normaliseLabelCase(resultSet.getString(2)),
                    source_table: normaliseLabelCase(resultSet.getString(3)),
                    source_column: normaliseLabelCase(resultSet.getString(4)),
                    count: resultSet.getLong(5),
                    similarity: resultSet.getFloat(7)
                ]
            }
        }

        resultSet.close()
        stmt.close()
        conn.close()

        ListResponse.from(items)
    }

    @Get('/api/demo/element/{schemaName}/{tableName}/{columnName}')
    DataElement show(String schemaName, String tableName, String columnName) {
        List<DataClass> schemaClasses = dataClassRepository.readAllByDataModelAndParentDataClassIsNull(new DataModel(id: goldModelId))

        DataClass schemaClass = schemaClasses.find {it.label == schemaName}

        List<DataClass> tableClasses = dataClassRepository.readAllByParentDataClass(schemaClass)

        DataClass tableClass = tableClasses.find {it.label == tableName}

        DataElement dataElement = dataElementRepository.readByDataClassAndLabel(tableClass, columnName)

        dataElementController.show(dataElement.id)
    }

    static String normaliseLabelCase(String label) {
        label.replaceAll(/(_|^)([a-zA-Z0-9]*)/, {it[1] + it[2].toLowerCase().capitalize()})
    }
}
