package org.maurodata.test.domain.overlay

import org.maurodata.domain.overlay.OverlayActionPolicy
import spock.lang.Specification
import spock.lang.Unroll

class OverlayActionPolicyTest extends Specification {

    @Unroll
    void 'parses the open string policy identifier #identifier'() {
        expect:
        OverlayActionPolicy.fromIdentifier(identifier) == expected

        where:
        identifier        | expected
        'always'           | OverlayActionPolicy.ALWAYS
        'overwriteIfEmpty' | OverlayActionPolicy.OVERWRITE_IF_EMPTY
        'add'              | OverlayActionPolicy.ADD
    }

    void 'each enum value has one unique canonical identifier'() {
        expect:
        OverlayActionPolicy.values()*.identifier.unique().size() == OverlayActionPolicy.values().size()
        OverlayActionPolicy.values().every { policy -> OverlayActionPolicy.fromIdentifier(policy.identifier) == policy }
    }

    @Unroll
    void 'rejects noncanonical policy identifier #identifier'() {
        when:
        OverlayActionPolicy.fromIdentifier(identifier)

        then:
        thrown(IllegalArgumentException)

        where:
        identifier << ['if_empty', 'ifempty', 'overwriteifempty', 'additive', 'addifnotempty', 'ALWAYS', ' add']
    }
}
