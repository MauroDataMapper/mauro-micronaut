package org.maurodata.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.type.Argument
import io.micronaut.core.type.MutableHeaders
import io.micronaut.http.HttpHeaders
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Produces
import io.micronaut.http.body.TypedMessageBodyWriter
import io.micronaut.http.codec.CodecException
import jakarta.inject.Singleton

@CompileStatic
@Produces(MediaType.APPLICATION_YAML)
@Singleton
class YamlMessageBodyWriter implements TypedMessageBodyWriter<Object> {

    private final ObjectMapper yamlMapper

    YamlMessageBodyWriter(ObjectMapper objectMapper) {
        this.yamlMapper = objectMapper.copyWith(
            YAMLFactory.builder()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .build()
        )
    }

    @Override
    @NonNull
    Argument<Object> getType() {
        Argument.OBJECT_ARGUMENT
    }

    @Override
    boolean isBlocking() {
        true
    }

    @Override
    void writeTo(@NonNull Argument<Object> type,
                 @NonNull MediaType mediaType,
                 Object object,
                 @NonNull MutableHeaders outgoingHeaders,
                 @NonNull OutputStream outputStream) throws CodecException {
        outgoingHeaders.setIfMissing(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_YAML)
        try {
            yamlMapper.writeValue(outputStream, object)
        } catch (IOException e) {
            throw new CodecException("Error encoding object [${object}] to YAML: ${e.message}", e)
        }
    }
}
