package uk.ac.ox.softeng.mauro.domain.authority

import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.domain.security.SecurableResource

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import jakarta.persistence.Transient

@CompileStatic
@AutoClone
@MappedEntity(schema = 'core')
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class Authority extends AdministeredItem implements SecurableResource {

    String url

    Boolean readableByEveryone = false

    Boolean readableByAuthenticatedUsers = false

    @Override
    @Transient
    @JsonIgnore
    AdministeredItem getParent() {
        null
    }

    @Override
    @Transient
    @JsonIgnore
    void setParent(AdministeredItem parent) {
        null
    }

    @Override
    @Transient
    @JsonIgnore
    Authority getOwner() {
        this
    }
}
