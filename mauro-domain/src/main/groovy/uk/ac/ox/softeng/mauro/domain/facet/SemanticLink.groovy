package uk.ac.ox.softeng.mauro.domain.facet

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import jakarta.persistence.Transient

@CompileStatic
@MappedEntity(schema = 'core')
@AutoClone
@Indexes([@Index(columns = ['multi_facet_aware_item_id'], unique = true)])
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class SemanticLink extends Facet {

    /*
    {
        "id":"f7c6ea44-88d9-44f3-959d-d7a94e0a70e6",
        "linkType":"Refines",
        "domainType":"SemanticLink",
        "unconfirmed":false,

        "sourceMultiFacetAwareItem":{"id":"c30f95df-d9a9-4d1c-b675-1a2fae047aaa","domainType":"DataClass","label":"Data class 1","model":"0c35c0f4-b0df-4147-9f11-44d06ee7c50e","breadcrumbs":[{"id":"0c35c0f4-b0df-4147-9f11-44d06ee7c50e","label":"test dm","domainType":"DataModel","finalised":false}]},
        "targetMultiFacetAwareItem":{"id":"af3c4f09-04ca-4902-9150-cbfcfde76d51","domainType":"DataClass","label":"Data class 2","model":"0c35c0f4-b0df-4147-9f11-44d06ee7c50e","breadcrumbs":[{"id":"0c35c0f4-b0df-4147-9f11-44d06ee7c50e","label":"test dm","domainType":"DataModel","finalised":false}]}}


        needs:

        domainType = SemanticLink
        sourceMultiFacetAwareItem
        targetMultiFacetAwareItem

        as

        @JsonIgnore
        @Transient

        This may need a SemanticLinkDTO for transferring the information about and putting it in the API ?
        Or else some processing before sending the objects out

     */

    @JsonAlias(['link_type'])
    SemanticLinkType linkType
    @JsonAlias(['target_multi_facet_aware_item_id'])
    UUID targetMultiFacetAwareItemId
    @JsonAlias(['target_multi_facet_aware_item_domain_type'])
    String targetMultiFacetAwareItemDomainType
    Boolean unconfirmed

    SemanticLink() {
        unconfirmed = false
    }
}