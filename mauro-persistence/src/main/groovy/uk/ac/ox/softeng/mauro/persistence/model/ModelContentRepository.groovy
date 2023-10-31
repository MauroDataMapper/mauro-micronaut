package uk.ac.ox.softeng.mauro.persistence.model

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.exception.MauroInternalException

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Bean
import io.micronaut.core.annotation.NonNull
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import jakarta.validation.Valid
import reactor.core.publisher.Mono

@Slf4j
@CompileStatic
@Bean
class ModelContentRepository<M extends Model> extends AdministeredItemContentRepository<M> {

    @Inject
    List<AdministeredItemRepository> administeredItemRepositories

    @Transactional
    Mono<M> updateWithContent(@Valid @NonNull M model) {
        Collection<AdministeredItem> contents = model.getAllContents()
        contents.each {
            if (it.owner != model) throw new MauroInternalException('Content must belong to model being updated')
        }
        Mono.zip(update(model), Mono.zip(contents.collect {getRepository(it).update(it)}, {Optional.empty()}).defaultIfEmpty(Optional.empty())).map {it.getT1()}
    }

    @Transactional
    Mono<Long> deleteWithContent(@NonNull M model) {
        Collection<AdministeredItem> contents = model.getAllContents()
        contents.each {
            if (it.owner != model) throw new MauroInternalException('Content must belong to model being deleted')
        }
        Mono.zip(delete(model), Mono.zip(contents.collect {getRepository(it).delete(it)}, {Optional.empty()}).defaultIfEmpty(Optional.empty())).map {it.getT1()}
    }

    @NonNull
    AdministeredItemRepository getRepository(AdministeredItem item) {
        administeredItemRepositories.find {it.handles(item.class)}
    }

    Mono<M> update(M model) {
        log.debug "ModelContentRepository::update $model.label $model.id"
        getRepository(model).update(model)
    }

    Mono<Long> delete(M model) {
        getRepository(model).delete(model)
    }
}
