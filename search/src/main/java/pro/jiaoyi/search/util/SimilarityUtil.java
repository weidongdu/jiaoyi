package pro.jiaoyi.search.util;

import java.util.*;

public class SimilarityUtil {

    public static double calculateCosineSimilarity(List<String> keywords1, List<String> keywords2) {
        Map<String, Integer> termFrequency1 = calculateTermFrequency(keywords1);
        Map<String, Integer> termFrequency2 = calculateTermFrequency(keywords2);

        Set<String> uniqueTerms = new HashSet<>(termFrequency1.keySet());
        uniqueTerms.addAll(termFrequency2.keySet());

        double dotProduct = calculateDotProduct(termFrequency1, termFrequency2, uniqueTerms);
        double magnitude1 = calculateVectorMagnitude(termFrequency1.values());
        double magnitude2 = calculateVectorMagnitude(termFrequency2.values());

        return dotProduct / (magnitude1 * magnitude2);
    }

    private static Map<String, Integer> calculateTermFrequency(List<String> keywords) {
        Map<String, Integer> termFrequency = new HashMap<>();
        for (String term : keywords) {
            termFrequency.put(term, termFrequency.getOrDefault(term, 0) + 1);
        }
        return termFrequency;
    }

    private static double calculateDotProduct(Map<String, Integer> termFrequency1, Map<String, Integer> termFrequency2, Set<String> uniqueTerms) {
        double dotProduct = 0;
        for (String term : uniqueTerms) {
            int frequency1 = termFrequency1.getOrDefault(term, 0);
            int frequency2 = termFrequency2.getOrDefault(term, 0);
            dotProduct += frequency1 * frequency2;
        }
        return dotProduct;
    }

    private static double calculateVectorMagnitude(Collection<Integer> frequencies) {
        int magnitudeSquared = 0;
        for (int frequency : frequencies) {
            magnitudeSquared += frequency * frequency;
        }
        return Math.sqrt(magnitudeSquared);
    }
}
