package org.maurodata.domain.model

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import org.maurodata.domain.model.Path

/** Root-relative path selector for matching model items. */
@CompileStatic
@Introspected
class ModelPathMatch {
    /** Root-relative path selector, for example 'DM:Warehouse$main|DC:Customer'. */
    Path path = new Path('')
    /** Permit this selector to fan out across matching model versions/branches. */
    boolean allowMultipleModels = false
    /** Match the selector at the end of an item's path, allowing model/folder prefixes to vary. */
    boolean matchPathSuffix = false
    /** Match only the first selector node's identifier by suffix. Later selector nodes remain exact. */
    boolean matchFirstNodeIdentifierSuffix = false
}
