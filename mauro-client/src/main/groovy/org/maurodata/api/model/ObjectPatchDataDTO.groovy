package org.maurodata.api.model

class ObjectPatchDataDTO {

    UUID sourceId
    UUID targetId
    String label
    List<FieldPatchDataDTO> patches = []
}
