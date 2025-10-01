package org.maurodata.domain.facet

import org.maurodata.domain.model.Item
import org.maurodata.domain.model.ItemUtils

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Transient

@CompileStatic
@MappedEntity(schema = 'core')
@AutoClone
@Indexes([@Index(columns = ['multi_facet_aware_item_id'], unique = true)])
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class Edit extends Facet {


    EditType title
    String description

    /****
     * Methods for building a tree-like DSL
     */

    static Edit build(
        Map args,
        @DelegatesTo(value = Edit, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        new Edit(args).tap(closure)
    }

    static Edit build(
        @DelegatesTo(value = Edit, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        build [:], closure
    }

    /**
     * DSL helper method for setting the title.  Returns the title passed in.
     *
     * @see #title
     */
    String title(String title) {
        this.title = title
        this.title
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

    @Transient
    @JsonIgnore
    @Override
    String getPathPrefix() {
        'ed'
    }

    @Transient
    @JsonIgnore
    @Override
    String getPathIdentifier() {
        title
    }

    @Override
    void copyInto(Item into) {
        super.copyInto(into)
        Edit intoEdit = (Edit) into
        intoEdit.title = ItemUtils.copyItem(this.title, intoEdit.title)
        intoEdit.description = ItemUtils.copyItem(this.description, intoEdit.description)
    }

    @Override
    Item shallowCopy() {
        Edit editShallowCopy = new Edit()
        this.copyInto(editShallowCopy)
        return editShallowCopy
    }
}