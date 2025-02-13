package uk.ac.ox.softeng.mauro.service.core

import uk.ac.ox.softeng.mauro.domain.authority.Authority
import uk.ac.ox.softeng.mauro.persistence.cache.ItemCacheableRepository

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import jakarta.inject.Inject

@CompileStatic
@Slf4j
class AuthorityService {
    final ItemCacheableRepository.AuthorityCacheableRepository authorityRepository

    @Inject
    AuthorityService(ItemCacheableRepository.AuthorityCacheableRepository authorityRepository) {
        this.authorityRepository = authorityRepository
    }

    Authority getDefaultAuthority() {
        authorityRepository.findByDefaultAuthority(true)
    }

    Authority create(Authority authority) {
        authorityRepository.save(authority)
    }

    Authority find(UUID id) {
        authorityRepository.findById(id)
    }

    List<Authority> findAll() {
        authorityRepository.findAll()
    }

     Authority readById(UUID id) {
         authorityRepository.readById(id)
    }

    Authority update(Authority authority) {
        authorityRepository.update(authority)
    }

    long delete(Authority authority) {
        authorityRepository.delete(authority)
    }
}
