package org.maurodata.controller.facet

import org.maurodata.api.Paths
import org.maurodata.api.facet.MetadataApi
import org.maurodata.audit.Audit

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.security.Role
import org.maurodata.persistence.cache.FacetCacheableRepository
import org.maurodata.web.ListResponse

@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class MetadataController extends FacetController<Metadata> implements MetadataApi {

    /**
     * Properties disallowed in a simple update request.
     */
    List<String> getDisallowedProperties() {
        super.getDisallowedProperties() + ['multiFacetAwareItemDomainType', 'multiFacetAwareItemId']
    }

    FacetCacheableRepository.MetadataCacheableRepository metadataRepository

    MetadataController(FacetCacheableRepository.MetadataCacheableRepository metadataRepository) {
        super(metadataRepository)
        this.metadataRepository = metadataRepository
    }

    @Audit
    @Get(Paths.METADATA_LIST)
    ListResponse<Metadata> list(String domainType, UUID domainId) {
        AdministeredItem administeredItem = findAdministeredItem(domainType, domainId)
        accessControlService.checkRole(Role.READER, administeredItem)
        ListResponse.from(!administeredItem.metadata ? []: administeredItem.metadata)
    }

    @Audit
    @Get(Paths.METADATA_ID)
    Metadata show(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID id) {
        accessControlService.checkRole(Role.READER, readAdministeredItem(domainType, domainId))
        Metadata validMetadata = super.validateAndGet(domainType, domainId, id) as Metadata
        validMetadata
    }

    @Audit
    @Put(Paths.METADATA_ID)
    Metadata update(@NonNull String domainType, @NonNull UUID domainId, UUID id, @Body @NonNull Metadata metadata) {
        super.update(id, metadata)
    }


    @Audit
    @Post(Paths.METADATA_LIST)
    Metadata create(@NonNull String domainType, @NonNull UUID domainId, @Body @NonNull Metadata metadata) {
        super.create(domainType, domainId, metadata) as Metadata
    }

    @Override
    @Audit(deletedObjectDomainType = Metadata)
    @Delete(Paths.METADATA_ID)
    HttpResponse delete(String domainType, UUID domainId, UUID id) {
        super.delete(id)
    }
}
