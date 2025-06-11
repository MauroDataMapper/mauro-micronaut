package org.maurodata.api.model

import com.fasterxml.jackson.annotation.JsonAlias

class MergeFieldDiffDTO {

    String fieldName
    String path
    Object sourceValue
    Object targetValue
    Object commonAncestorValue
    boolean isMergeConflict
    @JsonAlias(['type'])
    String _type
}
