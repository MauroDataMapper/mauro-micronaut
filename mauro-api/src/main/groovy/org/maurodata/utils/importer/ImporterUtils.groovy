package org.maurodata.utils.importer

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.http.multipart.CompletedFileUpload
import io.micronaut.http.multipart.CompletedPart
import io.micronaut.http.server.multipart.MultipartBody
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.plugin.importer.FileParameter
import org.maurodata.plugin.importer.ImportParameters
import reactor.core.publisher.Flux

import java.nio.charset.StandardCharsets

@Slf4j
@CompileStatic
@Singleton
class ImporterUtils {
    @Inject
    ObjectMapper objectMapper



    <P extends ImportParameters> P readFromMultipartFormBody(MultipartBody body, Class<P> parametersClass) {
        Map<String, Object> importMap = Flux.from(body).collectList().block().collectEntries {CompletedPart cp ->
            if (cp instanceof CompletedFileUpload) {
                return [cp.name, new FileParameter(cp.filename, cp.contentType.toString(), cp.bytes)]
            } else {
                return [cp.name, new String(cp.bytes, StandardCharsets.UTF_8)]
            }
        }
        return objectMapper.convertValue(importMap, parametersClass)
    }
}
