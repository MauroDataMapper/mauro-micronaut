package org.maurodata.persistence.overlay

import jakarta.inject.Inject
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.overlay.Overlay
import org.maurodata.persistence.ContentsService
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.persistence.cache.ModelCacheableRepository
import spock.lang.Specification

@ContainerizedTest
class OverlayRepositorySpec extends Specification {

    @Inject
    OverlayRepository overlayRepository

    @Inject
    ModelCacheableRepository.OverlayCacheableRepository overlayCacheableRepository

    @Inject
    ModelCacheableRepository.FolderCacheableRepository folderRepository

    @Inject
    ContentsService contentsService

    void 'repository identifies Overlay types and path prefixes'() {
        expect:
        overlayRepository.domainClass == Overlay
        overlayRepository.handles(Overlay)
        overlayRepository.handles('overlay')
        overlayRepository.handles('OVERLAY')
        !overlayRepository.handles((String) null)
        overlayRepository.handlesPathPrefix('ov')
        overlayRepository.handlesPathPrefix('OV')
        !overlayRepository.handlesPathPrefix('dm')
    }

    void 'repository reads overlays by id and folder ids'() {
        given:
        Folder firstFolder = folderRepository.save(new Folder(label: 'Overlay repository folder one'))
        Folder secondFolder = folderRepository.save(new Folder(label: 'Overlay repository folder two'))
        Overlay firstOverlay = contentsService.saveWithContent(new Overlay(label: 'First overlay', folder: firstFolder)) as Overlay
        Overlay secondOverlay = contentsService.saveWithContent(new Overlay(label: 'Second overlay', folder: firstFolder)) as Overlay
        Overlay otherFolderOverlay = contentsService.saveWithContent(new Overlay(label: 'Other folder overlay', folder: secondFolder)) as Overlay

        when:
        Overlay found = overlayRepository.findById(firstOverlay.id)
        List<Overlay> firstFolderOverlays = overlayRepository.findAllByFolderId(firstFolder.id)
        List<Overlay> overlaysInBothFolders = overlayRepository.readAllByFolderIdIn([firstFolder.id, secondFolder.id])

        then:
        found.id == firstOverlay.id
        found.label == 'First overlay'
        firstFolderOverlays*.id as Set == ([firstOverlay.id, secondOverlay.id] as Set)
        overlaysInBothFolders*.id as Set == ([firstOverlay.id, secondOverlay.id, otherFolderOverlay.id] as Set)
    }
}
