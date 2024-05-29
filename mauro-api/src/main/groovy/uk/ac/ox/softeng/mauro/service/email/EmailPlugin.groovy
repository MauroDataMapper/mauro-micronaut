package uk.ac.ox.softeng.mauro.service.email

import groovy.transform.CompileStatic
import uk.ac.ox.softeng.mauro.domain.email.Email
import uk.ac.ox.softeng.mauro.plugin.MauroPlugin
import uk.ac.ox.softeng.mauro.plugin.PluginType

@CompileStatic
trait EmailPlugin extends MauroPlugin {

    String version = '4.0.0'

    String displayName = 'JSON Terminology Importer'


    boolean enabled

    abstract boolean configure(Map props)

    abstract String sendEmail(Email email)

    abstract boolean testConnection()

    abstract String validateEmail(Email email)

    PluginType getPluginType() {
        return PluginType.Email
    }


}
