package uk.ac.ox.softeng.mauro.domain.model

import com.fasterxml.jackson.databind.util.StdConverter

import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

class InstantConverter extends StdConverter<String, Instant> {

    @Override
    Instant convert(String value) {
        try {
            return OffsetDateTime.parse(value).toInstant()
        } catch (DateTimeParseException ignored) {
            // if timezone is missing, assume UTC
            return OffsetDateTime.parse(value + 'Z').toInstant()
        }
    }
}
