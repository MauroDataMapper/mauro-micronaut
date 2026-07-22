package org.maurodata.test.domain.model

import org.maurodata.domain.model.Path

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
