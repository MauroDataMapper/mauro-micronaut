package uk.ac.ox.softeng.mauro.domain.datamodel

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.ModelItem

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import jakarta.persistence.Transient

/**
 * A datatype describes the range of values that a column or field in a dataset may take.
 *
 */
@CompileStatic
@AutoClone(excludes = ['dataModel'])
@Introspected
@MappedEntity
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
@Indexes([@Index(columns = ['dataModel_id'], unique = true)])
class DataType extends ModelItem<DataModel> {

    @JsonIgnore
    DataModel dataModel

    @Override
    @Transient
    @JsonIgnore
    DataModel getParent() {
        dataModel
    }

    @Override
    @Transient
    @JsonIgnore
    void setParent(AdministeredItem dataModel) {
        this.dataModel = (DataModel) dataModel
    }

    @Override
    @Transient
    @JsonIgnore
    String getPathPrefix() {
        'dt'
    }

    /****
     * Methods for building a tree-like DSL
     */

    static DataType build(
            Map args,
            @DelegatesTo(value = DataType, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        new DataType(args).tap(closure)
    }

    static DataType build(@DelegatesTo(value = DataType, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        build [:], closure
    }

}
