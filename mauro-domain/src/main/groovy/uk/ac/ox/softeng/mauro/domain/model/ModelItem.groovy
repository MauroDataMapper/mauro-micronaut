package uk.ac.ox.softeng.mauro.domain.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.core.order.Ordered
import io.micronaut.data.annotation.MappedEntity
import jakarta.persistence.Transient

/**
 * A ModelItem is a component of a model that is ordered - for example the data classes in a data model or the
 * data elements within a data class.
 */
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
        idx ?: 0
    }

    @Transient
    @JsonIgnore
    void setOrder(int order) {
        idx = order
    }
}