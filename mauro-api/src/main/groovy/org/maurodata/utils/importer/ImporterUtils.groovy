package org.maurodata.utils.importer

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpRequest
import io.micronaut.http.multipart.StreamingFileUpload
import io.micronaut.http.server.netty.MicronautHttpData
import io.micronaut.http.server.netty.NettyHttpRequest
import io.netty.handler.codec.http.multipart.HttpData
import jakarta.inject.Inject
import jakarta.inject.Singleton
import io.netty.handler.codec.http.multipart.InterfaceHttpData
import org.apache.commons.lang3.reflect.FieldUtils
import org.maurodata.plugin.importer.FileParameter
import org.maurodata.plugin.importer.ImportParameters
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux

import java.lang.reflect.Field
import java.nio.file.Files
import java.nio.file.Path
import java.nio.charset.StandardCharsets
import java.nio.file.StandardOpenOption

@Slf4j
@CompileStatic
@Singleton
class ImporterUtils {
    protected static final Integer COPY_BUFFER_SIZE = 1024 * 1024
    protected static final Long COPY_PROGRESS_BYTES = 100L * 1024L * 1024L
    protected static final Long COPY_PROGRESS_MILLIS = 10_000L

    @Inject
    ObjectMapper objectMapper

    <P extends ImportParameters> List<P> readFromStreamingMultipart(HttpRequest<?> request, @Nullable Publisher<StreamingFileUpload> importFiles, Class<P> parametersClass) {
        long start = System.nanoTime()
        log.debug('Reading streaming multipart import request for parameter class [{}]', parametersClass.simpleName)
        List<FileParameter> fileParameters = importFiles ?
            Flux.from(importFiles).map {StreamingFileUpload importFile ->
                createTemporaryFileParameter(importFile.filename,
                                             importFile.contentType.map {it.toString()}.orElse(null),
                                             importFile.asInputStream())
            }.collectList().block() :
            []
        Map<String, Object> scalarParts = readUnclaimedScalarParts(request)

        log.debug('Read streaming multipart import request for parameter class [{}] in [{} ms]: [{}] scalar part(s), [{}] import file part(s)',
                  parametersClass.simpleName,
                  elapsedMillis(start),
                  scalarParts.size(),
                  fileParameters?.size() ?: 0)
        createParametersList(scalarParts, fileParameters, parametersClass)
    }

    protected <P extends ImportParameters> List<P> createParametersList(Map<String, Object> scalarParts, List<FileParameter> fileParameters, Class<P> parametersClass) {
        if (!fileParameters) {
            return [objectMapper.convertValue(scalarParts, parametersClass)]
        }

        try {
            if (!acceptsImportFile(parametersClass)) {
                cleanupTemporaryFileParameters(fileParameters)
                return [objectMapper.convertValue(scalarParts, parametersClass)]
            }

            fileParameters.collect {FileParameter importFile ->
                P parameters = objectMapper.convertValue(scalarParts, parametersClass)
                FieldUtils.writeField(parameters, 'importFile', importFile, true)
                parameters
            } as List<P>
        } catch (Throwable throwable) {
            cleanupTemporaryFileParameters(fileParameters)
            throw throwable
        }
    }

    protected static Boolean acceptsImportFile(Class<? extends ImportParameters> parametersClass) {
        FieldUtils.getField(parametersClass, 'importFile', true) != null
    }

    static void cleanupTemporaryFiles(Collection<? extends ImportParameters> parametersList) {
        parametersList.each {ImportParameters parameters ->
            Field importFileField = FieldUtils.getField(parameters.class, 'importFile', true)
            if (importFileField) {
                Object importFile = FieldUtils.readField(importFileField, parameters, true)
                if (importFile instanceof FileParameter) {
                    importFile.close()
                }
            }
        }
    }

    protected static void cleanupTemporaryFileParameters(Collection<FileParameter> fileParameters) {
        fileParameters.each {FileParameter fileParameter ->
            log.debug('Cleaning up temporary import file [{}] for original file [{}]', fileParameter.filePath, fileParameter.fileName)
            fileParameter.close()
        }
    }

    protected static FileParameter createTemporaryFileParameter(String filename, String contentType, InputStream inputStream) {
        Path temporaryFile = Files.createTempFile('mauro-import-', '.tmp')
        long start = System.nanoTime()
        try {
            log.debug('Buffering import file [{}] with content type [{}] to temporary file [{}]', filename, contentType, temporaryFile)
            long copied = copyToTemporaryFile(inputStream, temporaryFile, filename)
            log.debug('Buffered import file [{}] to temporary file [{}]: [{}] in [{} ms]',
                      filename,
                      temporaryFile,
                      formatBytes(copied),
                      elapsedMillis(start))
            new FileParameter(filename, contentType, temporaryFile, true, copied)
        } catch (Throwable throwable) {
            log.debug('Failed buffering import file [{}] to temporary file [{}] after [{} ms]; deleting temporary file',
                      filename,
                      temporaryFile,
                      elapsedMillis(start),
                      throwable)
            Files.deleteIfExists(temporaryFile)
            throw throwable
        }
    }

    protected static long copyToTemporaryFile(InputStream inputStream, Path temporaryFile, String filename) {
        long copied = 0
        long lastProgressBytes = 0
        long lastProgressTime = System.nanoTime()
        byte[] buffer = new byte[COPY_BUFFER_SIZE]

        inputStream.withCloseable {InputStream source ->
            Files.newOutputStream(temporaryFile, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING).withCloseable {OutputStream target ->
                int read = source.read(buffer)
                while (read != -1) {
                    target.write(buffer, 0, read)
                    copied += read
                    long now = System.nanoTime()
                    if (copied - lastProgressBytes >= COPY_PROGRESS_BYTES || elapsedMillis(lastProgressTime, now) >= COPY_PROGRESS_MILLIS) {
                        log.debug('Buffered [{}] of import file [{}] to temporary file [{}]',
                                  formatBytes(copied),
                                  filename,
                                  temporaryFile)
                        lastProgressBytes = copied
                        lastProgressTime = now
                    }
                    read = source.read(buffer)
                }
            }
        }
        copied
    }

    protected static Long elapsedMillis(long start) {
        elapsedMillis(start, System.nanoTime())
    }

    protected static Long elapsedMillis(long start, long end) {
        (end - start).intdiv(1000000L)
    }

    protected static String formatBytes(long bytes) {
        if (bytes < 1024L) {
            return "${bytes} B"
        }
        if (bytes < 1024L * 1024L) {
            return "${String.format('%.1f', bytes / 1024D)} KiB"
        }
        if (bytes < 1024L * 1024L * 1024L) {
            return "${String.format('%.1f', bytes / 1024D / 1024D)} MiB"
        }
        "${String.format('%.2f', bytes / 1024D / 1024D / 1024D)} GiB"
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
