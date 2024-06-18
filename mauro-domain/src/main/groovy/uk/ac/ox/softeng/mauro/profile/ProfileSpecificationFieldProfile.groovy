package uk.ac.ox.softeng.mauro.profile

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Singleton

@Singleton
class ProfileSpecificationFieldProfile extends JsonBasedProfile {

    ProfileSpecificationFieldProfile(ObjectMapper objectMapper) {
        super(objectMapper)
    }

    @Override
    String getJsonFileName() {
        'ProfileSpecificationFieldProfile.json'
    }

    String version = "1.0.0"

    String displayName = "Profile Specification Profile"

}
