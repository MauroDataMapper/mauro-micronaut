package org.maurodata.domain.comparison

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import org.maurodata.domain.model.Model

@CompileStatic
@Introspected
class ComparisonContext {

    @Nullable
    Model leftModelResource

    @Nullable
    Model rightModelResource
}
