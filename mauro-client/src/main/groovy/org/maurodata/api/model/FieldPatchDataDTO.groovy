package org.maurodata.api.model

import org.maurodata.domain.model.Path

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.CompileStatic

@CompileStatic
class FieldPatchDataDTO implements Comparable<FieldPatchDataDTO>{

    String fieldName
    Path path
    Object sourceValue
    Object targetValue
    Object commonAncestorValue
    boolean isMergeConflict
    @JsonProperty('type')
    String _type

    @Override
    int compareTo(FieldPatchDataDTO that) {
        switch (this._type) {
            case 'modification':
                if (that._type == 'modification') return 0
                else return 1
            case 'creation':
                if (that._type == 'modification') return -1
                if (that._type == 'deletion') return -1
                return 0
            case 'deletion':
                if (that._type == 'modification') return -1
                if (that._type == 'creation') return 1
                return 0
        }
    }

    @Override
    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        FieldPatchDataDTO that = (FieldPatchDataDTO) o

        if (isMergeConflict != that.isMergeConflict) return false
        if (fieldName != that.fieldName) return false
        if (path != that.path) return false
        if (_type != that._type) return false

        return true
    }

    @JsonIgnore
    FieldPatchDataDTO getParentPathPatch() {
        final FieldPatchDataDTO parentPatch = new FieldPatchDataDTO()
        parentPatch.isMergeConflict = this.isMergeConflict
        parentPatch._type = this._type
        parentPatch.path = this.path.parent

        return parentPatch
    }
}
