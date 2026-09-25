package org.maurodata.service.datamodel

import groovy.transform.CompileStatic
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.comparison.ComparisonContext
import org.maurodata.domain.comparison.ComparisonResult
import org.maurodata.domain.model.Model
import org.maurodata.domain.security.Role
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.Terminology
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.DataTypeCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository
import org.maurodata.persistence.service.RepositoryService
import org.maurodata.plugin.MauroPluginService
import org.maurodata.plugin.datatype.DataTypeComparisonProviderPlugin
import org.maurodata.security.AccessControlService

@CompileStatic
@Singleton
class DataTypeComparisonService {

    final DataTypeCacheableRepository dataTypeRepository
    final MauroPluginService mauroPluginService
    final RepositoryService repositoryService
    final AccessControlService accessControlService

    @Inject
    DataTypeComparisonService(DataTypeCacheableRepository dataTypeRepository, MauroPluginService mauroPluginService, RepositoryService repositoryService,
                              AccessControlService accessControlService) {
        this.dataTypeRepository = dataTypeRepository
        this.mauroPluginService = mauroPluginService
        this.repositoryService = repositoryService
        this.accessControlService = accessControlService
    }

    List<ComparisonResult> compare(UUID leftId, UUID rightId) {
        DataType left = dataTypeRepository.readById(leftId)
        if (!left) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "DataType ${leftId} not found")
        }
        accessControlService.checkRole(Role.READER, left)
        left = dataTypeRepository.loadWithContent(left.id)

        DataType right = dataTypeRepository.readById(rightId)
        if (!right) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "DataType ${rightId} not found")
        }
        accessControlService.checkRole(Role.READER, right)
        right = dataTypeRepository.loadWithContent(right.id)

        compare(left, right)
    }

    List<ComparisonResult> compare(DataType left, DataType right) {
        ComparisonContext context = new ComparisonContext(
            leftModelResource: loadComparableModelResource(left),
            rightModelResource: loadComparableModelResource(right)
        )
        mauroPluginService.listPlugins(DataTypeComparisonProviderPlugin).collectMany {DataTypeComparisonProviderPlugin provider ->
            provider.compare(left, right, context)
        }
    }

    private Model loadComparableModelResource(DataType dataType) {
        if (dataType.dataTypeKind != DataType.DataTypeKind.MODEL_TYPE || !dataType.modelResourceDomainType || !dataType.modelResourceId) {
            return null
        }

        if (!(dataType.modelResourceDomainType in [Terminology.simpleName, CodeSet.simpleName])) {
            return null
        }

        ModelCacheableRepository repository = repositoryService.getModelRepository(dataType.modelResourceDomainType)
        if (!repository) {
            return null
        }

        Model model = (Model) repository.readById(dataType.modelResourceId)
        if (!model) {
            return null
        }

        accessControlService.checkRole(Role.READER, model)
        (Model) repository.loadWithContent(model.id)
    }
}
