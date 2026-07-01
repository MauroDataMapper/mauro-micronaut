package org.maurodata.persistence

import groovy.util.logging.Slf4j
import io.micronaut.context.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Model
import org.maurodata.domain.security.CatalogueUser
import org.maurodata.persistence.shredder.ShredVisitor
import org.maurodata.visitor.GenericDomainTraversalVisitor
import org.maurodata.visitor.common.SetCreatePropertiesVisitor
import org.maurodata.visitor.common.SmallExportVisitor
import org.maurodata.visitor.common.TreeifyVisitor

@Slf4j
@Singleton
class ContentsService {

    @Inject ApplicationContext applicationContext

    AdministeredItem importWithContent(AdministeredItem item, CatalogueUser catalogueUser = null) {
        ContentHandler contentHandler = applicationContext.createBean(ContentHandler)
        item.setAssociations()
        contentHandler.shred(item)
        contentHandler.setCreateProperties(catalogueUser, true)
        contentHandler.saveWithContent()
        return item
    }

    AdministeredItem saveWithContent(AdministeredItem item, CatalogueUser catalogueUser = null) {
        ContentHandler contentHandler = applicationContext.createBean(ContentHandler)
        item.setAssociations()

        ShredVisitor shredVisitor = new ShredVisitor()
        GenericDomainTraversalVisitor visitor = new SetCreatePropertiesVisitor(catalogueUser) + shredVisitor
        item.accept(visitor)

        contentHandler.saveWithContent(shredVisitor.shreddedContent)
        return item
    }

    DataModel saveContentOnly(DataModel dataModel, CatalogueUser catalogueUser = null) {
        ContentHandler contentHandler = applicationContext.createBean(ContentHandler)
        dataModel.setAssociations()
        contentHandler.shred(dataModel)
        contentHandler.dataModels = [] as Set
        //contentHandler.setCreateProperties(catalogueUser)
        contentHandler.saveWithContent()
        return dataModel
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

    AdministeredItem loadWithContent(AdministeredItem item) {
        ContentHandler contentHandler = applicationContext.createBean(ContentHandler)
        contentHandler.loadWithContent(item)
        item.setAssociations()
        return item
    }

}
