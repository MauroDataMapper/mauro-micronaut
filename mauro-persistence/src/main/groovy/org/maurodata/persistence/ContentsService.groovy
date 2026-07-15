package org.maurodata.persistence

import groovy.util.logging.Slf4j
import jakarta.inject.Provider
import jakarta.inject.Singleton
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.security.CatalogueUser
import org.maurodata.shredder.ShredVisitor
import org.maurodata.shredder.ShreddedContent
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
        // Cache the dataModel id, because the shredder will reset it to null if resetIds is true
        UUID dataModelId = dataModel.id
        ShreddedContent shreddedContent = shred(dataModel, catalogueUser, true, resetIds)
        dataModel.id = dataModelId
        shreddedContent.dataModels = []
        contentHandler.saveWithContent(shreddedContent)
        dataModel.setAssociations()
        return dataModel
    }

    void saveShreddedContent(ShreddedContent shreddedContent) {
        contentHandler.saveWithContent(shreddedContent)
    }

    boolean deleteWithContent(AdministeredItem item) {
        return contentHandler.deleteWithContent(shred(item, null, false))
    }

    <T extends AdministeredItem> T loadWithContent(T administeredItem) {
        ShreddedContent shreddedContent = new ShreddedContent(administeredItem)
        contentHandler.loadContent(shreddedContent)
        administeredItem.setAssociations()
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
