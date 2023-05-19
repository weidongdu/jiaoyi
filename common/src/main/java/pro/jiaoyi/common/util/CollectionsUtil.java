package pro.jiaoyi.common.util;


import java.util.*;

public class CollectionsUtil {

    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map, boolean asc) {
        List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
        if (asc){
            //升序
            list.sort(Map.Entry.comparingByValue());
        }else {
            //降序
            list.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));
        }

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }


}
