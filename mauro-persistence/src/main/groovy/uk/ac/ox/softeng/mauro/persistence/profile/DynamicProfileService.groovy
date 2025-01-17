package uk.ac.ox.softeng.mauro.persistence.profile

import uk.ac.ox.softeng.mauro.persistence.datamodel.DataModelContentRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataModelRepository
import uk.ac.ox.softeng.mauro.profile.DataModelBasedProfile
import uk.ac.ox.softeng.mauro.profile.ProfileSpecificationProfile

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
