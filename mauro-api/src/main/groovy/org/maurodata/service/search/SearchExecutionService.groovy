package org.maurodata.service.search

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import jakarta.inject.Singleton
import org.maurodata.domain.classifier.Classifier
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.search.dto.SearchRequestDTO
import org.maurodata.domain.search.dto.SearchResultsDTO
import org.maurodata.domain.security.Role
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.ClassifierCacheableRepository
import org.maurodata.persistence.model.PathRepository
import org.maurodata.persistence.search.SearchRepository
import org.maurodata.security.AccessControlService
import org.maurodata.web.ListResponse

import java.util.function.BiFunction

@Slf4j
@CompileStatic
@Singleton
class SearchExecutionService {

    private final SearchRepository searchRepository
    private final AccessControlService accessControlService
    private final PathRepository pathRepository
    private final ClassifierCacheableRepository classifierCacheableRepository

    SearchExecutionService(
        SearchRepository searchRepository,
        AccessControlService accessControlService,
        PathRepository pathRepository,
        ClassifierCacheableRepository classifierCacheableRepository
    ) {
        this.searchRepository = searchRepository
        this.accessControlService = accessControlService
        this.pathRepository = pathRepository
        this.classifierCacheableRepository = classifierCacheableRepository
    }

    ListResponse<SearchResultsDTO> executeSearch(
        SearchRequestDTO requestDTO,
        BiFunction<String, UUID, AdministeredItem> itemLookup
    ) {
        long startTime = System.currentTimeMillis()
        List<SearchResultsDTO> searchResults = retrieveSearchResults(requestDTO)
        log.debug('Search time taken (retrieve): {}', System.currentTimeMillis() - startTime)

        List<SearchResultsDTO> searchResultsReadable = filterReadable(searchResults, requestDTO, itemLookup)

        log.debug('Search time taken (retrieve + filter): {}', System.currentTimeMillis() - startTime)
        ListResponse.from(searchResultsReadable, requestDTO)
    }

    List<SearchResultsDTO> retrieveSearchResults(SearchRequestDTO requestDTO) {
        searchRepository.search(requestDTO)
    }

    List<SearchResultsDTO> retrieveSearchResults(SearchRequestDTO requestDTO, Integer limit) {
        searchRepository.search(requestDTO, limit)
    }

    List<SearchResultsDTO> filterReadable(
        List<SearchResultsDTO> searchResults,
        SearchRequestDTO requestDTO,
        BiFunction<String, UUID, AdministeredItem> itemLookup
    ) {
        filterReadableUntil(searchResults, requestDTO, itemLookup, Integer.MAX_VALUE).readable
    }

    FilteredSearchResults filterReadableUntil(
        List<SearchResultsDTO> searchResults,
        SearchRequestDTO requestDTO,
        BiFunction<String, UUID, AdministeredItem> itemLookup,
        int requiredReadable
    ) {
        Set<UUID> allClassifierIds = [] as Set<UUID>
        if (requestDTO.classifiers) {
            allClassifierIds.addAll(requestDTO.classifiers as Collection<UUID>)
        }
        List<Classifier> allClassifiers = allClassifierIds.isEmpty() ?
            Collections.<Classifier>emptyList() :
            classifierCacheableRepository.readAllByIdIn(allClassifierIds)
        Map<UUID, Set<UUID>> classifierMap = [:].withDefault { [] as Set<UUID> } as Map<UUID, Set<UUID>>
        allClassifiers.each { Classifier classifier ->
            classifierMap[classifier.classificationScheme.id] << classifier.id
        }

        int unreadableCount = 0
        int scannedCount = 0
        List<SearchResultsDTO> readable = new ArrayList<SearchResultsDTO>()
        for (SearchResultsDTO result : searchResults ?: Collections.<SearchResultsDTO>emptyList()) {
            scannedCount++
            AdministeredItem item
            try {
                item = itemLookup.apply(result.domainType, result.id)
            } catch (HttpStatusException e) {
                if (e.status == HttpStatus.NOT_FOUND) {
                    unreadableCount++
                    log.debug('Skipping stale search result for deleted or missing {} {}', result.domainType, result.id)
                    continue
                }
                throw e
            }
            if (item == null) {
                unreadableCount++
                log.debug('Skipping stale search result for deleted or missing {} {}', result.domainType, result.id)
                continue
            }
            if (!accessControlService.canDoRole(Role.READER, item)) {
                unreadableCount++
                continue
            }
            pathRepository.readParentItems(item)
            item.updateBreadcrumbs()
            result.breadcrumbs = item.breadcrumbs
            result.classifiers = item.classifiers
            Set<UUID> resultClassifierIds = result.classifiers.id as Set<UUID>

            boolean classifierFilter = allClassifierIds.isEmpty() ||
                classifierMap.every { UUID ignored, Set<UUID> classifiers ->
                    classifiers.any { UUID cid -> resultClassifierIds.contains(cid) }
                }
            if (!classifierFilter) {
                continue
            }
            readable.add(result)
            if (readable.size() >= requiredReadable) {
                return new FilteredSearchResults(
                    readable: readable,
                    unreadableCount: unreadableCount,
                    scannedCount: scannedCount,
                    exhausted: scannedCount >= (searchResults?.size() ?: 0)
                )
            }
        }
        new FilteredSearchResults(
            readable: readable,
            unreadableCount: unreadableCount,
            scannedCount: scannedCount,
            exhausted: true
        )
    }

    static class FilteredSearchResults {
        List<SearchResultsDTO> readable = []
        int unreadableCount
        int scannedCount
        boolean exhausted
    }
}
