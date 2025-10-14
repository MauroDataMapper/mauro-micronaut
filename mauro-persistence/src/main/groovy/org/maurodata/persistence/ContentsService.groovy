package org.maurodata.persistence

import groovy.util.logging.Slf4j
import io.micronaut.context.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Model
import org.maurodata.domain.security.CatalogueUser

@Slf4j
@Singleton
class ContentsService {

    @Inject ApplicationContext applicationContext

    AdministeredItem saveWithContent(AdministeredItem item, CatalogueUser catalogueUser) {
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


}
