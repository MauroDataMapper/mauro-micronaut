package uk.ac.ox.softeng.mauro.domain.security

interface SecurableResource {

    UUID id

    Boolean readableByEveryone

    Boolean readableByAuthenticatedUsers
}