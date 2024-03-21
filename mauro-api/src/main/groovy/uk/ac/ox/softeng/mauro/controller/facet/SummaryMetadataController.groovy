package uk.ac.ox.softeng.mauro.controller.facet

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import io.micronaut.transaction.annotation.Transactional
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadata
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.persistence.cache.FacetCacheableRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

@CompileStatic
@Controller('/{domainType}/{domainId}/summaryMetadata')
class SummaryMetadataController extends FacetController<SummaryMetadata> {


    FacetCacheableRepository.SummaryMetadataCacheableRepository summaryMetadataRepository

    /**
     * Properties disallowed in a simple update request.
     */
    List<String> getDisallowedProperties() {
        super.getDisallowedProperties() + ['multiFacetAwareItemDomainType', 'multiFacetAwareItemId']
    }

    SummaryMetadataController(FacetCacheableRepository.SummaryMetadataCacheableRepository summaryMetadataRepository) {
        super(summaryMetadataRepository)
        this.summaryMetadataRepository = summaryMetadataRepository
    }

    @Get
    ListResponse<SummaryMetadata> list(String domainType, UUID domainId) {
        AdministeredItem administeredItem = findAdministeredItem(domainType, domainId)
        ListResponse.from(administeredItem.summaryMetadata)
    }

    @Get('/{id}')
    SummaryMetadata show(UUID id) {
        super.show(id)
    }

    @Post
    SummaryMetadata create(String domainType, UUID domainId, @Body @NonNull SummaryMetadata summaryMetadata) {
        super.create(domainType,domainId, summaryMetadata )
    }

    @Put('/{id}')
    SummaryMetadata update(UUID id, @Body @NonNull SummaryMetadata summaryMetadata) {
        super.update(id, summaryMetadata)
    }

    @Delete('/{id}')
    @Transactional
    HttpStatus delete(UUID id, @Body @Nullable SummaryMetadata summaryMetadata) {
        super.delete(id, summaryMetadata)
    }
}