package uk.ac.ox.softeng.mauro.profile

import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem

class ProfileSection {

    String label
    String description

    List<ProfileField> fields

    List<String> validate(AdministeredItem item, List<Metadata> profileMetadata) {
        List<String> errors = []
        fields.each { field ->
            String value = profileMetadata.find {it.key == field.getMetadataKey(label)}?.value
            errors.addAll(field.validate(value))
        }
        return errors
    }

}
