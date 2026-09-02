package org.maurodata.domain.security

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import org.maurodata.domain.model.Item
import org.maurodata.domain.model.ItemUtils

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.util.StdConverter
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import jakarta.persistence.Transient

import java.time.Instant

@CompileStatic
@MappedEntity(schema = 'security')
@AutoClone
@Indexes([@Index(columns = ['email_address'], unique = true)])
class CatalogueUser extends Item {

    String emailAddress

    String firstName
    String lastName
    String jobTitle
    String organisation

    Boolean pending
    Boolean disabled
    String profilePicture
    // should be UserImageFile type
    String userPreferences
    UUID resetToken

    String creationMethod
    Instant lastLogin

    @JsonIgnore
    byte[] salt

    @JsonIgnore
    byte[] password

    @JsonIgnore
    String tempPassword

    @Relation(Relation.Kind.MANY_TO_MANY)
    @JsonDeserialize(converter = CatalogueUserGroupsConverter)
    Set<UserGroup> groups = []

    @Transient
    List<String> availableActions = []

    @Transient
    boolean getNeedsToResetPassword(){
        tempPassword != null || resetToken != null
    }

    static class CatalogueUserGroupsConverter extends StdConverter<JsonNode, Set<UserGroup>> {
        @Override
        Set<UserGroup> convert(JsonNode node) {

            Set<UserGroup> groups = []

            if (node.isArray()) {
                node.forEach {JsonNode element ->

                    if (element.isTextual()) {
                        UUID id = UUID.fromString(element.asText())
                        groups.add(new UserGroup(id: id))
                    } else if (element.isObject()) {
                        try {
                            JsonNode idNode = element.get("id")
                            UUID id = UUID.fromString(idNode.asText())
                            groups.add(new UserGroup(id: id))
                        }
                        catch (Exception ex) {
                            throw new RuntimeException("Failed to parse UserGroup JSON: " + element, ex)
                        }
                    }
                }
            }

            groups
        }
    }

    static class CatalogueUserDeserializer extends StdDeserializer<CatalogueUser> {

        CatalogueUserDeserializer() { super(CatalogueUser) }

        @Override
        CatalogueUser deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p)

            if (node.isTextual()) {
                return new CatalogueUser(id: UUID.fromString(node.asText()))
            }

            CatalogueUser catalogueUser = new CatalogueUser()
            JsonNode idNode = node.get("id")
            if (idNode != null && !idNode.isNull()) {
                catalogueUser.setId(UUID.fromString(idNode.asText()))
            }
            if (node.has("email_address")) {
                catalogueUser.setEmailAddress(node.get("email_address").asText(null))
            }
            if (node.has("first_name")) {
                catalogueUser.setFirstName(node.get("first_name").asText(null))
            }
            if (node.has("last_name")) {
                catalogueUser.setLastName(node.get("last_name").asText(null))
            }
            if (node.has("job_title")) {
                catalogueUser.setJobTitle(node.get("job_title").asText(null))
            }
            if (node.has("organisation")) {
                catalogueUser.setOrganisation(node.get("organisation").asText(null))
            }
            if (node.has("profile_picture")) {
                catalogueUser.setProfilePicture(node.get("profile_picture").asText(null))
            }
            return catalogueUser
        }
    }

    @JsonIgnore
    @Transient
    String getFullName() {
        "$firstName $lastName"
    }

    @Override
    void copyInto(Item into) {
        super.copyInto(into)
        CatalogueUser intoCatalogueUser = (CatalogueUser) into
        intoCatalogueUser.emailAddress = ItemUtils.copyItem(this.emailAddress, intoCatalogueUser.emailAddress)
        intoCatalogueUser.firstName = ItemUtils.copyItem(this.firstName, intoCatalogueUser.firstName)
        intoCatalogueUser.lastName = ItemUtils.copyItem(this.lastName, intoCatalogueUser.lastName)
        intoCatalogueUser.jobTitle = ItemUtils.copyItem(this.jobTitle, intoCatalogueUser.jobTitle)
        intoCatalogueUser.organisation = ItemUtils.copyItem(this.organisation, intoCatalogueUser.organisation)
        intoCatalogueUser.pending = ItemUtils.copyItem(this.pending, intoCatalogueUser.pending)
        intoCatalogueUser.disabled = ItemUtils.copyItem(this.disabled, intoCatalogueUser.disabled)
        intoCatalogueUser.profilePicture = ItemUtils.copyItem(this.profilePicture, intoCatalogueUser.profilePicture)
        intoCatalogueUser.userPreferences = ItemUtils.copyItem(this.userPreferences, intoCatalogueUser.userPreferences)
        intoCatalogueUser.resetToken = ItemUtils.copyItem(this.resetToken, intoCatalogueUser.resetToken)
        intoCatalogueUser.creationMethod = ItemUtils.copyItem(this.creationMethod, intoCatalogueUser.creationMethod)
        intoCatalogueUser.lastLogin = ItemUtils.copyItem(this.lastLogin, intoCatalogueUser.lastLogin)
        intoCatalogueUser.salt = ItemUtils.copyItem(this.salt, intoCatalogueUser.salt)
        intoCatalogueUser.password = ItemUtils.copyItem(this.password, intoCatalogueUser.password)
        intoCatalogueUser.tempPassword = ItemUtils.copyItem(this.tempPassword, intoCatalogueUser.tempPassword)
        intoCatalogueUser.groups = ItemUtils.copyItems(this.groups, intoCatalogueUser.groups)
    }

    @Override
    Item shallowCopy() {
        CatalogueUser catalogueUserShallowCopy = new CatalogueUser()
        this.copyInto(catalogueUserShallowCopy)
        return catalogueUserShallowCopy
    }
}
