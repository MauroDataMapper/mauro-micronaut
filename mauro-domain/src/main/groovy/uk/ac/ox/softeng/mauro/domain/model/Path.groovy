package uk.ac.ox.softeng.mauro.domain.model

import com.fasterxml.jackson.annotation.JsonValue
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Prototype
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.convert.ConversionContext
import io.micronaut.data.model.runtime.convert.AttributeConverter

import java.util.regex.Matcher
import java.util.regex.Pattern

@Introspected
@CompileStatic
class Path {

    final static int PATH_MAX_NODES = 256

    @JsonValue
    String pathString

    List<PathNode> nodes

    void setPathString(String pathString) {
        this.pathString = pathString
        nodes = pathString?.split(/\|/)?.collect {PathNode.from(it)}
    }

    void setNodes(List<PathNode> nodes) {
        this.nodes = nodes
        pathString = nodes.collect {it.toString()}.join('|')
    }

    String toString() {
        pathString
    }

    Path join(PathNode node) {
        Path joined = new Path(nodes: nodes, pathString: pathString)
        joined.@nodes += node
        joined.@pathString += '|' + node.toString()
        joined
    }

    static class PathNode {
        String prefix
        String identifier
        String modelIdentifier
        String attribute

        @Override
        String toString() {
            String value = prefix + ':' + identifier
            if (modelIdentifier) value += '$' + modelIdentifier
            if (attribute) value += '@' + attribute
            value
        }

        static PathNode from(String str) {
            Pattern nodePattern = ~/^(?<prefix>\w\w):(?<identifier>.*)(\$(?<modelIdentifier>.*))?(@(?<attribute>.*))?$/
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
                value ? PathNode.from(value) : null
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
