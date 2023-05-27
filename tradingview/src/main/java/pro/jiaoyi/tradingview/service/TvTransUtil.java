package pro.jiaoyi.tradingview.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.jiaoyi.common.indicator.MaUtil.MaUtil;
import pro.jiaoyi.common.util.DateUtil;
import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.eastm.model.EmDailyK;
import pro.jiaoyi.tradingview.config.Colors;
import pro.jiaoyi.tradingview.model.TvChart;
import pro.jiaoyi.tradingview.model.chart.TvK;
import pro.jiaoyi.tradingview.model.chart.TvTimeValue;
import pro.jiaoyi.tradingview.model.chart.TvVol;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@Component
@Slf4j
public class TvTransUtil {
    @Autowired
    private EmClient emClient;

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

        //设置limit line
        List<TvTimeValue> limitUpList = new ArrayList<>(ks.size());
        List<TvTimeValue> limitDnList = new ArrayList<>(ks.size());

        BigDecimal b1_02 = new BigDecimal("1.02");
        BigDecimal b0_97 = new BigDecimal("0.97");
        BigDecimal close = ks.get(ks.size() - 1).getClose();
        BigDecimal upLimit = close.multiply(b1_02).setScale(2, RoundingMode.HALF_UP);
        BigDecimal dnLimit = close.multiply(b0_97).setScale(2, RoundingMode.HALF_UP);

        for (int i = 0; i < ks.size(); i++) {
            EmDailyK k = ks.get(i);
            if (i == 0) {
                tvChart.setCode(k.getCode());
                tvChart.setName(k.getName());
                tvChart.setCcList(Collections.singletonList(emClient.getCodeBk(k.getCode())));
            }

            String time = DateUtil.strToLocalDate(k.getTradeDate(), DateUtil.PATTERN_yyyyMMdd).toString();

            //设置涨跌幅线
            TvTimeValue upLine = new TvTimeValue();
            upLine.setValue(upLimit);
            upLine.setTime(time);
            limitUpList.add(upLine);

            TvTimeValue dnLine = new TvTimeValue();
            dnLine.setValue(dnLimit);
            dnLine.setTime(time);
            limitDnList.add(dnLine);

            if (i == ks.size() - 1) {
                LocalDate localDate = DateUtil.strToLocalDate(k.getTradeDate(), DateUtil.PATTERN_yyyyMMdd);
                for (int j = 1; j < 10; j++) {

                    TvTimeValue upLine_ = new TvTimeValue();
                    upLine_.setValue(upLimit);
                    upLine_.setTime(localDate.plusDays(j).toString());
                    limitUpList.add(upLine_);

                    TvTimeValue dnLine_ = new TvTimeValue();
                    dnLine_.setValue(dnLimit);
                    dnLine_.setTime(localDate.plusDays(j).toString());
                    limitDnList.add(dnLine_);
                }
            }

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
        int[] kma = {5, 10, 20, 30, 60, 120, 250};
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
        for (int ma : kma) {
            BigDecimal[] maLine = MaUtil.ma(ma, closeArr, 2);
            ma(kList, kMaMap, ma, maLine);
        }
        kMaMap.put("up", limitUpList);
        kMaMap.put("dn", limitDnList);


        //设置成交量均线
        HashMap<String, List<TvTimeValue>> vMaMap = new HashMap<>();
        tvChart.setVMaLines(vMaMap);
        for (int ma : vma) {
            BigDecimal[] maLine = MaUtil.ma(ma, vArr, 0);
            ma(kList, vMaMap, ma, maLine);
        }
        //设置HSL均线
        HashMap<String, List<TvTimeValue>> hslMaMap = new HashMap<>();
        tvChart.setHslMaLines(hslMaMap);
        for (int ma : vma) {
            BigDecimal[] maLine = MaUtil.ma(ma, hslArr, 2);
            ma(kList, hslMaMap, ma, maLine);
        }
        //设置振幅均线
        HashMap<String, List<TvTimeValue>> oscMaMap = new HashMap<>();
        tvChart.setOscMaLines(oscMaMap);
        for (int ma : vma) {
            BigDecimal[] maLine = MaUtil.ma(ma, oscArr, 2);
            ma(kList, oscMaMap, ma, maLine);
        }


        return tvChart;
    }

    private void ma(List<TvK> k, HashMap<String, List<TvTimeValue>> vMaMap, int ma, BigDecimal[] maLine) {
        List<TvTimeValue> maList = new ArrayList<>();
        for (int i = 0; i < maLine.length; i++) {
            if (maLine[i].compareTo(BigDecimal.ZERO) <= 0) {
                //均线为0的不显示
                continue;
            }
            TvTimeValue tvTimeValue = new TvTimeValue();
            tvTimeValue.setTime(k.get(i).getTime());
            tvTimeValue.setValue(maLine[i]);
            maList.add(tvTimeValue);
        }
        vMaMap.put("ma" + ma, maList);
    }
}
