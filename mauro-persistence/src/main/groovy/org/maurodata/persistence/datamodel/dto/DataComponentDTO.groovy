package org.maurodata.persistence.datamodel.dto

import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import org.maurodata.domain.datamodel.DataComponent
import org.maurodata.persistence.model.dto.AdministeredItemDTO

@CompileStatic
@AutoClone
@Introspected
@MappedEntity(value = 'data_component', schema = 'datamodel', alias = 'data_component_')
class DataComponentDTO extends DataComponent implements AdministeredItemDTO {
}
