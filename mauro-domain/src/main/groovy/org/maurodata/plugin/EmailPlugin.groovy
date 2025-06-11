package org.maurodata.plugin

import groovy.transform.CompileStatic
import org.maurodata.domain.email.Email
import org.maurodata.domain.security.CatalogueUser

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
