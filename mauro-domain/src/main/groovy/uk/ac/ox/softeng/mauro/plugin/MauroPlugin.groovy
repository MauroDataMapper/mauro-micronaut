package uk.ac.ox.softeng.mauro.plugin

import uk.ac.ox.softeng.mauro.domain.diff.ArrayDiff

import com.fasterxml.jackson.annotation.JsonIgnore

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
