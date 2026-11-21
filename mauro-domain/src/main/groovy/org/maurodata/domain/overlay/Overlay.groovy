package org.maurodata.domain.overlay

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Transient
import org.maurodata.domain.model.Model
import org.maurodata.domain.diff.DiffBuilder
import org.maurodata.domain.diff.DiffableItem
import org.maurodata.domain.diff.ObjectDiff
import org.maurodata.domain.model.Item
import org.maurodata.domain.model.ModelPathMatch
import org.maurodata.domain.model.Path
import org.maurodata.visitor.DomainVisitor

@CompileStatic
@AutoClone
@Introspected
@MappedEntity(schema = 'core')
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class Overlay extends Model implements DiffableItem<Overlay> {
    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    List<OverlayRule> overlayRules

    @Override
    <T> T accept(DomainVisitor<T> visitor) { visitor.visitOverlay(this) }

    @Override
    @Transient
    @JsonIgnore
    String getPathPrefix() { 'ov' }

    @Override
    String getDomainType() { 'Overlay' }

    @Override
    ObjectDiff<Overlay> diff(Overlay other, String lhsPathRoot, String rhsPathRoot) {
        ObjectDiff<Overlay> result = DiffBuilder.objectDiff(Overlay)
            .leftHandSide(id?.toString(), this).rightHandSide(other.id?.toString(), other)
        result.label = label
        result.appendString(DiffBuilder.DESCRIPTION, description, other.description, this, other)
        result
    }

    @Override
    void copyInto(Item into) {
        super.copyInto(into)
        ((Overlay) into).overlayRules = overlayRules?.collect { OverlayRule overlayRule ->
            new OverlayRule(id: overlayRule.id, overlayId: overlayRule.overlayId, order: overlayRule.order,
                match: new ModelPathMatch(path: overlayRule.match?.path ? new Path(overlayRule.match.path.toString()) : new Path(''),
                                                    allowMultipleModels: overlayRule.match?.allowMultipleModels,
                                                    matchPathSuffix: overlayRule.match?.matchPathSuffix,
                                                    matchFirstNodeIdentifierSuffix: overlayRule.match?.matchFirstNodeIdentifierSuffix),
                actions: overlayRule.actions?.collect { OverlayAction action -> new OverlayAction(id: action.id, type: action.type, field: action.field,
                    namespace: action.namespace, key: action.key, value: action.value, policy: action.policy, source: action.source,
                    label: action.label, description: action.description) } ?: [])
        } ?: null
    }

    @Override
    Item shallowCopy() {
        Overlay copy = new Overlay()
        copyInto(copy)
        copy
    }

    @PrePersist
    @PreUpdate
    void prePersist() {
        super.prePersist()
        overlayRules?.each { OverlayRule overlayRule ->
            overlayRule.ensureId()
            overlayRule.actions?.each { OverlayAction action -> action.ensureId() }
        }
        if (!getModelWithVersion()) branchName = 'main'
    }

    static Overlay build(Map args, @DelegatesTo(value = Overlay, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        new Overlay(args).tap(closure)
    }

    static Overlay build(@DelegatesTo(value = Overlay, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) { build([:], closure) }
}
