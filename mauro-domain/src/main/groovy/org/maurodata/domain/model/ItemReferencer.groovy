package org.maurodata.domain.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Transient

/**
Any item in the model that holds references to other items
should implement ItemReferencer
 */
interface ItemReferencer {
    /**
     * The list of ItemReference this holds references to
     */
    @Transient
    @JsonIgnore
    List<ItemReference> getItemReferences()

    /**
     * A map of ItemReference to replace with new references
     */
    @Transient
    @JsonIgnore
    void replaceItemReferences(final Map<UUID, ItemReference> replacements)
}
