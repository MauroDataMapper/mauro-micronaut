package uk.ac.ox.softeng.mauro.controller.config

import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.config.ApiPropertyApi
import uk.ac.ox.softeng.mauro.audit.Audit
import uk.ac.ox.softeng.mauro.web.PaginationParams

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import uk.ac.ox.softeng.mauro.controller.model.ItemController
import uk.ac.ox.softeng.mauro.domain.config.ApiProperty
import uk.ac.ox.softeng.mauro.domain.model.Item
import uk.ac.ox.softeng.mauro.persistence.cache.ItemCacheableRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

@CompileStatic
@Slf4j
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class ApiPropertyController extends ItemController<ApiProperty> implements ApiPropertyApi {

    ItemCacheableRepository.ApiPropertyCacheableRepository apiPropertyRepository

    ApiPropertyController(ItemCacheableRepository.ApiPropertyCacheableRepository apiPropertyRepository) {
        super(apiPropertyRepository)
        this.apiPropertyRepository = apiPropertyRepository
    }

    @Override
    List<String> getDisallowedProperties() {
        super.getDisallowedProperties() + ['lastUpdatedBy']
    }

    @Audit
    @Get(Paths.API_PROPERTY_LIST_PUBLIC_PAGED)
    ListResponse<ApiProperty> listPubliclyVisible(@Nullable PaginationParams params = new PaginationParams()) {
        ListResponse.from(apiPropertyRepository.findAllByPubliclyVisibleTrue(), params)
    }

    @Audit
    @Get(Paths.API_PROPERTY_LIST_ALL_PAGED)
    ListResponse<ApiProperty> listAll(@Nullable PaginationParams params = new PaginationParams()) {
        accessControlService.checkAdministrator()
        ListResponse.from(apiPropertyRepository.findAll())
    }

    @Audit
    @Get(Paths.API_PROPERTY_SHOW)
    ApiProperty show(UUID id) {
        accessControlService.checkAdministrator()

        apiPropertyRepository.findById(id)
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Post(Paths.API_PROPERTY_LIST_ALL)
    ApiProperty create(@Body @NonNull ApiProperty apiProperty) {
        accessControlService.checkAdministrator()

        cleanBody(apiProperty)

        updateCreationProperties(apiProperty)

        apiPropertyRepository.save(apiProperty)
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Put(Paths.API_PROPERTY_SHOW)
    ApiProperty update(UUID id, @Body @NonNull ApiProperty apiProperty) {
        accessControlService.checkAdministrator()

        cleanBody(apiProperty)
        ApiProperty existing = apiPropertyRepository.readById(id)

        boolean hasChanged = updateProperties(existing, apiProperty)

        if (hasChanged) {
            apiPropertyRepository.update(existing)
        } else {
            existing
        }
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Delete(Paths.API_PROPERTY_SHOW)
    HttpResponse delete(UUID id, @Body @Nullable ApiProperty apiProperty) {
        accessControlService.checkAdministrator()

        ApiProperty apiPropertyToDelete = apiPropertyRepository.readById(id)

        if (apiProperty?.version) apiPropertyToDelete.version = apiProperty.version
        Long deleted = apiPropertyRepository.delete(apiPropertyToDelete)
        if (deleted) {
            HttpResponse.status(HttpStatus.NO_CONTENT)
        } else {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, 'Not found for deletion')
        }
    }

    @Override
    protected ApiProperty updateCreationProperties(Item item) {
        super.updateCreationProperties(item)
        ((ApiProperty) item).lastUpdatedBy = accessControlService.user
        (ApiProperty) item
    }
}
