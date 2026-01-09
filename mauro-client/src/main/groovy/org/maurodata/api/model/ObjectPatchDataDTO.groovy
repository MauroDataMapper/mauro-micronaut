package org.maurodata.api.model

import groovy.transform.CompileStatic

@CompileStatic
class ObjectPatchDataDTO {

    UUID sourceId
    UUID targetId
    String label
    List<FieldPatchDataDTO> patches = []
}
