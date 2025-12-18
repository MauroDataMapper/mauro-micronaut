package org.maurodata.plugin

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileStatic

@CompileStatic
trait MauroPlugin {

    String getNamespace() {
        getClass().getPackage().getName()
    }

    String getName() {
        getClass().getSimpleName()
    }

    String version

    String displayName

    String description

    @JsonIgnore
    abstract PluginType getPluginType()

    String getProviderType() {
        pluginType.toString()
    }

}
