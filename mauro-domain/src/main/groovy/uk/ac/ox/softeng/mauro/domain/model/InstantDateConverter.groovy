package uk.ac.ox.softeng.mauro.domain.model

import com.fasterxml.jackson.databind.util.StdConverter
import groovy.transform.CompileStatic

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoField

@CompileStatic
class InstantDateConverter extends StdConverter<Instant, String> {

    SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy")


    @Override
    String convert(Instant value) {
        if(!value) return null

        try {
            Date date = Date.from(value)
            return formatter.format(date)
        } catch (Exception e) {
            e.printStackTrace()
            return "????-??-??"
        }
    }
}
