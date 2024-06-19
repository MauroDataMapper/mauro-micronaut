package uk.ac.ox.softeng.mauro.profile

import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel

abstract class DataModelBasedProfile implements Profile {


    DataModelBasedProfile(DataModel dataModel) {
        Map<String, String> metadataMap = dataModel.metadataAsMap(ProfileSpecificationProfile.NAMESPACE)
        displayName = dataModel.label
        version = dataModel.modelVersionTag?:dataModel.modelVersion
        description = dataModel.description
        metadataNamespace = metadataMap["metadataNamespace"]
        if(metadataMap["canBeEditedAfterFinalisation"]) {
            canBeEditedAfterFinalisation = Boolean.parseBoolean(metadataMap["canBeEditedAfterFinalisation"])
        }
        if(metadataMap["profileApplicableForDomains"]) {
            profileApplicableForDomains = metadataMap["profileApplicableForDomains"].split(";")
        }
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
            if(dataElement.dataType.enumerationValues) {
                allowedValues = dataElement.dataType.enumerationValues.collect {it.value}
                dataType = ProfileFieldDataType.ENUMERATION
            } else {
                dataType = ProfileFieldDataType.fromString(dataElement.dataType.label)
                if(!dataType) { // We'll default to STRING
                    dataType = ProfileFieldDataType.STRING
                }
            }
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
