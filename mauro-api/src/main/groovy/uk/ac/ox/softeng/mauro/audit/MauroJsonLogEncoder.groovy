package uk.ac.ox.softeng.mauro.audit

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.encoder.EncoderBase
import com.fasterxml.jackson.databind.ObjectMapper

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MauroJsonLogEncoder extends EncoderBase<ILoggingEvent> {

    static ObjectMapper objectMapper = new ObjectMapper()

    static DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneId.systemDefault())

    @Override
    byte[] headerBytes() {
        return []
    }
    @Override
    byte[] footerBytes() {
        return []
    }

    @Override
    byte[] encode(ILoggingEvent iLoggingEvent) {
        Map response = [:]
        response['timestamp'] = formatter.format(Instant.ofEpochMilli(iLoggingEvent.timeStamp))
        response['message'] = iLoggingEvent.message
        response['throwable'] = iLoggingEvent.throwableProxy
        response['level'] = iLoggingEvent.level.toString()
        response['threadname'] = iLoggingEvent.threadName
        response['sequenceNumber'] = iLoggingEvent.sequenceNumber
        iLoggingEvent.keyValuePairs.each {keyValuePair ->
            response[keyValuePair.key] = keyValuePair.value
        }
        (objectMapper.writeValueAsString(response) + '\n').bytes
    }


}
