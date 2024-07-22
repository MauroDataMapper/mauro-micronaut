package uk.ac.ox.softeng.mauro.domain.datamodel

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import jakarta.persistence.Transient
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.domain.model.ModelItem

/**
 * A DataModel describes a data asset, or a data standard
 */
@CompileStatic
@AutoClone
@Introspected
@MappedEntity(schema = 'datamodel')
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class DataModel extends Model {

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'dataModel')
    List<DataType> dataTypes = []

    @JsonAlias("childDataClasses")
    // for importing models exported from the Grails implementation
    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'dataModel')
    List<DataClass> dataClasses = []

    @Transient
    @JsonIgnore
    Set<DataClass> allDataClasses = []

    @Transient
    @JsonIgnore
    List<DataElement> dataElements = []

    @Transient
    @JsonIgnore
    Set<EnumerationValue> enumerationValues = []


    @Override
    @Transient
    @JsonIgnore
    String getPathPrefix() {
        'dm'
    }

    void setModelType(String modelType) {
        modelType = DataModelType.values().find { it.label.toLowerCase() == modelType.toLowerCase() }?.label
    }


    @Override
    @Transient
    @JsonIgnore
    List<Collection<? extends ModelItem<DataModel>>> getAllAssociations() {
        [dataTypes, enumerationValues, allDataClasses, dataElements] as List<Collection<? extends ModelItem<DataModel>>>
    }


    @Transient
    @JsonIgnore
    List<DataClass> getChildDataClasses() {
        dataClasses
    }


    @Override
    //can't use setAssociations to set cloned associations. need both original and cloned info
    //this method does deep copy
    DataModel clone() {
        Map<DataClass, DataClass> clonedDataClassLookup = [:]
        Map<DataType, DataType> clonedDataTypeLookup = [:]
        DataModel cloned = (DataModel) super.clone()

        cloned.dataTypes = dataTypes.collect {
            DataType origDataType = it
            it.clone().tap {
                clonedDataTypeLookup.put(origDataType, it)
                it.parent = cloned
            }
        }

        cloned.allDataClasses = allDataClasses.collect {
            DataClass orig = it
            it.clone().tap {
                it.dataModel = cloned
                clonedDataClassLookup.put(orig, it)
            }
        } as Set<DataClass>

        cloned.dataElements = dataElements.collect {
            DataElement originalDataElement = it
            it.clone().tap {
                it.dataModel = cloned
                it.parent = clonedDataClassLookup.get(originalDataElement.parent)
                it.dataType = clonedDataTypeLookup.get(originalDataElement.dataType)
            }
        }

        cloned.enumerationValues = enumerationValues.collect {
            EnumerationValue origEnumerationValue = it
            it.clone().tap {
                it.dataModel = cloned
                it.enumerationType = clonedDataTypeLookup.get(origEnumerationValue.enumerationType)
            }
        } as Set<EnumerationValue>
        cloned
    }

    @Transient
    @JsonIgnore
    @Override
    void setAssociations() {
        Map<String, DataType> dataTypesMap = dataTypes.collectEntries {[it.label, it]}

        dataTypes.each {dataType ->
            dataType.parent = this
            dataType.enumerationValues.each {enumerationValue ->
                enumerationValue.parent = dataType
                enumerationValues.add(enumerationValue)
                enumerationValue.dataModel = this
                this.enumerationValues.add(enumerationValue)
            }
        }

        dataClasses.each {dataClass ->
            setDataClassAssociations(dataClass, dataTypesMap)
        }
        this
    }

    void setDataClassAssociations(DataClass dataClass, Map<String, DataType> dataTypesMap) {
        allDataClasses.add(dataClass)
        dataClass.dataModel = this
        dataClass.dataClasses.each { childDataClass ->
            setDataClassAssociations(childDataClass, dataTypesMap)
            childDataClass.parentDataClass = dataClass
        }
        dataClass.dataElements.each {dataElement ->
            dataElement.dataModel = this
            this.dataElements.add(dataElement)
            dataElement.dataClass = dataClass
            dataElement.dataType = dataTypesMap[dataElement?.dataType?.label]
        }
    }


    /****
     * Methods for building a tree-like DSL
     */

    static DataModel build(
            Map args,
            @DelegatesTo(value = DataModel, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        new DataModel(args).tap(closure)
    }

    static DataModel build(
            @DelegatesTo(value = DataModel, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        build [:], closure
    }

    String modelType(DataModelType dataModelType) {
        this.modelType = dataModelType.label
        this.modelType
    }

    DataType primitiveType(DataType primitiveType) {
        this.dataTypes.add(primitiveType)
        primitiveType.dataModel = this
        primitiveType.setDomainType(DataType.DataTypeKind.PRIMITIVE_TYPE)
        primitiveType
    }

    DataType primitiveType(Map args, @DelegatesTo(value = DataType, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        DataType dt = DataType.build(args + [dataModel: this, dataTypeKind: DataType.DataTypeKind.PRIMITIVE_TYPE], closure)
        primitiveType dt
    }

    DataType primitiveType(@DelegatesTo(value = DataType, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        primitiveType [:], closure
    }

    DataType enumerationType(DataType enumerationType) {
        this.dataTypes.add(enumerationType)
        enumerationType.dataModel = this
        enumerationType.setDomainType(DataType.DataTypeKind.ENUMERATION_TYPE)
        enumerationType
    }

    DataType enumerationType(Map args, @DelegatesTo(value = DataType, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        DataType dt = DataType.build(args + [dataModel: this, dataTypeKind: DataType.DataTypeKind.ENUMERATION_TYPE], closure)
        enumerationType(dt)
    }

    DataType enumerationType(@DelegatesTo(value = DataType, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        enumerationType [:], closure
    }


    DataClass dataClass(DataClass dataClass) {
        this.dataClasses.add(dataClass)
        this.allDataClasses.add(dataClass)
        dataClass.dataModel = this
        dataClass
    }

    DataClass dataClass(Map args, @DelegatesTo(value = DataClass, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        DataClass dataClass1 = DataClass.build(args + [dataModel: this], closure)
        dataClass dataClass1
    }

    DataClass dataClass(@DelegatesTo(value = DataClass, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        dataClass [:], closure
    }

}
