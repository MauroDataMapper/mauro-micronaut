package org.maurodata.domain.overlay

import groovy.transform.CompileStatic
import jakarta.inject.Singleton
import org.maurodata.FieldConstants
import org.maurodata.domain.model.ModelService

@CompileStatic
@Singleton
class OverlayService extends ModelService<Overlay> {
    @Override Boolean handles(Class clazz) { Overlay.isAssignableFrom(clazz) }
    @Override Boolean handles(String domainType) {
        domainType != null && domainType.toLowerCase() in [FieldConstants.OVERLAY_LOWERCASE, FieldConstants.OVERLAYS_LOWERCASE]
    }
}
