package org.maurodata.service.model

import groovy.transform.CompileStatic
import jakarta.inject.Singleton
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.model.Model

@CompileStatic
@Singleton
class ModelMembershipService {
    static final String NAMESPACE = 'org.maurodata.model'
    static final String KEY = 'membership'

    static Metadata metadata(List<Map<String, String>> membership) {
        new Metadata(namespace: NAMESPACE, key: KEY, value: groovy.json.JsonOutput.toJson(membership ?: []))
    }

    static List<Map<String, String>> read(Model model) {
        Metadata membership = model.metadata?.find { Metadata item -> item.namespace == NAMESPACE && item.key == KEY }
        if (!membership?.value) return []
        Object parsed = new groovy.json.JsonSlurper().parseText(membership.value)
        (parsed as List<Map<String, String>>)
    }

    static boolean changed(Model current, Model successor) {
        read(current) != read(successor)
    }
}
