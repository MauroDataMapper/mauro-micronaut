package org.maurodata.persistence.cache

import io.micronaut.core.annotation.Nullable
import jakarta.inject.Inject
import org.maurodata.domain.security.ApiKey
import org.maurodata.persistence.security.ApiKeyRepository

import org.maurodata.domain.facet.Rule
import org.maurodata.domain.facet.RuleRepresentation
import org.maurodata.persistence.facet.RuleRepresentationRepository

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.cache.annotation.CacheConfig
import io.micronaut.cache.annotation.CacheInvalidate
import io.micronaut.cache.annotation.Cacheable
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Singleton
import org.maurodata.domain.authority.Authority
import org.maurodata.domain.config.ApiProperty
import org.maurodata.domain.facet.SummaryMetadata
import org.maurodata.domain.facet.federation.SubscribedCatalogue
import org.maurodata.domain.facet.federation.SubscribedModel
import org.maurodata.domain.model.Item
import org.maurodata.domain.facet.SummaryMetadataReport
import org.maurodata.domain.security.CatalogueUser
import org.maurodata.domain.security.Role
import org.maurodata.domain.security.SecurableResourceGroupRole
import org.maurodata.domain.security.UserGroup
import org.maurodata.persistence.authority.AuthorityRepository
import org.maurodata.persistence.config.ApiPropertyRepository
import org.maurodata.persistence.federation.SubscribedCatalogueRepository
import org.maurodata.persistence.federation.SubscribedModelRepository
import org.maurodata.persistence.model.ItemRepository
import org.maurodata.persistence.facet.SummaryMetadataReportRepository
import org.maurodata.persistence.security.CatalogueUserRepository
import org.maurodata.persistence.security.SecurableResourceGroupRoleRepository
import org.maurodata.persistence.security.UserGroupRepository

@Slf4j
@CompileStatic
@CacheConfig(cacheNames = 'items-cache', keyGenerator = StringCacheKeyGenerator)
abstract class ItemCacheableRepository<I extends Item> implements ItemRepository<I> {

    static final String FIND_BY_ID = 'find'
    static final String READ_BY_ID = 'read'

    ItemRepository<I> repository
    String domainType

    ItemCacheableRepository(ItemRepository<I> itemRepository) {
        this.repository = itemRepository
        this.domainType = repository.domainClass.simpleName
    }

    I findById(UUID id) {
        cachedLookupById(FIND_BY_ID, domainType, id)
    }

    I readById(UUID id) {
        cachedLookupById(READ_BY_ID, domainType, id)
    }

    boolean existsById(UUID id){
        repository.existsById(id)
    }

    I save(I item) {
        I saved = repository.save(item)
        invalidate(item)
        saved
    }

    List<I> saveAll(Iterable<I> items) {
        List<I> saved = repository.saveAll(items)
        items.each { invalidate(it) }
        saved
    }

    I update(I item) {
        I updated = repository.update(item)
        invalidate(item)
        updated
    }

    List<I> updateAll(Iterable<I> items) {
        List<I> updated = repository.updateAll(items)
        items.each { invalidate(it) }
        updated
    }

    Long delete(I item) {
        Long deleted = repository.delete(item)
        invalidate(item)
        deleted
    }

    Long deleteAll(Iterable<I> items) {
        Long deleted = repository.deleteAll(items)
        items.each { invalidate(it) }
        deleted
    }


    List<I> readAll() {
        repository.readAll()
    }
    /**
     * Single method to perform all cached repository methods.
     * This allows all responses to be cached in a single cache which can have a configured maximum size.
     *
     * @param lookup Type of repository lookup to perform
     * @param id Object UUID
     * @return Mono of the object
     */
    I cachedLookupById(String lookup, String domainType, UUID id) {
        mutableCachedLookupById(lookup, domainType, id)?.clone() as I
    }

    @Cacheable
    I mutableCachedLookupById(String lookup, String domainType, UUID id) {
        switch (lookup) {
            case FIND_BY_ID -> repository.findById(id)
            case READ_BY_ID -> repository.readById(id)
        }
    }

    void invalidate(I item) {
        invalidate(item.id)
    }

    void invalidate(UUID id) {
        invalidateCachedLookupById(FIND_BY_ID, domainType, id)
        invalidateCachedLookupById(READ_BY_ID, domainType, id)
    }

    @CacheInvalidate
    void invalidateCachedLookupById(String lookup, String domainType, UUID id) {
        null // The '@CacheInvalidate' annotation does the work here
    }

    Class getDomainClass() {
        repository.domainClass
    }

    Boolean handles(Class clazz) {
        repository.handles(clazz)
    }

    Boolean handles(String domainType) {
        repository.handles(domainType)
    }

    Boolean handlesPathPrefix(final String pathPrefix) {
        repository.handlesPathPrefix(pathPrefix)
    }

    // Cacheable Item Repository definitions

    @Singleton
    @CompileStatic
    @CacheConfig(cacheNames = 'security-cache', keyGenerator = StringCacheKeyGenerator)
    static class SecurableResourceGroupRoleCacheableRepository extends ItemCacheableRepository<SecurableResourceGroupRole> {
        SecurableResourceGroupRoleCacheableRepository(SecurableResourceGroupRoleRepository securableResourceGroupRoleRepository) {
            super(securableResourceGroupRoleRepository)
        }

        @Override
        @CacheInvalidate(all = true) // clear the whole cache on any insert/update/delete
        void invalidateCachedLookupById(String lookup, String domainType, UUID id) {
            null
        }

        @CacheInvalidate(all = true)
        void invalidateAll() {}

        List<SecurableResourceGroupRole> readAllBySecurableResourceDomainTypeAndSecurableResourceId(String securableResourceDomainType, UUID securableResourceId) {
            mutableReadAllBySecurableResourceDomainTypeAndSecurableResourceId(securableResourceDomainType, securableResourceId).collect {it.clone()}
        }

        @Cacheable
        List<SecurableResourceGroupRole> mutableReadAllBySecurableResourceDomainTypeAndSecurableResourceId(String securableResourceDomainType, UUID securableResourceId) {
            ((SecurableResourceGroupRoleRepository) repository).readAllBySecurableResourceDomainTypeAndSecurableResourceId(securableResourceDomainType, securableResourceId)
        }

        // not cached

        Long deleteBySecurableResourceDomainTypeAndSecurableResourceIdAndRoleAndUserGroupId(String securableResourceDomainType, UUID securableResourceId, Role role, UUID userGroupId) {
            invalidateAll()
            ((SecurableResourceGroupRoleRepository) repository).deleteBySecurableResourceDomainTypeAndSecurableResourceIdAndRoleAndUserGroupId(securableResourceDomainType, securableResourceId, role, userGroupId)
        }
    }

    @Singleton
    @CompileStatic
    @CacheConfig(cacheNames = 'security-cache', keyGenerator = StringCacheKeyGenerator)
    static class UserGroupCacheableRepository extends ItemCacheableRepository<UserGroup> {
        UserGroupCacheableRepository(UserGroupRepository userGroupRepository) {
            super(userGroupRepository)
        }

        @Override
        @CacheInvalidate(all = true) // clear the whole cache on any insert/update/delete
        void invalidateCachedLookupById(String lookup, String domainType, UUID id) {
            null
        }

        @CacheInvalidate(all = true)
        void invalidateAll() {}

        List<UserGroup> readAllByCatalogueUserId(UUID catalogueUserId) {
            mutableReadAllByCatalogueUserId(catalogueUserId).collect {it.clone()}
        }

        @Cacheable
        List<UserGroup> mutableReadAllByCatalogueUserId(UUID catalogueUserId) {
            ((UserGroupRepository) repository).readAllByCatalogueUserId(catalogueUserId)
        }

        UserGroup addCatalogueUser(@NonNull UUID uuid, @NonNull UUID catalogueUserId) {
            invalidateAll()
            ((UserGroupRepository) repository).addCatalogueUser(uuid, catalogueUserId)
        }
    }

    @Singleton
    @CompileStatic
    @CacheConfig(keyGenerator = StringCacheKeyGenerator)
    static class CatalogueUserCacheableRepository extends ItemCacheableRepository<CatalogueUser> {
        CatalogueUserCacheableRepository(CatalogueUserRepository catalogueUserRepository) {
            super(catalogueUserRepository)
        }

        // not cached

        CatalogueUser readByEmailAddress(String emailAddress) {
            ((CatalogueUserRepository) repository).readByEmailAddress(emailAddress)
        }
    }

    @Singleton
    @CompileStatic
    @CacheConfig(keyGenerator = StringCacheKeyGenerator)
    static class ApiKeyCacheableRepository extends ItemCacheableRepository<ApiKey> {
        ApiKeyCacheableRepository(ApiKeyRepository apiKeyRepository) {
            super(apiKeyRepository)
        }

        // not cached

        List<ApiKey> readByCatalogueUserId(UUID catalogueUserId) {
            ((ApiKeyRepository) repository).readByCatalogueUserId(catalogueUserId)
        }
    }

    @Singleton
    @CompileStatic
    @CacheConfig(cacheNames = 'api-property-cache', keyGenerator = StringCacheKeyGenerator)
    static class ApiPropertyCacheableRepository extends ItemCacheableRepository<ApiProperty> {

        static final String FIND_ALL_PUBLIC_API_PROPERTIES = 'findAllPublicApiProperties'
        static final String FIND_ALL_API_PROPERTIES = 'findAllApiProperties'

        ApiPropertyCacheableRepository(ApiPropertyRepository apiPropertyRepository) {
            super(apiPropertyRepository)
        }

        @Override
        @CacheInvalidate(all = true) // clear the whole cache on any insert/update/delete
        void invalidateCachedLookupById(String lookup, String domainType, UUID id) {
            null
        }

        List<ApiProperty> cachedLookup(String lookup) {
            mutableCachedLookup(lookup).collect {it.clone()}
        }

        @Cacheable
        List<ApiProperty> mutableCachedLookup(String lookup) {
            switch (lookup) {
                case FIND_ALL_PUBLIC_API_PROPERTIES -> ((ApiPropertyRepository) repository).findByPubliclyVisibleTrue()
                case FIND_ALL_API_PROPERTIES -> ((ApiPropertyRepository) repository).findAll()

            }
        }

        List<ApiProperty> findAllByPubliclyVisibleTrue() {
            cachedLookup(FIND_ALL_PUBLIC_API_PROPERTIES)
        }

        List<ApiProperty> findAll() {
            cachedLookup(FIND_ALL_API_PROPERTIES)
        }

        @Cacheable
        List<ApiProperty> findByCategory(String category) {
            ((ApiPropertyRepository) repository).findByCategory(category)
        }

    }

    @Singleton
    @CompileStatic
    static class SummaryMetadataReportCacheableRepository extends ItemCacheableRepository<SummaryMetadataReport> {

        @Inject FacetCacheableRepository.SummaryMetadataCacheableRepository summaryMetadataCacheableRepository


        SummaryMetadataReportCacheableRepository(SummaryMetadataReportRepository summaryMetadataReportRepository) {
            super(summaryMetadataReportRepository)
        }

        Long delete(SummaryMetadataReport summaryMetadataReport, SummaryMetadata summaryMetadata) {
            Long deleted = repository.delete(summaryMetadataReport)
            invalidateChain(summaryMetadataReport, summaryMetadata)
            deleted
        }

        SummaryMetadataReport update(SummaryMetadataReport summaryMetadataReport, SummaryMetadata summaryMetadata) {
            SummaryMetadataReport updated = repository.update(summaryMetadataReport)
            invalidateChain(updated, summaryMetadata)
            updated
        }

        List<SummaryMetadataReport> saveAll(Iterable<SummaryMetadataReport> items) {
            List<SummaryMetadataReport> saved = repository.saveAll(items)
            items.each { invalidate(it) }
            saved
        }

        private void invalidateChain(SummaryMetadataReport summaryMetadataReport, SummaryMetadata summaryMetadata) {
            invalidate(summaryMetadataReport)
            summaryMetadataCacheableRepository.invalidate(summaryMetadata)
        }
    }

    @Singleton
    @CompileStatic
    static class RuleRepresentationCacheableRepository extends ItemCacheableRepository<RuleRepresentation> {

        @Inject FacetCacheableRepository.RuleCacheableRepository ruleCacheableRepository

        RuleRepresentationCacheableRepository(RuleRepresentationRepository ruleRepresentationRepository) {
            super(ruleRepresentationRepository)
        }

        @Override
        void invalidate(RuleRepresentation item) {
            super.invalidate(item)
            Rule rule = ruleCacheableRepository.readById(item.ruleId)
            ruleCacheableRepository.invalidate(rule)
        }
    }

    @CompileStatic
    @Singleton
    static class SubscribedCatalogueCacheableRepository extends ItemCacheableRepository<SubscribedCatalogue> {
        SubscribedCatalogueCacheableRepository(SubscribedCatalogueRepository subscribedCatalogueRepository) {
            super(subscribedCatalogueRepository)
        }

        List<SubscribedCatalogue> findAll(){
            ((SubscribedCatalogueRepository) repository).findAll()
        }
    }

    @CompileStatic
    @Singleton
    static class SubscribedModelCacheableRepository extends ItemCacheableRepository<SubscribedModel> {
        SubscribedModelCacheableRepository(SubscribedModelRepository subscribedModelRepository) {
            super(subscribedModelRepository)
        }
        @Nullable
        List<SubscribedModel> findAll(){
            ((SubscribedModelRepository) repository).findAll()
        }

        @Nullable
        List<SubscribedModel> findAllBySubscribedCatalogueId(UUID subscribedCatalogueId){
            ((SubscribedModelRepository) repository).findAllBySubscribedCatalogueId(subscribedCatalogueId)
        }

        @Nullable
        SubscribedModel findBySubscribedModelIdAndSubscribedCatalogueId(UUID id, SubscribedCatalogue subscribedCatalogue) {
            ((SubscribedModelRepository) repository).findByIdAndSubscribedCatalogueId(id, subscribedCatalogue.id)
        }

    }

    @CompileStatic
    @Singleton
    static class AuthorityCacheableRepository extends ItemCacheableRepository<Authority> {
        AuthorityCacheableRepository(AuthorityRepository authorityRepository) {
            super(authorityRepository)
        }
        @Nullable
        Authority findByDefaultAuthority(boolean defaultAuthority){
            ((AuthorityRepository) repository).findByDefaultAuthority(defaultAuthority)
        }
        @Nullable
        List<Authority> findAll(){
            ((AuthorityRepository) repository).findAll()
        }

        Authority findByLabel(String label) {
            ((AuthorityRepository) repository).findByLabel(label)
        }

    }
}
