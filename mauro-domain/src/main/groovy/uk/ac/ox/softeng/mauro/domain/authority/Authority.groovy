package uk.ac.ox.softeng.mauro.domain.authority


import uk.ac.ox.softeng.mauro.domain.model.Item
import uk.ac.ox.softeng.mauro.domain.security.SecurableResource

import com.fasterxml.jackson.annotation.JsonAlias
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.data.annotation.MappedEntity

@CompileStatic
@AutoClone
@MappedEntity(schema = 'core')
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class Authority extends Item implements SecurableResource {

    String url

    Boolean readableByEveryone = false

    Boolean readableByAuthenticatedUsers = false

    String label

    @JsonAlias(['default_authority'])
    Boolean defaultAuthority = false

}
