package org.maurodata.domain.overlay

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import org.maurodata.domain.model.Item
import org.maurodata.domain.model.Path
import org.maurodata.domain.model.ModelPathMatch

@CompileStatic
@Introspected
@JsonInclude(JsonInclude.Include.NON_NULL)
@MappedEntity(schema = 'core', value = 'overlay_rule')
class OverlayRule {
    @Id
    UUID id = Item.timeBasedEpochGenerator.generate()
    @MappedProperty('overlay_id')
    UUID overlayId
    /** Backend-owned sparse rank. The API permits moving relative to another rule, never setting this value. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @MappedProperty('rule_order')
    Long order
    @TypeDef(type = DataType.JSON)
    @MappedProperty('match_definition')
    ModelPathMatch match = new ModelPathMatch()
    @TypeDef(type = DataType.JSON)
    @MappedProperty('actions_definition')
    List<OverlayAction> actions = []
    @MappedProperty('first_node_prefix')
    String firstNodePrefix
    @MappedProperty('first_node_identifier')
    String firstNodeIdentifier
    @MappedProperty('first_node_identifier_suffix')
    boolean firstNodeIdentifierSuffix
    @MappedProperty('match_path_suffix')
    boolean matchPathSuffix
    @MappedProperty('selector_node_count')
    Integer selectorNodeCount
    @MappedProperty('last_node_prefix')
    String lastNodePrefix
    @MappedProperty('last_node_identifier')
    String lastNodeIdentifier

    void ensureId() {
        if (id == null) id = Item.timeBasedEpochGenerator.generate()
    }

    @PrePersist
    @PreUpdate
    void prepareForPersistence() {
        ensureId()
        Path.PathNode firstNode = match?.path?.nodes ? match.path.nodes[0] : null
        firstNodePrefix = firstNode?.prefix?.toLowerCase()
        firstNodeIdentifier = firstNode?.identifier
        firstNodeIdentifierSuffix = match?.matchFirstNodeIdentifierSuffix ?: false
        matchPathSuffix = match?.matchPathSuffix ?: false
        selectorNodeCount = match?.path?.nodes?.size() ?: 0
        Path.PathNode lastNode = match?.path?.nodes ? match.path.nodes.last() : null
        lastNodePrefix = lastNode?.prefix?.toLowerCase()
        lastNodeIdentifier = lastNode?.identifier
        actions?.each { OverlayAction action -> action.ensureId() }
    }
}
