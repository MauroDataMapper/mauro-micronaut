package org.maurodata.persistence.overlay

import jakarta.inject.Inject
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.overlay.Overlay
import org.maurodata.domain.model.ModelPathMatch
import org.maurodata.domain.overlay.OverlayRule
import org.maurodata.domain.model.Path
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

    @Inject OverlayRuleRepository overlayRuleRepository

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

    void 'Folder content persists and reloads OverlayRules in backend order'() {
        given:
        Folder folder = folderRepository.save(new Folder(label: 'Overlay rule folder'))
        Overlay overlay = new Overlay(label: 'Ordered rules', folder: folder, overlayRules: [
            new OverlayRule(match: new ModelPathMatch(path: new Path('dc:First'))),
            new OverlayRule(match: new ModelPathMatch(path: new Path('dc:Second')))
        ])

        when:
        Overlay saved = contentsService.saveWithContent(overlay) as Overlay
        Overlay loaded = contentsService.loadWithContent(overlayRepository.readById(saved.id)) as Overlay

        then:
        overlayRuleRepository.countByOverlayId(saved.id) == 2L
        loaded.overlayRules.collect { it.match.path.toString() } == ['dc:First', 'dc:Second']
        loaded.overlayRules*.order == [1000L, 2000L]
    }
}
