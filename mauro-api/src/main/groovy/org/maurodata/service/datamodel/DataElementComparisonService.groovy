package org.maurodata.service.datamodel

import groovy.transform.CompileStatic
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.comparison.ComparisonContext
import org.maurodata.domain.comparison.ComparisonResult
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.security.Role
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.DataElementCacheableRepository
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.DataTypeCacheableRepository
import org.maurodata.plugin.MauroPluginService
import org.maurodata.plugin.datamodel.DataElementComparisonProviderPlugin
import org.maurodata.security.AccessControlService

@CompileStatic
@Singleton
class DataElementComparisonService {

    final DataElementCacheableRepository dataElementRepository
    final DataTypeCacheableRepository dataTypeRepository
    final MauroPluginService mauroPluginService
    final DataTypeComparisonService dataTypeComparisonService
    final AccessControlService accessControlService

    @Inject
    DataElementComparisonService(DataElementCacheableRepository dataElementRepository, DataTypeCacheableRepository dataTypeRepository,
                                 MauroPluginService mauroPluginService, DataTypeComparisonService dataTypeComparisonService,
                                 AccessControlService accessControlService) {
        this.dataElementRepository = dataElementRepository
        this.dataTypeRepository = dataTypeRepository
        this.mauroPluginService = mauroPluginService
        this.dataTypeComparisonService = dataTypeComparisonService
        this.accessControlService = accessControlService
    }

    List<ComparisonResult> compare(UUID leftId, UUID rightId) {
        DataElement left = readDataElement(leftId)
        DataElement right = readDataElement(rightId)

        List<ComparisonResult> results = mauroPluginService.listPlugins(DataElementComparisonProviderPlugin).collectMany {
            DataElementComparisonProviderPlugin provider -> provider.compare(left, right, new ComparisonContext())
        }

        results.addAll(dataTypeComparisonService.compare(left.dataType, right.dataType).collect {ComparisonResult result ->
            nestedDataTypeResult(result, left.dataType, right.dataType)
        })

        results
    }

    private DataElement readDataElement(UUID id) {
        DataElement dataElement = dataElementRepository.readById(id)
        if (!dataElement) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "DataElement ${id} not found")
        }
        accessControlService.checkRole(Role.READER, dataElement)

        dataElement = dataElementRepository.loadWithContent(dataElement.id)
        if (dataElement.dataType?.id) {
            DataType dataType = dataTypeRepository.readById(dataElement.dataType.id)
            if (!dataType) {
                throw new HttpStatusException(HttpStatus.NOT_FOUND, "DataType ${dataElement.dataType.id} not found")
            }
            accessControlService.checkRole(Role.READER, dataType)
            dataElement.dataType = dataTypeRepository.loadWithContent(dataType.id)
        }
        dataElement
    }

    private ComparisonResult nestedDataTypeResult(ComparisonResult result, DataType leftDataType, DataType rightDataType) {
        Map<String, Object> metadata = [:]
        metadata.putAll(result.metadata ?: [:])
        metadata['nestedComparison'] = 'dataType'
        metadata['leftDataTypeId'] = leftDataType.id
        metadata['rightDataTypeId'] = rightDataType.id

        new ComparisonResult(
            provider: result.provider,
            comparisonType: "dataType.${result.comparisonType}",
            comparedProperty: "dataType.${result.comparedProperty}",
            conclusion: result.conclusion,
            left: result.left,
            right: result.right,
            interpretation: result.interpretation,
            metadata: metadata
        )
    }
}
