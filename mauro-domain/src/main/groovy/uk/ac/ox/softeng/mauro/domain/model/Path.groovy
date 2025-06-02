package uk.ac.ox.softeng.mauro.domain.model

import com.fasterxml.jackson.annotation.JsonValue
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Prototype
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.core.convert.ConversionContext
import io.micronaut.data.model.runtime.convert.AttributeConverter
import jakarta.persistence.Transient

import java.util.regex.Matcher
import java.util.regex.Pattern

@Introspected
@CompileStatic
class Path {

    final static int PATH_MAX_NODES = 256

    @JsonValue
    String pathString

    List<PathNode> nodes

    protected Path() {

    }

    Path(String str) {
        setPathString(str)
    }

    Path(List<PathNode> nodes) {
        setNodes(nodes)
    }

    void setPathString(String pathString) {
        setNodes(pathString?.split(/\|/)?.collect {PathNode.from(it)})
    }

    void setNodes(List<PathNode> nodes) {
        this.nodes = nodes
        pathString = nodes.collect {it.toString()}.join('|')
    }

    String toString() {
        return pathString
    }

    Path join(PathNode node) {
        Path joined = new Path(nodes: nodes, pathString: pathString)
        joined.@nodes += node
        joined.@pathString += '|' + node.toString()
        return joined
    }

    @Transient
    Item findAncestorNodeItem(UUID ofUUID, @Nullable String domainType) {
        // Find ofUUID
        int at = -1
        for (int i = 0; i < this.nodes.size(); i++) {
            Item node = this.nodes.get(i).node
            if (node == null) {continue}
            if (node.id == ofUUID) {at = i; break}
        }

        if (at <= 0) {return null}

        // Find the first node with domainType before at

        for (int i = at - 1; i >= 0; i--) {
            Item node = this.nodes.get(i).node
            if (node == null) {continue}
            if (node.id != null && (domainType == null || node.domainType == domainType)) {
                return node
            }
        }

        return null
    }

    static class PathNode {
        String prefix
        String identifier
        String modelIdentifier
        String attribute

        @Transient
        Item node

        @Override
        String toString() {
            String value = prefix + ':' + identifier
            if (modelIdentifier) value += '$' + modelIdentifier
            if (attribute) value += '@' + attribute
            value
        }

        static PathNode from(String str) {
            Pattern nodePattern = ~/^(?<prefix>\w\w\w?):(?<identifier>[^@$]*)(\$(?<modelIdentifier>[^@]*))?(@(?<attribute>.*))?$/
            Matcher matcher = str =~ nodePattern
            if (!matcher.matches()) {
                throw new IllegalArgumentException('String [' + str + '] is not a valid PathNode')
            }
            new PathNode(prefix: matcher.group('prefix'), identifier: matcher.group('identifier'), modelIdentifier: matcher.group('modelIdentifier'),
                         attribute: matcher.group('attribute'))
        }

        @Prototype
        class PathNodeConverter implements AttributeConverter<PathNode, String> {
            @Override
            String convertToPersistedValue(PathNode pathNode, ConversionContext context) {
                pathNode ? pathNode.toString() : null
            }

            @Override
            PathNode convertToEntityValue(String value, ConversionContext context) {
                value ? from(value) : null
            }

        }
    }

    @Prototype
    static class PathConverter implements AttributeConverter<Path, String> {
        @Override
        String convertToPersistedValue(Path path, ConversionContext context) {
            path ? path.toString() : null
        }

        @Override
        Path convertToEntityValue(String value, ConversionContext context) {
            Path path = new Path()
            path.pathString = value
            path
        }
    }
}
