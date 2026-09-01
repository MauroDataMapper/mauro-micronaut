package org.maurodata.plugin

import groovy.transform.CompileStatic

@CompileStatic
enum PluginType {

    Importer("importer"),
    Exporter("exporter"),
    Email("email"),
    Profile("profile"),
    DefaultDataTypeProvider("defaultdatatypeprovider")

    private String pluginKind

    PluginType(final String pluginKind) {
        this.pluginKind = pluginKind
    }

    boolean kindMatches(final String kind) {
        return this.pluginKind.equalsIgnoreCase(kind)
    }
}
