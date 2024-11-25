package uk.ac.ox.softeng.mauro.domain.facet

import uk.ac.ox.softeng.mauro.domain.diff.CollectionDiff
import uk.ac.ox.softeng.mauro.domain.diff.DiffBuilder
import uk.ac.ox.softeng.mauro.domain.diff.DiffableItem
import uk.ac.ox.softeng.mauro.domain.diff.ObjectDiff
import uk.ac.ox.softeng.mauro.domain.diff.RuleDiff

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
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
@Indexes([@Index(columns = ['multi_facet_aware_item_id'])])
class Rule extends Facet implements DiffableItem<Rule> {

    // TODO: Rename this 'label'?
    @NotBlank
    @Pattern(regexp = /[^\$@|]*/, message = 'Cannot contain $, | or @')
    String name

    String description

    @Relation(Relation.Kind.ONE_TO_MANY)
    List<RuleRepresentation> ruleRepresentations

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
        if (!DiffBuilder.isNull(this.ruleRepresentationss) ||!DiffBuilder.isNull(other.ruleRepresentationss)) {
            base.appendCollection(DiffBuilder.SUMMARY_METADATA_REPORT, this.ruleRepresentationss as Collection<DiffableItem>, other.ruleRepresentationss as Collection<DiffableItem>)
        }
        base
    }

}
