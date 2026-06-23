package org.maurodata.api.model

import io.micronaut.core.annotation.Nullable

class DataClassRefDTO extends ModelItemRefDTO
{
    @Nullable
    Integer minMultiplicity

    @Nullable
    Integer maxMultiplicity

}
