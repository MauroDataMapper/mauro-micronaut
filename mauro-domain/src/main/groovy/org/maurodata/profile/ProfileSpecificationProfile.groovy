package org.maurodata.profile

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Singleton

@Singleton
class ProfileSpecificationProfile extends JsonBasedProfile {

    public final static String NAMESPACE = "org.maurodata.profile"

    ProfileSpecificationProfile(ObjectMapper objectMapper) {
        super(objectMapper)
    }

    @Override
    String getJsonFileName() {
        'ProfileSpecificationProfile.json'
    }

    String version = "1.0.0"

    String displayName = "Profile Specification Profile"

    String metadataNamespace = NAMESPACE


}
