package pro.jiaoyi.tradingview.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.eastm.config.IndexEnum;
import pro.jiaoyi.eastm.model.EmCList;
import pro.jiaoyi.eastm.model.EmDailyK;
import pro.jiaoyi.tradingview.model.TvChart;

import java.time.LocalDate;
import java.util.*;

@Service
@Slf4j
public class TvService {

    @Autowired
    private TvTransUtil tvTransUtil;
    @Autowired
    private EmClient emClient;

    /**
     * 获取 tv chart 数据
     *
     * @param code
     * @param date
     * @param limit
     * @return //"002422", LocalDate.now(), 500
     */
    public TvChart getTvChart(String code,String codeType, LocalDate date, Integer limit) {
        if ("BkValue".equalsIgnoreCase(codeType)){
            code = emClient.getBkCodeByBkValue(code);
        }
        return getTvChart(code, date, limit);
    }

    public TvChart getTvChart(String code, LocalDate date, Integer limit) {
        List<EmDailyK> dailyKs = emClient.getDailyKs(code, date, limit, false);
        return tvTransUtil.tranEmDailyKLineToTv(dailyKs);
    }

    public List<EmCList> getIndex(String type, boolean sort) {
        IndexEnum indexEnum = IndexEnum.getByType(type);
        if (indexEnum == null) {
            return Collections.emptyList();
        }

        List<EmCList> index = emClient.getIndex(indexEnum);
        if (sort) {
            index.sort(Comparator.comparing(EmCList::getF100bk));
        }
        return index;
    }

    public Map<String, List<String>> getAllIndex() {
        HashMap<String, List<String>> map = new HashMap<>();
        IndexEnum[] values = IndexEnum.values();
        for (IndexEnum value : values) {
            List<EmCList> lists = getIndex(value.getType(), true);

            ArrayList<String> codeList = new ArrayList<>();
            for (EmCList list : lists) {
                codeList.add(list.getF12Code());
            }
            map.put(value.getType(), codeList);

        }
        return map;
    }
}
