package org.maurodata.domain.datamodel


import org.maurodata.domain.model.Item
import org.maurodata.domain.model.ItemReference
import org.maurodata.domain.model.ItemReferencer
import org.maurodata.domain.model.ItemReferencerUtils
import org.maurodata.domain.model.ItemUtils

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
import org.maurodata.domain.model.Model
import org.maurodata.domain.model.ModelItem

/**
 * A DataModel describes a data asset, or a data standard
 */
@CompileStatic
@AutoClone
@Introspected
@MappedEntity(schema = 'datamodel')
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class DataModel extends Model implements ItemReferencer {

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
        [allDataClasses, dataTypes, enumerationValues, dataElements] as List<Collection<? extends ModelItem<DataModel>>>
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
        Map<UUID, DataClass> clonedDataClassLookup = [:]
        Map<UUID, DataClass> clonedChildDataClassLookup = [:]
        Map<UUID, DataElement> clonedDataElementLookup = [:]
        Map<UUID, DataType> clonedDataTypeLookup = [:]
        Map<UUID, EnumerationValue> clonedEnumerationValueLookup = [:]

        cloned.dataTypes = dataTypes.collect {it ->
            it.clone().tap {clonedDT ->
                clonedDataTypeLookup.put(it.id, clonedDT)
                clonedDT.parent = cloned
                clonedDT.enumerationValues.clear()
            }
        }
        List<DataClass> clonedDataClasses = dataClasses.collect {
            it.clone().tap {clonedDC ->
                clonedDataClassLookup.put(it.id, clonedDC)
                clonedDC.dataModel = cloned
            }
        }
        clonedDataClasses.each {
            List<DataClass> clonedChildList = it.dataClasses.collect {child ->
                child.clone().tap {clonedChild ->
                    clonedChildDataClassLookup.put(child.id, clonedChild)
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
            it.clone().tap {clonedDataElement ->
                clonedDataElementLookup.put(it.id, clonedDataElement)
                Map<UUID, DataClass> allDataClassLookup = clonedDataClassLookup
                allDataClassLookup.putAll(clonedChildDataClassLookup)
                clonedDataElement.dataClass = allDataClassLookup[dataClass.id]
                clonedDataElement.dataModel = cloned
                clonedDataElement.dataType = clonedDataTypeLookup[clonedDataElement.dataType.id]
            }
        }
        cloned.allDataClasses.each {dataClass ->
            dataClass.dataElements = dataClass.dataElements.collect {dataElementIt ->
                clonedDataElementLookup[dataElementIt.id]
            }
            dataClass.extendsDataClasses = dataClass.extendsDataClasses.collect {extendedDataClass ->
                clonedDataClassLookup[extendedDataClass.id]
            }
            dataClass.extendedBy = dataClass.extendedBy.collect {extendsDataClass ->
                clonedDataClassLookup[extendsDataClass.id]
            }
        }
        cloned.enumerationValues = enumerationValues.collect {
            it.clone().tap {clonedEV ->
                clonedEnumerationValueLookup.put(it.id, clonedEV)
                clonedEV.dataModel = cloned
                clonedEV.parent = clonedDataTypeLookup[it.parent.id]
            }
        } as Set<EnumerationValue>

        cloned.allDataClasses = cloned.allDataClasses.toSorted {it.parentDataClass} as Set<DataClass>
        cloned
    }

    @Transient
    @JsonIgnore
    String getDiffIdentifier() {
        if (folder != null) {return "${folder.getDiffIdentifier()}|${getPathNodeString()}"}
        return "${getPathNodeString()}"
    }

    @Transient
    @JsonIgnore
    @Override
    void setAssociations() {
        super.setAssociations()
        Map<String, DataType> dataTypesMap = dataTypes.collectEntries {[it.id?:it.label, it]}
        List<? extends DataType> referenceTypes = dataTypeReferenceTypes()

        dataTypes.each {dataType ->
            dataType.parent = this
            dataType.dataModel = this
            dataType.enumerationValues.each {enumerationValue ->
                enumerationValue.parent = dataType
                enumerationValue.enumerationType = dataType
                enumerationValues.add(enumerationValue)
                enumerationValue.dataModel = this
                this.enumerationValues.add(enumerationValue)
            }
            dataType.setAssociations()
        }

        dataClasses.each {dataClass ->
            setDataClassAssociations(dataClass, dataTypesMap, referenceTypes)
        }
        dataTypes.each {dataType ->
            if(dataType.dataTypeKind == DataType.DataTypeKind.REFERENCE_TYPE) {
                if(!dataType.dataModel.allDataClasses.contains(dataType.referenceClass)) {
                    dataType.referenceClass = dataType.dataModel.allDataClasses.find {dataClass ->
                        (dataType.referenceClass.id && dataClass.id && dataClass.id == dataType.referenceClass.id) ||
                        (dataType.referenceClass.label && dataClass.label && dataClass.label == dataType.referenceClass.label)
                    }
                }
            }
        }

        this
    }

    void setDataClassAssociations(DataClass dataClass, Map<String, DataType> dataTypesMap,
                                  List<? extends DataType> referenceTypes) {
        dataClass.setAssociations()
        dataClass.dataModel = this
        if(!dataClass.dataModel.allDataClasses.contains(dataClass)) {
            dataClass.dataModel.allDataClasses.add(dataClass)
        }
        dataClass.dataClasses.each {childDataClass ->
            setDataClassAssociations(childDataClass, dataTypesMap, referenceTypes)
            childDataClass.parentDataClass = dataClass
        }
        dataClass.dataElements.each {dataElement ->
            dataElement.dataModel = this
            if(!dataElement.dataModel.dataElements.contains(dataElement)) {
                dataElement.dataModel.dataElements.add(dataElement)
            }
            dataElement.dataClass = dataClass
            dataElement.dataType = dataTypesMap[dataElement.dataType?.id ?: dataElement.dataType?.label]
            if (!this.dataElements.contains(dataElement)) {
                this.dataElements.add(dataElement)
            }
            dataElement.setAssociations()
        }
        dataClass.referenceTypes = referenceTypes.findAll {it.referenceClass?.id == dataClass.id} as List<DataType>
    }

    protected List<DataType> dataTypeReferenceTypes() {
        dataTypes.findAll {it.isReferenceType()}
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

    @Transient
    @JsonIgnore
    @Override
    List<ItemReference> retrieveItemReferences() {
        List<ItemReference> pathsBeingReferenced = [] + super.retrieveItemReferences()

        ItemReferencerUtils.addItems(dataTypes, pathsBeingReferenced)
        ItemReferencerUtils.addItems(enumerationValues, pathsBeingReferenced)
        ItemReferencerUtils.addItems(dataElements, pathsBeingReferenced)
        ItemReferencerUtils.addItems(dataClasses, pathsBeingReferenced)
        ItemReferencerUtils.addItem(parent, pathsBeingReferenced)

        return pathsBeingReferenced
    }

    @Transient
    @JsonIgnore
    @Override
    void replaceItemReferencesByIdentity(IdentityHashMap<Item, Item> replacements, List<Item> notReplaced) {
        super.replaceItemReferencesByIdentity(replacements, notReplaced)
        parent = ItemReferencerUtils.replaceItemByIdentity(parent, replacements, notReplaced)
        dataTypes = ItemReferencerUtils.replaceItemsByIdentity(dataTypes, replacements, notReplaced)
        dataClasses = ItemReferencerUtils.replaceItemsByIdentity(dataClasses, replacements, notReplaced)
        dataElements = ItemReferencerUtils.replaceItemsByIdentity(dataElements, replacements, notReplaced)
        enumerationValues = ItemReferencerUtils.replaceItemsByIdentity(enumerationValues, replacements, notReplaced)
    }

    @Override
    void copyInto(Item into) {
        super.copyInto(into)
        DataModel intoDataModel = (DataModel) into
        intoDataModel.dataTypes = ItemUtils.copyItems(this.dataTypes, intoDataModel.dataTypes)
        intoDataModel.enumerationValues = ItemUtils.copyItems(this.enumerationValues, intoDataModel.enumerationValues)
        intoDataModel.dataClasses = ItemUtils.copyItems(this.dataClasses, intoDataModel.dataClasses)
        intoDataModel.dataElements = ItemUtils.copyItems(this.dataElements, intoDataModel.dataElements)
        intoDataModel.modelType = ItemUtils.copyItem(this.modelType, intoDataModel.modelType)
        intoDataModel.dataModelType = ItemUtils.copyItem(this.dataModelType, intoDataModel.dataModelType)
    }

    @Override
    Item shallowCopy() {
        DataModel dataModelShallowCopy = new DataModel()
        this.copyInto(dataModelShallowCopy)
        return dataModelShallowCopy
    }
}
