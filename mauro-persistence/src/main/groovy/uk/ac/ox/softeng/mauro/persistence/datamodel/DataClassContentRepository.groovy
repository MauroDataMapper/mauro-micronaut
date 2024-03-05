package uk.ac.ox.softeng.mauro.persistence.datamodel

import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.DataClassCacheableRepository

import groovy.transform.CompileStatic
import jakarta.inject.Inject
import jakarta.inject.Singleton
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemContentRepository

@CompileStatic
@Singleton
class DataClassContentRepository extends AdministeredItemContentRepository {

    @Inject
    DataClassCacheableRepository dataClassCacheableRepository

    @Override
    DataClass readWithContentById(UUID id) {
        DataClass dataClass = dataClassCacheableRepository.readById(id)

        dataClass
    }
}
