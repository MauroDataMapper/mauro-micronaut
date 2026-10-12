package org.maurodata.service.search

import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.search.dto.SearchRequestDTO
import org.maurodata.domain.search.dto.SearchResultsDTO
import org.maurodata.domain.security.Role
import org.maurodata.persistence.model.PathRepository
import org.maurodata.security.AccessControlService
import spock.lang.Specification

class SearchExecutionServiceSpec extends Specification {

    void 'readability filtering skips stale search rows for deleted catalogue items'() {
        given:
        UUID staleId = UUID.randomUUID()
        UUID liveId = UUID.randomUUID()
        DataModel liveItem = new DataModel(id: liveId, label: 'Live model')
        SearchExecutionService service = new SearchExecutionService(
            null,
            new AccessControlService() {
                @Override
                boolean canDoRole(Role role, AdministeredItem item) {
                    true
                }
            },
            new PathRepository() {
                @Override
                List<AdministeredItem> readParentItems(AdministeredItem item) {
                    []
                }
            },
            null
        )

        when:
        SearchExecutionService.FilteredSearchResults filtered = service.filterReadableUntil(
            [
                new SearchResultsDTO(id: staleId, domainType: 'DataModel', label: 'Deleted model'),
                new SearchResultsDTO(id: liveId, domainType: 'DataModel', label: 'Live model')
            ],
            new SearchRequestDTO(searchTerm: 'model'),
            {String domainType, UUID id ->
                if (id == staleId) {
                    throw new HttpStatusException(HttpStatus.NOT_FOUND, 'AdministeredItem not found by ID')
                }
                liveItem
            },
            10
        )

        then:
        filtered.readable*.id == [liveId]
        filtered.unreadableCount == 1
        filtered.scannedCount == 2
        filtered.exhausted
    }
}
