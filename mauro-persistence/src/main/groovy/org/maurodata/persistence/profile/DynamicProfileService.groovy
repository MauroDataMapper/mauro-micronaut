package org.maurodata.persistence.profile

import org.maurodata.persistence.datamodel.DataModelContentRepository
import org.maurodata.persistence.datamodel.DataModelRepository
import org.maurodata.profile.DataModelBasedProfile
import org.maurodata.profile.ProfileSpecificationProfile

import jakarta.inject.Inject
import jakarta.inject.Singleton


@Singleton
class DynamicProfileService {

    @Inject
    DataModelRepository dataModelRepository

    @Inject
    DataModelContentRepository dataModelContentRepository


    List<DataModelBasedProfile> getDynamicProfiles() {
        dataModelRepository.getAllModelsByNamespace(ProfileSpecificationProfile.NAMESPACE).collect {
            dataModel -> new DataModelBasedProfile(
                    dataModelContentRepository.findWithContentById(dataModel.id))
        }
    }

}
