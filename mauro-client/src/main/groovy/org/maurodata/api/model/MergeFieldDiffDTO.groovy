package org.maurodata.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.maurodata.domain.model.Breadcrumb

class MergeFieldDiffDTO {

    String fieldName
    String path
    List<Breadcrumb> breadcrumbs
    Object sourceValue
    Object targetValue
    Object commonAncestorValue
    boolean isMergeConflict
    @JsonProperty('type')
    String _type
}
