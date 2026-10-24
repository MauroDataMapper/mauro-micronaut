package org.maurodata.test.domain.model

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import org.maurodata.domain.model.Path
import spock.lang.Specification
import spock.lang.Unroll

@MicronautTest
class PathMethodsTest extends Specification {

    @Unroll
    void 'test findLastPathNodeByPrefix, for #pathPrefix, #fullPath'() {
        when:
        Path.PathNode pathNode = new Path(fullPath).findLastPathNodeByPrefix(pathPrefix)

        then:
        pathNode
        pathNode.identifier == expectedSubPath

        where:
        pathPrefix | fullPath                                                                                       | expectedSubPath
        'de'       | "fo:soluta eum architecto|dm:modi unde est\$matrix|dc:est quasi vel|de:new data element label" | "new data element label"
        'dm'       | "fo:soluta eum architecto|dm:modi unde est\$matrix|dc:est quasi vel|de:new data element label" | "modi unde est"
        'fo'       | "fo:soluta eum architecto|dm:modi unde est\$matrix|dc:est quasi vel|de:new data element label" | "soluta eum architecto"
        'dm'       | "dm:BC_Bloods\$2.0.0"                                                                          | "BC_Bloods"
        'fo'       | "fo:soluta eum architecto"                                                                     | "soluta eum architecto"
        'fo'       | "fo:soluta eum architecto|te:Dewey Decimal Classification v22\$main"                           | "soluta eum architecto"
        'te'       | "fo:soluta eum architecto|te:Dewey Decimal Classification v22\$main"                           | "Dewey Decimal Classification v22"
        'dc'       | "fo:soluta eum architecto|dm:modi unde est\$1.0.0|dc:est quasi vel|dc:est sed hic"              | "est sed hic"
        'DE'       | "fo:soluta eum architecto|dm:modi unde est\$matrix|dc:est quasi vel|de:new data element label" | "new data element label"
        'dm'       | "FO:soluta eum architecto|DM:modi unde est\$matrix|DC:est quasi vel|DE:new data element label" | "modi unde est"
    }

    @Unroll
    void 'test modelIdentifier for #fullPath'() {
        when:
        String modelIdentifier = new Path(fullPath).modelIdentifier

        then:
        modelIdentifier == expectedVersion

        where:
        fullPath                                                                                            | expectedVersion
        'fo:soluta eum architecto|dm:modi unde est$matrix|dc:est quasi vel|de:new data element label$2.0.0' | "matrix"
        'dm:BC_Bloods$2.0.0'                                                                                | "2.0.0"
        'fo:soluta eum architecto'                                                                          | null
        'fo:soluta eum architecto|te:Dewey Decimal Classification v22$main'                                 | "main"
        'fo:soluta eum architecto|vf:versionio de folder$main|te:Dewey Decimal Classification v22$main'     | "main"
        'FO:soluta eum architecto|DM:modi unde est$matrix|DC:est quasi vel|DE:new data element label$2.0.0' | "matrix"
        'VF:versionio de folder$main|TE:Dewey Decimal Classification v22'                                   | "main"
    }

    @Unroll
    void 'test trimUntil keeps identifiers case sensitive and prefixes case insensitive for #pathRoot'() {
        when:
        Path trimmedPath = new Path(fullPath).trimUntil(pathRoot)

        then:
        trimmedPath.toString() == expectedPath

        where:
        fullPath                                                                                       | pathRoot              | expectedPath
        'FO:Folder|DM:Model$main|DC:Name|DE:Element'                                                   | 'dm:Model$main'       | 'DM:Model$main|DC:Name|DE:Element'
        'FO:Folder|DM:Model$main|DC:Name|DE:Element'                                                   | 'dm:model$main'       | ''
        'fo:Folder|dm:Model$main|dc:Name|de:Element'                                                   | 'DM:Model$main'       | 'dm:Model$main|dc:Name|de:Element'
        'fo:Folder|dm:Model$main|dc:Name|de:Element'                                                   | 'DM:model$main'       | ''
    }

    void 'test set modelIdentifier recognises mixed case model prefixes'() {
        given:
        Path path = new Path('FO:Folder|DM:Model|DC:Name')

        when:
        path.modelIdentifier = 'main'

        then:
        path.toString() == 'FO:Folder|DM:Model$main|DC:Name'
    }

    void 'path rooted at first model identifier discards earlier nodes and later identifiers'() {
        when:
        Path path = Path.fromPathRootedAtFirstModelIdentifier(
            'fo:Ignored folder|dm:Model$1.0.0|vf:Nested version$2.0.0|dc:Class')

        then:
        path.toString() == 'dm:Model$1.0.0|vf:Nested version|dc:Class'
        path.nodes.count { it.modelIdentifier != null } == 1
    }

    void 'relative path remains unchanged when rooted at a model identifier'() {
        expect:
        Path.fromPathRootedAtFirstModelIdentifier('dc:Class|de:Element').toString() == 'dc:Class|de:Element'
    }

    void 'path exposes its absolute model root and compares roots at node level'() {
        given:
        Path firstVersion = new Path('dm:Model$1.0.0|dc:Class')
        Path secondVersion = new Path('DM:Model$2.0.0|dc:Class')
        Path relative = new Path('dc:Class|de:Element')

        expect:
        firstVersion.isAbsolute()
        firstVersion.modelRootNode.prefix == 'dm'
        firstVersion.modelRootNode.isSameModelRoot(new Path('DM:Model$1.0.0|dc:Other').modelRootNode)
        !firstVersion.modelRootNode.isSameModelRoot(secondVersion.modelRootNode)
        firstVersion.modelRootNode.hasSamePrefixAndIdentifier(secondVersion.modelRootNode)
        !relative.isAbsolute()
        relative.modelRootNode == null
    }

    void 'path rooted at a node type that cannot carry a model identifier is rejected'() {
        when:
        Path.fromPathRootedAtFirstModelIdentifier('dc:Column$1.0.0|de:Element')

        then:
        IllegalArgumentException exception = thrown()
        exception.message == 'The absolute path has an invalid modelIdentifier root'
    }

    void 'test getting path from string'() {
        when:
        Path path = new Path(fullPath)

        then:
        path.nodes.size() == expectedSize
        path.nodes.findIndexOf {it.modelIdentifier} == expectedModelIdentifierIndex
        path.nodes[0].prefix

        where:
        fullPath                                                                                        | expectedSize  | expectedModelIdentifierIndex
        "fo:soluta eum architecto|dm:modi unde est\$matrix|dc:est quasi vel|de:new data element label"  | 4             | 1
        "dm:BC_Bloods\$2.0.0"                                                                           | 1             | 0
        "fo:soluta eum architecto"                                                                      | 1             | -1
        "vf:soluta eum architecto\$main|te:Dewey Decimal Classification v22"                            | 2             | 0
        "fo:soluta eum architecto|te:Dewey Decimal Classification v22"                                  | 2             | -1
        "fo:soluta eum architecto|dm:modi unde est\$1.0.0|dc:est quasi vel|dc:est sed hic"              | 4             | 1
        "FO:soluta eum architecto|DM:modi unde est\$matrix|DC:est quasi vel|DE:new data element label"  | 4             | 1

    }


}
