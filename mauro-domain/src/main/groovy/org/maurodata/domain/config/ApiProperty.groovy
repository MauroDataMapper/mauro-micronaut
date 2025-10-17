package org.maurodata.domain.config

import org.maurodata.domain.model.ItemUtils

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.*
import jakarta.validation.constraints.NotBlank
import org.maurodata.domain.model.Item
import org.maurodata.domain.security.CatalogueUser

@CompileStatic
@MappedEntity(schema = 'core')
@AutoClone
@Indexes([@Index(columns = ['key'], unique = true)])
class ApiProperty extends Item {

    String key

    String value

    Boolean publiclyVisible

    @Nullable
    @NotBlank
    String category

    @Nullable
    @MappedProperty('last_updated_by')
    @JsonIgnore
    CatalogueUser lastUpdatedBy

    @Transient
    @JsonProperty('lastUpdatedBy')
    String getLastUpdatedByEmailAddress() {
        lastUpdatedBy?.emailAddress
    }

    @Override
    void copyInto(Item into) {
        super.copyInto(into)
        ApiProperty intoApiProperty = (ApiProperty) into
        intoApiProperty.key = ItemUtils.copyItem(this.key, intoApiProperty.key)
        intoApiProperty.value = ItemUtils.copyItem(this.value, intoApiProperty.value)
        intoApiProperty.publiclyVisible = ItemUtils.copyItem(publiclyVisible, intoApiProperty.publiclyVisible)
        intoApiProperty.category = ItemUtils.copyItem(this.category, intoApiProperty.category)
        intoApiProperty.lastUpdatedBy = ItemUtils.copyItem(this.lastUpdatedBy, intoApiProperty.lastUpdatedBy)
    }

    @Override
    Item shallowCopy() {
        ApiProperty apiPropertyShallowCopy = new ApiProperty()
        this.copyInto(apiPropertyShallowCopy)
        return apiPropertyShallowCopy
    }

    enum ApiPropertyEnum {
        SITE_URL('site.url'),
        EMAIL_ADMIN_REGISTER_BODY('email.admin_register.body'),
        EMAIL_ADMIN_REGISTER_SUBJECT('email.admin_register.subject'),
        EMAIL_SELF_REGISTER_BODY('email.self_register.body'),
        EMAIL_SELF_REGISTER_SUBJECT('email.self_register.subject'),
        EMAIL_ADMIN_CONFIRM_REGISTRATION_BODY('email.admin_confirm_registration.body'),
        EMAIL_ADMIN_CONFIRM_REGISTRATION_SUBJECT('email.admin_confirm_registration.subject'),
        EMAIL_INVITE_VIEW_SUBJECT('email.invite_view.subject'),
        EMAIL_INVITE_VIEW_BODY('email.invite_view.body'),
        EMAIL_INVITE_EDIT_SUBJECT('email.invite_edit.subject'),
        EMAIL_INVITE_EDIT_BODY('email.invite_edit.body'),
        EMAIL_FROM_ADDRESS('email.from.address'),
        EMAIL_FROM_NAME('email.from.name'),
        EMAIL_FORGOTTEN_PASSWORD_SUBJECT('email.forgotten_password.subject'),
        EMAIL_FORGOTTEN_PASSWORD_BODY('email.forgotten_password.body'),
        EMAIL_PASSWORD_RESET_SUBJECT('email.password_reset.subject'),
        EMAIL_PASSWORD_RESET_BODY('email.password_reset.body'),
        SECURITY_RESTRICT_ROOT_FOLDER('security.restrict.root.folder'),
        SECURITY_RESTRICT_CLASSIFIER_CREATE('security.restrict.classifier.create'),
        SECURITY_HIDE_EXCEPTIONS('security.hide.exception'),
        FEATURE_COPY_ANNOTATIONS_TO_NEW_VERSION('feature.copy_annotations_to_new_version'),
        FEATURE_ATTACHMENT_SIZE_LIMIT('feature.attachment_size_limit_mb'),
        FEATURE_CREATE_REFINEMENT_LINKS_BETWEEN_VERSIONS('feature.create_refinement_links')

        String key

        ApiPropertyEnum(String key) {
            this.key = key
        }

        String toString() {
            key
        }
    }
}
