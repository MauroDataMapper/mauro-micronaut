package org.maurodata.service.overlay

import spock.lang.Specification
import spock.lang.Unroll

class OverlayApplicationServiceTest extends Specification {

    @Unroll
    void 'empty values follow Overlay overwrite semantics for #value'() {
        expect:
        OverlayApplicationService.isEmptyValue(value) == empty

        where:
        value | empty
        null  | true
        ''    | true
        '  '  | true
        []    | true
        [:]   | true
        false | false
        0     | false
        '0'   | false
    }
}
