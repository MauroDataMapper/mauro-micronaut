package org.maurodata.persistence.search

import java.time.Instant

record SearchDomainsRefreshedEvent(long version, Instant refreshedAt) {
}
