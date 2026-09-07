package org.maurodata.service.command

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.OutputStreamAppender
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.ApplicationContext
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.inject.BeanDefinition
import io.micronaut.scheduling.TaskExecutors
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.ParseResult
import picocli.CommandLine.Model.CommandSpec
import picocli.CommandLine.Model.OptionSpec

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ExecutorService
import java.util.stream.Stream

@CompileStatic
@Slf4j
@Singleton
class CommandService {

    Map<String, Object> availableCommands

    private static final byte frame_type_stdout = 0x01
    private static final byte frame_type_stderr = 0x02
    // private static final byte frame_type_logging = 0x03
    private static final byte frame_type_file = 0x04
    private static final byte frame_type_error = 0x05
    private static final byte frame_type_exit = 0x06


    @Inject
    @Named(TaskExecutors.IO)
    ExecutorService executor

    Path workspaceRoot

    CommandService(ApplicationContext applicationContext) {

        Collection<BeanDefinition<Object>> definitions = applicationContext.getBeanDefinitions(Object).findAll {
            BeanDefinition<Object> commandBeanDefinition ->
                commandBeanDefinition.hasAnnotation(Command) &&
                commandBeanDefinition.getValue(Command, "name", String)
                    .orElse(null)
        }

        this.availableCommands = [:]

        definitions.forEach {BeanDefinition<Object> commandBeanDefinition ->
            try {
                Object commandBean = applicationContext.getBean(commandBeanDefinition.beanType)
                this.availableCommands.put(commandBeanDefinition.getValue(Command, "name", String).get(), commandBean)
            } catch (Throwable th) {
                log.warn(th.toString())
            }
        }

        final String tmpDir = System.getProperty("java.io.tmpdir")
        if (tmpDir) {
            workspaceRoot = Paths.get(tmpDir, "command-executions")
            Files.createDirectories(workspaceRoot)
        }
    }

    List<Map<String, String>> commands() {
        final List<Map<String, String>> commands = []

        availableCommands.keySet().forEach {
            String commandName ->
                Object commandBean = availableCommands.get(commandName)

                CommandLine command = new CommandLine(commandBean)

                final Map<String, String> commandDescription = [:]

                commandDescription.put('name', command.getCommandName())
                commandDescription.put('help', command.getHelp().fullSynopsis())

                commands << commandDescription
        }

        return commands
    }

    Map<String, Object> planCommand(final String commandName, final String[] commandArgs) {
        if (commandName == null) {throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'Missing command name')}
        if (commandArgs == null) {throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'Missing command arguments')}

        final Object commandBean = availableCommands.get(commandName)
        if (commandBean == null) {throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Unknown command ${commandName}")}

        final CommandLine commandLine = new CommandLine(commandBean)
        ParseResult parseResult = commandLine.parseArgs(commandArgs)

        CommandSpec commandSpec = commandLine.getCommandSpec()

        List parameters = []

        List<Map<String, Object>> outputFileReferences = new ArrayList<>(2)

        String executionId = null
        Path execDir = null
        Path filesDir = null
        if (workspaceRoot) {
            executionId = UUID.randomUUID().toString()
            execDir = workspaceRoot.resolve(executionId)
            Files.createDirectory(execDir)
            filesDir = execDir.resolve("output_files")
            Files.createDirectories(filesDir)
        }

        commandSpec.options().each {opt ->

            String[] desc = opt.description()
            String marker = desc ? desc[0].trim() : null

            Class<?> type = opt.type()

            OptionSpec matched = parseResult.matchedOption(opt.longestName())

            if (matched) {

                List<String> rawValues = matched.originalStringValues()

                List<Integer> positions = []
                String[] argsCopy = commandArgs.clone()
                rawValues.each {String value ->
                    int idx = argsCopy.findIndexOf {it == value}
                    if (idx >= 0) {
                        positions << idx
                        argsCopy[idx] = null // avoid matching duplicate values twice
                    }
                }

                String markerFlags = marker?.startsWith('@') ? marker.toLowerCase() : null

                parameters << [
                    options  : opt.names(),
                    label    : opt.paramLabel(),
                    type     : type.getCanonicalName(),
                    values   : rawValues,
                    marker   : markerFlags,
                    positions: positions
                ]

                if (markerFlags != null && markerFlags.contains('@output') && execDir != null) {

                    for (int p = 0; p < rawValues.size(); p++) {
                        String fileValue = rawValues.get(p)
                        Path originalFilePath = Paths.get(fileValue)
                        int positionInArguments = positions.get(p)
                        String uniqueFileValue = UUID.randomUUID().toString() + '_' + originalFilePath.getFileName().toString()
                        Path reservedOutput = filesDir.resolve(uniqueFileValue)

                        outputFileReferences << ([
                            position    : positionInArguments,
                            path        : reservedOutput.toString(),
                            originalName: fileValue
                        ] as Map<String, Object>)
                    }
                }
            }
        }

        List<String> unmatched = parseResult.unmatched()

        Map<String, Object> manifest = [
            "executionId"        : executionId,
            "commandName"        : commandName,
            "commandArgs"        : commandArgs,
            "parameters"         : parameters,
            "unmatchedParameters": unmatched
        ] as Map<String, Object>

        if (execDir) {

            Map<String, Object> manifestLocal
            if (!outputFileReferences.isEmpty()) {
                manifestLocal = [:] as Map<String, Object>
                manifestLocal.putAll(manifest)
                manifestLocal.put('output_files', outputFileReferences)
            } else {
                manifestLocal = manifest
            }
            Path manifestPath = execDir.resolve("manifest.json")
            String json = JsonOutput.prettyPrint(JsonOutput.toJson(manifestLocal))
            Files.writeString(manifestPath, json, StandardCharsets.UTF_8)
            log.info("Wrote manifest to ${manifestPath}")
        }

        return manifest
    }

    void fileCommand(final String executionId, final int positionInArguments, final String filename, final InputStream theFileContents) {

        if (!workspaceRoot) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'There is no temp directory for the Command Service to save files to')
        }

        Path execDir = workspaceRoot.resolve(executionId)
        Path filesDir = execDir.resolve("input_files")
        Files.createDirectories(filesDir)

        Path targetFile = filesDir.resolve(filename)

        theFileContents.withCloseable {input ->
            Files.newOutputStream(targetFile).withCloseable {out ->
                input.transferTo(out)
            }
        }

        Path manifestPath = execDir.resolve("manifest.json")
        Map<String, Object> manifest = new JsonSlurper().parse(manifestPath.toFile()) as Map<String, Object>
        List<Map<String, Object>> fileReferences = manifest.get("input_files") as List<Map<String, Object>>

        if (!fileReferences) {
            fileReferences = new ArrayList<>(2)
            manifest.put('input_files', fileReferences)
        }

        fileReferences << ([
            position: positionInArguments,
            path    : targetFile.toString()
        ] as Map<String, Object>)

        Files.writeString(manifestPath, JsonOutput.prettyPrint(JsonOutput.toJson(manifest)), StandardCharsets.UTF_8)

        log.debug("Saved file '${filename}' for position ${positionInArguments} to ${targetFile}")
    }

    InputStream runCommand(final String executionId) {

        if (!workspaceRoot) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'There is no temp directory for the Command Service to save files to')
        }

        Path execDir = workspaceRoot.resolve(executionId)

        if (!Files.exists(execDir) || !Files.isDirectory(execDir) || !Files.isReadable(execDir)) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'Command Service: there is no readable directory for ${executionId}')
        }

        Path manifestPath = execDir.resolve("manifest.json")
        Map<String, Object> manifest = new JsonSlurper().parse(manifestPath.toFile()) as Map<String, Object>

        log.info("Loaded manifest from ${manifestPath}")
        log.info(JsonOutput.prettyPrint(JsonOutput.toJson(manifest)))

        final String commandName = manifest.get('commandName') as String
        final List<String> commandArgs = manifest.get('commandArgs') as List<String>

        if (commandName == null) {throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'Missing command name')}
        if (commandArgs == null) {throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'Missing command arguments')}

        final Object commandBean = availableCommands.get(commandName)
        if (commandBean == null) {throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Unknown command ${commandName}")}

        // Create the actual command arguments
        final String[] localCommandArgs = commandArgs.toArray(new String[0])

        // Substitute any input files
        List<Map<String, Object>> inputFileReferences = manifest.get("input_files") as List<Map<String, Object>>
        if (inputFileReferences) {
            inputFileReferences.forEach {Map<String, Object> fileReference ->
                int position = fileReference.get('position') as Integer
                String path = fileReference.get('path') as String
                localCommandArgs[position] = path
            }
        }

        // Substitute any output files
        List<Map<String, Object>> outputFileReferences = manifest.get("output_files") as List<Map<String, Object>>
        if (outputFileReferences) {
            outputFileReferences.forEach {Map<String, Object> fileReference ->
                int position = fileReference.get('position') as Integer
                String path = fileReference.get('path') as String
                localCommandArgs[position] = path
            }
        }

        log.info("Running ${commandName} ${localCommandArgs}")

        PipedOutputStream pos = new PipedOutputStream()
        PipedInputStream pis = new PipedInputStream(pos, 65536)

        executor.submit {
            executeCommandStreaming(commandName, localCommandArgs, pos, outputFileReferences)
        }

        return pis
    }

    void executeCommandStreaming(String commandName,
                                 String[] commandArgs,
                                 OutputStream stream,
                                 List<Map<String, Object>> outputFileReferences) {

        Logger rootLogger = null
        OutputStreamAppender<ILoggingEvent> appender = null
        final PrintStream systemOut = System.out
        final PrintStream systemErr = System.err
        final ThreadLocal<PrintStream> localOut = new ThreadLocal<>()
        final ThreadLocal<PrintStream> localErr = new ThreadLocal<>()

        try {

            Object commandBean = availableCommands.get(commandName)

            FrameOutputStream stdout = new FrameOutputStream(stream, frame_type_stdout)
            FrameOutputStream stderr = new FrameOutputStream(stream, frame_type_stderr)

            PrintStream stdoutPrintStream = new PrintStream(stdout, true)
            PrintStream stderrPrintStream = new PrintStream(stderr, true)

            // Set up ThreadLocal PrintStreams to capture the output

            ThreadLocalPrintStream tlpsOut = new ThreadLocalPrintStream(localOut, systemOut)
            ThreadLocalPrintStream tlpsErr = new ThreadLocalPrintStream(localErr, systemErr)

            localOut.set(stdoutPrintStream)
            localErr.set(stderrPrintStream)

            CommandLine commandLine = new CommandLine(commandBean)

            commandLine.setOut(new PrintWriter(stdoutPrintStream, true))
            commandLine.setErr(new PrintWriter(stderrPrintStream, true))
            commandLine.setExecutionExceptionHandler((ex, cmd, parseResult) -> {
                ex.printStackTrace(cmd.getErr())
                return cmd.getCommandSpec().exitCodeOnExecutionException()
            })

            ParseResult result = commandLine.parseArgs(commandArgs)
            if (!result.isUsageHelpRequested() && !result.isVersionHelpRequested()) {
                System.setOut(tlpsOut)
                System.setErr(tlpsErr)
            }

            if (result.isUsageHelpRequested()) {
                commandLine.usage(commandLine.getOut())
                sendExitFrame(stream, 0)
                return
            }

            if (result.isVersionHelpRequested()) {
                commandLine.printVersionHelp(commandLine.getOut())
                sendExitFrame(stream, 0)
                return
            }

            int exitCode = commandLine.getCommandSpec()
                .commandLine()
                .getExecutionStrategy()
                .execute(result)

            if (outputFileReferences) {
                outputFileReferences.forEach {Map<String, Object> fileReference ->
                    String path = fileReference.get('path') as String
                    String originalName = fileReference.get('originalName') as String
                    File file = new File(path)
                    if (file.exists()) {
                        sendFileFrame(stream, file, originalName)
                    }
                }
            }

            sendExitFrame(stream, exitCode)

        } catch (Throwable t) {

            FrameOutputStream errors = new FrameOutputStream(stream, frame_type_error)

            errors.write(t.message.getBytes('UTF-8'))
            errors.flush()

        } finally {
            System.setOut(systemOut)
            System.setErr(systemErr)

            localOut.remove()
            localErr.remove()

            stream.close()
            if (appender) {
                if (rootLogger) {
                    rootLogger.detachAppender(appender)
                }
                appender.stop()
            }
        }
    }

    static void sendExitFrame(OutputStream out, int code) {

        ByteBuffer payload = ByteBuffer.allocate(4)
        payload.putInt(code)

        ByteBuffer header = ByteBuffer.allocate(9)
        header.put(frame_type_exit)
        header.putLong(4)

        out.write(header.array())
        out.write(payload.array())
    }

    static void sendFileFrame(final OutputStream out, final File file, final String filename) {

        // [type 1][length 8] + [filename length 4][filename ...][file size 8][file contents ...]
        byte[] filenameBytes = filename.getBytes(StandardCharsets.UTF_8)
        int filenameSize = filenameBytes.length

        long fileSize = file.length()

        int bufferHeaderSize = 4 + filenameSize + 8
        long bufferSize = bufferHeaderSize + fileSize

        ByteBuffer header = ByteBuffer.allocate(9)
        header.put(frame_type_file)
        header.putLong(bufferSize)

        ByteBuffer bufferHeader = ByteBuffer.allocate(bufferHeaderSize)
        bufferHeader.putInt(filenameSize)
        bufferHeader.put(filenameBytes)
        bufferHeader.putLong(fileSize)

        out.write(header.array())
        out.write(bufferHeader.array())
        try (InputStream fis = new FileInputStream(file)) {
            fis.transferTo(out)
        }
        out.flush()
    }

    /* */

    private class FrameOutputStream extends OutputStream {

        OutputStream out
        byte type

        FrameOutputStream(OutputStream out, byte type) {
            this.out = out
            this.type = type
        }

        @Override
        void write(byte[] b, int off, int len) {

            ByteBuffer header = ByteBuffer.allocate(9)
            header.put(type)
            header.putLong(len)

            out.write(header.array())
            out.write(b, off, len)
            out.flush()
        }

        @Override
        void write(int b) {
            write([(byte) b] as byte[], 0, 1)
        }
    }

    void closeCommand(final String executionId) {

        if (!workspaceRoot) {
            return
        }

        Path execDir = workspaceRoot.resolve(executionId)

        if (!Files.exists(execDir) || !Files.isDirectory(execDir) || !Files.isReadable(execDir)) {
            return
        }

        try (Stream<Path> walk = Files.walk(execDir)) {
            walk.sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path)
                    } catch (IOException e) {
                        throw new UncheckedIOException(e)
                    }
                })
        }
    }
}
