package uk.ac.ox.softeng.mauro.persistence.model


import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.Model

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Singleton

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


    protected List<M> findAllModelsForFolder(ModelRepository modelRepository, Folder folder) {
        modelRepository.findAllByFolderId(folder.id)
    }

}