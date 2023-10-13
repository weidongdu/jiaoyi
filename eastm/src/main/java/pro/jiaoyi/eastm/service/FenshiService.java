package pro.jiaoyi.eastm.service;

import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pro.jiaoyi.common.util.BDUtil;
import pro.jiaoyi.common.util.DateUtil;
import pro.jiaoyi.eastm.api.EmClient;
import pro.jiaoyi.eastm.dao.entity.EastGetStockFenShiTransEntity;
import pro.jiaoyi.eastm.dao.repo.FenshiRepo;
import pro.jiaoyi.eastm.model.EmDailyK;
import pro.jiaoyi.eastm.model.fenshi.EastGetStockFenShiTrans;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class FenshiService {

    @Resource
    private FenshiRepo fenshiRepo;
    @Resource
    private EmClient emClient;


    public int saveOrUpdate(EastGetStockFenShiTrans f, List<EmDailyK> ks) {

        LocalDate td = LocalDate.now();
        if (ks != null && ks.size() > 0) {
            EmDailyK lastK = ks.get(ks.size() - 1);
            String tradeDate = lastK.getTradeDate();
            td = DateUtil.strToLocalDate(tradeDate, "yyyyMMdd");
        }

        EastGetStockFenShiTransEntity dbEntity = fenshiRepo.findByCodeAndCreateDate(f.getCode(), td);

        if (dbEntity != null) {
            if (dbEntity.getTotalCount() == f.getTotalCount()) {
                log.info("old fenshi data, total count is same, return");
                return 0;
            } else {
                log.info("old fenshi data, total count is not same, update");
                update(f, dbEntity, ks);
                return 1;
            }
        }

        save(f, ks);
        return 1;
    }

    private void save(EastGetStockFenShiTrans f, List<EmDailyK> ks) {
        EastGetStockFenShiTransEntity entity = new EastGetStockFenShiTransEntity();
        entity.setCode(f.getCode());//String code;//: "300144",
        entity.setMarket(f.getMarket());//int market;//: 0,
        entity.setName(f.getName());//String name;//: "宋城演艺",
        entity.setCt(f.getCt());//int ct;//: 0,
        entity.setClosePre(f.getClosePre());//BigDecimal closePre;//: 15270,
        entity.setTotalCount(f.getTotalCount());//int totalCount;//: 4409,
        entity.setData(JSON.toJSONString(f.getData()));//List<DetailTrans> data;
        entity.setOpenPrice(f.getOpenPrice());//BigDecimal openPrice;//开盘价
        if (f.getOpenPrice() == null) {
            entity.setOpenPct(BigDecimal.ZERO);//BigDecimal openPrice;//开盘价
        } else {
            BigDecimal openPct = f.getOpenPrice().subtract(f.getClosePre()).divide(f.getClosePre(), 4, RoundingMode.HALF_UP);
            entity.setOpenPct(openPct);//BigDecimal openPrice;//开盘价
        }
        entity.setOpenVol(f.getOpenVol());//BigDecimal openVol;//开盘量
        entity.setOpenVolStr(BDUtil.amtHuman(f.getOpenVol()));//BigDecimal openVol;//开盘量
        entity.setOpenAmt(f.getOpenAmt());//BigDecimal openAmt;//开盘额
        entity.setOpenAmtStr(BDUtil.amtHuman(f.getOpenAmt()));//BigDecimal openAmt;//开盘额

        if (ks == null || ks.size() == 0) {
            entity.setCreateTime(LocalDateTime.now());//BigDecimal openAmt;//开盘额
            entity.setUpdateTime(LocalDateTime.now());//BigDecimal openAmt;//开盘额
            entity.setCreateDate(LocalDate.now());//BigDecimal openAmt;//开盘额
        } else {
            String tradeDate = ks.get(ks.size() - 1).getTradeDate();
            LocalDateTime tdt = DateUtil.strToLocalDateTime(tradeDate + " 00:00:00", DateUtil.PATTERN_yyyyMMdd + " " + DateUtil.PATTERN_HH_mm_ss);
            LocalDate td = DateUtil.strToLocalDate(tradeDate, DateUtil.PATTERN_yyyyMMdd);
            entity.setCreateTime(tdt);//BigDecimal openAmt;//开盘额
            entity.setUpdateTime(tdt);//BigDecimal openAmt;//开盘额
            entity.setCreateDate(td);//BigDecimal openAmt;//开盘额
        }

        setFx(entity, ks);
        fenshiRepo.saveAndFlush(entity);
    }

    private void update(EastGetStockFenShiTrans f, EastGetStockFenShiTransEntity dbEntity, List<EmDailyK> ks) {
        dbEntity.setCt(f.getCt());//int ct;//: 0,
        dbEntity.setTotalCount(f.getTotalCount());//int totalCount;//: 4409,
        dbEntity.setData(JSON.toJSONString(f.getData()));//List<DetailTrans> data;
        dbEntity.setUpdateTime(LocalDateTime.now());//BigDecimal openAmt;//开盘额

        setFx(dbEntity, ks);
        fenshiRepo.saveAndFlush(dbEntity);
    }


    public void setFx(EastGetStockFenShiTransEntity dbEntity, List<EmDailyK> ks) {
//        String code = dbEntity.getCode();
//        List<EmDailyK> ks = emClient.getDailyKs(code, LocalDate.now(), 200, true);
        if (ks == null || ks.size() < 70) return;
        EmDailyK k = ks.get(ks.size() - 1);
        ks.remove(ks.size() - 1);
        //计算 amt
        BigDecimal dayAmtTop10 = emClient.amtTop10p(ks);
        BigDecimal hourAmt = dayAmtTop10.divide(BigDecimal.valueOf(4), 0, RoundingMode.HALF_UP);
        BigDecimal fAmt = BDUtil.b0_1.multiply(hourAmt);
        //成交量放大倍数


        dbEntity.setHourAmt(hourAmt);
        dbEntity.setHourAmtStr(BDUtil.amtHuman(hourAmt));
        dbEntity.setFAmtM1(fAmt);
        dbEntity.setFAmtM1Str(BDUtil.amtHuman(fAmt));

        if (dbEntity.getOpenAmt() != null) {
            BigDecimal fx = dbEntity.getOpenAmt().divide(fAmt, 4, RoundingMode.HALF_UP);
            dbEntity.setFx(fx);
        }

        dbEntity.setAmt(k.getAmt());
        dbEntity.setAmtStr(BDUtil.amtHuman(k.getAmt()));
        dbEntity.setClose(k.getClose());
        dbEntity.setPct(k.getPct());
        dbEntity.setHsl(k.getHsl());
        dbEntity.setBk(k.getBk());

        if (k.getOpen().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal openPct = k.getOpen().subtract(k.getPreClose()).divide(k.getPreClose(), 4, RoundingMode.HALF_UP);
            dbEntity.setOpenPrice(k.getOpen());
            dbEntity.setOpenPrice(openPct);

            BigDecimal holdPct = k.getClose().subtract(k.getOpen()).divide(k.getOpen(), 4, RoundingMode.HALF_UP);
            dbEntity.setHoldPct(holdPct);
        }

    }
}
