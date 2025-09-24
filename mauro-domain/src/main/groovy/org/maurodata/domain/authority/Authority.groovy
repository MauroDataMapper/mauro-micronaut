package org.maurodata.domain.authority

import org.maurodata.domain.model.Item
import org.maurodata.domain.model.ItemUtils
import org.maurodata.domain.security.SecurableResource

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

    @Override
    void copyInto(Item into) {
        super.copyInto(into)
        Authority intoAuthority = (Authority) into
        intoAuthority.url = ItemUtils.copyItem(this.url, intoAuthority.url)
        intoAuthority.readableByEveryone = ItemUtils.copyItem(this.readableByEveryone, intoAuthority.readableByEveryone)
        intoAuthority.readableByAuthenticatedUsers = ItemUtils.copyItem(this.readableByAuthenticatedUsers, intoAuthority.readableByAuthenticatedUsers)
        intoAuthority.label = ItemUtils.copyItem(this.label, intoAuthority.label)
        intoAuthority.defaultAuthority = ItemUtils.copyItem(this.defaultAuthority, intoAuthority.defaultAuthority)
    }

    @Override
    Item shallowCopy() {
        Authority authorityShallowCopy = new Authority()
        this.copyInto(authorityShallowCopy)
        return authorityShallowCopy
    }
}
