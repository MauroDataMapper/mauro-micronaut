package org.maurodata.test.domain.model

import org.maurodata.domain.model.Path

import io.micronaut.test.extensions.spock.annotation.MicronautTest
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
    }

}
