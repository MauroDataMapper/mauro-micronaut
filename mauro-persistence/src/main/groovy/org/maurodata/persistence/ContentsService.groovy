package org.maurodata.persistence

import groovy.util.logging.Slf4j
import io.micronaut.context.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Model
import org.maurodata.domain.security.CatalogueUser
import org.maurodata.persistence.datamodel.DataModelRepository

@Slf4j
@Singleton
class ContentsService {

    @Inject ApplicationContext applicationContext

    @Inject DataModelRepository dataModelRepository

    AdministeredItem saveWithContent(AdministeredItem item, CatalogueUser catalogueUser = null) {
        ContentHandler contentHandler = applicationContext.createBean(ContentHandler)
        item.setAssociations()
        contentHandler.shred(item)
        contentHandler.setCreateProperties(catalogueUser)
        contentHandler.saveWithContent()
        return item
    }

    boolean deleteWithContent(AdministeredItem item) {
        ContentHandler contentHandler = applicationContext.createBean(ContentHandler)
        item.setAssociations()
        contentHandler.shred(item)
        return contentHandler.deleteWithContent()
    }

    AdministeredItem loadWithContent(Model model) {
        ContentHandler contentHandler = applicationContext.createBean(ContentHandler)
        contentHandler.loadWithContent(model)
        model.setAssociations()
        return model
    }

    DataModel loadDataModelWithContent(UUID id) {
        ContentHandler contentHandler = applicationContext.createBean(ContentHandler)
        DataModel dataModel = dataModelRepository.readById(id)
        contentHandler.loadWithContent(dataModel)
        dataModel.setAssociations()
        return dataModel
    }


}
