package org.maurodata.domain.model

import com.fasterxml.jackson.annotation.JsonIgnore
import io.micronaut.core.annotation.Nullable
import jakarta.persistence.Transient

interface Pathable {

    /**
     * The prefix used in this item's Path, if this item is pathable.
     */
    @Transient
    @JsonIgnore
    String getPathPrefix()

    /**
     * The identifier (label) used in this item's Path, if this item is pathable.
     */
    @Transient
    @JsonIgnore
    String getPathIdentifier()

    /**
     * The model identifier (version string or branch label) used in this item's Path, if this item is pathable.
     */
    @Transient
    @JsonIgnore
    @Nullable
    String getPathModelIdentifier()
}