package uk.ac.ox.softeng.mauro.domain.datamodel

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
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

    @Transient
    String modelType = domainType

    @JsonProperty('type')
    @MappedProperty('model_type')
    String dataModelType = DataModelType.DATA_ASSET.label

    @Override
    @Transient
    @JsonIgnore
    String getPathPrefix() {
        'dm'
    }

    void setDataModelType(String dataModelType) {
        this.dataModelType = DataModelType.values().find {it.label.toLowerCase() == dataModelType.toLowerCase()}?.label
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

    @Transient
    @JsonIgnore
    @Override
    DataModel clone() {
        DataModel cloned = (DataModel) super.clone()
        cloned.updateCreationProperties()
        Map<DataClass, DataClass> clonedDataClassLookup = [:]
        Map<DataClass, DataClass> clonedChildDataClassLookup = [:]
        Map<DataElement, DataElement> clonedDataElementLookup = [:]
        Map<DataType, DataType> clonedDataTypeLookup = [:]
        Map<EnumerationValue, EnumerationValue> clonedEnumerationValueLookup = [:]

        cloned.dataTypes = dataTypes.collect {it->
            it.clone().tap { clonedDT ->
                clonedDT.updateCreationProperties()
                clonedDataTypeLookup.put(it, clonedDT)
                clonedDT.parent = cloned
                clonedDT.enumerationValues.clear()
            }
        }
        List<DataClass> clonedDataClasses = dataClasses.collect {
            it.clone().tap { clonedDC ->
                clonedDC.updateCreationProperties()
                clonedDataClassLookup.put(it, clonedDC)
                clonedDC.dataModel = cloned
            }
        }
        clonedDataClasses.each {
            List<DataClass> clonedChildList = it.dataClasses.collect { child ->
                child.clone().tap { clonedChild ->
                    clonedChild.updateCreationProperties()
                    clonedChildDataClassLookup.put(child, clonedChild)
                    clonedChild.parentDataClass = it
                    clonedChild.dataModel = cloned
                }
            }
            it.dataClasses = clonedChildList
        }
        cloned.dataClasses = clonedDataClasses
        List<DataClass> clonedChildren = clonedChildDataClassLookup.values() as List<DataClass>
        clonedChildren.addAll(clonedDataClasses)
        cloned.allDataClasses = clonedChildren as Set<DataClass>

        cloned.dataElements = dataElements.collect {
            it.clone().tap { clonedDataElement ->
                clonedDataElement.updateCreationProperties()
                clonedDataElementLookup.put(it, clonedDataElement)
                Map<DataClass, DataClass> allDataClassLookup = clonedDataClassLookup
                allDataClassLookup.putAll(clonedChildDataClassLookup)
                clonedDataElement.dataClass = allDataClassLookup[dataClass]
                clonedDataElement.dataModel = cloned
                clonedDataElement.dataType = clonedDataTypeLookup[clonedDataElement.dataType]
            }
        }
        cloned.allDataClasses.each {
            it.dataElements = it.dataElements.collect { dataElementIt ->
                clonedDataElementLookup[dataElementIt]
            }
        }
        cloned.enumerationValues = enumerationValues.collect {
            it.clone().tap { clonedEV ->
                clonedEV.updateCreationProperties()
                clonedEnumerationValueLookup.put(it, clonedEV)
                clonedEV.dataModel = cloned
                clonedEV.parent = clonedDataTypeLookup[it.parent]
            }
        } as Set<EnumerationValue>

        cloned.setAssociations()
        cloned.allDataClasses = cloned.allDataClasses.toSorted {it.parentDataClass} as Set<DataClass>
        cloned
    }

    @Transient
    @JsonIgnore
    @Override
    void setAssociations() {
        Map<String, DataType> dataTypesMap = dataTypes.collectEntries { [it.label, it] }

        dataTypes.each { dataType ->
            dataType.parent = this
            dataType.enumerationValues.each { enumerationValue ->
                enumerationValue.parent = dataType
                enumerationValues.add(enumerationValue)
                enumerationValue.dataModel = this
                this.enumerationValues.add(enumerationValue)
            }
        }

        dataClasses.each { dataClass ->
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
        dataClass.dataElements.each { dataElement ->
            dataElement.dataModel = this
            dataElement.dataClass = dataClass
            dataElement.dataType = dataTypesMap[dataElement?.dataType?.label]
            if (!this.dataElements.contains(dataElement)) {
                this.dataElements.add(dataElement)
            }
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

    String dataModelType(DataModelType dataModelType) {
        this.dataModelType = dataModelType.label
        this.dataModelType
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
