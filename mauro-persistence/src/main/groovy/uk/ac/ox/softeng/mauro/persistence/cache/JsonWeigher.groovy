package uk.ac.ox.softeng.mauro.persistence.cache

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.benmanes.caffeine.cache.Weigher
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import jakarta.inject.Inject
import jakarta.inject.Singleton

import java.nio.charset.StandardCharsets

/**
 * Bean used by Caffeine cache to approximately weigh cache entries, to allow limiting cache by maximum total weight.
 */
@CompileStatic
@Singleton
@Slf4j
class JsonWeigher implements Weigher {

    @Inject
    ObjectMapper objectMapper

    /**
     * Weigher using JSON serialisation.
     * @param key the key to weigh
     * @param value the value to weigh
     * @return Weight of value, approximated by size in bytes of its JSON serialization.
     */
    @Override
    int weigh(Object key, Object value) {
        objectMapper.writeValueAsString(value).getBytes(StandardCharsets.UTF_16).size()
    }
}
