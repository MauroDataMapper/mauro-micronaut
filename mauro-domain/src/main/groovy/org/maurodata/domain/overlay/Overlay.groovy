package org.maurodata.domain.overlay

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Transient
import org.maurodata.domain.model.Model
import org.maurodata.domain.diff.DiffBuilder
import org.maurodata.domain.diff.DiffableItem
import org.maurodata.domain.diff.ObjectDiff
import org.maurodata.domain.model.Item
import org.maurodata.domain.model.Path
import org.maurodata.visitor.DomainVisitor

@CompileStatic
@AutoClone
@Introspected
@MappedEntity(schema = 'core')
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class Overlay extends Model implements DiffableItem<Overlay> {
    @TypeDef(type = DataType.JSON)
    List<OverlayRule> rules = []

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
        ((Overlay) into).rules = rules.collect { OverlayRule rule ->
            new OverlayRule(id: rule.id ?: UUID.randomUUID(), match: new OverlayMatch(path: rule.match?.path ? new Path(rule.match.path.toString()) : new Path(''),
                                                    allowMultipleModels: rule.match?.allowMultipleModels),
                            actions: rule.actions?.collect { OverlayAction action -> new OverlayAction(id: action.id ?: UUID.randomUUID(), type: action.type, field: action.field,
                                namespace: action.namespace, key: action.key, value: action.value, policy: action.policy, source: action.source) } ?: [])
        }
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
        if (!getModelWithVersion()) branchName = 'main'
    }

    static Overlay build(Map args, @DelegatesTo(value = Overlay, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        new Overlay(args).tap(closure)
    }

    static Overlay build(@DelegatesTo(value = Overlay, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) { build([:], closure) }
}
