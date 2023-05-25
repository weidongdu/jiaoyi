package pro.jiaoyi.tradingview.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.eastm.model.EmDailyK;
import pro.jiaoyi.tradingview.model.TvChart;

import java.time.LocalDate;
import java.util.List;

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
    public TvChart getTvChart(String code, LocalDate date, Integer limit) {
        List<EmDailyK> dailyKs = emClient.getDailyKs(code, date, limit, false);
        return tvTransUtil.tranEmDailyKLineToTv(dailyKs);
    }
}
