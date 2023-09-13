package pro.jiaoyi.search.util;



import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class TFIDFSimilarity {

    // 计算词频（Term Frequency）
    public static double tf(String term, List<String> document) {
        double termFrequency = 0;
        for (String word : document) {
            if (term.equalsIgnoreCase(word)) {
                termFrequency++;
            }
        }
        return termFrequency / document.size();
    }

    // 计算逆文档频率（Inverse Document Frequency）
    public static double idf(String term, List<List<String>> documents) {
        double documentFrequency = 0;
        for (List<String> document : documents) {
            for (String word : document) {
                if (term.equalsIgnoreCase(word)) {
                    documentFrequency++;
                    break;
                }
            }
        }
        return Math.log(documents.size() / (documentFrequency + 1)); // 加1来避免除以0
    }

    // 计算TF-IDF向量
    public static Map<String, Double> tfidfVector(List<String> document, List<List<String>> documents) {
        Map<String, Double> tfidfVector = new HashMap<>();

        // 统计词频
        Map<String, Integer> termFrequencyMap = new HashMap<>();
        for (String word : document) {
            termFrequencyMap.put(word, termFrequencyMap.getOrDefault(word, 0) + 1);
        }

        // 计算TF-IDF值
        for (String term : termFrequencyMap.keySet()) {
            double tf = (double) termFrequencyMap.get(term) / document.size();
            double idf = idf(term, documents);
            double tfidf = tf * idf;
            tfidfVector.put(term, tfidf);
        }

        return tfidfVector;
    }

    // 计算余弦相似度
    public static BigDecimal cosineSimilarity(Map<String, Double> vector1, Map<String, Double> vector2) {
        double dotProduct = 0;
        double norm1 = 0;
        double norm2 = 0;

        // 计算向量的点积和范数
        for (String term : vector1.keySet()) {
            if (vector2.containsKey(term)) {
                dotProduct += vector1.get(term) * vector2.get(term);
            }
            norm1 += Math.pow(vector1.get(term), 2);
        }
        for (String term : vector2.keySet()) {
            norm2 += Math.pow(vector2.get(term), 2);
        }

        // 计算余弦相似度
        if (norm1 == 0 || norm2 == 0) {
            return BigDecimal.ZERO; // 避免除以0
        } else {
            double v = dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
            return new BigDecimal(String.valueOf(v)).setScale(8, RoundingMode.HALF_UP);
        }
    }

    public static void main(String[] args) {
        List<String> document1 = Arrays.asList("咖啡","咖啡师","资格","资格证","怎么","考","1","怎么","考","1");
        List<String> document2 = Arrays.asList("咖啡","咖啡师","资格","资格证","怎么","考");
//        List<String> document2 = Arrays.asList("咖啡","的","种类","及","口味");

        List<List<String>> documents = Arrays.asList(document1, document2);

        // 计算文档的TF-IDF向量
        Map<String, Double> vector1 = tfidfVector(document1, documents);
        Map<String, Double> vector2 = tfidfVector(document2, documents);

        // 计算文档的余弦相似度
        double similarity = cosineSimilarity(vector1, vector2).doubleValue();

        System.out.println("Cosine similarity between document 1 and document 2: " + similarity);
    }
}