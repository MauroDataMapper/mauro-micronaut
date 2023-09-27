package uk.ac.ox.softeng.mauro.web

import uk.ac.ox.softeng.mauro.folder.Folder
import uk.ac.ox.softeng.mauro.folder.FolderRepository
import uk.ac.ox.softeng.mauro.model.Model
import uk.ac.ox.softeng.mauro.model.ModelRepository

import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Bean
import io.micronaut.core.annotation.NonNull
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import jakarta.inject.Inject
import jakarta.validation.Valid
import reactor.core.publisher.Mono

@Slf4j
abstract class ModelController<M extends Model> {

    final static List<String> DISALLOWED_PROPERTIES = ['class', 'id']

    ModelRepository<M> modelRepository

    FolderRepository folderRepository

    static Class modelClass

    ModelController(Class<M> modelClass, ModelRepository<M> modelRepository, FolderRepository folderRepository) {
        this.modelClass = modelClass
        this.modelRepository = modelRepository
        this.folderRepository = folderRepository
    }

    @Get('/#\\{\'terminologies\'\\}/{id}')
    Mono<M> show(UUID id) {
        log.info 'ModelController::show'
        modelRepository.findById(id)
    }

    @Post('/folders/{folderId}/terminologies')
    Mono<M> create(UUID folderId, @Body @Valid @NonNull M model) {
        folderRepository.readById(folderId).flatMap {Folder folder ->
            model.folder = folder
            model.createdBy = 'USER'
            modelRepository.save(model)
        }
    }


    //    Mono<M> create(UUID folderId, )

    static String getDomainTypePath() {
        modelClass.simpleName.toLowerCase()
    }
}
