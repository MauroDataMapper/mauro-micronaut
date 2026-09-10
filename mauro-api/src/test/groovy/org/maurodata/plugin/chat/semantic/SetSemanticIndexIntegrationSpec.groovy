package org.maurodata.plugin.chat.semantic

import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.maurodata.service.semantic.EmbeddingProfile
import spock.lang.Shared
import spock.lang.Specification

import java.sql.Connection
import java.security.MessageDigest

/** Runs real migrations, triggers, generation transactions and pgvector retrieval in an isolated container. */
class SetSemanticIndexIntegrationSpec extends Specification {
    @Shared HikariDataSource dataSource
    @Shared SemanticRepository repository
    @Shared SetSemanticIndexRepository indexes
    @Shared SetSemanticIndexingService indexing
    @Shared EmbeddingProfile profile
    @Shared UUID folderId = UUID.randomUUID()
    @Shared UUID existingDeclaration = UUID.randomUUID()
    static final String CORPUS = 'catalogue-items'

    void setupSpec() {
        dataSource = new HikariDataSource()
        dataSource.jdbcUrl = 'jdbc:tc:pgvector:pg16:///set_semantic_tests?TC_INITFUNCTION=org.maurodata.persistence.PgVectorTestDatabase::createVectorExtension'
        dataSource.driverClassName = 'org.testcontainers.jdbc.ContainerDatabaseDriver'
        dataSource.maximumPoolSize = 6
        Flyway.configure().dataSource(dataSource).locations('classpath:db/migration/core').schemas('core').load().migrate()
        Flyway.configure().dataSource(dataSource).locations('classpath:db/migration/chat_semantic').schemas('chat_semantic')
            .target('18').load().migrate()
        execute('''INSERT INTO core.folder (id, version, label, readable_by_everyone, readable_by_authenticated_users, finalised, deleted)
            VALUES (?, 0, 'Set index tests', true, false, false, false)''', [folderId])
        profile = new EmbeddingProfile(id: UUID.randomUUID(), name: 'set-integration', provider: 'local', embeddingModel: 'fixture-v1', dimension: 3, distanceMetric: 'cosine')
        execute('''INSERT INTO semantic.embedding_profile (id, name, provider, embedding_model, dimension) VALUES (?, ?, ?, ?, ?)''',
            [profile.id, profile.name, profile.provider, profile.embeddingModel, profile.dimension])
        execute('''INSERT INTO semantic.semantic_model_index (corpus_id, embedding_profile_id, mauro_model_id, status)
            SELECT id, ?, ?, 'READY' FROM semantic.semantic_corpus WHERE name = ?''', [profile.id, existingDeclaration, CORPUS])
        Flyway.configure().dataSource(dataSource).locations('classpath:db/migration/chat_semantic').schemas('chat_semantic').load().migrate()
        repository = new SemanticRepository(dataSource, 100, 100, 100, 30)
        indexes = new SetSemanticIndexRepository(dataSource)
        indexing = new SetSemanticIndexingService(repository, indexes, 8, 0.05D, 0.20D, false)
    }

    void cleanupSpec() { dataSource?.close() }

    void 'derived lifecycle migration does not mark ordinary indexes stale'() {
        expect:
        scalar('SELECT status FROM semantic.semantic_model_index WHERE mauro_model_id = ?', [existingDeclaration]) == 'READY'
    }

    void 'rebuild uses current meaning text and reports total eligible and represented members separately'() {
        given:
        UUID terminology = model('terminology.terminology')
        UUID complete = term(terminology, 'a meaningful definition')
        term(terminology, '')
        term(terminology, 'not yet embedded')
        embed(complete, terminology, 'a meaningful definition', [1F, 0F, 0F])
        when:
        def state = rebuild(terminology)
        def centroid = repository.setSemanticCentroids(profile, CORPUS, terminology).first()
        then:
        state.status == 'PARTIAL'
        !state.stale
        centroid.metadata.totalMemberCount == 3
        centroid.metadata.eligibleMemberCount == 2
        centroid.metadata.currentEmbeddingMemberCount == 1
        centroid.metadata.incomplete
        when: 'catalogue text changes without refreshing its stored chunk'
        execute('UPDATE terminology.term SET definition = ? WHERE id = ?', ['changed definition', complete])
        then:
        indexes.state(profile, CORPUS, terminology).stale
        repository.setSemanticCentroids(profile, CORPUS, terminology).size() == 1
        !repository.setSemanticMemberEmbeddings(profile, CORPUS, terminology).find { it.memberId == complete && it.vectorFamily == 'meaning' }.embedding
        when:
        rebuild(terminology)
        then:
        repository.setSemanticCentroids(profile, CORPUS, terminology).size() == 1
        indexes.state(profile, CORPUS, terminology).stale
        indexes.state(profile, CORPUS, terminology).metadata.retainedVectorCount == 1
        indexes.state(profile, CORPUS, terminology).metadata.sets.values().first().missingEmbeddingMemberCount == 2
    }

    void 'term embedding changes invalidate dependent codesets and rebuild without embedding generation'() {
        given:
        UUID terminology = model('terminology.terminology')
        UUID codeset = model('terminology.code_set')
        UUID member = term(terminology, 'shared meaning')
        execute('INSERT INTO terminology.code_set_term (term_id, code_set_id) VALUES (?, ?)', [member, codeset])
        UUID chunk = embed(member, terminology, 'shared meaning', [1F, 0F, 0F])
        rebuild(terminology)
        rebuild(codeset)
        when:
        execute("UPDATE semantic.semantic_embedding SET embedding = '[0,1,0]'::vector WHERE chunk_id = ?", [chunk])
        then:
        indexes.state(profile, CORPUS, terminology).stale
        indexes.state(profile, CORPUS, codeset).stale
        when:
        rebuild(codeset)
        then:
        repository.setSemanticCentroids(profile, CORPUS, codeset).first().embedding.toList() == [0F, 1F, 0F]
        when:
        execute('DELETE FROM semantic.semantic_chunk WHERE id = ?', [chunk])
        then:
        indexes.state(profile, CORPUS, codeset).stale
        when:
        rebuild(codeset)
        then:
        indexes.state(profile, CORPUS, codeset).status == 'PARTIAL'
        repository.setSemanticCentroids(profile, CORPUS, codeset).size() == 1
        indexes.state(profile, CORPUS, codeset).stale
    }

    void 'failed publication rolls back deletion and changes during generation queue a followup'() {
        given:
        UUID terminology = model('terminology.terminology')
        UUID member = term(terminology, 'retained meaning')
        embed(member, terminology, 'retained meaning', [1F, 0F, 0F])
        rebuild(terminology)
        def original = repository.setSemanticCentroids(profile, CORPUS, terminology)
        indexes.request(profile, CORPUS, terminology)
        def job = indexes.claim(profile, CORPUS, terminology)
        def invalid = new SetSemanticCentroid(setId: terminology, setDomainType: 'Terminology', mauroModelId: terminology,
            vectorFamily: 'meaning', centroidKind: 'overall', memberCount: 0, sourceMemberCount: 1,
            sourceFingerprint: 'bad', embedding: [0F, 1F, 0F] as float[])
        when:
        indexes.publish(job, [invalid], [:], false)
        then:
        thrown(Exception)
        repository.setSemanticCentroids(profile, CORPUS, terminology).first().embedding.toList() == original.first().embedding.toList()
        when:
        indexes.failed(job, new IllegalStateException('fixture failure'))
        then:
        indexes.state(profile, CORPUS, terminology).status == 'FAILED'
        indexes.state(profile, CORPUS, terminology).last_error == 'fixture failure'
        when:
        indexes.request(profile, CORPUS, terminology)
        job = indexes.claim(profile, CORPUS, terminology)
        execute('UPDATE terminology.term SET definition = ? WHERE id = ?', ['new text', member])
        indexes.publish(job, original, [:], false)
        then:
        indexes.state(profile, CORPUS, terminology).status == 'QUEUED'
        indexes.state(profile, CORPUS, terminology).stale
    }

    void 'owner deletion removes centroids and enumeration owners remain DataTypes'() {
        given:
        UUID dataModel = model('datamodel.data_model')
        UUID type = UUID.randomUUID()
        UUID value = UUID.randomUUID()
        execute("INSERT INTO datamodel.data_type (id, version, domain_type, data_model_id, label) VALUES (?, 0, 'EnumerationType', ?, 'local values')", [type, dataModel])
        execute("INSERT INTO datamodel.enumeration_value (id, version, enumeration_type_id, label, key, value) VALUES (?, 0, ?, 'local', 'a', 'local meaning')", [value, type])
        UUID chunk = embed(value, dataModel, 'local meaning', [1F, 0F, 0F], 'EnumerationValue')
        when:
        rebuild(dataModel)
        then:
        repository.setSemanticCentroids(profile, CORPUS, type).first().setDomainType == 'DataType'
        indexes.resolveSource('DataType', type) == [id: type, domainType: 'DataType']
        when:
        execute('DELETE FROM datamodel.enumeration_value WHERE id = ?', [value])
        execute('DELETE FROM datamodel.data_type WHERE id = ?', [type])
        then:
        repository.setSemanticCentroids(profile, CORPUS, type).empty
        indexes.state(profile, CORPUS, dataModel).stale
    }

    void 'ANN retrieval finds a small codeset against a local region of a broad terminology'() {
        given:
        UUID terminology = model('terminology.terminology')
        UUID codeset = model('terminology.code_set')
        UUID isolated = term(terminology, 'isolated region')
        embed(isolated, terminology, 'isolated region', [0F, 1F, 0F])
        execute('INSERT INTO terminology.code_set_term (term_id, code_set_id) VALUES (?, ?)', [isolated, codeset])
        15.times { int n ->
            UUID member = term(terminology, "broad term $n")
            embed(member, terminology, "broad term $n", [1F, 0F, 0F])
        }
        rebuild(terminology)
        rebuild(codeset)
        when:
        def source = repository.setSemanticCentroids(profile, CORPUS, codeset)
        def hits = repository.searchSetSemanticCandidates(profile, CORPUS, source, 100)
        then:
        hits.find { it.targetSetId == terminology && it.targetCentroidKind == 'region' && it.similarity > 0.999D }
        !hits.any { it.targetSetId == codeset }
        indexes.state(profile, CORPUS, terminology).status == 'READY'
        when:
        def overall = repository.overallSetSemanticCandidates(profile, CORPUS, source, [terminology])
        then:
        overall.size() == 1
        overall.first().targetSetId == terminology
        overall.first().targetCentroidKind == 'overall'
        overall.first().sourceCentroidKind == 'overall'
        Math.abs(overall.first().similarity - 1D / Math.sqrt(226D)) < 0.00001D
    }

    void 'scoped projections follow external shared sets and deduplicate all ancestors before paging'() {
        given:
        UUID terminology = model('terminology.terminology')
        UUID member = term(terminology, 'shared meaning')
        embed(member, terminology, 'shared meaning', [1F, 0F, 0F])
        rebuild(terminology)
        UUID dictionary = model('datamodel.data_model')
        UUID outside = model('datamodel.data_model')
        UUID parent = UUID.randomUUID()
        UUID child = UUID.randomUUID()
        UUID outsideClass = UUID.randomUUID()
        for (def row : [[parent, dictionary, null], [child, dictionary, parent], [outsideClass, outside, null]]) {
            execute('INSERT INTO datamodel.data_class (id, version, label, data_model_id, parent_data_class_id) VALUES (?, 0, ?, ?, ?)',
                [row[0], row[0].toString(), row[1], row[2]])
        }
        UUID type = UUID.randomUUID()
        execute("INSERT INTO datamodel.data_type (id, version, label, domain_type, data_model_id, model_resource_domain_type, model_resource_id) VALUES (?, 0, 'reference', 'ModelDataType', ?, 'Terminology', ?)", [type, dictionary, terminology])
        UUID source = UUID.randomUUID()
        UUID destination = UUID.randomUUID()
        UUID hidden = UUID.randomUUID()
        for (UUID id : [source, destination, hidden]) {
            execute('INSERT INTO datamodel.data_element (id, version, label, data_class_id, data_type_id) VALUES (?, 0, ?, ?, ?)',
                [id, id.toString(), child, type])
        }
        def service = new SetSemanticSearchService(repository, indexes, new EmbeddingProviderRegistry([]), 100)
        def readable = { String domainType, UUID id -> id != terminology && id != hidden } as java.util.function.BiPredicate<String, UUID>
        def request = new SetSemanticSearchRequest(diagnostics: true, corpus: 'catalogue-items', embeddingProfile: profile.name, withinModelId: dictionary, domainTypes: ['DataElement'], max: 1)
        when:
        def result = service.candidates(request, 'DataElement', source, readable)
        then:
        result instanceof org.maurodata.web.ListResponse
        result.items*.id == [destination]
        result.items.first().overallSimilarity > 0.999D
        !result.items.first().supportingSets.first().containsKey('id')
        !result.source.containsKey('set')
        !result.countIsExact
        when:
        request.domainTypes = ['DataClass']
        def classes = service.candidates(request, 'DataType', type, readable)
        request.offset = 1
        def next = service.candidates(request, 'DataType', type, readable)
        then:
        (classes.items + next.items)*.id.toSet() == [parent, child].toSet()
        classes.items.first().supportingSets.size() == 1
        when:
        request.offset = 0
        request.domainTypes = ['DataModel']
        def models = service.candidates(request, 'DataElement', source, readable)
        then:
        models.items*.id == [dictionary]
        when:
        UUID nestedFolder = UUID.randomUUID()
        execute("INSERT INTO core.folder (id, version, label, parent_folder_id, readable_by_everyone, readable_by_authenticated_users, finalised, deleted) VALUES (?, 0, 'nested scope', ?, true, false, false, false)", [nestedFolder, folderId])
        execute('UPDATE datamodel.data_model SET folder_id = ? WHERE id = ?', [nestedFolder, dictionary])
        request.withinModelId = folderId
        def folderResults = service.candidates(request, 'DataElement', source, readable)
        then:
        folderResults.items*.id == [dictionary]
        when:
        request.withinModelId = outside
        def empty = service.candidates(request, 'DataElement', source, readable)
        then:
        empty.items.empty
        when:
        def resolved = indexes.resolveSource('DataType', type)
        def direct = indexes.destinations(profile, CORPUS, dictionary, ['Terminology'])
        then:
        resolved == [id: terminology, domainType: 'Terminology']
        direct.empty // The external set is not itself contained by the dictionary.
    }

    void 'omission resolves only established enabled public set index dimensions'() {
        given:
        UUID terminology = model('terminology.terminology')
        UUID member = term(terminology, 'dimension availability')
        embed(member, terminology, 'dimension availability', [1F, 0F, 0F])
        rebuild(terminology)
        expect:
        indexes.discoveryDimensions(null, null).contains([profile: profile.name, corpus: CORPUS])
        indexes.discoveryDimensions(profile.name, CORPUS) == [[profile: profile.name, corpus: CORPUS]]
        indexes.discoveryDimensions('not-established', null).empty
        when:
        execute('UPDATE semantic.embedding_profile SET enabled = false WHERE id = ?', [profile.id])
        then:
        indexes.discoveryDimensions(profile.name, null).empty
        cleanup:
        execute('UPDATE semantic.embedding_profile SET enabled = true WHERE id = ?', [profile.id])
    }

    void 'freshness checks do not load member embeddings and interrupted claims recover'() {
        given:
        UUID terminology = model('terminology.terminology')
        indexes.declare(profile, CORPUS, terminology, indexing.configurationFingerprint(profile))
        when:
        indexes.claim(profile, CORPUS, terminology)
        indexes.recoverInterrupted()
        then:
        indexes.state(profile, CORPUS, terminology).status == 'QUEUED'
        indexing.needsRefresh(profile, CORPUS, terminology)
    }

    void 'recovery leaves live workers alone and profile spaces cannot silently change'() {
        given:
        UUID terminology = model('terminology.terminology')
        UUID member = term(terminology, 'stable space')
        embed(member, terminology, 'stable space', [1F, 0F, 0F])
        rebuild(terminology)
        indexes.request(profile, CORPUS, terminology)
        indexes.claim(profile, CORPUS, terminology)
        when:
        def live = indexes.withScopeLock(profile, CORPUS, terminology, {
            indexes.recoverInterrupted()
            indexes.state(profile, CORPUS, terminology)
        })
        then:
        live.status == 'RUNNING'
        when:
        execute('UPDATE semantic.embedding_profile SET embedding_model = ? WHERE id = ?', ['incompatible-new-model', profile.id])
        then:
        thrown(java.sql.SQLException)
        repository.findProfileByName(profile.name).embeddingModel == 'fixture-v1'
    }

    void 'tied set vectors have stable membership before the retrieval limit'() {
        given:
        List<UUID> owners = (1..8).collect {
            UUID owner = model('terminology.terminology')
            execute("UPDATE terminology.terminology SET label = 'Tied candidate' WHERE id = ?", [owner])
            UUID member = term(owner, 'identical candidate meaning')
            embed(member, owner, 'identical candidate meaning', [0F, 0F, -1F])
            rebuild(owner)
            owner
        }
        def sources = repository.setSemanticCentroids(profile, CORPUS, owners.first())
        List<UUID> expected = owners.sort(false) { it.toString() }.take(3)

        expect: 'tied owners are selected by identity, independently of eligibility input order'
        (1..6).every { int attempt ->
            def eligible = attempt % 2 ? owners : owners.reverse(false)
            def hits = repository.scopedSetSemanticCandidates(profile, CORPUS, sources, 3, eligible)
            hits*.targetSetId == expected
        }
        and: 'the natural set path uses the same ordering while excluding the source'
        repository.searchSetSemanticCandidates(profile, CORPUS, sources, 3)*.targetSetId ==
            owners.findAll { it != owners.first() }.sort { it.toString() }.take(3)
    }

    void 'limited tied retrieval is independent of database scan strategy'() {
        given:
        UUID owner = model('terminology.terminology')
        UUID member = term(owner, 'scan strategy fixture')
        embed(member, owner, 'scan strategy fixture', [0F, 0F, -1F])
        rebuild(owner)
        execute('''INSERT INTO semantic.set_semantic_embedding
            (corpus_id, embedding_profile_id, set_id, set_domain_type, set_label, mauro_model_id,
             vector_family, centroid_kind, region_ordinal, member_count, source_member_count,
             source_fingerprint, embedding, metadata)
            SELECT corpus_id, embedding_profile_id, md5(? || n::text)::uuid, set_domain_type,
                   'Scan strategy fixture', mauro_model_id, vector_family, centroid_kind, region_ordinal,
                   member_count, source_member_count, source_fingerprint, embedding, metadata
            FROM semantic.set_semantic_embedding CROSS JOIN generate_series(1, 240) n
            WHERE set_id = ?''', [owner.toString(), owner])
        def sources = repository.setSemanticCentroids(profile, CORPUS, owner)
        Map<String, List<UUID>> results = [:]

        when:
        ['sequential', 'index'].each { String strategy ->
            Connection connection = dataSource.connection
            connection.autoCommit = false
            try (def statement = connection.createStatement()) {
                statement.execute("SET LOCAL enable_seqscan = ${strategy == 'sequential' ? 'on' : 'off'}")
                statement.execute("SET LOCAL enable_indexscan = ${strategy == 'index' ? 'on' : 'off'}")
                statement.execute('SET LOCAL enable_bitmapscan = off')
            }
            javax.sql.DataSource single = Stub(javax.sql.DataSource) {
                getConnection() >> connection
            }
            def isolatedRepository = new SemanticRepository(single, 100, 100, 100, 30)
            results[strategy] = isolatedRepository.searchSetSemanticCandidates(profile, CORPUS, sources, 100)*.targetSetId
        }

        then:
        results.sequential.size() == 100
        results.index == results.sequential

        cleanup:
        execute('DELETE FROM semantic.set_semantic_embedding WHERE mauro_model_id = ?', [owner])
    }

    private static Map planState(Connection connection, String sql) {
        Map state = [:]
        connection.createStatement().withCloseable { statement ->
            statement.executeQuery('SHOW plan_cache_mode').withCloseable { rs ->
                rs.next(); state.plan_cache_mode = rs.getString(1)
            }
        }
        connection.prepareStatement('SELECT generic_plans, custom_plans FROM pg_prepared_statements WHERE statement = ?').withCloseable { statement ->
            statement.setString(1, sql.replace('?', '$1'))
            statement.executeQuery().withCloseable { rs ->
                if (rs.next()) state.prepared = [genericPlans: rs.getLong(1), customPlans: rs.getLong(2)]
            }
        }
        state
    }

    void 'candidate plan scope forces custom execution and restores caller settings'() {
        expect:
        [true, false].every { boolean autoCommit ->
            try (Connection connection = dataSource.connection) {
                connection.autoCommit = autoCommit
                try (def setting = connection.createStatement()) { setting.execute('SET plan_cache_mode = force_generic_plan') }
                String sql = 'SELECT ?::integer AS scoped_plan_fixture'
                Map observed
                new SetCandidatePlanScope(connection).withCloseable {
                    try (def statement = connection.prepareStatement(sql)) {
                        statement.unwrap(Class.forName('org.postgresql.PGStatement')).setPrepareThreshold(1)
                        12.times { int n ->
                            statement.setInt(1, n)
                            try (def rs = statement.executeQuery()) { assert rs.next() }
                        }
                        observed = planState(connection, sql)
                    }
                }
                assert observed.plan_cache_mode == 'force_custom_plan'
                assert observed.prepared.customPlans >= 12
                assert observed.prepared.genericPlans == 0
                assert planState(connection, sql).plan_cache_mode == 'force_generic_plan'
                try {
                    new SetCandidatePlanScope(connection).withCloseable {
                        try (def failing = connection.createStatement()) { failing.execute('SELECT 1 / 0') }
                    }
                    assert false
                } catch (java.sql.SQLException expected) {
                    assert planState(connection, sql).plan_cache_mode == 'force_generic_plan'
                }
                if (!autoCommit) connection.rollback()
                try (def setting = connection.createStatement()) { setting.execute('SET plan_cache_mode = auto') }
                if (!autoCommit) connection.commit()
            }
            true
        }
    }

    void 'distance-only set lookup can use the HNSW index'() {
        given:
        UUID terminology = model('terminology.terminology')
        UUID member = term(terminology, 'indexed region')
        embed(member, terminology, 'indexed region', [1F, 0F, 0F])
        rebuild(terminology)
        List<String> plan = []
        when:
        try (Connection connection = dataSource.connection) {
            connection.autoCommit = false
            try (def setting = connection.createStatement()) { setting.execute('SET LOCAL enable_seqscan = off') }
            try (def statement = connection.prepareStatement("""
                EXPLAIN SELECT sse.set_id FROM semantic.set_semantic_embedding sse
                JOIN semantic.semantic_corpus corpus ON corpus.id = sse.corpus_id
                WHERE corpus.name = ? AND sse.embedding_profile_id = ? AND sse.vector_family = 'meaning'
                  AND NOT (sse.set_id = ? AND sse.set_domain_type = 'CodeSet')
                ORDER BY sse.embedding::vector(3) <=> '[1,0,0]'::vector(3) LIMIT 20
            """)) {
                statement.setString(1, CORPUS)
                statement.setObject(2, profile.id)
                statement.setObject(3, UUID.randomUUID())
                try (def rs = statement.executeQuery()) { while (rs.next()) plan.add(rs.getString(1)) }
            }
            connection.rollback()
        }
        then:
        plan.any { it.contains('hnsw_idx') }
    }

    void 'empty eligible sets report a current generation and cancelling sets preserve each seed identity'() {
        given:
        UUID empty = model('terminology.terminology')
        term(empty, '')
        rebuild(empty)
        def search = new SetSemanticSearchService(repository, indexes, new EmbeddingProviderRegistry([]), 100)
        when:
        def result = search.candidates(new SetSemanticSearchRequest(diagnostics: true, corpus: 'catalogue-items', embeddingProfile: profile.name), 'Terminology', empty,
            { String type, UUID id -> true } as java.util.function.BiPredicate<String, UUID>)
        then:
        result.items.empty
        !result.source.searchable
        result.source.index.status == 'READY'
        !result.source.index.stale
        result.source.index.generation.eligibleMemberCount == 0
        when:
        UUID cancelling = model('terminology.terminology')
        UUID left = term(cancelling, 'left')
        UUID right = term(cancelling, 'right')
        embed(left, cancelling, 'left', [1F, 0F, 0F])
        embed(right, cancelling, 'right', [-1F, 0F, 0F])
        rebuild(cancelling)
        def regions = repository.setSemanticCentroids(profile, CORPUS, cancelling)
        then:
        regions.size() == 2
        regions*.metadata*.seedMemberId.toSet() == [left.toString(), right.toString()].toSet()
        !indexes.state(profile, CORPUS, cancelling).metadata.sets.values().first().containsKey('seedMemberId')
    }

    void 'derived repository works with the framework contextual datasource outside a connection scope'() {
        given:
        // Load the real connection/AOP beans, without starting Mauro or its schedulers.
        def context = new ConnectionTestContext()
        context.registerSingleton(javax.sql.DataSource, dataSource, io.micronaut.inject.qualifiers.Qualifiers.byName('default'))
        context.start()
        def qualifier = io.micronaut.inject.qualifiers.Qualifiers.byName('default')
        def event = new io.micronaut.context.event.BeanCreatedEvent(context,
            context.getBeanDefinition(javax.sql.DataSource, qualifier),
            io.micronaut.inject.BeanIdentifier.of('default'), dataSource)
        javax.sql.DataSource contextual = new io.micronaut.data.connection.jdbc.advice.ContextualAwareDataSource(context).onCreated(event)
        assert contextual instanceof io.micronaut.data.connection.jdbc.advice.DelegatingDataSource
        def managedIndexes = new SetSemanticIndexRepository(contextual)
        when:
        contextual.connection.prepareStatement('SELECT 1')
        then:
        thrown(io.micronaut.data.connection.exceptions.NoConnectionException)
        when:
        managedIndexes.recoverInterrupted()
        then:
        noExceptionThrown()
        cleanup:
        context.close()
    }

    void 'cancelled catalogue jobs cannot be overwritten by late worker outcomes'() {
        given:
        UUID job = repository.createIndexJob(existingDeclaration, profile.name, CORPUS, false, null, null)
        repository.updateJobStatus(job, 'RUNNING', [stage: 'preparation'])
        repository.cancelJob(job, 'shutdown')
        Object completed = scalar('SELECT completed_at FROM semantic.semantic_index_job WHERE id = ?', [job])
        expect:
        !repository.updateJobStatus(job, 'RUNNING', [stage: 'late progress'])
        !repository.updateJobStatus(job, 'FAILED', null, 'pool closed')
        !repository.updateJobStatus(job, 'SUCCEEDED', [stage: 'late completion'])
        scalar('SELECT status FROM semantic.semantic_index_job WHERE id = ?', [job]) == 'CANCELLED'
        scalar('SELECT completed_at FROM semantic.semantic_index_job WHERE id = ?', [job]) == completed
    }

    @groovy.transform.CompileStatic
    private static class ConnectionTestContext extends io.micronaut.context.DefaultApplicationContext {
        ConnectionTestContext() { super('test') }

        @Override
        protected List<io.micronaut.inject.BeanDefinitionReference> resolveBeanDefinitionReferences() {
            super.resolveBeanDefinitionReferences().findAll { io.micronaut.inject.BeanDefinitionReference reference ->
                String name = reference.getBeanDefinitionName()
                name.startsWith('io.micronaut.data.connection.') ||
                    name.startsWith('io.micronaut.aop.') ||
                    name.startsWith('io.micronaut.context.')
            }
        }
    }

    private Map rebuild(UUID owner) {
        indexes.declare(profile, CORPUS, owner, indexing.configurationFingerprint(profile))
        indexes.request(profile, CORPUS, owner)
        indexing.rebuildSetIndexes(profile, CORPUS, owner)
    }

    private UUID model(String table) {
        UUID id = UUID.randomUUID()
        execute("""INSERT INTO $table (id, version, label, readable_by_everyone, readable_by_authenticated_users, finalised, deleted, model_type, folder_id)
            VALUES (?, 0, ?, true, false, false, false, 'test', ?)""".toString(), [id, id.toString(), folderId])
        id
    }

    private UUID term(UUID owner, String meaning) {
        UUID id = UUID.randomUUID()
        execute('INSERT INTO terminology.term (id, version, terminology_id, label, code, definition) VALUES (?, 0, ?, ?, ?, ?)',
            [id, owner, id.toString(), id.toString(), meaning])
        id
    }

    private UUID embed(UUID member, UUID model, String text, List<Float> vector, String type = 'Term') {
        UUID chunk = UUID.randomUUID()
        String hash = MessageDigest.getInstance('SHA-256').digest(text.getBytes('UTF-8')).encodeHex().toString()
        // A deduplicated label chunk containing the exact definition must be reusable too.
        execute('''INSERT INTO semantic.semantic_chunk (id, corpus_id, source_type, source_id, source_domain_type, mauro_model_id,
            chunk_kind, chunk_ordinal, source_text, content_hash, chunk_group)
            SELECT ?, id, 'catalogue-item', ?, ?, ?, 'label', 0, ?, ?, 'catalogue' FROM semantic.semantic_corpus WHERE name = ?''',
            [chunk, member, type, model, text, hash, CORPUS])
        execute('INSERT INTO semantic.semantic_embedding (chunk_id, embedding_profile_id, content_hash, embedding, chunk_group) VALUES (?, ?, ?, ?::vector, ?)',
            [chunk, profile.id, hash, '[' + vector.join(',') + ']', 'catalogue'])
        chunk
    }

    private void execute(String sql, List values = []) {
        try (Connection connection = dataSource.connection; def statement = connection.prepareStatement(sql)) {
            values.eachWithIndex { value, int i -> statement.setObject(i + 1, value) }
            statement.executeUpdate()
        }
    }

    private Object scalar(String sql, List values = []) {
        try (Connection connection = dataSource.connection; def statement = connection.prepareStatement(sql)) {
            values.eachWithIndex { value, int i -> statement.setObject(i + 1, value) }
            try (def rs = statement.executeQuery()) { rs.next(); rs.getObject(1) }
        }
    }
}
