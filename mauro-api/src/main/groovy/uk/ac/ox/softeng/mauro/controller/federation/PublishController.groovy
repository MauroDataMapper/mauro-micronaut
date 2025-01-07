package uk.ac.ox.softeng.mauro.controller.federation

import uk.ac.ox.softeng.mauro.controller.Paths
import uk.ac.ox.softeng.mauro.domain.federation.PublishService
import uk.ac.ox.softeng.mauro.domain.federation.PublishedModelResponse
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.service.RepositoryService
import uk.ac.ox.softeng.mauro.security.AccessControlService

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.AuthorizationException
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject

@Slf4j
@Controller()
@CompileStatic
@Secured(SecurityRule.IS_ANONYMOUS)
class PublishController {
    final RepositoryService repositoryService
    final PublishService publishService
    final AccessControlService accessControlService

    @Inject
    PublishController(RepositoryService repositoryService, PublishService publishService, AccessControlService accessControlService) {
        this.repositoryService = repositoryService
        this.publishService = publishService
        this.accessControlService = accessControlService
    }

    @Get(Paths.PUBLISHED_MODELS_ROUTE)
    PublishedModelResponse show() {
        PublishedModelResponse publishedModelResponse
        //todo: Authority
        try {
            List<Model> finalisedModels = getFinalisedModels()
            publishedModelResponse = new PublishedModelResponse(null, publishService.getPublishedModels(finalisedModels))
        } catch (AuthorizationException e) {
            publishedModelResponse = new PublishedModelResponse(null, Collections.emptyList())
        }
        publishedModelResponse
    }


    @Get(Paths.PUBLISHED_MODELS_NEWER_VERSIONS_ROUTE)
    PublishedModelResponse newerVersions(@NonNull UUID id) {
        PublishedModelResponse publishedModelResponse
        try {
            List<Model> finalisedModels = getFinalisedModels()
            Model publishedVersion = finalisedModels.find {it.id == id}
            if (!publishedVersion) {
                throw new HttpStatusException(HttpStatus.NOT_FOUND, "Entity not found, $id")
            }
            publishedModelResponse = new PublishedModelResponse(null, publishService.getPublishedModels(finalisedModels.findAll {
                it.id != publishedVersion.id
                    && it.label == publishedVersion.label
                    && it.version > publishedVersion.version
            }))
        } catch (AuthorizationException e) {
            publishedModelResponse = new PublishedModelResponse(null, Collections.emptyList())
        }
        publishedModelResponse
    }

    protected List<Model> getFinalisedModels() throws AuthorizationException {
        repositoryService.modelCacheableRepositories.sort(false) {
        }.collectMany {ModelCacheableRepository modelCacheableRepository ->
            modelCacheableRepository.readAllByFinalisedTrue()
                .collect {
                    accessControlService.checkRole(Role.READER, it as AdministeredItem)
                    it
                }
                .sort()
        } as List<Model>
    }
}
