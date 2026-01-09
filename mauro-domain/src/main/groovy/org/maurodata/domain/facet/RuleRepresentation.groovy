package org.maurodata.domain.facet

import jakarta.persistence.PrePersist
import org.maurodata.domain.diff.CollectionDiff
import org.maurodata.domain.diff.DiffBuilder
import org.maurodata.domain.diff.DiffableItem
import org.maurodata.domain.diff.ObjectDiff
import org.maurodata.domain.diff.RuleRepresentationDiff
import org.maurodata.domain.model.Item
import org.maurodata.domain.model.ItemUtils
import org.maurodata.domain.model.Pathable

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Transient

@CompileStatic
@MappedEntity(value = 'rule_representation', schema = 'core', alias = 'rule_representation_')
@AutoClone
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class RuleRepresentation extends Item implements DiffableItem<RuleRepresentation>, Pathable {
    String language

    String representation

    @Transient
    @JsonIgnore
    Rule rule

    @JsonAlias(['rule_id'])
    UUID ruleId

    @Override
    @JsonIgnore
    @Transient
    CollectionDiff fromItem() {
        new RuleRepresentationDiff(id, language, representation, getDiffIdentifier())
    }

    @Override
    @JsonIgnore
    @Transient
    String getDiffIdentifier() {
        return "${pathPrefix}:${language}"
    }

    @PrePersist
    void prePersist() {
        if(rule) {
            ruleId = rule.id
        }
    }


    @Override
    @JsonIgnore
    @Transient
    ObjectDiff<RuleRepresentation> diff(RuleRepresentation other, String lhsPathRoot, String rhsPathRoot) {
        ObjectDiff<RuleRepresentation> base = DiffBuilder.objectDiff(RuleRepresentation)
            .leftHandSide(id?.toString(), this)
            .rightHandSide(other.id?.toString(), other)

        base.appendString(DiffBuilder.LANGUAGE, this.language, other.language, this, other)
        base.appendString(DiffBuilder.REPRESENTATION, this.representation, other.representation, this, other)
        base
    }

    /****
     * Methods for building a tree-like DSL
     */

    static RuleRepresentation build(
        Map args,
        @DelegatesTo(value = RuleRepresentation, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        new RuleRepresentation(args).tap(closure)
    }

    static RuleRepresentation build(
        @DelegatesTo(value = RuleRepresentation, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        build [:], closure
    }

    /**
     * DSL helper method for setting the language.  Returns the language passed in.
     *
     * @see #language
     */
    String language(String language) {
        this.language = language
        this.language
    }

    /**
     * DSL helper method for setting the representation.  Returns the representation passed in.
     *
     * @see #representation
     */
    String representation(String representation) {
        this.representation = representation
        this.representation
    }

    @Transient
    @JsonIgnore
    @Override
    String getPathPrefix() {
        'rr'
    }

    @Transient
    @JsonIgnore
    @Override
    String getPathIdentifier() {
        language
    }

    @Transient
    @JsonIgnore
    @Override
    @Nullable
    String getPathModelIdentifier() {
        return null
    }

    @Override
    void copyInto(Item into) {
        super.copyInto(into)
        RuleRepresentation intoRuleRepresentation = (RuleRepresentation) into
        intoRuleRepresentation.language = ItemUtils.copyItem(this.language, intoRuleRepresentation.language)
        intoRuleRepresentation.representation = ItemUtils.copyItem(this.representation, intoRuleRepresentation.representation)
        intoRuleRepresentation.ruleId = ItemUtils.copyItem(this.ruleId, intoRuleRepresentation.ruleId)
    }

    @Override
    Item shallowCopy() {
        RuleRepresentation ruleRepresentationShallowCopy = new RuleRepresentation()
        this.copyInto(ruleRepresentationShallowCopy)
        return ruleRepresentationShallowCopy
    }
}
