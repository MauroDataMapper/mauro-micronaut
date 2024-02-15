package uk.ac.ox.softeng.mauro.persistence.datamodel

import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.terminology.Term
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationship
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationshipType
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.model.ModelContentRepository

import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Bean
import jakarta.inject.Inject
import reactor.core.publisher.Mono

@CompileStatic
@Bean
class DataModelContentRepository extends ModelContentRepository<DataModel> {

    @Inject
    DataModelRepository dataModelRepository

    @Inject
    DataClassRepository dataClassRepository

    @Inject
    DataTypeRepository dataTypeRepository

    @Inject
    DataElementRepository dataElementRepository

    Mono<DataModel> findWithAssociations(UUID id) {
        dataModelRepository.findById(id).flatMap {
            Mono.zip(dataClassRepository.findAllByDataModel(it).collectList(), termRelationshipTypeRepository.findAllByTerminology(it).collectList(),
                     termRelationshipRepository.findAllByTerminology(it).collectList()).map {Tuple3<List<Term>, List<TermRelationshipType>, List<TermRelationship>> tuple3 ->
                it.terms = tuple3.getT1()
                it.termRelationshipTypes = tuple3.getT2()
                it.termRelationships = tuple3.getT3()

                it
            }
        } as Mono<DataModel>
    }
}
