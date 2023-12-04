package uk.ac.ox.softeng.mauro.controller.tree

import groovy.transform.CompileStatic
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import jakarta.inject.Inject
import reactor.core.publisher.Mono
import uk.ac.ox.softeng.mauro.domain.model.ModelService
import uk.ac.ox.softeng.mauro.persistence.model.ModelRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TerminologyRepository
import uk.ac.ox.softeng.mauro.domain.tree.TreeItem

@CompileStatic
@Controller('/tree/{domainType}/{id}')
class TreeItemController {

    @Inject
    List<ModelService> modelServices

    @Inject
    TerminologyRepository terminologyRepository

    @Inject
    List<ModelRepository> modelRepositories

    // TODO
//    @Get
//    Mono<List<TreeItem>> show(String domainType, UUID id) {
//        ModelRepository modelRepository = modelRepositories.find {it.handles(domainType)}
//        ModelService modelService = modelServices.find {it.handles(domainType)}
//
//        modelRepository.findById(id).map {
//            modelService.buildTree(it, null)
//        }
//    }
}
