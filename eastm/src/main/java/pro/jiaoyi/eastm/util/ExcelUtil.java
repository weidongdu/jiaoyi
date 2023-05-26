package pro.jiaoyi.eastm.util;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import pro.jiaoyi.eastm.model.EmCList;
import pro.jiaoyi.eastm.model.excel.Index1000XlsData;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExcelUtil {
    public static void simpleRead(String absPath, List<EmCList> list, List<EmCList> all) {
        // 写法2：
        EasyExcel.read(absPath, Index1000XlsData.class, new ReadListener<Index1000XlsData>() {
            private Set<String> set = new HashSet<>(1000);
            @Override
            public void invoke(Index1000XlsData data, AnalysisContext context) {
                set.add(data.getCode());
            }
            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                //遍历 all  , 将匹配到cache 的数据 存入list
                for (EmCList emCList : all) {
                    if (set.contains(emCList.getF12Code())) {
                        list.add(emCList);
                    }
                }
                set.clear();
            }
        }).sheet().doRead();
    }
}
