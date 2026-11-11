package org.maurodata.test.domain.overlay

import org.maurodata.domain.overlay.Overlay
import org.maurodata.domain.overlay.OverlayAction
import org.maurodata.domain.overlay.OverlayRule
import spock.lang.Specification

class OverlayIdentifierTest extends Specification {

    void 'new rule and action identifiers use UUIDv7'() {
        expect:
        new OverlayRule().id.version() == 7
        new OverlayAction().id.version() == 7
    }

    void 'Overlay persistence assigns UUIDv7 to missing nested identifiers'() {
        given:
        Overlay overlay = new Overlay(overlayRules: [
            new OverlayRule(id: null, actions: [new OverlayAction(id: null)])
        ])

        when:
        overlay.prePersist()

        then:
        overlay.overlayRules[0].id.version() == 7
        overlay.overlayRules[0].actions[0].id.version() == 7
    }

    void 'copy preserves nested identifiers until removed for creation'() {
        given:
        Overlay original = new Overlay(overlayRules: [
            new OverlayRule(actions: [new OverlayAction(type: 'value', field: 'description', value: 'text')])
        ])

        when:
        Overlay copy = original.shallowCopy() as Overlay

        then:
        copy.overlayRules[0].id == original.overlayRules[0].id
        copy.overlayRules[0].actions[0].id == original.overlayRules[0].actions[0].id

        when:
        copy.removeIds()
        copy.prePersist()

        then:
        copy.overlayRules[0].id.version() == 7
        copy.overlayRules[0].actions[0].id.version() == 7
        copy.overlayRules[0].id != original.overlayRules[0].id
        copy.overlayRules[0].actions[0].id != original.overlayRules[0].actions[0].id
    }
}
