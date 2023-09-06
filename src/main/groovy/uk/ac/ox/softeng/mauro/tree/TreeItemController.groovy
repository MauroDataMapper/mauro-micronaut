package uk.ac.ox.softeng.mauro.tree

import uk.ac.ox.softeng.mauro.model.Model
import uk.ac.ox.softeng.mauro.model.ModelRepository
import uk.ac.ox.softeng.mauro.model.ModelService
import uk.ac.ox.softeng.mauro.terminology.TerminologyRepository
import uk.ac.ox.softeng.mauro.terminology.TerminologyService

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import jakarta.inject.Inject
import reactor.core.publisher.Mono

@Controller('/tree/{domainType}/{id}')
class TreeItemController {

    @Inject
    List<ModelService> modelServices

    @Inject
    TerminologyRepository terminologyRepository

    @Inject
    List<ModelRepository> modelRepositories

    @Get
    Mono<List<TreeItem>> show(String domainType, UUID id) {
        ModelRepository modelRepository = modelRepositories.find {it.handles(domainType)}
        ModelService modelService = modelServices.find {it.handles(domainType)}

        modelRepository.findById(id).map {
            modelService.buildTree(it, null)
        }
    }
}
