package org.maurodata.profile

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Singleton

@Singleton
class ProfileSpecificationFieldProfile extends JsonBasedProfile {

    public static final String NAMESPACE = "org.maurodata.profile.dataelement"

    ProfileSpecificationFieldProfile(ObjectMapper objectMapper) {
        super(objectMapper)
    }

    @Override
    String getJsonFileName() {
        'ProfileSpecificationFieldProfile.json'
    }

    String version = "1.0.0"

    String displayName = "Profile Specification Field Profile"

    String metadataNamespace = NAMESPACE

    @Override
    List<String> getProfileApplicableForDomains() {
        return ['DataElement']
    }
}
