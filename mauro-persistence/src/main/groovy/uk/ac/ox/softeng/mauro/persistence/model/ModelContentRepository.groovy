package uk.ac.ox.softeng.mauro.persistence.model

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.domain.model.ModelItem
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.exception.MauroInternalException

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Bean
import io.micronaut.core.annotation.NonNull
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import jakarta.validation.Valid
import reactor.core.publisher.Flux
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
    Mono<M> saveWithContent(@NonNull M model) {
        log.info '** start saveWithContent... **'
        Collection<AdministeredItem> contents = model.getAllContents()
        contents.each {
            if (it.owner != model) throw new MauroInternalException('Content must belong to the model being saved')
        }
        log.info '** saveWithContent before save **'
        getRepository(model).save(model).flatMap {AdministeredItem savedModel ->
//            Mono.just(contents).flatMapIterable {AdministeredItem item ->
//                getRepository(item).save(item)
//            }.then(Mono.just((M) savedModel))
            int i = 0
            Flux.fromIterable(contents).concatMap {AdministeredItem item ->
                i += 1
                if (i % 1000 == 0) log.info "** Saving item $i [$item.label]"
                getRepository(item).save(item)
            }.then(Mono.just((M) savedModel))
        }
    }

    Mono<M> saveWithAssociations(@NonNull Terminology terminology) {
        List<List<? extends ModelItem<Terminology>>> associations = terminology.getAllAssociations()
        getRepository(terminology).save(terminology).flatMap {AdministeredItem savedModel ->
            Flux.fromIterable(associations).concatMap {association ->
                if (association) {
                    getRepository(association.first()).saveAll(association)
                }
            }.then(Mono.just(savedModel))
        }
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
