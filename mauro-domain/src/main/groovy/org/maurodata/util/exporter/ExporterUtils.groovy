package org.maurodata.util.exporter

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Model
import org.maurodata.domain.model.ModelItem
import org.maurodata.plugin.exporter.ModelExporterPlugin
import org.maurodata.plugin.exporter.ModelItemExporterPlugin

@Slf4j
@CompileStatic
class ExporterUtils {

     static HttpResponse<byte[]> createExportResponse(ModelExporterPlugin mauroPlugin, List<Model> models) {
         if(!models || models.size() == 0) {
             throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Empty list of models to export!")
         }
         byte[] fileContents = mauroPlugin.exportModels(models)
         String filename = 'dataModels.json'
         if(models.size() == 1) {
             filename = mauroPlugin.getFileName(models[0])
         }
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
