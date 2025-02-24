package uk.ac.ox.softeng.mauro.domain.facet

import uk.ac.ox.softeng.mauro.domain.diff.CollectionDiff
import uk.ac.ox.softeng.mauro.domain.diff.DiffBuilder
import uk.ac.ox.softeng.mauro.domain.diff.DiffableItem
import uk.ac.ox.softeng.mauro.domain.diff.ObjectDiff
import uk.ac.ox.softeng.mauro.domain.diff.RuleDiff

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
        new RuleDiff(id, name, description)
    }

    @Override
    @JsonIgnore
    @Transient
    String getDiffIdentifier() {
        name
    }

    @Override
    @JsonIgnore
    @Transient
    ObjectDiff<Rule> diff(Rule other) {
        ObjectDiff<Rule> base = DiffBuilder.objectDiff(Rule)
                .leftHandSide(id?.toString(), this)
                .rightHandSide(other.id?.toString(), other)
        base.label = this.name
        base.appendString(DiffBuilder.DESCRIPTION, this.description, other.description)
        if (!DiffBuilder.isNull(this.ruleRepresentations) ||!DiffBuilder.isNull(other.ruleRepresentations)) {
            base.appendCollection(DiffBuilder.SUMMARY_METADATA_REPORT, this.ruleRepresentations as Collection<DiffableItem>, other.ruleRepresentations as Collection<DiffableItem>)
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


}
