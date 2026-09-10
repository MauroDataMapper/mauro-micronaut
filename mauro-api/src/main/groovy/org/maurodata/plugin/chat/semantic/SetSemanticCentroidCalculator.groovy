package org.maurodata.plugin.chat.semantic

import groovy.transform.CompileStatic

/** Pure, deterministic coverage construction. All distances are cosine distances. */
@CompileStatic
class SetSemanticCentroidCalculator {
    static final String ALGORITHM_VERSION = 'coverage-merge-refill-v3'
    private static final double EPSILON = 1E-7D

    List<SetSemanticCentroid> calculate(List<SetSemanticMemberEmbedding> members,
                                        int maxLocalRegionCount,
                                        double coverageRadius,
                                        double neighbourhoodRadius) {
        if (maxLocalRegionCount < 1 || !Double.isFinite(coverageRadius) || coverageRadius < 0D || coverageRadius > 2D ||
            !Double.isFinite(neighbourhoodRadius) || neighbourhoodRadius < 0D || neighbourhoodRadius > 2D) {
            throw new IllegalArgumentException('Require a positive local region cap and cosine radii between 0 and 2')
        }
        List<SetSemanticMemberEmbedding> usable = (members ?: []).findAll {
            isUsable(it.embedding)
        }.sort(false) { a, b -> a.memberId.toString() <=> b.memberId.toString() } as List<SetSemanticMemberEmbedding>
        if (usable.isEmpty()) return []
        if (usable.any { it.memberId == null } || usable*.memberId.unique().size() != usable.size()) {
            throw new IllegalArgumentException('Members must have unique IDs')
        }
        int dimension = usable.first().embedding.length
        if (usable.any { it.embedding.length != dimension }) {
            throw new IllegalArgumentException('Member embedding dimensions must agree')
        }
        List<float[]> vectors = usable.collect { normalize(it.embedding.clone() as float[]) } as List<float[]>
        float[] overall = normalize(mean(vectors))
        // A cancelling mean has no cosine direction and must not enter a cosine ANN index.
        boolean overallAvailable = isUsable(overall)
        List<float[]> indexed = overallAvailable ? [overall] as List<float[]> : []
        List<Integer> seeds = []
        List<List<Integer>> neighbourhoods = []
        List<float[]> localCentroids = []
        List<Double> seedResiduals = []
        Map<Integer, double[]> seedDistances = [:]
        double[] seedCoverage = new double[vectors.size()]
        for (int i = 0; i < vectors.size(); i++) seedCoverage[i] = nearestDistance(vectors[i], indexed)

        // Initial farthest-point sampling maintains the unsmoothed seed coverage curve.
        while (seeds.size() < maxLocalRegionCount && maximum(seedCoverage) > coverageRadius + EPSILON) {
            int seed = farthestDistance(seedCoverage, seeds)
            if (seed < 0) break
            seeds.add(seed)
            updateCoverage(seedCoverage, distances(vectors, seed, seedDistances))
            seedResiduals.add(maximum(seedCoverage))
        }
        int initialSeedCount = seeds.size()
        // Smoothing may open coverage gaps. Repair by adding seeds through the same process.
        while (true) {
            neighbourhoods = grow(vectors, seeds, neighbourhoodRadius, seedDistances)
            localCentroids = neighbourhoods.collect { List<Integer> neighbours ->
                normalize(mean(neighbours.collect { Integer i -> vectors[i] } as List<float[]>))
            } as List<float[]>
            // Bound centroid drift: a region must still represent its own seed.
            for (int i = 0; i < seeds.size(); i++) {
                while (neighbourhoods[i].size() > 1 && (!isUsable(localCentroids[i]) ||
                    unitDistance(localCentroids[i], vectors[seeds[i]]) > coverageRadius + EPSILON)) {
                    neighbourhoods[i].remove(neighbourhoods[i].size() - 1)
                    localCentroids[i] = normalize(mean(neighbourhoods[i].collect { Integer n -> vectors[n] } as List<float[]>))
                }
            }
            indexed = overallAvailable ? [overall] as List<float[]> : []
            indexed.addAll(localCentroids)
            if (radius(vectors, indexed) <= coverageRadius + EPSILON || seeds.size() >= maxLocalRegionCount) break
            int seed = farthest(vectors, indexed, seeds)
            if (seed < 0) break
            seeds.add(seed)
            updateCoverage(seedCoverage, distances(vectors, seed, seedDistances))
            seedResiduals.add(maximum(seedCoverage))
        }
        // Compact the completed seed construction, then refill only within the configured cap.
        List<Region> regions = neighbourhoods.collect { List<Integer> ids -> region(vectors, ids) }
        List<float[]> base = overallAvailable ? [overall] as List<float[]> : []
        int mergeCount = 0
        int addedSingletonCount = 0
        while (true) {
            if (mergeOne(vectors, base, regions, coverageRadius)) { mergeCount++; continue }
            indexed = new ArrayList<float[]>(base)
            indexed.addAll(regions.collect { Region r -> r.vector })
            if (regions.size() >= maxLocalRegionCount || radius(vectors, indexed) <= coverageRadius + EPSILON) break
            int seed = farthest(vectors, indexed, Collections.<Integer>emptyList())
            if (seed < 0) break
            regions.add(region(vectors, [seed] as List<Integer>))
            addedSingletonCount++
        }
        neighbourhoods = regions.collect { Region r -> r.members }
        localCentroids = regions.collect { Region r -> r.vector }
        double finalRadius = radius(vectors, indexed)
        int uncovered = vectors.count { nearestDistance(it, indexed) > coverageRadius + EPSILON }.intValue()
        Set<Integer> contributors = new LinkedHashSet<Integer>()
        neighbourhoods.each { contributors.addAll(it) }
        Map<String, Object> metadata = [
            algorithmVersion: ALGORITHM_VERSION, maxLocalRegionCount: maxLocalRegionCount,
            coverageRadius: coverageRadius, neighbourhoodRadius: neighbourhoodRadius,
            representedMemberCount: usable.size(), invalidEmbeddingCount: (members ?: []).size() - usable.size(),
            coveredMemberCount: usable.size() - uncovered, uncoveredMemberCount: uncovered,
            localContributorCount: contributors.size(), overallAvailable: overallAvailable,
            finalResidualRadius: finalRadius, seedResidualRadii: seedResiduals, initialSeedCount: initialSeedCount,
            mergeCount: mergeCount, addedSingletonCount: addedSingletonCount,
            initialResidualRadius: overallAvailable ? radius(vectors, [overall] as List<float[]>) : 2D,
            stoppingReason: uncovered == 0 ? 'coverage-target' : (regions.size() >= maxLocalRegionCount ? 'region-cap' : 'no-new-seed')
        ] as Map<String, Object>
        List<SetSemanticCentroid> result = []
        if (overallAvailable) result.add(centroid(usable.first(), overall, 'overall', 0, usable.size(), usable.size(),
            finalRadius, radius(vectors, [overall] as List<float[]>), metadata))
        for (int i = 0; i < regions.size(); i++) {
            Map<String, Object> regionMetadata = new LinkedHashMap<String, Object>(metadata)
            if (neighbourhoods[i].size() == 1) regionMetadata.put('seedMemberId', usable[neighbourhoods[i].first()].memberId.toString())
            result.add(centroid(usable.first(), localCentroids[i], 'region', i + 1, neighbourhoods[i].size(), usable.size(),
                finalRadius, radius(neighbourhoods[i].collect { Integer n -> vectors[n] } as List<float[]>,
                [localCentroids[i]] as List<float[]>), regionMetadata))
        }
        result
    }

    private static class Region {
        List<Integer> members
        float[] vector
        double[] distances
    }

    private static class Pair {
        int a
        int b
        double distance
    }

    private static Region region(List<float[]> vectors, List<Integer> members) {
        List<Integer> ids = new ArrayList<Integer>(new LinkedHashSet<Integer>(members))
        Collections.sort(ids)
        float[] vector = normalize(mean(ids.collect { Integer i -> vectors[i] } as List<float[]>))
        double[] distances = new double[vectors.size()]
        for (int i = 0; i < vectors.size(); i++) distances[i] = unitDistance(vectors[i], vector)
        new Region(members: ids, vector: vector, distances: distances)
    }

    /** Closest pair first with deterministic ties; coverage is checked against all remaining representatives. */
    private static boolean mergeOne(List<float[]> vectors, List<float[]> overall, List<Region> regions, double bound) {
        if (regions.size() < 2) return false
        double[] before = new double[vectors.size()]
        for (int i = 0; i < vectors.size(); i++) {
            before[i] = nearestDistance(vectors[i], overall)
            for (Region r : regions) before[i] = Math.min(before[i], r.distances[i])
        }
        List<Pair> pairs = []
        for (int a = 0; a < regions.size(); a++) {
            for (int b = a + 1; b < regions.size(); b++) {
                pairs.add(new Pair(a: a, b: b, distance: unitDistance(regions[a].vector, regions[b].vector)))
            }
        }
        pairs.sort { Pair a, Pair b -> (a.distance <=> b.distance) ?: (a.a <=> b.a) ?: (a.b <=> b.b) }
        for (Pair pair : pairs) {
            List<Integer> members = new ArrayList<Integer>(regions[pair.a].members)
            members.addAll(regions[pair.b].members)
            Region merged = region(vectors, members)
            if (!isUsable(merged.vector)) continue
            boolean preserves = true
            for (int i = 0; i < vectors.size() && preserves; i++) {
                if (before[i] > bound + EPSILON || merged.distances[i] <= bound + EPSILON ||
                    nearestDistance(vectors[i], overall) <= bound + EPSILON) continue
                boolean covered = false
                for (int r = 0; r < regions.size(); r++) {
                    if (r != pair.a && r != pair.b && regions[r].distances[i] <= bound + EPSILON) { covered = true; break }
                }
                preserves = covered
            }
            if (preserves) {
                regions.remove(pair.b)
                regions.remove(pair.a)
                regions.add(merged)
                return true
            }
        }
        false
    }

    /** Parallel expansion: each seed admits its next nearest member per round. */
    private static List<List<Integer>> grow(List<float[]> vectors, List<Integer> seeds, double bound, Map<Integer, double[]> cache) {
        List<List<Integer>> neighbourhoods = seeds.collect { Integer seed -> [seed] as List<Integer> }
        List<List<Integer>> pending = []
        for (Integer seed : seeds) {
            double[] fromSeed = distances(vectors, seed, cache)
            double otherSeedDistance = 3D
            for (Integer other : seeds) {
                if (other != seed) otherSeedDistance = Math.min(otherSeedDistance, fromSeed[other])
            }
            List<Integer> neighbours = []
            for (int i = 0; i < vectors.size(); i++) {
                double distance = fromSeed[i]
                if (i != seed && !seeds.contains(i) && distance <= bound + EPSILON && distance < otherSeedDistance - EPSILON) {
                    neighbours.add(i)
                }
            }
            neighbours.sort { Integer a, Integer b ->
                (fromSeed[a] <=> fromSeed[b]) ?: (a <=> b)
            }
            pending.add(neighbours)
        }
        Set<Integer> covered = new LinkedHashSet<Integer>(seeds)
        int round = 0
        while (covered.size() < vectors.size()) {
            boolean expanded = false
            for (int i = 0; i < seeds.size(); i++) {
                if (round < pending[i].size()) {
                    Integer member = pending[i][round]
                    neighbourhoods[i].add(member)
                    covered.add(member)
                    expanded = true
                }
            }
            if (!expanded) break
            round++
        }
        neighbourhoods
    }

    private static SetSemanticCentroid centroid(SetSemanticMemberEmbedding owner, float[] vector, String kind,
                                                int ordinal, int count, int total, double residual, double local,
                                                Map<String, Object> metadata) {
        new SetSemanticCentroid(setId: owner.setId, setDomainType: owner.setDomainType, setLabel: owner.setLabel,
            mauroModelId: owner.mauroModelId, vectorFamily: owner.vectorFamily, centroidKind: kind,
            regionOrdinal: ordinal, memberCount: count, sourceMemberCount: total,
            residualRadius: residual, localRadius: local, embedding: vector, metadata: metadata)
    }

    private static int farthest(List<float[]> vectors, List<float[]> representatives, List<Integer> selected) {
        int best = -1
        double distance = -1D
        for (int i = 0; i < vectors.size(); i++) {
            double candidate = nearestDistance(vectors[i], representatives)
            if (!selected.contains(i) && candidate > distance + EPSILON) { best = i; distance = candidate }
        }
        best
    }

    private static double radius(List<float[]> members, List<float[]> representatives) {
        double maximum = 0D
        for (float[] member : members) maximum = Math.max(maximum, nearestDistance(member, representatives))
        maximum
    }

    private static double nearestDistance(float[] vector, List<float[]> representatives) {
        double nearest = 2D
        for (float[] representative : representatives) nearest = Math.min(nearest, unitDistance(vector, representative))
        nearest
    }

    private static int farthestDistance(double[] coverage, List<Integer> seeds) {
        int best = -1
        for (int i = 0; i < coverage.length; i++) {
            if (!seeds.contains(i) && (best < 0 || coverage[i] > coverage[best] + EPSILON)) best = i
        }
        best
    }

    private static double[] distances(List<float[]> vectors, int seed, Map<Integer, double[]> cache) {
        if (!cache.containsKey(seed)) {
            double[] result = new double[vectors.size()]
            for (int i = 0; i < vectors.size(); i++) result[i] = unitDistance(vectors[seed], vectors[i])
            cache.put(seed, result)
        }
        cache.get(seed)
    }

    private static void updateCoverage(double[] coverage, double[] distances) {
        for (int i = 0; i < coverage.length; i++) coverage[i] = Math.min(coverage[i], distances[i])
    }

    private static double maximum(double[] values) {
        double maximum = 0D
        for (double value : values) maximum = Math.max(maximum, value)
        maximum
    }

    /** Only used after normalisation; avoids repeatedly recalculating member norms. */
    private static double unitDistance(float[] a, float[] b) {
        double dot = 0D
        for (int i = 0; i < a.length; i++) dot += (double) a[i] * b[i]
        Math.max(0D, Math.min(2D, 1D - dot))
    }

    static boolean isUsable(float[] vector) {
        if (vector == null || vector.length == 0) return false
        double norm = 0D
        for (float value : vector) {
            if (!Float.isFinite(value)) return false
            norm += (double) value * value
        }
        norm > 1E-20D
    }

    static double cosineDistance(float[] left, float[] right) { 1D - cosineSimilarity(left, right) }

    static double cosineSimilarity(float[] left, float[] right) {
        if (!isUsable(left) || !isUsable(right) || left.length != right.length) return 0D
        double dot = 0D, leftNorm = 0D, rightNorm = 0D
        for (int i = 0; i < left.length; i++) {
            dot += (double) left[i] * right[i]
            leftNorm += (double) left[i] * left[i]
            rightNorm += (double) right[i] * right[i]
        }
        Math.max(-1D, Math.min(1D, dot / Math.sqrt(leftNorm * rightNorm)))
    }

    static float[] mean(List<float[]> vectors) {
        if (!vectors) return new float[0]
        int dimension = vectors.first().length
        double[] sum = new double[dimension]
        for (float[] vector : vectors) {
            if (vector == null || vector.length != dimension) throw new IllegalArgumentException('Inconsistent vector dimensions')
            for (int i = 0; i < dimension; i++) sum[i] += vector[i]
        }
        float[] result = new float[dimension]
        for (int i = 0; i < dimension; i++) result[i] = (float) (sum[i] / vectors.size())
        result
    }

    static float[] normalize(float[] vector) {
        if (!isUsable(vector)) return vector
        double norm = 0D
        for (float value : vector) norm += (double) value * value
        norm = Math.sqrt(norm)
        for (int i = 0; i < vector.length; i++) vector[i] = (float) (vector[i] / norm)
        vector
    }
}
