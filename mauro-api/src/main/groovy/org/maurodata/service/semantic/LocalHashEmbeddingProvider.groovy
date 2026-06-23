package org.maurodata.service.semantic

import groovy.transform.CompileStatic
import jakarta.inject.Singleton

@CompileStatic
@Singleton
class LocalHashEmbeddingProvider implements EmbeddingProvider {

    @Override
    String id() {
        'test'
    }

    @Override
    boolean supports(EmbeddingProfile profile) {
        profile?.provider == id() && profile.embeddingModel == 'hash-384'
    }

    @Override
    List<float[]> embed(EmbeddingProfile profile, List<String> texts) {
        int dimension = profile?.dimension ?: 384
        List<float[]> vectors = new ArrayList<float[]>(texts?.size() ?: 0)
        for (String text : texts ?: []) {
            vectors.add(embedOne(text ?: '', dimension))
        }
        vectors
    }

    private static float[] embedOne(String text, int dimension) {
        float[] vector = new float[dimension]
        for (String token : tokens(text)) {
            int hash = token.hashCode()
            int index = Math.floorMod(hash, dimension)
            int sign = (hash & 1) == 0 ? 1 : -1
            vector[index] += sign
        }
        normalize(vector)
        vector
    }

    private static List<String> tokens(String text) {
        List<String> out = new ArrayList<String>()
        java.util.regex.Matcher matcher = (text.toLowerCase(Locale.ROOT) =~ /[\p{L}\p{N}]+/)
        while (matcher.find()) {
            String token = matcher.group()
            if (token.length() > 1) {
                out.add(token)
            }
        }
        out
    }

    private static void normalize(float[] vector) {
        double sum = 0D
        for (float value : vector) {
            sum += value * value
        }
        if (sum <= 0D) {
            return
        }
        double norm = Math.sqrt(sum)
        for (int i = 0; i < vector.length; i++) {
            vector[i] = (float) (vector[i] / norm)
        }
    }
}
