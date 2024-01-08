package uk.ac.ox.softeng.mauro.plugin.email

import uk.ac.ox.softeng.mauro.plugin.MauroPlugin

import jakarta.validation.constraints.Email

trait EmailPlugin extends MauroPlugin {

    boolean enabled

    abstract boolean configure(Map props)

    abstract String sendEmail(Email email)

    abstract void testConnection()

    String validateEmail(Email email) {
        null
    }


}