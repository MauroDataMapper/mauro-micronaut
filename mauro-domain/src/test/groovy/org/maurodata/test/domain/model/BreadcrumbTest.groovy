package org.maurodata.test.domain.model

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.Breadcrumb
import spock.lang.Specification

@MicronautTest
class BreadcrumbTest extends Specification {

    void 'breadcrumbs include this item, the owning model and all containing folders'() {
        given:
        Folder catalogueFolder = new Folder(id: UUID.randomUUID(), label: 'Catalogue')
        Folder nestedFolder = new Folder(id: UUID.randomUUID(), label: 'Nested folder', parentFolder: catalogueFolder)
        DataModel model = new DataModel(id: UUID.randomUUID(), label: 'Model', folder: nestedFolder, branchName: 'main')
        DataClass dataClass = new DataClass(id: UUID.randomUUID(), label: 'Data class', dataModel: model)

        when:
        List<Breadcrumb> breadcrumbs = dataClass.updateBreadcrumbs()

        then:
        breadcrumbs*.label == ['Catalogue', 'Nested folder', 'Model', 'Data class']
        dataClass.breadcrumbs.is(breadcrumbs)
    }

    void 'breadcrumbs for a model include itself and its containing folders'() {
        given:
        Folder catalogueFolder = new Folder(id: UUID.randomUUID(), label: 'Catalogue')
        Folder nestedFolder = new Folder(id: UUID.randomUUID(), label: 'Nested folder', parentFolder: catalogueFolder)
        DataModel model = new DataModel(id: UUID.randomUUID(), label: 'Model', folder: nestedFolder, modelVersionTag: 'published')

        when:
        List<Breadcrumb> breadcrumbs = model.updateBreadcrumbs()

        then:
        breadcrumbs*.label == ['Catalogue', 'Nested folder', 'Model']
    }
}
