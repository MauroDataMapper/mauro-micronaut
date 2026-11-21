package org.maurodata.service.overlay

import groovy.transform.CompileStatic
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import org.maurodata.domain.overlay.OverlayRule
import org.maurodata.persistence.overlay.OverlayRuleRepository
import org.maurodata.web.ListResponse
import org.maurodata.web.PaginationParams

@CompileStatic
@Singleton
class OverlayRuleService {
    static final long ORDER_GAP = 1000L
    final OverlayRuleRepository repository

    OverlayRuleService(OverlayRuleRepository repository) { this.repository = repository }

    @Transactional(readOnly = true)
    ListResponse<OverlayRule> list(UUID overlayId, PaginationParams params) {
        int offset = Math.max(0, params?.offset ?: 0)
        int max = params?.max ?: -1
        if (max <= 0 || params?.all?.equalsIgnoreCase('true')) {
            List<OverlayRule> all = repository.findAllByOverlayIdOrderByOrderAsc(overlayId)
            return ListResponse.from(all, all.size())
        }
        ListResponse.from(repository.readPageByOverlayId(overlayId, max, offset), repository.countByOverlayId(overlayId))
    }

    @Transactional(readOnly = true)
    OverlayRule read(UUID overlayId, UUID ruleId) {
        OverlayRule rule = repository.findById(ruleId).orElse(null)
        if (!rule || rule.overlayId != overlayId) throw new HttpStatusException(HttpStatus.NOT_FOUND, 'OverlayRule not found')
        rule
    }

    @Transactional
    OverlayRule create(UUID overlayId, OverlayRule rule) {
        lock(overlayId)
        List<OverlayRule> rules = repository.findAllByOverlayIdOrderByOrderAsc(overlayId)
        Long maxOrder = rules ? rules*.order.findAll { it != null }.max() as Long : null
        if (maxOrder != null && maxOrder > Long.MAX_VALUE - ORDER_GAP) {
            persistOrdering(overlayId, rules)
            rules = repository.findAllByOverlayIdOrderByOrderAsc(overlayId)
        }
        rule.overlayId = overlayId
        rule.ensureId()
        rule.order = nextOrder(rules)
        rule.prepareForPersistence()
        repository.save(rule)
    }

    @Transactional
    OverlayRule update(UUID overlayId, UUID ruleId, OverlayRule update) {
        lock(overlayId)
        OverlayRule existing = read(overlayId, ruleId)
        existing.match = update.match
        existing.actions = update.actions ?: []
        existing.prepareForPersistence()
        repository.update(existing)
    }

    @Transactional
    void delete(UUID overlayId, UUID ruleId) {
        lock(overlayId)
        read(overlayId, ruleId)
        repository.deleteById(ruleId)
    }

    @Transactional
    OverlayRule move(UUID overlayId, UUID ruleId, UUID beforeRuleId, UUID afterRuleId) {
        if ((beforeRuleId == null) == (afterRuleId == null)) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'Specify exactly one of beforeRuleId or afterRuleId')
        }
        lock(overlayId)
        List<OverlayRule> rules = repository.findAllByOverlayIdOrderByOrderAsc(overlayId)
        OverlayRule moving = rules.find { OverlayRule rule -> rule.id == ruleId }
        if (!moving) throw new HttpStatusException(HttpStatus.NOT_FOUND, 'OverlayRule not found')
        UUID anchorId = beforeRuleId ?: afterRuleId
        OverlayRule anchor = rules.find { OverlayRule rule -> rule.id == anchorId }
        if (!anchor || anchor.id == moving.id) throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'Invalid OverlayRule move anchor')
        rules.remove(moving)
        int anchorIndex = rules.findIndexOf { OverlayRule rule -> rule.id == anchor.id }
        rules.add(beforeRuleId ? anchorIndex : anchorIndex + 1, moving)
        persistOrdering(overlayId, rules)
        moving
    }

    /** Bulk Folder imports use array position as the source ordering; numeric ranks are backend-owned. */
    @Transactional
    void replaceAll(UUID overlayId, List<OverlayRule> importedRules) {
        lock(overlayId)
        repository.deleteAllForOverlay(overlayId)
        List<OverlayRule> rules = importedRules ?: []
        rules.eachWithIndex { OverlayRule rule, int index ->
            rule.overlayId = overlayId
            rule.ensureId()
            rule.order = (index + 1L) * ORDER_GAP
            rule.prepareForPersistence()
        }
        if (rules) repository.saveAll(rules)
    }

    @Transactional(readOnly = true)
    List<OverlayRule> readAll(UUID overlayId) { repository.findAllByOverlayIdOrderByOrderAsc(overlayId) }

    private void persistOrdering(UUID overlayId, List<OverlayRule> rules) {
        // The owning Overlay row lock serialises writers. A temporary negative phase avoids unique-key
        // collisions while assigning the new sparse ranks.
        repository.negateOrders(overlayId)
        rules.eachWithIndex { OverlayRule rule, int index -> rule.order = (index + 1L) * ORDER_GAP }
        repository.updateAll(rules)
    }

    private void lock(UUID overlayId) {
        if (!repository.lockOverlay(overlayId)) throw new HttpStatusException(HttpStatus.NOT_FOUND, 'Overlay not found')
    }

    private static long nextOrder(List<OverlayRule> rules) {
        Long max = rules ? rules*.order.findAll { it != null }.max() as Long : null
        (max ?: 0L) + ORDER_GAP
    }
}
