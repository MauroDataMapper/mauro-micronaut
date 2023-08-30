package uk.ac.ox.softeng.mauro.model.version

import com.fasterxml.jackson.databind.util.StdConverter
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Prototype
import io.micronaut.core.convert.ConversionContext
import io.micronaut.data.model.runtime.convert.AttributeConverter

@CompileStatic
@Prototype
class ModelVersionConverter extends StdConverter<String, ModelVersion> implements AttributeConverter<ModelVersion, String> {
    @Override
    ModelVersion convert(String value) {
        ModelVersion.from(value)
    }

    @Override
    String convertToPersistedValue(ModelVersion version, ConversionContext context) {
        version ? version.toString() : null
    }

    @Override
    ModelVersion convertToEntityValue(String value, ConversionContext context) {
        value ? ModelVersion.from(value) : null
    }
}