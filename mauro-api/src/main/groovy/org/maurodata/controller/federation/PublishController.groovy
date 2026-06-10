package org.maurodata.controller.federation

import io.swagger.v3.oas.annotations.Operation
import groovy.util.logging.Slf4j
import org.maurodata.ErrorHandler
import org.maurodata.api.Paths
import org.maurodata.api.federation.PublishApi
import org.maurodata.audit.Audit
import org.maurodata.domain.authority.Authority
import org.maurodata.domain.facet.federation.PublishService
import org.maurodata.domain.facet.federation.response.AuthorityResponse
import org.maurodata.domain.facet.federation.response.PublishedModelResponse
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Model
import org.maurodata.domain.security.Role
import org.maurodata.persistence.cache.ModelCacheableRepository
import org.maurodata.persistence.service.RepositoryService
import org.maurodata.security.AccessControlService
import org.maurodata.service.core.AuthorityService

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.AuthorizationException
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject

@Slf4j
@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class PublishController implements PublishApi {
    final RepositoryService repositoryService
    final PublishService publishService
    final AccessControlService accessControlService
    final AuthorityService authorityService

    @Inject
    PublishController(RepositoryService repositoryService, PublishService publishService, AccessControlService accessControlService,
                      AuthorityService authorityService) {
        this.repositoryService = repositoryService
        this.publishService = publishService
        this.accessControlService = accessControlService
        this.authorityService = authorityService
    }

    @Audit
    @Operation(operationId = 'showPublishedModels', summary = "Get a publish", description = "Returns a publish. It is available to authenticated users.")
    @Get(Paths.PUBLISHED_MODELS)
    PublishedModelResponse show() {
        accessControlService.checkAuthenticated()
        PublishedModelResponse publishedModelResponse
        try {
            List<Model> finalisedModels = getFinalisedModelsForDefaultAuthority()
            Authority defaultAuthority = authorityService.getDefaultAuthority()
            publishedModelResponse = new PublishedModelResponse(new AuthorityResponse().tap {
                label = defaultAuthority.label
                url = defaultAuthority.url
            }, publishService.getPublishedModels(finalisedModels))
            return publishedModelResponse
        }
        catch (Exception e) {
            new PublishedModelResponse(null, Collections.emptyList())
        }
    }


    @Audit
    @Operation(summary = "List the publishes", description = "Returns the publishes.")
    @Get(Paths.PUBLISHED_MODELS_NEWER_VERSIONS)
    PublishedModelResponse newerVersions(@NonNull UUID publishedModelId) {
        PublishedModelResponse publishedModelResponse
        try {
            List<Model> finalisedModels = getFinalisedModelsForDefaultAuthority()
            Model publishedVersion = finalisedModels.find {it.id == publishedModelId}
            ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, publishedVersion, "Published model with Id $publishedModelId not found")

            publishedModelResponse = new PublishedModelResponse(null, publishService.getPublishedModels(finalisedModels.findAll {
                it.id != publishedVersion.id && it.label == publishedVersion.label && it.modelVersion > publishedVersion.modelVersion
            }))
        } catch (AuthorizationException e) {
            publishedModelResponse = new PublishedModelResponse(null, Collections.emptyList())
        }
        publishedModelResponse
    }

    protected List<Model> getFinalisedModelsForDefaultAuthority() throws AuthorizationException {
        repositoryService.modelCacheableRepositories.sort(false) {}.collectMany {ModelCacheableRepository modelCacheableRepository ->
            modelCacheableRepository.readAllByFinalisedTrue()
                .collect {
                    ((Model) it).authority.defaultAuthority == true
                    accessControlService.checkRole(Role.READER, it as AdministeredItem)
                    it
                }
                .sort()
        } as List<Model>
    }
}
