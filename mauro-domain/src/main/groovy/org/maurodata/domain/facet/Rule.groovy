package org.maurodata.domain.facet

import org.maurodata.domain.diff.CollectionDiff
import org.maurodata.domain.diff.DiffBuilder
import org.maurodata.domain.diff.DiffableItem
import org.maurodata.domain.diff.ObjectDiff
import org.maurodata.domain.diff.RuleDiff
import org.maurodata.domain.model.Item
import org.maurodata.domain.model.ItemReference
import org.maurodata.domain.model.ItemReferencerUtils
import org.maurodata.domain.model.ItemUtils

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import io.micronaut.data.annotation.Transient
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

@CompileStatic
@MappedEntity(value = 'rule', schema = 'core', alias = 'rule_')
@AutoClone()
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
@Indexes([@Index(columns = ['multi_facet_aware_item_id'])])
class Rule extends Facet implements DiffableItem<Rule> {

    // TODO: Rename this 'label'?
    @NotBlank
    @Pattern(regexp = /[^\$@|]*/, message = 'Cannot contain $, | or @')
    String name

    String description

    @Relation(Relation.Kind.ONE_TO_MANY)
    @JsonAlias(['rule_representations'])
    List<RuleRepresentation> ruleRepresentations = []

    @Override
    @JsonIgnore
    @Transient
    CollectionDiff fromItem() {
        new RuleDiff(id, name, description, getDiffIdentifier())
    }

    @Override
    @JsonIgnore
    @Transient
    String getDiffIdentifier() {
        if (multiFacetAwareItem != null) {return "${multiFacetAwareItem.getDiffIdentifier()}|${pathPrefix}:${name}"}
        return "${pathPrefix}:${name}"
    }

    @Override
    @JsonIgnore
    @Transient
    ObjectDiff<Rule> diff(Rule other, String lhsPathRoot, String rhsPathRoot) {
        ObjectDiff<Rule> base = DiffBuilder.objectDiff(Rule)
            .leftHandSide(id?.toString(), this)
            .rightHandSide(other.id?.toString(), other)
        base.label = this.name
        base.appendString(DiffBuilder.DESCRIPTION, this.description, other.description, this, other)
        if (!DiffBuilder.isNull(this.ruleRepresentations) || !DiffBuilder.isNull(other.ruleRepresentations)) {
            base.appendCollection(DiffBuilder.SUMMARY_METADATA_REPORT, this.ruleRepresentations as Collection<DiffableItem>,
                                  other.ruleRepresentations as Collection<DiffableItem>, lhsPathRoot, rhsPathRoot)
        }
        base
    }

    /****
     * Methods for building a tree-like DSL
     */

    static Rule build(
        Map args,
        @DelegatesTo(value = Rule, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        new Rule(args).tap(closure)
    }

    static Rule build(
        @DelegatesTo(value = Rule, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        build [:], closure
    }

    /**
     * DSL helper method for setting the name.  Returns the name passed in.
     *
     * @see #name
     */
    String name(String name) {
        this.name = name
        this.name
    }

    /**
     * DSL helper method for setting the description.  Returns the description passed in.
     *
     * @see #description
     */
    String description(String description) {
        this.description = description
        this.description
    }

    RuleRepresentation ruleRepresentation(RuleRepresentation ruleRepresentation) {
        this.ruleRepresentations.add(ruleRepresentation)
        ruleRepresentation.ruleId = this.id
        ruleRepresentation
    }

    RuleRepresentation ruleRepresentation(Map args, @DelegatesTo(value = RuleRepresentation, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        RuleRepresentation rr = RuleRepresentation.build(args + [ruleId: this.id], closure)
        ruleRepresentation(rr)
    }

    RuleRepresentation ruleRepresentation(@DelegatesTo(value = RuleRepresentation, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        ruleRepresentation [:], closure
    }

    @Transient
    @JsonIgnore
    @Override
    String getPathPrefix() {
        'ru'
    }

    @Transient
    @JsonIgnore
    @Override
    String getPathIdentifier() {
        name
    }

    @Transient
    @JsonIgnore
    @Override
    List<ItemReference> retrieveItemReferences() {
        List<ItemReference> pathsBeingReferenced = [] + super.retrieveItemReferences()

        ItemReferencerUtils.addItems(ruleRepresentations, pathsBeingReferenced)

        return pathsBeingReferenced
    }

    @Transient
    @JsonIgnore
    @Override
    void replaceItemReferencesByIdentity(IdentityHashMap<Item, Item> replacements, List<Item> notReplaced) {
        super.replaceItemReferencesByIdentity(replacements, notReplaced)
        ruleRepresentations = ItemReferencerUtils.replaceItemsByIdentity(ruleRepresentations, replacements, notReplaced)
    }

    @Override
    void copyInto(Item into) {
        super.copyInto(into)
        Rule intoRule = (Rule) into
        intoRule.name = ItemUtils.copyItem(this.name, intoRule.name)
        intoRule.description = ItemUtils.copyItem(this.description, intoRule.description)
        intoRule.ruleRepresentations = ItemUtils.copyItems(this.ruleRepresentations, intoRule.ruleRepresentations)
    }

    @Override
    Item shallowCopy() {
        Rule ruleShallowCopy = new Rule()
        this.copyInto(ruleShallowCopy)
        return ruleShallowCopy
    }
}
