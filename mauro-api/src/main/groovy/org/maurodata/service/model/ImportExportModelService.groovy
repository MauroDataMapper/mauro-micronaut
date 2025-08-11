package org.maurodata.service.model

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.multipart.CompletedFileUpload
import io.micronaut.http.multipart.CompletedPart
import io.micronaut.http.server.multipart.MultipartBody
import jakarta.inject.Inject
import org.maurodata.ErrorHandler
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Model
import org.maurodata.domain.model.ModelItem
import org.maurodata.domain.security.Role
import org.maurodata.persistence.cache.ModelCacheableRepository
import org.maurodata.plugin.MauroPluginService
import org.maurodata.plugin.exporter.ModelExporterPlugin
import org.maurodata.plugin.exporter.ModelItemExporterPlugin
import org.maurodata.plugin.importer.FileParameter
import org.maurodata.plugin.importer.ImportParameters
import org.maurodata.plugin.importer.ModelImporterPlugin
import org.maurodata.plugin.importer.ModelItemImporterPlugin
import org.maurodata.security.AccessControlService
import org.maurodata.service.core.AdministeredItemService
import org.maurodata.service.plugin.PluginService
import reactor.core.publisher.Flux
import reactor.util.annotation.NonNull

import java.nio.charset.StandardCharsets

@Slf4j
@CompileStatic
class ImportExportModelService extends AdministeredItemService {

    AccessControlService accessControlService
    ModelCacheableRepository.FolderCacheableRepository folderRepository
    MauroPluginService mauroPluginService
    ObjectMapper objectMapper

    @Inject
    ImportExportModelService(AccessControlService accessControlService, ModelCacheableRepository.FolderCacheableRepository folderRepository, MauroPluginService mauroPluginService, ObjectMapper objectMapper) {
        this.accessControlService = accessControlService
        this.folderRepository = folderRepository
        this.mauroPluginService = mauroPluginService
        this.objectMapper = objectMapper
    }


    ModelExporterPlugin getExporterPlugin(String namespace, String name, String version) {
        ModelExporterPlugin mauroPlugin = mauroPluginService.getPlugin(ModelExporterPlugin, namespace, name, version)
        PluginService.handlePluginNotFound(mauroPlugin, namespace, name)
        mauroPlugin
    }

    ModelItemExporterPlugin getModelItemExporterPlugin(String namespace, String name, String version) {
        ModelItemExporterPlugin mauroPlugin = mauroPluginService.getPlugin(ModelItemExporterPlugin, namespace, name, version)
        PluginService.handlePluginNotFound(mauroPlugin, namespace, name)
        mauroPlugin
    }
    List<Model> importModel(@Body MultipartBody body, String namespace, String name, @Nullable String version) {
        ModelImporterPlugin mauroPlugin = mauroPluginService.getPlugin(ModelImporterPlugin, namespace, name, version)
        PluginService.handlePluginNotFound(mauroPlugin, namespace, name)

        ImportParameters importParameters = readFromMultipartFormBody(body, mauroPlugin.importParametersClass())

        if (importParameters.folderId == null) {
            ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, importParameters.folderId, "Please choose the folder into which the Model/s should be imported.")
        }
        List<Model> imported = (List<Model>) mauroPlugin.importModels(importParameters)

        Folder folder = folderRepository.readById(importParameters.folderId)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, folder, "Folder with id $importParameters.folderId not found")
        accessControlService.checkRole(Role.EDITOR, folder)
        imported.each {imp ->
            imp.folder = folder
            updateCreationProperties(imp)
        }
        imported
    }

    List<ModelItem> importModelItems(@NonNull UUID dataModelId, Class aClazz, @Body MultipartBody body, String namespace, String name, @Nullable String version) {
        ModelItemImporterPlugin mauroPlugin = mauroPluginService.getPlugin(aClazz, namespace, name, version) as ModelItemImporterPlugin
        PluginService.handlePluginNotFound(mauroPlugin, namespace, name)

        ImportParameters importParameters = readFromMultipartFormBody(body, mauroPlugin.importParametersClass())

        if (importParameters.folderId == null) {
            ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, importParameters.folderId, "Please choose the folder into which the Model/s should be imported.")
        }
        List<ModelItem> imported = (List<ModelItem>) mauroPlugin.importModelItems(importParameters)

        Folder folder = folderRepository.readById(importParameters.folderId)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, folder, "Folder with id $importParameters.folderId not found")
        accessControlService.checkRole(Role.EDITOR, folder)
        imported.each {imp ->
            imp.folder = folder
            updateCreationProperties(imp)
        }
        imported
    }
    <P extends ImportParameters> P readFromMultipartFormBody(MultipartBody body, Class<P> parametersClass) {
        Map<String, Object> importMap = Flux.from(body).collectList().block().collectEntries {CompletedPart cp ->
            if (cp instanceof CompletedFileUpload) {
                return [cp.name, new FileParameter(cp.filename, cp.contentType.toString(), cp.bytes)]
            } else {
                return [cp.name, new String(cp.bytes, StandardCharsets.UTF_8)]
            }
        }
        return objectMapper.convertValue(importMap, parametersClass)
    }

    static HttpResponse<byte[]> createExportResponse(ModelExporterPlugin mauroPlugin, Model model) {
        byte[] fileContents = mauroPlugin.exportModel(model)
        String filename = mauroPlugin.getFileName(model)
        HttpResponse
            .ok(fileContents)
            .header(HttpHeaders.CONTENT_LENGTH, Long.toString(fileContents.length))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=${filename}")
    }
    static HttpResponse<byte[]> createExportResponse(ModelItemExporterPlugin mauroPlugin, AdministeredItem administeredItem) {
        byte[] fileContents = mauroPlugin.exportModelItem(administeredItem as ModelItem)
        String filename = mauroPlugin.getFileName(administeredItem as ModelItem)
        HttpResponse
            .ok(fileContents)
            .header(HttpHeaders.CONTENT_LENGTH, Long.toString(fileContents.length))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=${filename}")
    }


}
