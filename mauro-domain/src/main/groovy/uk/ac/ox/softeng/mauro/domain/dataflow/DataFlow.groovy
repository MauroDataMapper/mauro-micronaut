package uk.ac.ox.softeng.mauro.domain.dataflow

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import jakarta.persistence.Transient
import jakarta.validation.constraints.NotNull
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.ModelItem

/**
 * A DataFlow is has source and target dataModels
 */
@CompileStatic
@AutoClone
@Introspected
@MappedEntity(schema = 'dataflow')
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class DataFlow extends ModelItem<DataModel> {
    @Nullable
    String diagramLayout

    @Nullable
    String definition

    @NotNull
    DataModel source

    @NotNull
    DataModel target

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'dataClassComponent')
    List<DataClassComponent> dataClassComponents = []

    @Transient
    UUID breadcrumbTreeId


    @Override
    String getDomainType() {
        DataFlow.simpleName
    }

    Boolean hasChildren(){
        dataClassComponents
    }

    @Override
    @Transient
    @JsonIgnore
    AdministeredItem getParent() {
        return target
    }

    @Override
    @Transient
    @JsonIgnore
    void setParent(AdministeredItem parent) {
        this.target = parent as DataModel
    }


    @Override
    @Transient
    @JsonIgnore
    String getPathPrefix() {
        'df'
    }

}