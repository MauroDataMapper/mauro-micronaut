package org.maurodata.api.model

import groovy.transform.CompileStatic

@CompileStatic
class MergeIntoDTO {

    ObjectPatchDataDTO patch
    boolean deleteBranch = false
    String changeNotice
}
