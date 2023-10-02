package uk.ac.ox.softeng.mauro.domain.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.core.order.Ordered
import io.micronaut.data.annotation.MappedEntity
import jakarta.persistence.Transient


@CompileStatic
@Introspected
@MappedEntity
abstract class ModelItem<M extends Model> extends AdministeredItem implements Ordered {

    @JsonProperty('index')
    @Nullable(inherited = true)
    Integer idx

    @Override
    @Transient
    @JsonIgnore
    int getOrder() {
        idx
    }

    @Transient
    @JsonIgnore
    void setOrder(int order) {
        idx = order
    }
}
