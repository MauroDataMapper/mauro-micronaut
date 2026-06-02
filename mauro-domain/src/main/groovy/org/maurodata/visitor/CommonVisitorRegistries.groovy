package org.maurodata.visitor

import groovy.transform.CompileStatic
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Item
import org.maurodata.domain.model.Model

/**
 * Reusable bundles of visitor handlers for common traversal tasks.
 */
@CompileStatic
final class CommonVisitorRegistries {

    private CommonVisitorRegistries() {
    }

    static VisitorRegistry treeifyVisitor() {
        VisitorRegistry registry = new VisitorRegistry()
        registry.on(DataType) {DataType dataType ->
            dataType.referenceClass = replaceWithStub(dataType.referenceClass)
            dataType.modelResource = replaceWithStub(dataType.modelResource)
        }
        registry.on(Folder) { Folder folder ->
            folder.parent = null
        }
        registry.on(DataModel) {DataModel dataModel ->
            dataModel.folder = null
            dataModel.dataElements = []
        }
        return registry
    }

    static VisitorRegistry smallExport() {
        VisitorRegistry registry = new VisitorRegistry()
        registry.on(Item) {Item item ->
            item.dateCreated = null
            item.lastUpdated = null
            item.version = null
            item.catalogueUser = null
        }
        .on(AdministeredItem) { AdministeredItem administeredItem ->
            administeredItem.edits = []

        }
        .on (Model) { Model model ->
            model.readableByAuthenticatedUsers = null
            model.readableByEveryone = null
            model.finalised = null
            model.modelType = null
            model.deleted = null
        }
        return registry
    }

    private static <T extends AdministeredItem> T replaceWithStub(T administeredItem) {
        if (!administeredItem) {
            return null
        }
        Class<T> itemClass = (Class<T>) administeredItem.class
        T stub = itemClass.getDeclaredConstructor().newInstance()
        stub.id = administeredItem.id
        stub.label = administeredItem.label
        return stub
    }
}

