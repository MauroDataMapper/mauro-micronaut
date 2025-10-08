package org.maurodata.persistence

import groovy.util.logging.Slf4j
import io.micronaut.context.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.model.AdministeredItem

@Slf4j
@Singleton
class ContentsService {

    @Inject ApplicationContext applicationContext

    AdministeredItem saveWithContent(AdministeredItem item) {
        ContentHandler contentHandler = applicationContext.createBean(ContentHandler)
        contentHandler.shred(item)
        contentHandler.saveWithContent()
        return item
    }

    boolean deleteWithContent(AdministeredItem item) {
        ContentHandler contentHandler = applicationContext.createBean(ContentHandler)
        contentHandler.shred(item)
        return contentHandler.deleteWithContent()
    }

    AdministeredItem loadWithContent(UUID uuid) {
        ContentHandler contentHandler = applicationContext.createBean(ContentHandler)
        contentHandler.loadFolderWithContent(uuid)
    }


}
