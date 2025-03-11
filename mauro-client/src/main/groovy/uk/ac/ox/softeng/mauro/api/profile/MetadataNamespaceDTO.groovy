package uk.ac.ox.softeng.mauro.api.profile

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem

import groovy.transform.CompileStatic
import groovy.transform.SelfType

@CompileStatic
@SelfType(AdministeredItem)
class MetadataNamespaceDTO implements Serializable {

    String namespace
    boolean defaultNamespace
    boolean editable
    List<String> keys = []



}
