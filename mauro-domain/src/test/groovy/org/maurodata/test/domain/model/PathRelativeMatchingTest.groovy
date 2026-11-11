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

    void 'matches selectors with explicit first-node suffix matching across models'() {
        given:
        Path root = new Path('vf:Silver$1.0.0')
        Path selector = new Path('dc:Order_Text|de:Description@description')

        expect:
        new Path('vf:Silver$1.0.0|fo:OUHFT|dm:Ouh_Cerner|dc:Ouh_Cerner_Order_Text|de:Description@description')
            .matchesRelativeTo(root, selector, true, true, true)
        !new Path('vf:Silver$1.0.0|fo:RBHFT|dm:Rbhft_Cerner|dc:Rbhft_Cerner_Other|de:Description@description')
            .matchesRelativeTo(root, selector, true, true, true)
    }

    void 'a leading asterisk is a literal identifier character, not a suffix selector'() {
        given:
        Path root = new Path('vf:Silver$1.0.0')
        Path candidate = new Path('vf:Silver$1.0.0|dc:Ouh_Order')

        expect:
        !candidate.matchesRelativeTo(root, new Path('dc:*Order'))
        candidate.matchesRelativeTo(root, new Path('dc:Order'), true, false, true)
    }

    void 'suffix matching is explicit and applies only to the first selector node'() {
        given:
        Path root = new Path('vf:Silver$1.0.0')
        Path selector = new Path('fo:HFT|dc:Ouh_Order|de:Accept_Size')

        expect:
        new Path('vf:Silver$1.0.0|fo:OUHFT|dc:Ouh_Order|de:Accept_Size')
            .matchesRelativeTo(root, selector, true, false, true)
        !new Path('vf:Silver$1.0.0|fo:OUHFT|dc:Ouh_Order|de:Accept_SizeExtra')
            .matchesRelativeTo(root, selector, true, false, true)
    }

    void 'whole path suffix matching remains independent from first node suffix matching'() {
        given:
        Path root = new Path('vf:Silver$1.0.0')
        Path selector = new Path('dc:Order|de:Accept_Size')

        expect:
        new Path('vf:Silver$1.0.0|fo:OUHFT|dm:Warehouse|dc:Ouh_Order|de:Accept_Size')
            .matchesRelativeTo(root, selector, true, true, true)
        !new Path('vf:Silver$1.0.0|fo:OUHFT|dm:Warehouse|dc:Ouh_Order|de:Accept_Size')
            .matchesRelativeTo(root, selector, true, false, true)
    }
}
