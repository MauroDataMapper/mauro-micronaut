package org.maurodata.domain.model

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
        updatePathString()
    }

    void updatePathString() {
        final String modelIdentifier = getModelIdentifier()
        nodes.each {PathNode pathNode -> pathNode.modelIdentifier = null}
        setModelIdentifier(modelIdentifier)
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
            if (node.id != null && (domainType == null || node.domainType.equalsIgnoreCase(domainType))) {
                return node
            }
        }

        return null
    }

    @Transient
    Item findNodeItem(UUID ofUUID) {
        for (int i = 0; i < this.nodes.size(); i++) {
            Item node = this.nodes.get(i).node
            if (node == null) {continue}
            if (node.id == ofUUID) {return node}
        }
        return null
    }

    Path getParent() {
        List<PathNode> lessNodes = []
        lessNodes.addAll(nodes)
        lessNodes.removeLast()
        new Path(lessNodes)
    }

    Item getItem() {
        ((PathNode) (nodes.get(nodes.size() - 1))).node
    }

    Path trimUntil(final String pathNodeString) {
        PathNode toMatch = PathNode.from(pathNodeString)
        List<PathNode> lessNodes = []
        int from
        for (from = 0; from < nodes.size(); from++) {
            final PathNode node = nodes.get(from)

            if (node.prefix == toMatch.prefix && node.identifier == toMatch.identifier) {break}
        }
        for (int copy = from; copy < nodes.size(); copy++) {
            lessNodes.add(nodes.get(copy))
        }
        new Path(lessNodes)
    }

    /*
    Returns a path that can be used to compare with paths from other
    models by producing a path that is not different even when:
      the model identifier may be different,
      the folder path may be different
      the items being compared may have different labels
     */

    static String branchAgnostic(final String pathString, final String pathRoot) {
        Path copy = new Path(pathString)
        for (int p = 0; p < copy.nodes.size(); p++) {
            copy.nodes[p].modelIdentifier = '--------'
        }
        copy.updatePathString()
        Path trimmed = copy.trimUntil(pathRoot)

        if (trimmed.nodes.size() > 0) {
            trimmed.nodes[0].identifier = '--------'
        }

        trimmed.updatePathString()
        trimmed.pathString
    }

    @Transient
    PathNode findLastPathNodeByPrefix(final String prefix) {
        for (int p = nodes.size() - 1; p >= 0; p--) {
            final PathNode pathNode = nodes.get(p)
            if (pathNode.prefix == prefix) {return pathNode}
        }
        return null
    }

    @Transient
    PathNode lastPathNode() {
        if (nodes.isEmpty()) {return null}
        return nodes.get(nodes.size() - 1)
    }

    @Transient
    String getModelIdentifier() {
        for (int p = 0, n = nodes.size(); p < n; p++) {
            final PathNode pathNode = nodes.get(p)
            if (!PathNode.canHaveModelIdentifier.contains(pathNode.prefix)) {continue}
            if (pathNode.modelIdentifier) {return pathNode.modelIdentifier}
        }
        return null
    }

    void setModelIdentifier(final String modelIdentifier) {
        nodes.each {PathNode pathNode -> pathNode.modelIdentifier = null}
        for (int p = 0, n = nodes.size(); p < n; p++) {
            final PathNode pathNode = nodes.get(p)
            if (!PathNode.canHaveModelIdentifier.contains(pathNode.prefix)) {continue}
            pathNode.modelIdentifier = modelIdentifier
            break
        }
        pathString = nodes.collect {it.toString()}.join('|')
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

        static List<String> canHaveModelIdentifier = ['dm','vf','csc','cs','te']

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
