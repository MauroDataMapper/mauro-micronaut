package org.maurodata.persistence.overlay

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import org.maurodata.domain.overlay.OverlayRule

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class OverlayRuleRepository implements CrudRepository<OverlayRule, UUID> {
    abstract List<OverlayRule> findAllByOverlayIdOrderByOrderAsc(UUID overlayId)

    abstract Long countByOverlayId(UUID overlayId)

    @Query('''select * from core.overlay_rule
              where overlay_id = :overlayId
              order by rule_order asc
              limit :max offset :offset''')
    abstract List<OverlayRule> readPageByOverlayId(UUID overlayId, Integer max, Integer offset)

    @Query(value = '''
        select rule.id as overlay_rule_id, data_class.id as item_id, 'DataClass' as item_domain_type
        from core.overlay_rule rule
        join datamodel.data_class data_class
          on rule.first_node_prefix = 'dc'
         and rule.selector_node_count = 1
         and rule.last_node_prefix = 'dc'
         and data_class.data_model_id in (:modelIds)
         and ((rule.first_node_identifier_suffix and right(data_class.label, length(rule.first_node_identifier)) = rule.first_node_identifier)
              or (not rule.first_node_identifier_suffix and data_class.label = rule.first_node_identifier))
        where rule.overlay_id in (:overlayIds)
        union all
        select rule.id as overlay_rule_id, data_element.id as item_id, 'DataElement' as item_domain_type
        from core.overlay_rule rule
        join datamodel.data_class data_class
          on rule.first_node_prefix = 'dc'
         and rule.selector_node_count = 2
         and rule.last_node_prefix = 'de'
         and data_class.data_model_id in (:modelIds)
         and ((rule.first_node_identifier_suffix and right(data_class.label, length(rule.first_node_identifier)) = rule.first_node_identifier)
              or (not rule.first_node_identifier_suffix and data_class.label = rule.first_node_identifier))
        join datamodel.data_element data_element
          on data_element.data_class_id = data_class.id
         and data_element.label = rule.last_node_identifier
        where rule.overlay_id in (:overlayIds)
        ''', nativeQuery = true)
    abstract List<OverlayRuleTargetDTO> findDataModelRuleTargets(Collection<UUID> overlayIds, Collection<UUID> modelIds)

    @Query('''select id from core.overlay where id = :overlayId for update''')
    @Nullable abstract UUID lockOverlay(UUID overlayId)

    @Query('''update core.overlay_rule set rule_order = -rule_order where overlay_id = :overlayId''')
    abstract long negateOrders(UUID overlayId)

    @Query('''delete from core.overlay_rule where overlay_id = :overlayId''')
    abstract long deleteAllForOverlay(UUID overlayId)
}
