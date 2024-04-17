package uk.ac.ox.softeng.mauro.domain.facet

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.util.StdConverter

@JsonDeserialize(converter = SummaryMetadataTypeConverter)
enum SummaryMetadataType {
    MAP,
    NUMBER,
    STRING

    static class SummaryMetadataTypeConverter extends StdConverter<String, SummaryMetadataType> {
        @Override
        SummaryMetadataType convert(String value) {
            value? valueOf(value.toUpperCase()): null
        }
    }
}
