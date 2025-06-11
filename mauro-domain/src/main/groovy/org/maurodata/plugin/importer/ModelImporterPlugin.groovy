package org.maurodata.plugin.importer

import org.maurodata.domain.model.Model
import org.maurodata.plugin.MauroPlugin
import org.maurodata.plugin.PluginType
import org.maurodata.plugin.importer.config.ImportGroupConfig
import org.maurodata.plugin.importer.config.ImportParameterConfig

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.Memoized
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.reflect.FieldUtils

import java.lang.reflect.Field

@Slf4j
trait ModelImporterPlugin <D extends Model, P extends ImportParameters> extends MauroPlugin {

    abstract List<D> importDomain(P params)

    Boolean getCanImportMultipleDomains() {
        true
    }

    abstract Boolean handlesContentType(String contentType)

    Boolean canFederate() { true }

    abstract Class<P> importParametersClass()

    @JsonIgnore
    PluginType getPluginType() {
        PluginType.Importer
    }

    @JsonIgnore
    abstract Class<D> getHandlesModelType()

    String getParamClassType() {
        importParametersClass().toString()
    }

    List<D> importModels(P parameters) {
        List<D> imported = importDomain(parameters)
        imported.each { importedModel ->
            importedModel.setAssociations()
            importedModel.updateCreationProperties()
            log.info '* start updateCreationProperties *'
            importedModel.getAllContents().each {it.updateCreationProperties()}
            log.info '* finish updateCreationProperties *'
        }
        imported
    }

    @Override
    String getProviderType() {
        return "${getHandlesModelType().simpleName}${this.pluginType}"
    }

    @Memoized
    List<Map<String, Object>> calculateParameterGroups() {
        List<Field> fields = FieldUtils.getAllFields(importParametersClass())
            .findAll { it.isAnnotationPresent(ImportParameterConfig)
                && !it.getAnnotation(ImportParameterConfig).hidden()
            }

        fields.collect {field ->
            String fieldType
            switch (field.getType().getSimpleName()) {
                case 'FileParameter':
                    fieldType = 'File'
                    break
                case 'UUID':
                    fieldType = 'Folder'
                    break
                default:
                    fieldType = field.getType().getSimpleName()
            }
            ImportParameterConfig config = field.getAnnotation(ImportParameterConfig)
            if (config.password())
                fieldType = 'Password'

            return [
                groupName: config.group().name(),
                groupOrder: config.group().order(),
                name: field.name,
                type: fieldType,
                order: config.order(),
                optional: config.optional(),
                displayName: config.displayName(),
                description: config.description().join(config.descriptionJoinDelimiter())
            ]
        }
        .groupBy {
            it.groupName
        }.collect {key, value ->
            [name: key, parameters: value.sort {it.order}]
        }.sort {
            it.parameters.first().groupOrder
        }

        
    }

}
