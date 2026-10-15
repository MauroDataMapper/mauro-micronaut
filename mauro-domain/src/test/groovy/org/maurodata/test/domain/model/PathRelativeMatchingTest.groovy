package org.maurodata.test.domain.model

import org.maurodata.domain.model.Path
import spock.lang.Specification

class PathRelativeMatchingTest extends Specification {

    void 'matches exact root-relative paths and exposes the terminal attribute'() {
        expect:
        new Path('fo:Catalog|dm:Customers$main|dc:Person|de:age').matchesRelativeTo(
            new Path('fo:Catalog'), new Path('dm:Customers$main|dc:Person|de:age@description'))
        new Path('de:age@description').attribute == 'description'
    }

    void 'an omitted model identifier matches versions, but an explicit one selects a branch'() {
        given:
        Path candidate = new Path('fo:Catalog|dm:Customers$release-2|dc:Person')
        Path root = new Path('fo:Catalog')

        expect:
        candidate.matchesRelativeTo(root, new Path('dm:Customers|dc:Person'))
        candidate.matchesRelativeTo(root, new Path('dm:Customers$release-2|dc:Person'))
        !candidate.matchesRelativeTo(root, new Path('dm:Customers$main|dc:Person'))
    }

    void 'requires an exact path by default and can match a descendant prefix explicitly'() {
        given:
        Path candidate = new Path('fo:Catalog|dm:Customers$main|dc:Person|de:age')
        Path root = new Path('fo:Catalog')
        Path selector = new Path('dm:Customers$main|dc:Person')

        expect:
        !candidate.matchesRelativeTo(root, selector)
        candidate.matchesRelativeTo(root, selector, false)
    }
}
