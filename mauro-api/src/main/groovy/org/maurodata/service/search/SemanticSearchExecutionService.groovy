package org.maurodata.service.search

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.data.connection.annotation.Connectable
import jakarta.inject.Singleton
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.search.dto.SemanticChunkMatchDTO
import org.maurodata.domain.search.dto.SemanticSearchRequestDTO
import org.maurodata.domain.search.dto.SemanticSearchResultsDTO
import org.maurodata.domain.security.Role
import org.maurodata.persistence.model.PathRepository
import org.maurodata.security.AccessControlService
import org.maurodata.service.semantic.EmbeddingProfile
import org.maurodata.service.semantic.EmbeddingProvider
import org.maurodata.service.semantic.EmbeddingProviderRegistry
import org.maurodata.service.semantic.SemanticCandidate
import org.maurodata.service.semantic.SemanticIndexingService
import org.maurodata.service.semantic.SemanticRepository
import org.maurodata.web.ListResponse

import java.util.function.BiFunction

@Slf4j
@CompileStatic
@Singleton
class SemanticSearchExecutionService implements SemanticSearchService {

    private final SemanticRepository semanticRepository
    private final EmbeddingProviderRegistry embeddingProviderRegistry
    private final SemanticIndexingService semanticIndexingService
    private final AccessControlService accessControlService
    private final PathRepository pathRepository

    SemanticSearchExecutionService(SemanticRepository semanticRepository,
                                   EmbeddingProviderRegistry embeddingProviderRegistry,
                                   SemanticIndexingService semanticIndexingService,
                                   AccessControlService accessControlService,
                                   PathRepository pathRepository) {
        this.semanticRepository = semanticRepository
        this.embeddingProviderRegistry = embeddingProviderRegistry
        this.semanticIndexingService = semanticIndexingService
        this.accessControlService = accessControlService
        this.pathRepository = pathRepository
    }

    @Connectable
    @Override
    ListResponse<SemanticSearchResultsDTO> executeSearch(SemanticSearchRequestDTO request,
                                                         BiFunction<String, UUID, AdministeredItem> itemLookup) {
        SemanticSearchRequestDTO safeRequest = request ?: new SemanticSearchRequestDTO()
        String queryText = queryText(safeRequest)
        if (queryText == null || queryText.trim().isEmpty()) {
            return ListResponse.from([] as List<SemanticSearchResultsDTO>, safeRequest)
        }

        String indexName = safeRequest.indexName ?: 'catalogue-items-default'
        if (Boolean.TRUE.equals(safeRequest.rebuildIfEmpty) && !semanticIndexingService.hasEmbeddings(indexName)) {
            semanticIndexingService.rebuildCatalogueIndex(indexName, safeRequest.corpus ?: 'catalogue-items', safeRequest.domainTypes, safeRequest.withinModelId)
        }

        List<EmbeddingProfile> profiles = profiles(safeRequest)
        int topN = Math.max(1, Math.min(safeRequest.topN ?: 50, 500))
        int topM = Math.max(1, Math.min(safeRequest.topM ?: safeRequest.max ?: 10, 100))
        List<SemanticCandidate> candidates = new ArrayList<SemanticCandidate>()
        for (EmbeddingProfile profile : profiles) {
            EmbeddingProvider provider = embeddingProviderRegistry.providerFor(profile)
            float[] queryEmbedding = provider.embed(profile, [queryText] as List<String>).first()
            candidates.addAll(semanticRepository.search(profile, queryEmbedding, safeRequest.corpus ?: 'catalogue-items', safeRequest.domainTypes, safeRequest.withinModelId, topN))
        }

        List<SemanticSearchResultsDTO> ranked = rerank(candidates, safeRequest.includeChunks != false)
        List<SemanticSearchResultsDTO> readable = ranked.findAll {SemanticSearchResultsDTO result ->
            AdministeredItem item = itemLookup.apply(result.domainType, result.id)
            if (!accessControlService.canDoRole(Role.READER, item)) {
                return false
            }
            pathRepository.readParentItems(item)
            item.updateBreadcrumbs()
            result.breadcrumbs = item.breadcrumbs
            result.classifiers = item.classifiers
            true
        } as List<SemanticSearchResultsDTO>

        int offset = safeRequest.offset ?: 0
        int max = safeRequest.max != null && safeRequest.max > 0 ? safeRequest.max : topM
        List<SemanticSearchResultsDTO> page = readable.drop(offset).take(max)
        new ListResponse<SemanticSearchResultsDTO>(
            count: readable.size(),
            items: page
        )
    }

    @Connectable
    @Override
    SemanticSearchAvailability availability(String indexName) {
        try {
            String safeIndexName = indexName ?: 'catalogue-items-default'
            if (!semanticIndexingService.hasEmbeddings(safeIndexName)) {
                return SemanticSearchAvailability.unavailable("no semantic embeddings are available for ${safeIndexName}".toString())
            }
            List<EmbeddingProfile> profiles = semanticRepository.profilesForIndex(safeIndexName)
            if (profiles.isEmpty()) {
                return SemanticSearchAvailability.unavailable("no enabled embedding profiles are linked to ${safeIndexName}".toString())
            }
            boolean meaningful = profiles.any {EmbeddingProfile profile -> meaningfulSemanticProfile(profile)}
            meaningful ?
                SemanticSearchAvailability.available() :
                SemanticSearchAvailability.unavailable('only test-only semantic profiles are active')
        } catch (Exception e) {
            log.debug('Semantic search is not available', e)
            SemanticSearchAvailability.unavailable(e.message ?: e.class.simpleName)
        }
    }

    private List<EmbeddingProfile> profiles(SemanticSearchRequestDTO request) {
        if (request.embeddingProfiles != null && !request.embeddingProfiles.isEmpty()) {
            return request.embeddingProfiles.collect {String name -> semanticRepository.findProfileByName(name)}
                .findAll {EmbeddingProfile profile -> profile != null} as List<EmbeddingProfile>
        }
        semanticRepository.profilesForIndex(request.indexName ?: 'catalogue-items-default')
    }

    private static String queryText(SemanticSearchRequestDTO request) {
        request.query ?: request.searchTerm
    }

    private static boolean meaningfulSemanticProfile(EmbeddingProfile profile) {
        if (profile == null) {
            return false
        }
        String provider = profile.provider ?: ''
        String name = profile.name ?: ''
        !(provider.equalsIgnoreCase('test') || name.startsWith('test-'))
    }

    private static List<SemanticSearchResultsDTO> rerank(List<SemanticCandidate> candidates, boolean includeChunks) {
        Map<String, List<SemanticCandidate>> grouped = candidates.groupBy {SemanticCandidate candidate ->
            "${candidate.sourceDomainType}:${candidate.sourceId}".toString()
        } as Map<String, List<SemanticCandidate>>
        List<SemanticSearchResultsDTO> results = new ArrayList<SemanticSearchResultsDTO>()
        for (List<SemanticCandidate> group : grouped.values()) {
            List<SemanticCandidate> sorted = group.sort {SemanticCandidate left, SemanticCandidate right ->
                (right.similarity ?: 0D) <=> (left.similarity ?: 0D)
            } as List<SemanticCandidate>
            SemanticCandidate best = sorted.first()
            double semanticScore = sorted.collect {SemanticCandidate candidate -> candidate.similarity ?: 0D}.max() ?: 0D
            double rerankScore = semanticScore + Math.min(sorted.size(), 5) * 0.01D
            SemanticSearchResultsDTO dto = new SemanticSearchResultsDTO(
                id: best.sourceId,
                domainType: best.sourceDomainType,
                label: best.sourceLabel,
                description: best.description,
                dateCreated: best.dateCreated,
                lastUpdated: best.lastUpdated,
                semanticScore: semanticScore,
                rerankScore: rerankScore,
                matchedChunkCount: sorted.size(),
                embeddingProfiles: sorted.collect {SemanticCandidate candidate -> candidate.embeddingProfile}.unique() as List<String>
            )
            if (includeChunks) {
                dto.chunks = sorted.take(5).collect {SemanticCandidate candidate ->
                    new SemanticChunkMatchDTO(
                        chunkId: candidate.chunkId,
                        chunkKind: candidate.chunkKind,
                        chunkOrdinal: candidate.chunkOrdinal,
                        sourceText: candidate.sourceText,
                        embeddingProfile: candidate.embeddingProfile,
                        distance: candidate.distance,
                        similarity: candidate.similarity
                    )
                } as List<SemanticChunkMatchDTO>
            }
            results.add(dto)
        }
        results.sort {SemanticSearchResultsDTO left, SemanticSearchResultsDTO right ->
            (right.rerankScore ?: 0D) <=> (left.rerankScore ?: 0D)
        } as List<SemanticSearchResultsDTO>
    }
}
