package pro.jiaoyi.search.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
TF-IDF（Term Frequency-Inverse Document Frequency）是一种用于信息检索与文本挖掘的常用技术。它结合了词频（Term Frequency，TF）和逆文档频率（Inverse Document Frequency，IDF）两个指标，用于评估一个词语在文档集中的重要性。

TF（词频）衡量一个词在一个文档中出现的频率，它认为一个词在文档中出现的次数越多，那么它对该文档的重要性就越高。

IDF（逆文档频率）衡量一个词的普遍重要性，它通过计算在整个文档集中包含该词的文档数量的倒数来得到。如果一个词在很多文档中都出现，那么它的IDF值就较低，认为它对区分文档的能力较弱；相反，如果一个词在少数文档中出现，那么它的IDF值就较高，认为它对区分文档的能力较强。

TF-IDF将TF和IDF结合起来，通过计算一个词的TF和IDF的乘积来衡量它在文档集中的重要性。一个词在某个文档中的TF-IDF值越高，表示它在该文档中的重要性越大，并且在其他文档中较少出现。

使用TF-IDF可以对文档集进行关键词提取、文档相似度计算、信息检索等任务，帮助识别和区分文本中重要的词语。
 */
public class TfIdf {

    // 计算词频（Term Frequency）
    private static BigDecimal tf(String term, List<String> document) {
        if (document == null || document.isEmpty()) {
            return BigDecimal.ZERO;
        }

        double termFrequency = 0;
        for (String word : document) {
            if (term.equalsIgnoreCase(word)) {
                termFrequency++;
            }
        }
        double v = termFrequency / document.size();
        return new BigDecimal(String.valueOf(v)).setScale(8, RoundingMode.HALF_UP);
    }

    // 计算逆文档频率（Inverse Document Frequency）
    private static BigDecimal idf(String term, List<List<String>> documents) {
        double documentFrequency = 0;
        for (List<String> document : documents) {
            for (String word : document) {
                if (term.equalsIgnoreCase(word)) {
                    documentFrequency++;
                    break;
                }
            }
        }
        if (documentFrequency == 0) {
            return BigDecimal.ZERO;
        }
        double v = Math.log(documents.size() / documentFrequency);
        return new BigDecimal(String.valueOf(v)).setScale(8, RoundingMode.HALF_UP);
    }

    /**
     * @param term      当前关键词
     * @param document  当前文档
     * @param documents 所有文档
     * @return
     */
    // 计算TF-IDF
    public static BigDecimal tfidf(String term, List<String> document, List<List<String>> documents) {
        BigDecimal tf = tf(term, document);
        BigDecimal idf = idf(term, documents);
        return tf.multiply(idf).setScale(8, RoundingMode.HALF_UP);
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
            double tfidf = tfidf(term, document, documents).doubleValue();
            tfidfVector.put(term, tfidf);
        }

        return tfidfVector;
    }

    // 计算余弦相似度
    public static double cosineSimilarity(Map<String, Double> vector1, Map<String, Double> vector2) {
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
            return 0; // 避免除以0
        } else {
            return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
        }
    }

    public static void main(String[] args) {
        List<String> document1 = Arrays.asList("apple", "banana", "apple", "orange", "banana");
        List<String> document2 = Arrays.asList("banana", "orange", "orange", "grape");
        List<String> document3 = Arrays.asList("apple", "orange", "grape", "orange");

        List<List<String>> documents = Arrays.asList(document1, document2, document3);

        System.out.println("TF-IDF for 'apple' in document 1: " + tfidf("apple", document1, documents));
        System.out.println("TF-IDF for 'orange' in document 2: " + tfidf("orange", document2, documents));
        System.out.println("TF-IDF for 'grape' in document 3: " + tfidf("grape", document3, documents));
    }

    //    public static void main(String[] args) {
//        List<String> document1 = Arrays.asList("apple", "banana", "apple", "orange", "banana");
//        List<String> document2 = Arrays.asList("apple", "banana", "apple", "orange", "banana","banana", "orange", "orange", "grape");
//
//        List<List<String>> documents = Arrays.asList(document1, document2);
//
//        // 计算文档的TF-IDF向量
//        Map<String, Double> vector1 = tfidfVector(document1, documents);
//        Map<String, Double> vector2 = tfidfVector(document2, documents);
//
//        // 计算文档的余弦相似度
//        double similarity = cosineSimilarity(vector1, vector2);
//
//        System.out.println("Cosine similarity between document 1 and document 2: " + similarity);
//    }

}
