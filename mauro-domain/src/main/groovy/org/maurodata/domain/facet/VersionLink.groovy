package org.maurodata.domain.facet

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.data.annotation.*
import org.maurodata.domain.diff.*
import org.maurodata.domain.model.Model
import org.maurodata.domain.security.CatalogueUser

@CompileStatic
@MappedEntity(value = 'version_link', schema = 'core')
@AutoClone
@Indexes([@Index(columns = ['multi_facet_aware_item_id'])])
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class VersionLink extends Facet  {

    final static String NEW_FORK_OF="NEW_FORK_OF", NEW_MODEL_VERSION_OF="NEW_MODEL_VERSION_OF"
    final static Map<String,String> descriptions=[:]
    static {
        descriptions.put(NEW_FORK_OF,"New Fork Of");
        descriptions.put(NEW_MODEL_VERSION_OF,"New Model Version Of")
    }

    @JsonAlias(['version_link_type'])
    String versionLinkType

    @JsonAlias(['target_model_id'])
    UUID targetModelId

    @JsonAlias(['target_model_domain_type'])
    String targetModelDomainType

    /****
     * Methods for building a tree-like DSL
     */

    static VersionLink build(
            Map args,
            @DelegatesTo(value = VersionLink, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        new VersionLink(args).tap(closure)
    }

    static VersionLink build(
            @DelegatesTo(value = VersionLink, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        build [:], closure
    }

    String setVersionLinkType(final String linkType) {
        this.versionLinkType = linkType
        this.versionLinkType
    }

    @JsonIgnore
    @Transient
    String getDescription()
    {
        if(versionLinkType==null){return ""}

        final String description=descriptions.get(versionLinkType)
        if(description!=null){return description}

        return "";
    }

    @JsonIgnore
    @Transient
    Model setTargetModel(final Model targetModel) {
        this.targetModelId = targetModel.id
        this.targetModelDomainType=targetModel.domainType
        targetModel
    }
}
