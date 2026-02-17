package org.maurodata.profile

import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel

import groovy.transform.CompileStatic

@CompileStatic
class DataModelBasedProfile implements Profile {

    DataModel dataModel
    private Map<String, String> metadataMap

    @Override
    String getName() {
        return dataModel.label
    }

    @Override
    String getDisplayName() {
        return dataModel.label
    }

    @Override
    String getVersion() {
        dataModel.modelVersionTag?:dataModel.modelVersion
    }

    String getDescription() {
        dataModel.description
    }

    @Override
    String getMetadataNamespace() {
        metadataMap["metadataNamespace"]
    }

    @Override
    boolean getCanBeEditedAfterFinalisation(){
        Boolean.parseBoolean(metadataMap["canBeEditedAfterFinalisation"]?:"false")
    }

    @Override
    List<String> getProfileApplicableForDomains() {
        (metadataMap["profileApplicableForDomains"]?:"").split(";").collect {it.trim()}
    }

    DataModelBasedProfile(DataModel dataModel) {
        this.dataModel = dataModel
        this.metadataMap = dataModel.metadataAsMap(ProfileSpecificationProfile.NAMESPACE)
        sections = dataModel.dataClasses.collect { sectionFromClass(it) }
    }

    private ProfileSection sectionFromClass(DataClass dataClass) {
        new ProfileSection().tap {
            label = dataClass.label
            description = dataClass.description
            fields = dataClass.dataElements.collect { fieldFromElement(it) }
        }
    }

    private ProfileField fieldFromElement(DataElement dataElement) {
        Map<String, String> metadataMap = dataElement.metadataAsMap(ProfileSpecificationFieldProfile.NAMESPACE)

        new ProfileField().tap {
            fieldName = dataElement.label
            description = dataElement.description
            minMultiplicity = dataElement.minMultiplicity
            maxMultiplicity = dataElement.maxMultiplicity
            if(dataElement.dataType?.enumerationValues) {
                allowedValues = dataElement.dataType?.enumerationValues?.collect {it.key}
                dataType = ProfileFieldDataType.ENUMERATION
            } else {
                dataType = ProfileFieldDataType.fromString(dataElement.dataType?.label)
                if(!dataType) { // We'll default to STRING
                    dataType = ProfileFieldDataType.STRING
                }
            }
            metadataPropertyName = metadataMap["metadataPropertyName"]
            regularExpression = metadataMap["regularExpression"]
            defaultValue = metadataMap["defaultValue"]
            if(metadataMap["editableAfterFinalisation"]) {
                editableAfterFinalisation = Boolean.parseBoolean(metadataMap["editableAfterFinalisation"])
            }
            // TODO add ways to provide values for "derived", "derivedFrom", and "uneditable"?
            this
        }
    }

}
