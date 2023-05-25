package pro.jiaoyi.tradingview.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pro.jiaoyi.common.indicator.MaUtil.MaUtil;
import pro.jiaoyi.common.util.DateUtil;
import pro.jiaoyi.eastm.model.EmDailyK;
import pro.jiaoyi.tradingview.config.Colors;
import pro.jiaoyi.tradingview.model.TvChart;
import pro.jiaoyi.tradingview.model.chart.TvK;
import pro.jiaoyi.tradingview.model.chart.TvTimeValue;
import pro.jiaoyi.tradingview.model.chart.TvVol;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@Component
@Slf4j
public class TvTransUtil {

    /**
     * 将K线数据转换为TV格式
     *
     * @param ks
     */
    public TvChart tranEmDailyKLineToTv(List<EmDailyK> ks) {
        if (ks == null || ks.size() == 0) {
            return null;
        }

        TvChart tvChart = new TvChart();

        List<TvK> tvKS = new ArrayList<>();
        List<TvTimeValue> pList = new ArrayList<>();
        List<TvTimeValue> oscList = new ArrayList<>();

        List<TvVol> vList = new ArrayList<>();
        List<TvVol> hslList = new ArrayList<>();

        tvChart.setK(tvKS);
        tvChart.setP(pList);
        tvChart.setOsc(oscList);
        tvChart.setV(vList);
        tvChart.setHsl(hslList);



        for (int i = 0; i < ks.size(); i++) {
            EmDailyK k = ks.get(i);
            if (i == 0) {
                tvChart.setCode(k.getCode());
                tvChart.setName(k.getName());
                tvChart.setCcList(Collections.singletonList("概念"));
            }

            String time = DateUtil.strToLocalDate(k.getTradeDate(), DateUtil.PATTERN_yyyyMMdd).toString();
            //红涨绿跌
            String color = k.getPctChange().compareTo(BigDecimal.ZERO) < 0 ? Colors.GREEN.getColor() : Colors.RED.getColor();

            //生成k线数据
            TvK tvK = new TvK();
            tvK.setOpen(k.getOpen());
            tvK.setClose(k.getClose());
            tvK.setHigh(k.getHigh());
            tvK.setLow(k.getLow());
            tvK.setTime(time);
            tvKS.add(tvK);

            //生成涨跌幅数据
            TvTimeValue pct = new TvTimeValue();
            pct.setTime(time);
            pct.setValue(k.getPct());
            pList.add(pct);


            //振幅数据
            TvTimeValue tvOsc = new TvTimeValue();
            tvOsc.setTime(time);
            tvOsc.setValue(k.getOsc());
            oscList.add(tvOsc);


            //成交量数据
            TvVol tvVol = new TvVol();
            tvVol.setTime(time);
            tvVol.setValue(k.getAmt().setScale(0, RoundingMode.HALF_UP));
            tvVol.setColor(color);
            vList.add(tvVol);

            //换手率数据
            TvVol tvHsl = new TvVol();
            tvHsl.setTime(time);
            tvHsl.setValue(k.getHsl());
            tvHsl.setColor(color);
            hslList.add(tvHsl);

        }
        int[] kma = {5,10,20,30,60,120,250};
        int[] vma = {5, 60};
        //设置价格均线

        HashMap<String, List<TvTimeValue>> kMaMap = new HashMap<>();
        tvChart.setKMaLines(kMaMap);

        List<TvK> kList = tvChart.getK();
        BigDecimal[] closeArr = new BigDecimal[kList.size()];
        BigDecimal[] vArr = new BigDecimal[kList.size()];
        BigDecimal[] hslArr = new BigDecimal[kList.size()];
        BigDecimal[] oscArr = new BigDecimal[kList.size()];

        for (int i = 0; i < kList.size(); i++) {
            closeArr[i] = kList.get(i).getClose();
            vArr[i] = vList.get(i).getValue();
            hslArr[i] = hslList.get(i).getValue();
            oscArr[i] = oscList.get(i).getValue();
        }

        //设置k线均线
        for (int ma: kma) {
            BigDecimal[] maLine = MaUtil.ma(ma, closeArr, 2);
            ma(kList, kMaMap, ma, maLine);
        }

        //设置成交量均线
        HashMap<String, List<TvTimeValue>> vMaMap = new HashMap<>();
        tvChart.setVMaLines(vMaMap);
        for (int ma: vma) {
            BigDecimal[] maLine = MaUtil.ma(ma, vArr, 0);
            ma(kList, vMaMap, ma, maLine);
        }
        //设置HSL均线
        HashMap<String, List<TvTimeValue>> hslMaMap = new HashMap<>();
        tvChart.setHslMaLines(hslMaMap);
        for (int ma: vma) {
            BigDecimal[] maLine = MaUtil.ma(ma, hslArr, 2);
            ma(kList, hslMaMap, ma, maLine);
        }
        //设置振幅均线
        HashMap<String, List<TvTimeValue>> oscMaMap = new HashMap<>();
        tvChart.setOscMaLines(oscMaMap);
        for (int ma: vma) {
            BigDecimal[] maLine = MaUtil.ma(ma, oscArr, 2);
            ma(kList, oscMaMap, ma, maLine);
        }


        return tvChart;
    }

    private void ma(List<TvK> k, HashMap<String, List<TvTimeValue>> vMaMap, int ma, BigDecimal[] maLine) {
        List<TvTimeValue> maList = new ArrayList<>();
        for (int i = 0; i < maLine.length; i++) {
            TvTimeValue tvTimeValue = new TvTimeValue();
            tvTimeValue.setTime(k.get(i).getTime());
            tvTimeValue.setValue(maLine[i]);
            maList.add(tvTimeValue);
        }
        vMaMap.put("ma" + ma, maList);
    }
}
