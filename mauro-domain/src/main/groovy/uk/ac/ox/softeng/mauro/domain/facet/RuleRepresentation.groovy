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
import groovy.transform.MapConstructor
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Transient

@CompileStatic
@MappedEntity(value = 'rule_representation', schema = 'core', alias = 'rule_representation_')
@AutoClone
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
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

        base.appendString(DiffBuilder.LANGUAGE, this.language, other.language, this, other)
        base.appendString(DiffBuilder.REPRESENTATION ,this.representation, other.representation, this, other)
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

}
