package uk.ac.ox.softeng.mauro.profile

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Inject
import jakarta.inject.Singleton
import uk.ac.ox.softeng.mauro.plugin.PluginType

@Singleton
class ProfileSpecificationProfile extends JsonBasedProfile {

    public final static String NAMESPACE = "uk.ac.ox.softeng.maurodatamapper.profile"

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
