package uk.ac.ox.softeng.mauro.persistence.model

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Singleton
import uk.ac.ox.softeng.mauro.domain.facet.Annotation
import uk.ac.ox.softeng.mauro.domain.facet.Facet
import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.facet.ReferenceFile
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadata
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadataReport

@Slf4j
@CompileStatic
@Singleton
class ModelContentRepository<M extends Model> extends AdministeredItemContentRepository {

    M findWithContentById(UUID id) {
        (M) administeredItemRepository.findById(id)
    }

    M saveWithContent(@NonNull M model) {
        List<Collection<AdministeredItem>> associations = model.getAllAssociations()
        M saved = (M) getRepository(model).save(model)

        saveAllFacets(saved)
        associations.each {association ->
            if (association) {
                Collection<AdministeredItem> savedAssociation = getRepository(association.first()).saveAll((Collection<AdministeredItem>) association)
                saveAllFacets(savedAssociation)
            }
        }
        saved
    }

    void saveAllFacets(@NonNull AdministeredItem item) {
        saveAllFacets([item])
    }

    void saveAllFacets(List<AdministeredItem> items) {
        List<Metadata> metadata = []

        items.each {item ->
            if (item.metadata) {
                item.metadata.each {
                    updateMultiAwareData(item, it)
                }
                metadata.addAll(item.metadata)
            }
        }
        metadataRepository.saveAll(metadata)
        saveSummaryMetadataFacets(items)
        saveAnnotations(items)
        saveReferenceFiles(items)
    }

   void saveSummaryMetadataFacets(List<AdministeredItem> items) {
       List<SummaryMetadata> summaryMetadata = []
       List<SummaryMetadataReport> summaryMetadataReports = []
       SummaryMetadata saved
       items.each {item ->
           if (item.summaryMetadata) {
               item.summaryMetadata.each {
                   updateMultiAwareData(item, it)
                   saved = summaryMetadataRepository.save(it)
                   if (it.summaryMetadataReports){
                       it.summaryMetadataReports.forEach{
                           report ->
                               report.summaryMetadataId = saved.id
                       }
                       summaryMetadataReports.addAll(summaryMetadataReportCacheableRepository.saveAll(it.summaryMetadataReports))
                   }
                   summaryMetadata.add(saved)
               }
           }
       }
   }

    void saveAnnotations(List<AdministeredItem> items) {
        List<Annotation> annotations = []
        items.each { item ->
            if (item.annotations) {
                item.annotations.each {
                    updateMultiAwareData(item, it)
                    if (it.childAnnotations) {
                        it.childAnnotations.forEach { child ->
                            updateMultiAwareData(item, child)
                            child.parentAnnotationId = it.id
                        }
                    }
                }
                annotations.addAll(item.annotations)
                annotationCacheableRepository.saveAll(annotations)
            }
        }
    }

    void saveReferenceFiles(List<AdministeredItem> items) {
        List<ReferenceFile> referenceFiles = []
        items.each { item ->
            if (item.referenceFiles) {
                item.referenceFiles.each {
                    updateMultiAwareData(item, it)
                }
                referenceFiles.addAll(item.referenceFiles)
                referenceFileCacheableRepository.saveAll(referenceFiles)
            }
        }
    }

    protected List<M> findAllModelsForFolder(ModelRepository modelRepository, Folder folder) {
        modelRepository.findAllByFolderId(folder.id)
    }

    private void updateMultiAwareData(AdministeredItem item, Facet it) {
        it.multiFacetAwareItemDomainType = item.domainType
        it.multiFacetAwareItemId = item.id
        it.multiFacetAwareItem = item
    }
}