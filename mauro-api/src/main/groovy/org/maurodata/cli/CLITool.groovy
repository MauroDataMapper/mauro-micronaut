package org.maurodata.cli

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.ApplicationContext
import io.micronaut.http.uri.UriBuilder
import io.micronaut.inject.BeanDefinition
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters
import picocli.CommandLine.Option

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

@Slf4j
@CompileStatic
@Command(
    name = "cli-tool",
    mixinStandardHelpOptions = true,
    description = "Execute commands from one tool"
)
class CLITool implements Runnable {

    @Option(names = ['-l', '--list'], description = 'List commands', required = false)
    boolean list

    @Option(names = ['--api-key'], description = 'Authorisation API Key', required = false)
    String apiKey

    @Option(names = ['-s', '--server-api-url'], description = 'The URL of the Mauro api. e.g. http://localhost:8080/api', required = false)
    String serverApiURL

    @Parameters(
        index = "0",
        arity = "0..1",
        description = "Command name"
    )
    String commandName

    @Parameters(
        index = "1..*",
        arity = "0..*",
        description = "Command arguments"
    )
    String[] commandArgs = []

    private static final byte frame_type_stdout = 0x01
    private static final byte frame_type_stderr = 0x02
    private static final byte frame_type_logging = 0x03
    private static final byte frame_type_file = 0x04
    private static final byte frame_type_error = 0x05
    private static final byte frame_type_exit = 0x06

    private static final String CRLF = '\r\n'

    static void main(final String[] args) throws Throwable {
        CommandLine commandLine = new CommandLine(new CLITool())
        commandLine.setStopAtPositional(true)
        commandLine.execute(args)
    }

    @Override
    void run() {

        ApplicationContext applicationContext = ApplicationContext.run()
        try {

            final URI baseEndpoint
            if (apiKey == null) {baseEndpoint = null} else {
                final String serverURL = serverApiURL != null ? serverApiURL : 'http://localhost:8080/api'
                baseEndpoint = URI.create(serverURL)
            }

            // --list
            if (list) {
                listLocal(applicationContext)
                if (apiKey != null) {
                    listRemote(apiKey, baseEndpoint)
                }
                System.exit(0)
            }

            // Is this a local command?
            if (apiKey == null) {
                BeanDefinition<Object> commandAdaptor = lookupCommandByName(applicationContext, commandName)

                // Local command line
                if (commandAdaptor != null) {
                    System.exit(runLocalCommand(applicationContext, commandAdaptor))
                }
                log.error("Unknown command: ${commandName}")
                System.exit(1)
            }

            // Remote command line
            int exitCode = runRemoteCommand(apiKey, baseEndpoint, commandName, commandArgs)
            System.exit(exitCode)
        }
        finally {
            applicationContext.close()
        }

        System.exit(1)
    }

    private static void listLocal(final ApplicationContext applicationContext) {
        Collection<BeanDefinition<Object>> availableCommands = applicationContext.getBeanDefinitions(Object).findAll {
            BeanDefinition<Object> command ->
                command.hasAnnotation(Command) &&
                command.getValue(Command, "name", String)
                    .orElse(null) != null
        }
        availableCommands.forEach {
            BeanDefinition<Object> command ->
                System.out.println(command.getValue(Command, "name", String).get())
        }
    }

    private static void listRemote(final String apiKey, final URI baseEndpoint) {
        URI endpoint = UriBuilder.of(baseEndpoint.toString())
            .path("admin/commands")
            .build()

        HttpRequest request = HttpRequest.newBuilder()
            .uri(endpoint)
            .header("Content-Type", "application/json")
            .header("apiKey", apiKey)
            .GET()
            .build()

        HttpClient client = HttpClient.newHttpClient()
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString())

        System.out.println("${response.body()}")
    }

    private static BeanDefinition<Object> lookupCommandByName(final ApplicationContext applicationContext, final String commandName) {
        return applicationContext.getBeanDefinitions(Object).find {
            BeanDefinition<Object> command ->
                command.hasAnnotation(Command) &&
                command.getValue(Command, "name", String)
                    .orElse(null) == commandName
        }
    }

    private static int runLocalCommand(final ApplicationContext applicationContext, final BeanDefinition<Object> commandAdaptor, final String[] commandArgs) {
        Object commandBean = applicationContext.getBean(commandAdaptor.beanType)
        return new CommandLine(commandBean).execute(commandArgs)
    }

    private static Map<String, Object> manifestRemoteCommand(final String apiKey, final URI baseEndpoint, final String commandName, final String[] commandArgs) {
        URI endpoint = UriBuilder.of(baseEndpoint.toString())
            .path("admin/command/prepare/${commandName}")
            .build()

        String json = JsonOutput.toJson(commandArgs)

        HttpRequest request = HttpRequest.newBuilder()
            .uri(endpoint)
            .header("Content-Type", "application/json")
            .header("apiKey", apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build()

        HttpClient client = HttpClient.newHttpClient()
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString())

        JsonSlurper slurper = new JsonSlurper()
        return slurper.parseText(response.body()) as Map<String, Object>
    }

    private static int uploadInputFilesFromManifest(final String apiKey, final URI baseEndpoint, final String executionId, Map<String, Object> manifest) {
        List<Map<String, Object>> inputFileParameters = []

        manifest.parameters.each {
            Map<String, Object> parameter ->
                if (handleAsFileInput(parameter)) {

                    final Collection<String> values = parameter.get("values") as Collection<String>
                    final String label = parameter.label as String
                    final List<File> files = []

                    values.forEach {String fileValue ->
                        final File f = new File(fileValue)
                        if (!f.exists()) {
                            log.error("Input file not found for $label : $fileValue at ${f.absolutePath}")
                            return 1
                        }
                        files << f
                    }
                    parameter.put('files', files)
                    inputFileParameters.add(parameter)
                }
        }

        URI fileEndpoint = UriBuilder.of(baseEndpoint.toString())
            .path("admin/command/file/${executionId}")
            .build()

        inputFileParameters.forEach {Map<String, Object> inputFileParameter ->
            List<File> files = (List<File>) inputFileParameter.get("files")
            List<Integer> positions = (List<Integer>) inputFileParameter.get("positions")

            for (int p = 0; p < files.size(); p++) {
                File file = files.get(p)
                Integer position = positions.get(p)
                int code = uploadInputFile(apiKey, fileEndpoint, file, position)
                if (code != 0) {
                    return code
                }
            }
        }

        return 0
    }

    private static int uploadInputFile(final String apiKey, final URI fileEndpoint, final File file, final int position) {

        String boundary = "ToolBoundary${System.currentTimeMillis()}"

        log.trace("Sending...")

        HttpRequest request = HttpRequest.newBuilder()
            .uri(fileEndpoint)
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .header("apiKey", apiKey)
            .POST(HttpRequest.BodyPublishers.ofInputStream(() ->
                                                               createMultipartStream(boundary, file, position)))
            .build()

        HttpClient client = HttpClient.newHttpClient()
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())

        int statusCode = response.statusCode()

        String responseMessage = new String(response.body().readAllBytes(), StandardCharsets.UTF_8)

        if (statusCode < 200 || statusCode >= 300) {
            log.error(responseMessage)
            return 1
        }

        return 0
    }

    private static boolean handleAsFileInput(final Map<String, Object> parameter) {
        String parameterType = parameter.type as String
        String parameterMarker = parameter.marker as String

        return parameterMarker && parameterMarker.indexOf('@output') == -1 && parameterType && parameterType in ['java.io.File', 'java.nio.file.Path']
    }

    private static int runRemoteCommand(final String apiKey, final URI baseEndpoint, final String commandName, final String[] commandArgs) {
        log.trace("Making call to manifest")
        Map<String, Object> manifest = manifestRemoteCommand(apiKey, baseEndpoint, commandName, commandArgs)
        if (manifest.containsKey('_embedded')) {
            Map<String, Object> _embedded = manifest.get('_embedded') as Map<String, Object>
            List errors = _embedded.get('errors') as List
            Map map = errors.get(0) as Map
            map.keySet().forEach {
                Object k ->
                    log.error(String.valueOf(map.get(k)))
            }
            return 1
        }

        final String executionId = manifest.get("executionId")
        if (executionId == null) {
            log.debug("${manifest}")
            log.error("Server is unable to execute commands")
            return 1
        }

        log.info(JsonOutput.prettyPrint(JsonOutput.toJson(manifest)))

        int uploadCode = uploadInputFilesFromManifest(apiKey, baseEndpoint, executionId, manifest)
        if (uploadCode != 0) {
            closeRemotePreparedCommand(apiKey, baseEndpoint, executionId)
            return uploadCode
        }

        // Run the actual command

        int exitCode = runRemotePreparedCommand(apiKey, baseEndpoint, executionId)

        return exitCode
    }

    private static int runRemotePreparedCommand(final String apiKey, final URI baseEndpoint, final String executionId) {

        try {
            URI endpoint = UriBuilder.of(baseEndpoint.toString())
                .path("admin/command/run/${executionId}")
                .build()

            HttpRequest request = HttpRequest.newBuilder()
                .uri(endpoint)
                .header("apiKey", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(''))
                .build()

            HttpClient client = HttpClient.newHttpClient()
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())

            int statusCode = response.statusCode()

            if (statusCode < 200 || statusCode >= 300) {
                String error = new String(response.body().readAllBytes(), StandardCharsets.UTF_8)
                log.error(error)
                return 1
            }

            InputStream input = response.body()

            DataInputStream din = new DataInputStream(input)

            OutputStream stdout = System.out
            OutputStream stderr = System.err

            int exitCode = 1

            reading:
            while (true) {
                byte frameType = din.readByte()
                long length = din.readLong()

                switch (frameType) {
                    case frame_type_stdout:
                        transferXBytes(din, length, stdout)
                        break
                    case frame_type_stderr:
                        transferXBytes(din, length, stderr)
                        break
                    case frame_type_logging:
                        log.info(readXBytes(din, length))
                        break
                    case frame_type_error:
                        log.error(readXBytes(din, length))
                        break
                    case frame_type_file:
                        transferFile(din)
                        break
                    case frame_type_exit:
                        exitCode = din.readInt()
                        break reading
                    default:
                        // If there's an unknown frame type
                        // be nice and just discard it
                        log.info("Skipping a frame type ${frameType & 0xFF} of ${length} bytes")
                        skipXBytes(din, length)
                        break
                }
            }

            return exitCode
        } finally {
            closeRemotePreparedCommand(apiKey, baseEndpoint, executionId)
        }
    }

    private static InputStream createMultipartStream(
        final String boundary,
        final File file,
        final int position) throws IOException {

        PipedOutputStream pos = new PipedOutputStream()
        PipedInputStream pis = new PipedInputStream(pos, 65536)

        new Thread(() -> {
            try (OutputStream out = pos
                 Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {

                writer.write('--' + boundary + CRLF)
                writer.write('Content-Disposition: form-data; name="position"' + CRLF)
                writer.write('Content-Type: text/plain' + CRLF)
                writer.write(CRLF)
                writer.write(String.valueOf(position))
                writer.write(CRLF)
                writer.flush()

                writer.write('--' + boundary + CRLF)
                writer.write('Content-Disposition: form-data; name="file"; filename="' + file.getName() + '"' + CRLF)
                writer.write('Content-Type: application/octet-stream' + CRLF)
                writer.write(CRLF)
                writer.flush()

                try (InputStream fis = new FileInputStream(file)) {
                    fis.transferTo(out)
                }
                out.flush()
                writer.write(CRLF)
                writer.flush()

                writer.write('--' + boundary + '--' + CRLF)
                writer.write(CRLF)
                writer.flush()

            } catch (IOException e) {
                throw new RuntimeException(e)
            }
        }).start()

        return pis
    }

    private static void transferXBytes(final DataInputStream fromDin,
                                       final long length,
                                       final OutputStream toOs) throws IOException {

        byte[] buffer = new byte[(int) Math.min(length, 1024)]
        long remaining = length

        while (remaining > 0) {
            int bytesToRead = (int) Math.min(buffer.length, remaining)
            int read = fromDin.read(buffer, 0, bytesToRead)

            if (read == -1) {
                throw new EOFException("Stream ended before expected number of bytes were read in")
            }

            toOs.write(buffer, 0, read)
            remaining -= read
        }
    }

    private static String readXBytes(final DataInputStream fromDin,
                                     final long length) throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream((int) Math.min(length, 1024))
        byte[] buffer = new byte[(int) Math.min(length, 1024)]
        long remaining = length

        while (remaining > 0) {
            int bytesToRead = (int) Math.min(buffer.length, remaining)
            int read = fromDin.read(buffer, 0, bytesToRead)

            if (read == -1) {
                throw new EOFException("Stream ended before expected number of bytes were read in")
            }

            baos.write(buffer, 0, read)
            remaining -= read
        }

        return new String(baos.toByteArray(), StandardCharsets.UTF_8)
    }

    private static void skipXBytes(final DataInputStream fromDin,
                                   final long length
                                  ) throws IOException {

        byte[] buffer = new byte[(int) Math.min(length, 1024)]
        long remaining = length

        while (remaining > 0) {
            int bytesToRead = (int) Math.min(buffer.length, remaining)
            int read = fromDin.read(buffer, 0, bytesToRead)

            if (read == -1) {
                throw new EOFException("Stream ended before expected number of bytes were read in")
            }

            remaining -= read
        }
    }

    private static void transferFile(final DataInputStream fromDin) throws IOException {

        // [filename length 4][filename ...][file size 8][file contents ...]
        int filenameSize = fromDin.readInt()
        byte[] filenameBytes = new byte[filenameSize]
        fromDin.readFully(filenameBytes)
        String filename = new String(filenameBytes, StandardCharsets.UTF_8)

        long fileSize = fromDin.readLong()

        byte[] buffer = new byte[(int) Math.min(fileSize, 1024)]
        long remaining = fileSize

        File file = new File(filename)

        try (FileOutputStream fos = new FileOutputStream(file)) {

            while (remaining > 0) {
                int bytesToRead = (int) Math.min(buffer.length, remaining)
                int read = fromDin.read(buffer, 0, bytesToRead)

                if (read == -1) {
                    throw new EOFException("Stream ended before expected number of bytes were read in")
                }

                fos.write(buffer, 0, read)
                remaining -= read
            }
        }

    }

    private static void closeRemotePreparedCommand(final String apiKey, final URI baseEndpoint, final String executionId) {
        URI endpoint = UriBuilder.of(baseEndpoint.toString())
            .path("admin/command/close/${executionId}")
            .build()

        HttpRequest request = HttpRequest.newBuilder()
            .uri(endpoint)
            .header("apiKey", apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(''))
            .build()

        HttpClient client = HttpClient.newHttpClient()
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString())

        int statusCode = response.statusCode()

        if (statusCode < 200 || statusCode >= 300) {
            String error = response.body()
            log.error(error)
        }
    }
}