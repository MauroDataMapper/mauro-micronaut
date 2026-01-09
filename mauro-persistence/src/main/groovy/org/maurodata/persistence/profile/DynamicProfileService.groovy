package org.maurodata.persistence.profile

import org.maurodata.persistence.ContentsService
import org.maurodata.persistence.cache.ModelCacheableRepository
import org.maurodata.persistence.datamodel.DataModelRepository
import org.maurodata.profile.DataModelBasedProfile
import org.maurodata.profile.ProfileSpecificationProfile

import groovy.transform.CompileStatic
import jakarta.inject.Inject
import jakarta.inject.Singleton

@CompileStatic
@Singleton
class DynamicProfileService {

    @Inject
    ModelCacheableRepository.DataModelCacheableRepository dataModelCacheableRepository

    @Inject
    ContentsService contentsService


    List<DataModelBasedProfile> getDynamicProfiles() {
        dataModelCacheableRepository.getAllModelsByNamespace(ProfileSpecificationProfile.NAMESPACE).collect {
            dataModel -> new DataModelBasedProfile(
                dataModelCacheableRepository.loadWithContent(dataModel.id))
        }
    }

}
