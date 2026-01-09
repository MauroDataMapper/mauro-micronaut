package org.maurodata.plugin

import groovy.transform.CompileStatic

@CompileStatic
enum PluginType {

    Importer,
    Exporter,
    Email,
    Profile,
    DefaultDataTypeProvider

}