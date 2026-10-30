package org.maurodata.service.path

import groovy.transform.CompileStatic
import org.maurodata.domain.model.Model

@CompileStatic
class PathModelCandidates {
    final List<Model> finalisedModels
    final List<Model> draftModels

    PathModelCandidates(List<Model> finalisedModels, List<Model> draftModels) {
        this.finalisedModels = finalisedModels
        this.draftModels = draftModels
    }
}
