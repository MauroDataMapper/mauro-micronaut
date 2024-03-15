package uk.ac.ox.softeng.mauro.controller.facet

import groovy.transform.CompileStatic
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadata
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.persistence.cache.SummaryMetadataCacheableRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

@CompileStatic
@Controller('/{domainType}/{domainId}/summaryMetadata')
class SummaryMetadataController extends FacetController<SummaryMetadata> {


    SummaryMetadataCacheableRepository summaryMetadataRepository

    /**
     * Properties disallowed in a simple update request.
     */
    List<String> getDisallowedProperties() {
        super.getDisallowedProperties() + ['multiFacetAwareItemDomainType', 'multiFacetAwareItemId']
    }

    SummaryMetadataController(SummaryMetadataCacheableRepository summaryMetadataRepository) {
        super(summaryMetadataRepository)
        this.summaryMetadataRepository = summaryMetadataRepository
    }

    @Get
    ListResponse<SummaryMetadata> list(String domainType, UUID domainId) {
        AdministeredItem administeredItem = findAdministeredItem(domainType, domainId)
        ListResponse.from(administeredItem.summaryMetadata)
    }

}
