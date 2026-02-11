package org.maurodata.test.domain.util

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import org.maurodata.domain.model.Path
import org.maurodata.util.PathStringUtils
import spock.lang.Specification
import spock.lang.Unroll

@MicronautTest
class PathStringUtilsTest extends Specification {

    @Unroll
    void 'test getItemSubPath, for #pathPrefix, #fullPath'() {
        when:
        String subPath = PathStringUtils.getItemSubPath(pathPrefix, fullPath)

        then:
        subPath
        subPath == expectedSubPath

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
    void 'test getVersionFromPath for #fullPath'() {
        when:
        String version = PathStringUtils.getVersionFromPath(fullPath)

        then:
        version == expectedVersion

        where:
        fullPath                                                                                              | expectedVersion
        "fo:soluta eum architecto|dm:modi unde est\$matrix|dc:est quasi vel|de:new data element label\$2.0.0" | "2.0.0"
        "dm:BC_Bloods\$2.0.0"                                                                                 | "2.0.0"
        "fo:soluta eum architecto"                                                                            | null
        "fo:soluta eum architecto|te:Dewey Decimal Classification v22\$main"                                  | "main"
    }

    void 'test getting path from string'() {
        when:
        Path path = new Path(fullPath)

        then:
        path.nodes.size() == expectedSize
        path.nodes.findIndexOf {it.modelIdentifier} == expectedModelIdentifierIndex
        path.nodes.first.prefix

        where:
        fullPath                                                                                        | expectedSize  | expectedModelIdentifierIndex
        "fo:soluta eum architecto|dm:modi unde est\$matrix|dc:est quasi vel|de:new data element label"  | 4             | 1
        "dm:BC_Bloods\$2.0.0"                                                                           | 1             | 0
        "fo:soluta eum architecto"                                                                      | 1             | -1
        "fo:soluta eum architecto\$main|te:Dewey Decimal Classification v22"                            | 2             | 0
        "fo:soluta eum architecto|te:Dewey Decimal Classification v22"                                  | 2             | -1
        "fo:soluta eum architecto|dm:modi unde est\$1.0.0|dc:est quasi vel|dc:est sed hic"              | 4             | 1

    }


}
