package org.maurodata.utils.importer

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpRequest
import io.micronaut.http.multipart.CompletedFileUpload
import io.micronaut.http.multipart.CompletedPart
import io.micronaut.http.multipart.StreamingFileUpload
import io.micronaut.http.server.netty.MicronautHttpData
import io.micronaut.http.server.netty.NettyHttpRequest
import io.micronaut.http.server.multipart.MultipartBody
import io.netty.handler.codec.http.multipart.HttpData
import io.netty.handler.codec.http.multipart.FileUpload
import jakarta.inject.Inject
import jakarta.inject.Singleton
import io.netty.handler.codec.http.multipart.InterfaceHttpData
import org.apache.commons.lang3.reflect.FieldUtils
import org.maurodata.plugin.importer.FileParameter
import org.maurodata.plugin.importer.ImportParameters
import reactor.core.publisher.Flux

import java.nio.charset.StandardCharsets

@Slf4j
@CompileStatic
@Singleton
class ImporterUtils {
    @Inject
    ObjectMapper objectMapper


    <P extends ImportParameters> P readFromMultipartFormBody(MultipartBody body, Class<P> parametersClass) {
        readListFromMultipartFormBody(body, parametersClass).first()
    }

    <P extends ImportParameters> List<P> readListFromMultipartFormBody(MultipartBody body, Class<P> parametersClass) {
        Map<String, Object> scalarParts = [:]
        Map<String, List<FileParameter>> fileParts = [:].withDefault {[]}

        Flux.from(body).collectList().block().each {CompletedPart cp ->
            if (cp instanceof CompletedFileUpload) {
                fileParts[cp.name].add(new FileParameter(cp.filename, cp.contentType.toString(), cp.inputStream))
            } else {
                scalarParts[cp.name] = new String(cp.bytes, StandardCharsets.UTF_8)
            }
        }

        List<FileParameter> importFiles = fileParts.importFile
        if (!importFiles || !acceptsImportFile(parametersClass)) {
            return [objectMapper.convertValue(scalarParts, parametersClass)]
        }

        importFiles.collect {FileParameter importFile ->
            P parameters = objectMapper.convertValue(scalarParts, parametersClass)
            FieldUtils.writeField(parameters, 'importFile', importFile, true)
            parameters
        } as List<P>
    }

    <P extends ImportParameters> List<P> readFromStreamingMultipart(HttpRequest<?> request, Class<P> parametersClass) {
        readFromStreamingMultipart(request, null as StreamingFileUpload[], parametersClass)
    }

    <P extends ImportParameters> List<P> readFromStreamingMultipart(HttpRequest<?> request, @Nullable StreamingFileUpload[] importFiles, Class<P> parametersClass) {
        Map<String, Object> scalarParts = readUnclaimedScalarParts(request)
        List<FileParameter> fileParameters = readCompletedFileParts(request, 'importFile')
        if (!fileParameters && importFiles) {
            fileParameters = importFiles.collect {StreamingFileUpload importFile ->
                new FileParameter(importFile.filename,
                                  importFile.contentType.map {it.toString()}.orElse(null),
                                  importFile.asInputStream())
            }
        }

        if (!fileParameters) {
            return [objectMapper.convertValue(scalarParts, parametersClass)]
        }
        if (!acceptsImportFile(parametersClass)) {
            return [objectMapper.convertValue(scalarParts, parametersClass)]
        }

        fileParameters.collect {FileParameter importFile ->
            P parameters = objectMapper.convertValue(scalarParts, parametersClass)
            FieldUtils.writeField(parameters, 'importFile', importFile, true)
            parameters
        } as List<P>
    }

    protected static Boolean acceptsImportFile(Class<? extends ImportParameters> parametersClass) {
        FieldUtils.getField(parametersClass, 'importFile', true) != null
    }

    protected static List<FileParameter> readCompletedFileParts(HttpRequest<?> request, String partName) {
        if (!(request instanceof NettyHttpRequest) || !(request as NettyHttpRequest).hasFormRouteCompleter()) {
            return []
        }

        Set<MicronautHttpData<? extends HttpData>> allData = (Set<MicronautHttpData<? extends HttpData>>) FieldUtils.readField((request as NettyHttpRequest).formRouteCompleter(), 'allData', true)
        allData.findAll {MicronautHttpData<? extends HttpData> data ->
            data.name == partName &&
            data.httpDataType == InterfaceHttpData.HttpDataType.FileUpload &&
            data.completed
        }.collect {MicronautHttpData<? extends HttpData> data ->
            FileUpload fileUpload = data as FileUpload
            new FileParameter(fileUpload.filename, fileUpload.contentType, data.toStream())
        } as List<FileParameter>
    }

    protected static Map<String, Object> readUnclaimedScalarParts(HttpRequest<?> request) {
        if (!(request instanceof NettyHttpRequest) || !(request as NettyHttpRequest).hasFormRouteCompleter()) {
            return [:]
        }

        Set<MicronautHttpData<? extends HttpData>> allData = (Set<MicronautHttpData<? extends HttpData>>) FieldUtils.readField((request as NettyHttpRequest).formRouteCompleter(), 'allData', true)
        Map<String, Object> scalarParts = [:]
        Map<String, List<String>> repeatedParts = [:]

        allData.findAll {MicronautHttpData<? extends HttpData> data ->
            data.name != 'importFile' &&
            data.httpDataType != InterfaceHttpData.HttpDataType.FileUpload &&
            data.completed
        }.each {MicronautHttpData<?> data ->
            String value = data.getString(StandardCharsets.UTF_8)
            List<String> repeated = repeatedParts[data.name]
            if (repeated) {
                repeated.add(value)
            } else if (scalarParts.containsKey(data.name)) {
                repeated = [scalarParts[data.name] as String, value]
                repeatedParts[data.name] = repeated
                scalarParts[data.name] = repeated
            } else {
                scalarParts[data.name] = value
            }
        }
        scalarParts
    }
}
