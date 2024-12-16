package uk.ac.ox.softeng.mauro.plugin

import groovy.transform.CompileStatic
import uk.ac.ox.softeng.mauro.domain.email.Email
import uk.ac.ox.softeng.mauro.domain.security.CatalogueUser

@CompileStatic
trait EmailPlugin extends MauroPlugin {



    boolean enabled

    abstract boolean configure(Map props)

    abstract void retrySendEmail(Email email) throws Exception

    abstract void sendEmail(CatalogueUser catalogueUserRecipient, Email email) throws Exception

    abstract void testConnection() throws Exception

    PluginType getPluginType() {
        return PluginType.Email
    }


}
