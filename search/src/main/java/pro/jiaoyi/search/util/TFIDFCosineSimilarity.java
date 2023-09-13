package pro.jiaoyi.search.util;

import java.util.*;

public class TFIDFCosineSimilarity {
    public static void main(String[] args) {
        // 假设有两个文本
        String text1 = "This is the first document";
        String text2 = "This document is the second document";

        // 构建文本集合
        List<String> documents = new ArrayList<>();
        documents.add(text1);
        documents.add(text2);

        // 构建词汇表
        Set<String> vocabulary = buildVocabulary(documents);

        // 计算每个文本的词频
        Map<String, Map<String, Integer>> termFrequencyMap = calculateTermFrequency(documents);

        // 计算每个词语的逆文档频率
        Map<String, Double> inverseDocumentFrequencyMap = calculateInverseDocumentFrequency(documents, vocabulary);

        // 计算每个文本的TF-IDF向量
        Map<String, double[]> tfidfVectors = calculateTFIDFVectors(termFrequencyMap, inverseDocumentFrequencyMap);

        // 获取文本1和文本2的TF-IDF向量
        double[] tfidfVector1 = tfidfVectors.get(text1);
        double[] tfidfVector2 = tfidfVectors.get(text2);

        // 计算余弦相似度
        double similarity = calculateCosineSimilarity(tfidfVector1, tfidfVector2);

        // 输出相似度
        System.out.println("余弦相似度: " + similarity);
    }

    // 构建词汇表
    private static Set<String> buildVocabulary(List<String> documents) {
        Set<String> vocabulary = new HashSet<>();
        for (String document : documents) {
            String[] words = document.toLowerCase().split("\\s+");
            vocabulary.addAll(Arrays.asList(words));
        }
        return vocabulary;
    }

    // 计算每个文本的词频
    private static Map<String, Map<String, Integer>> calculateTermFrequency(List<String> documents) {
        Map<String, Map<String, Integer>> termFrequencyMap = new HashMap<>();
        for (String document : documents) {
            Map<String, Integer> termFrequency = new HashMap<>();
            String[] words = document.toLowerCase().split("\\s+");
            for (String word : words) {
                termFrequency.put(word, termFrequency.getOrDefault(word, 0) + 1);
            }
            termFrequencyMap.put(document, termFrequency);
        }
        return termFrequencyMap;
    }

    // 计算每个词语的逆文档频率
    private static Map<String, Double> calculateInverseDocumentFrequency(List<String> documents, Set<String> vocabulary) {
        Map<String, Double> inverseDocumentFrequencyMap = new HashMap<>();
        for (String word : vocabulary) {
            int documentCount = 0;
            for (String document : documents) {
                if (document.toLowerCase().contains(word)) {
                    documentCount++;
                }
            }
            double inverseDocumentFrequency = Math.log((double) documents.size() / (documentCount + 1));
            inverseDocumentFrequencyMap.put(word, inverseDocumentFrequency);
        }
        return inverseDocumentFrequencyMap;
    }

    // 计算每个文本的TF-IDF向量
    private static Map<String, double[]> calculateTFIDFVectors(Map<String, Map<String, Integer>> termFrequencyMap, Map<String, Double> inverseDocumentFrequencyMap) {
        Map<String, double[]> tfidfVectors = new HashMap<>();
        for (String document : termFrequencyMap.keySet()) {
            Map<String, Integer> termFrequency = termFrequencyMap.get(document);
            double[] tfidfVector = new double[inverseDocumentFrequencyMap.size()];
            int i = 0;
            for (String word : inverseDocumentFrequencyMap.keySet()) {
                double tfidf = termFrequency.getOrDefault(word, 0) * inverseDocumentFrequencyMap.get(word);
                tfidfVector[i] = tfidf;
                i++;
            }
            tfidfVectors.put(document, tfidfVector);
        }
        return tfidfVectors;
    }

    // 计算余弦相似度
    private static double calculateCosineSimilarity(double[] vector1, double[] vector2) {
        double dotProduct = 0;
        double norm1 = 0;
        double norm2 = 0;
        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            norm1 += Math.pow(vector1[i], 2);
            norm2 += Math.pow(vector2[i], 2);
        }
        double similarity = dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
        return similarity;
    }
}