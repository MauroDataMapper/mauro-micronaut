package uk.ac.ox.softeng.mauro.controller.federation

import uk.ac.ox.softeng.mauro.ErrorHandler
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.federation.PublishApi
import uk.ac.ox.softeng.mauro.audit.Audit
import uk.ac.ox.softeng.mauro.domain.authority.Authority
import uk.ac.ox.softeng.mauro.domain.facet.federation.PublishService
import uk.ac.ox.softeng.mauro.domain.facet.federation.response.AuthorityResponse
import uk.ac.ox.softeng.mauro.domain.facet.federation.response.PublishedModelResponse
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.service.RepositoryService
import uk.ac.ox.softeng.mauro.security.AccessControlService
import uk.ac.ox.softeng.mauro.service.core.AuthorityService

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.AuthorizationException
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject

@Slf4j
@Controller('/api')
@CompileStatic
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
