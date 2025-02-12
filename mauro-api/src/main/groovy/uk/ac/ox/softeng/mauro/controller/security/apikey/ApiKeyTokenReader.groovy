package uk.ac.ox.softeng.mauro.controller.security.apikey

import io.micronaut.security.token.reader.HttpHeaderTokenReader
import jakarta.inject.Singleton

@Singleton
class ApiKeyTokenReader extends HttpHeaderTokenReader {

    @Override
    protected String getPrefix() {
        return null
    }

    @Override
    protected String getHeaderName() {
        return 'apiKey'
    }

}
