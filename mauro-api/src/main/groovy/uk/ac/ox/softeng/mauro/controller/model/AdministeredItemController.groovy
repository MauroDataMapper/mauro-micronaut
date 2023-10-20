package uk.ac.ox.softeng.mauro.controller.model

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem


abstract class AdministeredItemController<I extends AdministeredItem> {

    /**
     * Properties disallowed in a simple update request.
     */
    List<String> getDisallowedProperties() {
        ['id', 'version', 'dateCreated', 'lastUpdated', 'domainType', 'createdBy', 'path', /*'breadcrumbTree',*/ 'parent', 'owner']
    }

    /**
     * Properties that trigger a cascaded update when updated.
     */
    List<String> getCascadeUpdateProperties() {
        ['label', 'path']
    }


}
