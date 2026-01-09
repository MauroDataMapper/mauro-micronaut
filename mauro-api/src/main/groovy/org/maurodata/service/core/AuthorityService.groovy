package org.maurodata.service.core

import jakarta.inject.Singleton
import org.maurodata.domain.authority.Authority
import org.maurodata.persistence.cache.ItemCacheableRepository

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import jakarta.inject.Inject

@CompileStatic
@Slf4j
@Singleton
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

    Authority findByLabel(String label) {
        authorityRepository.findByLabel(label)
    }
}
