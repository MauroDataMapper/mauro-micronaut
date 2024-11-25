package uk.ac.ox.softeng.mauro.domain.facet

import uk.ac.ox.softeng.mauro.domain.diff.CollectionDiff
import uk.ac.ox.softeng.mauro.domain.diff.DiffBuilder
import uk.ac.ox.softeng.mauro.domain.diff.DiffableItem
import uk.ac.ox.softeng.mauro.domain.diff.ObjectDiff
import uk.ac.ox.softeng.mauro.domain.diff.RuleRepresentationDiff
import uk.ac.ox.softeng.mauro.domain.model.Item

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Transient

@CompileStatic
@MappedEntity(value = 'rule_representation', schema = 'core', alias = 'rule_representation_')
@AutoClone
class RuleRepresentation extends Item implements DiffableItem<RuleRepresentation> {
    String language

    String representation

    @JsonAlias(['rule_id'])
    UUID ruleId

    @Override
    @JsonIgnore
    @Transient
    CollectionDiff fromItem() {
        new RuleRepresentationDiff(id, language, representation)
    }

    @Override
    @JsonIgnore
    @Transient
    String getDiffIdentifier() {
        language
    }

    @Override
    @JsonIgnore
    @Transient
    ObjectDiff<RuleRepresentation> diff(RuleRepresentation other) {
        ObjectDiff<RuleRepresentation> base = DiffBuilder.objectDiff(RuleRepresentation)
                .leftHandSide(id?.toString(), this)
                .rightHandSide(other.id?.toString(), other)

        base.appendString(DiffBuilder.LANGUAGE, this.language, other.language)
        base.appendString(DiffBuilder.REPRESENTATION ,this.representation, other.representation)
        base
    }
}
