package org.maurodata.api.model

import com.fasterxml.jackson.annotation.JsonProperty

class MergeFieldDiffDTO {

    String fieldName
    String path
    Object sourceValue
    Object targetValue
    Object commonAncestorValue
    boolean isMergeConflict
    @JsonProperty('type')
    String _type
}
