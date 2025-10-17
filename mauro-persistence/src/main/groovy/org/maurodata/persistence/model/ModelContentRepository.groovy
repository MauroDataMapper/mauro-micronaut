package org.maurodata.persistence.model

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Inject
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
import org.maurodata.persistence.ContentsService

@Slf4j
@CompileStatic
@Singleton
class ModelContentRepository<M extends Model> extends AdministeredItemContentRepository {

    @Inject
    ContentsService contentsService

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


    protected List<M> findAllModelsForFolder(ModelRepository modelRepository, Folder folder) {
        modelRepository.findAllByFolderId(folder.id)
    }

    private void updateMultiAwareData(AdministeredItem item, Facet it) {
        it.multiFacetAwareItemDomainType = item.domainType
        it.multiFacetAwareItemId = item.id
        it.multiFacetAwareItem = item
    }

    Boolean handles(String domainType) {
        administeredItemRepository.handles(domainType)
    }

    @Override
    Boolean handles(Class clazz) {
        Model.isAssignableFrom(clazz) && clazz != Model
    }
}