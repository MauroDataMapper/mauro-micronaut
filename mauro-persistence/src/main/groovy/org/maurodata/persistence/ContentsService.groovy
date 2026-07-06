package org.maurodata.persistence

import groovy.util.logging.Slf4j
import jakarta.inject.Provider
import jakarta.inject.Singleton
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.security.CatalogueUser
import org.maurodata.persistence.shredder.ShredVisitor
import org.maurodata.persistence.shredder.ShreddedContent
import org.maurodata.visitor.GenericDomainTraversalVisitor
import org.maurodata.visitor.common.RemoveIdVisitor
import org.maurodata.visitor.common.SetCreatePropertiesVisitor

@Slf4j
@Singleton
class ContentsService {

    private final Provider<ContentHandler> contentHandlerProvider

    ContentsService(Provider<ContentHandler> contentHandlerProvider) {
        this.contentHandlerProvider = contentHandlerProvider
    }

    private ContentHandler getContentHandler() {
        // Resolve lazily to avoid creating a startup-time circular dependency.
        contentHandlerProvider.get()
    }

    AdministeredItem saveWithContent(AdministeredItem item, CatalogueUser catalogueUser = null, boolean resetIds = false) {
        contentHandler.saveWithContent(shred(item, catalogueUser, true, resetIds))
        return item
    }

    DataModel saveContentOnly(DataModel dataModel, CatalogueUser catalogueUser = null, boolean resetIds = false) {
        ShreddedContent shreddedContent = shred(dataModel, catalogueUser, true)
        shreddedContent.dataModels = []
        contentHandler.saveWithContent(shreddedContent)
        return dataModel
    }

    boolean deleteWithContent(AdministeredItem item) {
        return contentHandler.deleteWithContent(shred(item, null, false))
    }

    <T extends AdministeredItem> T loadWithContent(T administeredItem) {
        ShreddedContent shreddedContent = new ShreddedContent(administeredItem)
        contentHandler.loadContent(shreddedContent)
        return administeredItem
    }

    static ShreddedContent shred(AdministeredItem item, CatalogueUser catalogueUser = null, boolean setCreateProperties = false, boolean resetIds = false) {
        item.setAssociations()
        ShredVisitor shredVisitor = new ShredVisitor()
        GenericDomainTraversalVisitor visitor = shredVisitor
        if(setCreateProperties) {
            visitor += new SetCreatePropertiesVisitor(catalogueUser)
        }
        if(resetIds) {
            visitor += new RemoveIdVisitor()
        }
        item.accept(visitor)
        return shredVisitor.shreddedContent
    }


}
