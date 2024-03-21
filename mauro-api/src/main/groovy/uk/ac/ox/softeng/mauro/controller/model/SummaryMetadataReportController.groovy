package uk.ac.ox.softeng.mauro.controller.model

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import reactor.util.annotation.NonNull
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadata
import uk.ac.ox.softeng.mauro.domain.model.SummaryMetadataReport
import uk.ac.ox.softeng.mauro.persistence.cache.FacetCacheableRepository.SummaryMetadataCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.model.SummaryMetadataReportRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

@CompileStatic
@Slf4j
@Controller('/{domainType}/{domainId}/summaryMetadata/{summaryMetadataId}/summaryMetadataReports')
class SummaryMetadataReportController extends ItemController<SummaryMetadataReport> {

    @Inject
    SummaryMetadataReportRepository summaryMetadataReportRepository
    @Inject
    SummaryMetadataCacheableRepository summaryMetadataCacheableRepository

    ItemCacheableRepository.SummaryMetadataReportCacheableRepository summaryMetadataReportCacheableRepository
    /**
     * Properties disallowed in a simple update request.
     */
    List<String> getDisallowedProperties() {
        super.getDisallowedProperties()
    }

    SummaryMetadataReportController(ItemCacheableRepository.SummaryMetadataReportCacheableRepository summaryMetadataReportCacheableRepository) {
        super(summaryMetadataReportCacheableRepository)
        this.summaryMetadataReportCacheableRepository = summaryMetadataReportCacheableRepository
    }

    @Post
    SummaryMetadataReport create(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID summaryMetadataId,
                                 @Body SummaryMetadataReport summaryMetadataReport) {
        super.cleanBody(summaryMetadataReport)
        SummaryMetadata summaryMetadata = summaryMetadataCacheableRepository.readById(summaryMetadataId)
        if (!summaryMetadata)
            throw new HttpStatusException(HttpStatus.NOT_FOUND, 'summaryMetadata not found: $summaryMetadataId')
        summaryMetadataReport.summaryMetadataId = summaryMetadata.id
        updateCreationProperties(summaryMetadataReport)
        summaryMetadataReportCacheableRepository.save(summaryMetadataReport)
    }

    @Get
    ListResponse<SummaryMetadataReport> list(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID summaryMetadataId) {
        List<SummaryMetadataReport> summaryMetadataReportList = summaryMetadataReportRepository.findAllBySummaryMetadataId(summaryMetadataId)
        summaryMetadataReportList.forEach(it -> summaryMetadataReportCacheableRepository.cache(it))
        ListResponse.from(summaryMetadataReportList)
    }

    @Get('/{id}')
    SummaryMetadataReport get(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID summaryMetadataId,
                              @NonNull UUID id) {
        summaryMetadataReportCacheableRepository.findById(id)
    }

    @Put('/{id}')
    SummaryMetadataReport update(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID summaryMetadataId,
                                 @NonNull UUID id, @Body @NonNull SummaryMetadataReport summaryMetadataReport) {
        super.cleanBody(summaryMetadataReport)
        SummaryMetadataReport existing = summaryMetadataReportCacheableRepository.readById(id)
        updateEntity(existing, summaryMetadataReport)
        summaryMetadataReport
    }

    @Delete('/{id}')
    @Transactional
    HttpStatus delete(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID summaryMetadataId,
                      @NonNull UUID id) {
        SummaryMetadataReport summaryMetadataReport = summaryMetadataReportCacheableRepository.readById(id)
        if (!summaryMetadataReport) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND,
                    "Summary metadata report with id : $id not found, cannot delete")
        }
        summaryMetadataReportCacheableRepository.delete(summaryMetadataReport)
        HttpStatus.NO_CONTENT
    }


    private SummaryMetadataReport updateEntity(@NonNull SummaryMetadataReport existing,
                                               @NonNull SummaryMetadataReport cleaned) {
        boolean hasChanged = updateProperties(existing, cleaned)
        if (hasChanged) {
            summaryMetadataReportRepository.update(existing)
        } else {
            existing
        }
    }
}