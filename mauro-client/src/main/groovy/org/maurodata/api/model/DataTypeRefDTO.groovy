package org.maurodata.api.model

import org.maurodata.domain.datamodel.EnumerationValue

import io.micronaut.core.annotation.Nullable

class DataTypeRefDTO extends ModelItemRefDTO
{
    @Nullable
    DataClassRefDTO referenceClass

    @Nullable
    String modelResourceDomainType

    @Nullable
    UUID modelResourceId

    // For Primitive Types only
    @Nullable
    String units

    List<EnumerationValue> enumerationValues = []
}
