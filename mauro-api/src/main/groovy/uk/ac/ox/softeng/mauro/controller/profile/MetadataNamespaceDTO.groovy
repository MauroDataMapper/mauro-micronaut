package uk.ac.ox.softeng.mauro.controller.profile

import groovy.transform.CompileStatic
import groovy.transform.SelfType
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem

@CompileStatic
@SelfType(AdministeredItem)
class MetadataNamespaceDTO implements Serializable {

    String namespace
    boolean defaultNamespace
    boolean editable
    List<String> keys = []



}
