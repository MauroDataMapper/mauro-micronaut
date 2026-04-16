package org.maurodata.persistence.search

import io.micronaut.context.event.ApplicationEventListener
import jakarta.inject.Singleton

@Singleton
class SearchIndexChangeListener implements ApplicationEventListener<DataChangeEvent> {

    final SearchIndexRefreshScheduler searchIndexRefreshScheduler

    SearchIndexChangeListener(SearchIndexRefreshScheduler searchIndexRefreshScheduler) {
        this.searchIndexRefreshScheduler = searchIndexRefreshScheduler
    }

    @Override
    void onApplicationEvent(DataChangeEvent event) {
        searchIndexRefreshScheduler.markIndexDirty()
    }

}
