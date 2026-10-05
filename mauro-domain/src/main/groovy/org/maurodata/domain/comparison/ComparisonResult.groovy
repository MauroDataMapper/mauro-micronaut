package org.maurodata.domain.comparison

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable

@CompileStatic
@Introspected
class ComparisonResult {

    String provider
    String comparisonType
    String comparedProperty
    ComparisonConclusion conclusion

    @Nullable
    Object left

    @Nullable
    Object right

    @Nullable
    String interpretation

    Map<String, Object> metadata = [:]
}
