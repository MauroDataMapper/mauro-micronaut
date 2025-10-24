package org.maurodata.test.domain.model

import org.maurodata.domain.model.Path

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Specification
import spock.lang.Unroll

@MicronautTest
class PathTest extends Specification {

    @Unroll
    void 'test path splitting, for #pathString'() {
        when:
        Path thePath = new Path(pathString)

        then:
        thePath
        thePath.nodes.size() == nodeCount

        where:
        pathString | nodeCount
        'vf:versioned folder|dm:data model|de:data element' | 3
        'vf:versioned folder$main|dm:data model|de:data element' | 3
        'vf:versioned folder$main|dm:data model|de:data element@attribute' | 3
        'vf:versioned%|folder|dm:data%|model|de:data%|element' | 3
        'vf:My%$versioned%|folder|dm:data%|model|de:data%|element' | 3
        'vf:My%$versioned%%%|folder|dm:data%|model|de:data%|element' | 3
        'vf:My%$versioned%%%|folder|dm:data%|model|de:data%|element@attribute' | 3
        'vf:My%$versioned%%%|folder|dm:data%|model|de:data%|element%@home@attribute' | 3
        'vf:My%$versioned%%%|folder%:%:|dm:data%|model|de:data%|element%@home@attribute' | 3
    }

    @Unroll
    void 'test path round trip, for #pathString'() {
        when:
        Path thePath = new Path(pathString)

        then:
        thePath
        thePath.toString() == pathString

        where:
        pathString | result
        'vf:versioned folder|dm:data model|de:data element' | ''
        'vf:versioned folder$main|dm:data model|de:data element' | ''
        'vf:versioned folder$main|dm:data model|de:data element@attribute' | ''
        'vf:versioned%|folder|dm:data%|model|de:data%|element' | ''
        'vf:My%$versioned%|folder|dm:data%|model|de:data%|element' | ''
        'vf:My%$versioned%%%|folder|dm:data%|model|de:data%|element' | ''
        'vf:My%$versioned%%%|folder|dm:data%|model|de:data%|element@attribute' | ''
        'vf:My%$versioned%%%|folder|dm:data%|model|de:data%|element%@home@attribute' | ''
        'vf:My%$versioned%%%|folder%:|dm:data%|model|de:data%|element%@home@attribute' | ''
    }

    @Unroll
    void 'test escape for #identifier'() {
        when:
        String escaped = Path.PathNode.escapeIdentifier(identifier)

        then:
        escaped
        escaped == escapedString

        where:
        identifier | escapedString
        'a node label' | 'a node label'
        '%' | '%%'
        '%%' | '%%%%'
        '|' | '%|'
        '%|' | '%%%|'
        '||' | '%|%|'
        '$' | '%$'
        '%$' | '%%%$'
        '$$' | '%$%$'
        '@' | '%@'
        '%@' | '%%%@'
        '@@' | '%@%@'
        ':' | '%:'
        '%:' | '%%%:'
        '::' | '%:%:'
    }

    @Unroll
    void 'test unescape for #escapedString'() {
        when:
        String unescapeIdentifier = Path.PathNode.unescapeIdentifier(escapedString)

        then:
        unescapeIdentifier
        unescapeIdentifier == identifier

        where:
        identifier | escapedString
        'a node label' | 'a node label'
        '%' | '%%'
        '%%' | '%%%%'
        '|' | '%|'
        '%|' | '%%%|'
        '||' | '%|%|'
        '$' | '%$'
        '%$' | '%%%$'
        '$$' | '%$%$'
        '@' | '%@'
        '%@' | '%%%@'
        '@@' | '%@%@'
        ':' | '%:'
        '%:' | '%%%:'
        '::' | '%:%:'
    }
}
