package pro.jiaoyi.tradingview.service;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.jiaoyi.common.util.DateUtil;
import pro.jiaoyi.common.util.FileUtil;
import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.eastm.config.IndexEnum;
import pro.jiaoyi.eastm.config.VipIndexEnum;
import pro.jiaoyi.eastm.model.EmCList;
import pro.jiaoyi.eastm.model.EmDailyK;
import pro.jiaoyi.eastm.util.EmMaUtil;
import pro.jiaoyi.tradingview.config.Colors;
import pro.jiaoyi.tradingview.model.TvChart;
import pro.jiaoyi.tradingview.model.chart.Constants;
import pro.jiaoyi.tradingview.model.chart.TvMarker;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

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
    public TvChart getTvChart(String code, String codeType, LocalDate date, Integer limit) {
        if ("BkValue".equalsIgnoreCase(codeType)) {
            code = emClient.getBkCodeByBkValue(code);
        }
        return getTvChart(code, date, limit);
    }

    public TvChart getTvChart(String code, LocalDate date, Integer limit) {
        List<EmDailyK> dailyKs = emClient.getDailyKs(code, date, limit, false);
        return tvTransUtil.tranEmDailyKLineToTv(dailyKs);
    }

    public TvChart getTvChart(String code, LocalDate date, Integer limit, boolean force) {
        List<EmDailyK> dailyKs = emClient.getDailyKs(code, date, limit, force);
        return tvTransUtil.tranEmDailyKLineToTv(dailyKs);
    }

    public List<EmCList> getIndex(String type, boolean sort, boolean sync) {
        IndexEnum indexEnum = IndexEnum.getByType(type);
        if (indexEnum == null) {
            return Collections.emptyList();
        }

        List<EmCList> index = emClient.getIndex(indexEnum, sync);
        if (sort) {
            if (index != null && index.size() > 0) {
                try {
                    index.sort(Comparator.comparing(EmCList::getF100bk));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return index;
    }

    private static AtomicLong COUNTER = new AtomicLong(0);

    public Map<String, List<String>> getAllIndex(boolean sync) {
        if (COUNTER.getAndIncrement() == 0 && sync) {
            //init
            List<EmDailyK> index = emClient.getDailyKs(VipIndexEnum.index_000001.getCode(), LocalDate.now(), 500, false);
            if (index.size() > 0) {
                List<EmCList> list = emClient.getClistDefaultSize(true);
                for (EmCList em : list) {
                    emClient.getDailyKs(em.getF12Code(), LocalDate.now(), 500, false);
                }
            }
        }

        HashMap<String, List<String>> map = new HashMap<>();
        IndexEnum[] values = IndexEnum.values();

        //这里改一下, 改成 默认获取全部信息


        for (IndexEnum value : values) {
            List<EmCList> lists = getIndex(value.getType(), true, sync);

            ArrayList<String> codeList = new ArrayList<>();
            for (EmCList list : lists) {
                codeList.add(list.getF12Code());
            }
            map.put(value.getType(), codeList);
        }
        String file = LocalDateTime.now().format(DateTimeFormatter.ofPattern(DateUtil.PATTERN_yyyyMMdd_HHmm)) + ".json";
        FileUtil.writeToFile(file, JSON.toJSONString(map));
        return map;
    }


    public TvChart getTvChartRandom(String code, String hitDateString) {
        List<EmDailyK> dailyKs = emClient.getDailyKs(code, LocalDate.now(), 500, false);
        LocalDate localDate = DateUtil.strToLocalDate(hitDateString, "yyyy-MM-dd");
        int index = 0;
        for (int i = 0; i < dailyKs.size(); i++) {
            LocalDate td = DateUtil.strToLocalDate(dailyKs.get(i).getTradeDate(), "yyyyMMdd");
            if (td.isEqual(localDate)) {
                index = i;
            }
        }

        int end = Math.min(index + 2, dailyKs.size() - 1);
        TvChart tvChart = tvTransUtil.tranEmDailyKLineToTv(dailyKs.subList(0, end));

        List<TvMarker> mks = tvChart.getMks();
//        int mkIndex = 0;
//        for (int i = 0; i < mks.size(); i++) {
//            TvMarker mk = mks.get(i);
//            String time = mk.getTime();
//            LocalDate td = DateUtil.strToLocalDate(time, "yyyy-MM-dd");
//            if (td.isEqual(localDate)) {
//                mkIndex = i;
//                break;
//            }
//        }
//
//        if (mkIndex == 0) {
//            //没有 对应在maker 创建新的 并插入
//
//            TvMarker tvMarker = new TvMarker();
//            tvMarker.setColor(Colors.WHITE.getColor());
//            tvMarker.setPosition(Constants.MARKER_POSITION_ABOVEBAR);
//            tvMarker.setShape(Constants.MARKER_SHAPE_ARROW_DOWN);
//            tvMarker.setText("⬇");
//            tvMarker.setTime(hitDateString);
//            mks.add(tvMarker);
//        } else {
//            TvMarker tvMarker = mks.get(mkIndex);
//            tvMarker.setColor(Colors.WHITE.getColor());
//            tvMarker.setPosition(Constants.MARKER_POSITION_ABOVEBAR);
//            tvMarker.setShape(Constants.MARKER_SHAPE_ARROW_DOWN);
//            tvMarker.setText("⬇");
//        }

        for (int i = 0; i < 2; i++) {
            TvMarker tvMarker = new TvMarker();
            tvMarker.setColor(Colors.WHITE.getColor());
            tvMarker.setPosition(Constants.MARKER_POSITION_ABOVEBAR);
            tvMarker.setShape(Constants.MARKER_SHAPE_ARROW_DOWN);
            tvMarker.setText("⬇");
            tvMarker.setTime(hitDateString);
            mks.add(tvMarker);
        }


        // mks 排序, 按 time 转换成 LocalDate 先后
        mks.sort(Comparator.comparing(o -> DateUtil.strToLocalDate(o.getTime(), "yyyy-MM-dd")));

        return tvChart;
    }

    public TvChart getTvChartRandom() {
        List<EmCList> clistDefaultSize = emClient.getClistDefaultSize(false);
        int size = clistDefaultSize.size();
        Random random = new Random();
        int i = random.nextInt(size);

        int count = 0;
        while (i == size && count < 10) {
            i = random.nextInt(size);
            count++;
        }

        String f12Code = clistDefaultSize.get(i).getF12Code();
        List<EmDailyK> dailyKs = emClient.getDailyKs(f12Code, LocalDate.now(), 500, false);

        if (dailyKs.size() <= 250) {
            return getTvChartRandom();
        }

        Map<String, BigDecimal[]> ma = EmMaUtil.ma(dailyKs);
        BigDecimal[] ma5 = ma.get("ma5");
        BigDecimal[] ma10 = ma.get("ma10");
        BigDecimal[] ma20 = ma.get("ma20");
        BigDecimal[] ma30 = ma.get("ma30");
        BigDecimal[] ma60 = ma.get("ma60");
        BigDecimal[] ma120 = ma.get("ma120");
        BigDecimal[] ma250 = ma.get("ma250");

        for (int j = 120; j < dailyKs.size(); j++) {


            if (dailyKs.get(j).getPct().compareTo(new BigDecimal(7)) > 0) {

                ArrayList<BigDecimal> mas = new ArrayList<>(Arrays.asList(ma5[j - 1], ma10[j - 1], ma20[j - 1], ma30[j - 1], ma60[j - 1], ma120[j - 1], ma250[j - 1]));
                Collections.sort(mas);
                BigDecimal maMax = mas.get(mas.size() - 1);

                if (dailyKs.get(j - 1).getClose().compareTo(new BigDecimal("0.98").multiply(maMax)) > 0) {
                    // 120 个交易日之后，涨幅超过 7% 的股票
                    return tvTransUtil.tranEmDailyKLineToTv(dailyKs.subList(0, j));
                }
            }
        }

        return getTvChartRandom();
    }

    public static void main(String[] args) {
        // 1 - 10
        ArrayList<Integer> ls = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));

        System.out.println(ls.subList(0, 5));

    }
}
