package org.maurodata.plugin.chat.semantic

import spock.lang.Specification

class SetSemanticCentroidCalculatorSpec extends Specification {
    SetSemanticCentroidCalculator calculator = new SetSemanticCentroidCalculator()
    UUID setId = UUID.randomUUID()

    void 'normalises members before averaging and leaves source vectors untouched'() {
        given:
        def members = [member(1, [10F, 0F]), member(2, [0F, 1F])]
        when:
        def centroids = calculator.calculate(members, 8, 0.4D, 0.2D)
        then:
        centroids.size() == 1
        Math.abs(centroids.first().embedding[0] - Math.sqrt(0.5D)) < 1E-6
        members.first().embedding[0] == 10F
        members*.memberId == [new UUID(0, 1), new UUID(0, 2)]
    }

    void 'compact large sets need no regions while small broad sets do'() {
        expect:
        calculator.calculate((1..100).collect { member(it, [1F, 0F]) }, 8, 0.05D, 0.2D).size() == 1
        def broad = calculator.calculate([member(1, [1F, 0F]), member(2, [0F, 1F])], 8, 0.05D, 0.2D)
        broad.count { it.centroidKind == 'region' } == 2
        broad.every { it.metadata.uncoveredMemberCount == 0 }
        broad.every { it.residualRadius < 1E-6 }
    }

    void 'isolated members remain in the overall mean and receive singleton regions'() {
        given:
        def members = (1..30).collect { member(it, [1F, 0F]) } + member(31, [0F, 1F])
        when:
        def result = calculator.calculate(members, 8, 0.05D, 0.2D)
        then:
        result.find { it.centroidKind == 'overall' }.memberCount == 31
        result.find { it.centroidKind == 'overall' }.embedding[1] > 0F
        def singleton = result.find { it.centroidKind == 'region' }
        singleton.memberCount == 1
        singleton.embedding.toList() == [0F, 1F]
        singleton.metadata.uncoveredMemberCount == 0
    }

    void 'region cap reports the actual uncovered distances rather than promising full coverage'() {
        given:
        def members = [member(1, [1F, 0F, 0F]), member(2, [0F, 1F, 0F]), member(3, [0F, 0F, 1F])]
        when:
        def result = calculator.calculate(members, 1, 0.01D, 0.1D)
        then:
        result.count { it.centroidKind == 'region' } == 1
        result.every { it.metadata.stoppingReason == 'region-cap' }
        result.every { it.metadata.uncoveredMemberCount == 2 }
        result.every { Math.abs(it.residualRadius - (1D - 1D / Math.sqrt(3D))) < 1E-6 }
    }

    void 'cancelling means never publish zero vectors and use local coverage instead'() {
        when:
        def result = calculator.calculate([member(1, [1F, 0F]), member(2, [-1F, 0F])], 4, 0.05D, 0.2D)
        then:
        result.size() == 2
        result.every { it.centroidKind == 'region' && !it.metadata.overallAvailable }
        result.every { SetSemanticCentroidCalculator.isUsable(it.embedding) }
        result.every { it.metadata.uncoveredMemberCount == 0 }
    }

    void 'rejects mixed dimensions and excludes unusable vectors'() {
        when:
        calculator.calculate([member(1, [1F, 0F]), member(2, [1F, 0F, 0F])], 4, 0.1D, 0.2D)
        then:
        thrown(IllegalArgumentException)
        when:
        def result = calculator.calculate([member(1, [0F, 0F]), member(2, [Float.NaN, 1F]), member(3, [1F, 0F])], 4, 0.1D, 0.2D)
        then:
        result.size() == 1
        result.first().metadata.invalidEmbeddingCount == 2
    }

    void 'final coverage measurements agree with indexed vectors across multimodal sets'() {
        given:
        Random random = new Random(417L)
        boolean repaired = false
        expect:
        40.times {
            def members = (1..20).collect { int id ->
                double angle = random.nextDouble() * Math.PI * 2
                member(id, [(float) Math.cos(angle), (float) Math.sin(angle)])
            }
            def result = calculator.calculate(members, 20, 0.015D, 0.4D)
            double measured = members.collect { m -> result.collect { c -> calculator.cosineDistance(m.embedding, c.embedding) }.min() }.max()
            assert Math.abs(result.first().residualRadius - measured) < 1E-6
            assert measured <= 0.015D + 1E-7
            assert result.every { Math.abs(calculator.cosineSimilarity(it.embedding, it.embedding) - 1D) < 1E-6 }
            repaired |= result.first().metadata.seedResidualRadii.size() > result.first().metadata.initialSeedCount
            def curve = result.first().metadata.seedResidualRadii
            assert (1..<curve.size()).every { curve[it] <= curve[it - 1] + 1E-7 }
            def reversed = calculator.calculate(members.reverse(), 20, 0.015D, 0.4D)
            assert result*.embedding.collect { it.toList() } == reversed*.embedding.collect { it.toList() }
        }
        repaired
    }

    void 'merges paired regions and refills freed slots without exceeding the cap'() {
        given:
        def members = (0..<3).collectMany { int pair ->
            float[] a = new float[6]
            float[] b = new float[6]
            a[2 * pair] = 1F
            b[2 * pair] = 0.7F
            b[2 * pair + 1] = (float)Math.sqrt(0.51D)
            [member(2 * pair + 1, a.toList()), member(2 * pair + 2, b.toList())]
        }

        when:
        def result = calculator.calculate(members, 4, 0.15D, 0.20D)
        def locals = result.findAll { it.centroidKind == 'region' }

        then:
        locals.size() == 3
        locals.every { it.memberCount == 2 }
        result.first().metadata.coveredMemberCount == 6
        result.first().metadata.mergeCount == 3
        result.first().metadata.addedSingletonCount == 2
        result.first().metadata.stoppingReason == 'coverage-target'
        result.first().metadata.algorithmVersion == 'coverage-merge-refill-v3'
        members.every { m -> result.any { calculator.cosineDistance(m.embedding, it.embedding) <= 0.15D + 1E-7D } }
        result.find { it.centroidKind == 'overall' }.embedding.toList() ==
            calculator.normalize(calculator.mean(members.collect { calculator.normalize(it.embedding.clone() as float[]) })).toList()
        calculator.calculate(members.reverse(), 4, 0.15D, 0.20D)*.embedding.collect { it.toList() } == result*.embedding.collect { it.toList() }

        and: 'a full cap with no mergeable pair remains a truthful partial representation'
        def capped = calculator.calculate(members, 3, 0.15D, 0.20D)
        capped.count { it.centroidKind == 'region' } == 3
        capped.first().metadata.uncoveredMemberCount == 3
        capped.first().metadata.stoppingReason == 'region-cap'
    }

    private SetSemanticMemberEmbedding member(int id, List<Number> vector) {
        new SetSemanticMemberEmbedding(setId: setId, setDomainType: 'Terminology', setLabel: 'Test', mauroModelId: setId,
            vectorFamily: 'meaning', memberId: new UUID(0L, id), memberDomainType: 'Term', memberText: "term $id",
            embedding: vector as float[])
    }
}
