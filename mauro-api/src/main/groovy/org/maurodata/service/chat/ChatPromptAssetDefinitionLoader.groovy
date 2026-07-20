package org.maurodata.service.chat

import groovy.transform.CompileStatic
import jakarta.inject.Singleton
import org.yaml.snakeyaml.Yaml

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.JarEntry
import java.util.jar.JarFile

@CompileStatic
@Singleton
class ChatPromptAssetDefinitionLoader {

    private static final List<String> PROMPT_ASSET_RESOURCE_DIRS = [
        'META-INF/mauro/chat/assets',
        'chat/assets'
    ] as List<String>

    private final List<ChatPromptAssetDefinition> definitions

    ChatPromptAssetDefinitionLoader() {
        this.definitions = loadDefinitions().asImmutable() as List<ChatPromptAssetDefinition>
    }

    List<ChatPromptAssetDefinition> listDefinitions() {
        definitions
    }

    private static List<ChatPromptAssetDefinition> loadDefinitions() {
        Yaml yaml = new Yaml()
        List<ChatPromptAssetDefinition> loaded = new ArrayList<ChatPromptAssetDefinition>()
        ClassLoader classLoader = ChatPromptAssetDefinitionLoader.classLoader
        List<String> resourcePaths = discoverPromptAssetResourcePaths(classLoader)

        for (String path : resourcePaths) {
            InputStream inputStream = classLoader.getResourceAsStream(path)
            if (inputStream == null) {
                throw new IllegalStateException("Missing chat prompt asset resource: ${path}")
            }
            try {
                Object parsed = yaml.load(inputStream)
                if (!(parsed instanceof Map)) {
                    throw new IllegalStateException("Chat prompt asset definition must be a map: ${path}")
                }
                @SuppressWarnings('unchecked')
                Map<String, Object> data = (Map<String, Object>) parsed
                loaded.add(fromMap(path, data))
            } finally {
                inputStream.close()
            }
        }
        loaded
    }

    private static List<String> discoverPromptAssetResourcePaths(ClassLoader classLoader) {
        Set<String> paths = new TreeSet<String>()
        for (String resourceDir : PROMPT_ASSET_RESOURCE_DIRS) {
            Enumeration<URL> resources = classLoader.getResources(resourceDir)
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement()
                if ('file'.equals(url.protocol)) {
                    paths.addAll(discoverFileResourcePaths(resourceDir, url))
                } else if ('jar'.equals(url.protocol)) {
                    paths.addAll(discoverJarResourcePaths(resourceDir, url))
                }
            }
        }
        if (paths.isEmpty()) {
            throw new IllegalStateException("No chat prompt asset definition resources found under ${PROMPT_ASSET_RESOURCE_DIRS.join(', ')}")
        }
        new ArrayList<String>(paths)
    }

    private static List<String> discoverFileResourcePaths(String resourceDir, URL url) {
        Path dir = Paths.get(url.toURI())
        if (!Files.exists(dir)) {
            return []
        }
        List<String> paths = new ArrayList<String>()
        Files.walk(dir).withCloseable {java.util.stream.Stream<Path> stream ->
            stream
                .filter {Path path -> Files.isRegularFile(path)}
                .filter {Path path -> isPromptAssetFile(path.fileName.toString())}
                .forEach {Path path ->
                    String relative = dir.relativize(path).toString().replace(File.separatorChar, '/' as char)
                    paths.add(resourceDir + '/' + relative)
                }
        }
        paths
    }

    private static List<String> discoverJarResourcePaths(String resourceDir, URL url) {
        JarURLConnection connection = (JarURLConnection) url.openConnection()
        JarFile jarFile = connection.jarFile
        List<String> paths = new ArrayList<String>()
        Enumeration<JarEntry> entries = jarFile.entries()
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement()
            String name = entry.name
            if (!entry.directory && name.startsWith(resourceDir + '/') && isPromptAssetFile(name)) {
                paths.add(name)
            }
        }
        paths
    }

    private static boolean isPromptAssetFile(String path) {
        String lower = path.toLowerCase(Locale.ROOT)
        lower.endsWith('.yml') || lower.endsWith('.yaml')
    }

    private static ChatPromptAssetDefinition fromMap(String path, Map<String, Object> data) {
        new ChatPromptAssetDefinition(
            id: requiredString(path, data, 'id'),
            name: requiredString(path, data, 'name'),
            description: requiredString(path, data, 'description'),
            scope: stringValue(data.get('scope')) ?: 'GLOBAL',
            version: stringValue(data.get('version')) ?: '1.0.0',
            type: stringValue(data.get('type')) ?: 'SKILL',
            priority: intValue(data.get('priority')),
            keywords: stringList(data.get('keywords')),
            seeAlso: stringList(data.get('seeAlso')),
            fragments: stringList(data.get('fragments')),
            toolApplicability: toolApplicabilityList(data.get('toolApplicability')),
            routing: routingValue(data.get('routing')),
            instruction: requiredString(path, data, 'instruction'),
            sourcePath: path,
            metadata: new LinkedHashMap<String, Object>(data)
        )
    }

    private static List<SkillToolApplicability> toolApplicabilityList(Object value) {
        if (!(value instanceof Collection)) {
            return []
        }
        List<SkillToolApplicability> items = new ArrayList<SkillToolApplicability>()
        for (Object item : (Collection<?>) value) {
            if (!(item instanceof Map)) {
                continue
            }
            @SuppressWarnings('unchecked')
            Map<String, Object> data = (Map<String, Object>) item
            String tool = stringValue(data.get('tool'))
            if (tool == null || tool.trim().isEmpty()) {
                continue
            }
            items.add(new SkillToolApplicability(
                tool: tool,
                relationship: stringValue(data.get('relationship')) ?: 'RECOMMENDED_PREREQUISITE',
                triggerTerms: stringList(data.get('triggerTerms')),
                useWhen: stringList(data.get('useWhen')),
                avoidWhen: stringList(data.get('avoidWhen')),
                examples: stringList(data.get('examples')),
                instructions: stringList(data.get('instructions'))
            ))
        }
        items
    }

    private static SkillRouting routingValue(Object value) {
        SkillRouting routing = new SkillRouting()
        if (!(value instanceof Map)) {
            return routing
        }
        @SuppressWarnings('unchecked')
        Map<String, Object> data = (Map<String, Object>) value
        routing.specificity = stringValue(data.get('specificity')) ?: 'NORMAL'
        routing.useWhen = stringList(data.get('useWhen'))
        routing.avoidWhen = stringList(data.get('avoidWhen'))
        routing.examples = stringList(data.get('examples'))

        Object toolObj = data.get('tool')
        if (toolObj instanceof Map) {
            @SuppressWarnings('unchecked')
            Map<String, Object> tool = (Map<String, Object>) toolObj
            routing.toolName = stringValue(tool.get('name'))
            Object argumentsObj = tool.get('arguments')
            if (argumentsObj instanceof Map) {
                @SuppressWarnings('unchecked')
                Map<String, Object> arguments = (Map<String, Object>) argumentsObj
                routing.toolArguments = arguments
            }
        } else {
            routing.toolName = stringValue(data.get('toolName'))
            Object argumentsObj = data.get('toolArguments')
            if (argumentsObj instanceof Map) {
                @SuppressWarnings('unchecked')
                Map<String, Object> arguments = (Map<String, Object>) argumentsObj
                routing.toolArguments = arguments
            }
        }
        routing
    }

    private static String requiredString(String path, Map<String, Object> data, String key) {
        String value = stringValue(data.get(key))
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Missing required chat prompt asset field '${key}' in ${path}")
        }
        value
    }

    private static String stringValue(Object value) {
        value == null ? null : String.valueOf(value)
    }

    private static Integer intValue(Object value) {
        if (value == null) {
            return null
        }
        if (value instanceof Number) {
            return Integer.valueOf(((Number) value).intValue())
        }
        Integer.valueOf(String.valueOf(value))
    }

    private static List<String> stringList(Object value) {
        if (!(value instanceof Collection)) {
            return []
        }
        List<String> strings = new ArrayList<String>()
        for (Object item : (Collection<?>) value) {
            if (item != null && !String.valueOf(item).trim().isEmpty()) {
                strings.add(String.valueOf(item))
            }
        }
        strings
    }
}
