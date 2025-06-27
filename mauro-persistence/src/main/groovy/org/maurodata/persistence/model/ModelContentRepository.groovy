package org.maurodata.persistence.model

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Singleton
import org.maurodata.domain.facet.Annotation
import org.maurodata.domain.facet.Facet
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.facet.ReferenceFile
import org.maurodata.domain.facet.Rule
import org.maurodata.domain.facet.RuleRepresentation
import org.maurodata.domain.facet.SemanticLink
import org.maurodata.domain.facet.SummaryMetadata
import org.maurodata.domain.facet.SummaryMetadataReport
import org.maurodata.domain.facet.VersionLink
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Model

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Singleton
import org.maurodata.domain.facet.*
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Model

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

    M saveContentOnly(@NonNull M model) {
        List<Collection<AdministeredItem>> associations = model.getAllAssociations()

        associations.each {association ->
            if (association) {
                Collection<AdministeredItem> savedAssociation = getRepository(association.first()).saveAll((Collection<AdministeredItem>) association)
                saveAllFacets(savedAssociation)
            }
        }
        model
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
        saveRules(items)
        saveAnnotations(items)
        saveReferenceFiles(items)
        saveVersionLinks(items)
        saveSemanticLinks(items)
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
                    if (it.summaryMetadataReports) {
                        it.summaryMetadataReports.forEach {
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

    void saveRules(List<AdministeredItem> items) {
        List<Rule> rules = []
        List<RuleRepresentation> ruleRepresentations = []
        Rule saved
        items.each {item ->
            if (item.rules) {
                item.rules.each {
                    updateMultiAwareData(item, it)
                    saved = ruleRepository.save(it)
                    if (it.ruleRepresentations) {
                        it.ruleRepresentations.forEach {
                            representation ->
                                representation.ruleId = saved.id
                        }
                        ruleRepresentations.addAll(ruleRepresentationCacheableRepository.saveAll(it.ruleRepresentations))
                    }
                    rules.add(saved)
                }
            }
        }
    }

    void saveAnnotations(List<AdministeredItem> items) {
        List<Annotation> annotations = []
        items.each {item ->
            if (item.annotations) {
                item.annotations.each {
                    updateMultiAwareData(item, it)
                    if (it.childAnnotations) {
                        it.childAnnotations.forEach {child ->
                            updateMultiAwareData(item, child)
                            child.parentAnnotationId = it.id
                        }
                    }
                }
                annotations.addAll(item.annotations)
            }
        }
        annotationCacheableRepository.saveAll(annotations)
    }

    void saveReferenceFiles(List<AdministeredItem> items) {
        List<ReferenceFile> referenceFiles = []
        items.each {item ->
            if (item.referenceFiles) {
                item.referenceFiles.each {
                    updateMultiAwareData(item, it)
                }
                referenceFiles.addAll(item.referenceFiles)
            }
        }
        referenceFileCacheableRepository.saveAll(referenceFiles)
    }

    void saveVersionLinks(List<AdministeredItem> items) {
        List<VersionLink> versionLinks = []
        items.each {item ->
            if(item instanceof Model) {
                final Model modelItem=(Model) item
                if (modelItem.versionLinks) {
                    modelItem.versionLinks.each {
                        updateMultiAwareData(item, it)
                    }
                    versionLinks.addAll(modelItem.versionLinks)
                }
            }
        }
        versionLinkCacheableRepository.saveAll(versionLinks)
    }

    void saveSemanticLinks(List<AdministeredItem> items) {
        List<SemanticLink> SemanticLinks = []
        items.each {item ->
            if (item.semanticLinks) {
                item.semanticLinks.each {
                    updateMultiAwareData(item, it)
                }
                SemanticLinks.addAll(item.semanticLinks)
            }
        }
        semanticLinkCacheableRepository.saveAll(SemanticLinks)
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