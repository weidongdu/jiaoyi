package pro.jiaoyi.eastm.model.fenshi;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


/*
{
    "rc": 0,
    "rt": 108,
    "svr": 182481519,
    "lt": 2,
    "full": 0,
    "data": {
        "c": "300144",
        "m": 0,
        "n": "宋城演艺",
        "ct": 0,
        "cp": 15270,
        "tc": 4409,
        "data": [
            {
                "t": 150003,
                "p": 15630,
                "v": 5009,
                "bs": 1
            }
        ]
    }
}
*/

public class EastGetStockFenShiTrans {

    private String code;//: "300144",
    private int market;//: 0,
    private String name;//: "宋城演艺",
    private int ct;//: 0,
    private BigDecimal closePre;//: 15270,
    private int totalCount;//: 4409,
    private List<DetailTrans> data;


    private BigDecimal openPrice;//开盘价
    private BigDecimal openVol;//开盘量
    private BigDecimal openAmt;//开盘额

    public void setOpenPrice(BigDecimal openPrice) {
        this.openPrice = openPrice;
    }

    public void setOpenVol(BigDecimal openVol) {
        this.openVol = openVol;
    }

    public void setOpenAmt(BigDecimal openAmt) {
        this.openAmt = openAmt;
    }

    public BigDecimal getOpenPrice() {
        return openPrice;
    }

    public BigDecimal getOpenVol() {
        return openVol;
    }

    public BigDecimal getOpenAmt() {
        return openAmt;
    }

    //    public void f(int mm) {
//        f(mm, null);
//    }
//
//    public static final long M1S = 60 * 1000L;
//
//    public static final BigDecimal B100 = new BigDecimal("100");
//    public static final BigDecimal B0 = BigDecimal.ZERO;
//    public static final BigDecimal B0_1 = new BigDecimal("0.1");
//    public static final BigDecimal B0_01 = new BigDecimal("0.01");
//    public static final BigDecimal B0_001 = new BigDecimal("0.001");

//    public List<StockEastVolEntity> f(int mm, BigDecimal avgTopAmt, boolean detail) {
//
//        ArrayList<StockEastVolEntity> list = new ArrayList<>();
//
//        for (int i = 0; i < data.size(); i++) {
//            //init
//            BigDecimal totalAmt = BigDecimal.ZERO;
//            BigDecimal amtS1 = BigDecimal.ZERO;
//            BigDecimal amtB2 = BigDecimal.ZERO;
//            BigDecimal totalVol = BigDecimal.ZERO;
//
//            DetailTrans detailTrans = data.get(i);
//            BigDecimal priceUpSpeedPct = BigDecimal.ZERO;
//
//            for (int j = 0; j < Integer.MAX_VALUE; j++) {
//                if (i - j < 0) break;
//
//                DetailTrans di = data.get(i - j);
//                //取三分钟涨速
//                if (di.getTs() < detailTrans.getTs() - M1S * 3) {
//                    priceUpSpeedPct = detailTrans.getPrice().subtract(di.getPrice()).divide(closePre, 4, RoundingMode.HALF_UP);
//                    break;
//                }
//
//                if (di.getTs() > detailTrans.getTs() - M1S * mm) {
//
//                    //价格 * 成交量
//                    totalAmt = totalAmt.add(di.getPrice().multiply(BigDecimal.valueOf(di.getVol()).multiply(B100)));
//                    totalVol = totalVol.add(BigDecimal.valueOf(di.getVol()));
//
//                    if (di.getBs() == 1) {
//                        amtS1 = amtS1.add(di.getPrice().multiply(BigDecimal.valueOf(di.getVol()).multiply(B100)));
//                    }
//
//                    if (di.getBs() == 2) {
//                        amtB2 = amtB2.add(di.getPrice().multiply(BigDecimal.valueOf(di.getVol()).multiply(B100)));
//                    }
//
//                }
//
//            }
//
//            if (avgTopAmt == null) {
////                System.out.println(out);
//            } else {
//                BigDecimal amtTotalPct = BigDecimal.ZERO;// 累计成交量/top20 Hour成交量 占比
//                BigDecimal amtNetVolPct = BigDecimal.ZERO;//净成交量
//                BigDecimal pricePct = detailTrans.getPrice().subtract(closePre).divide(closePre, 4, RoundingMode.HALF_UP);//.compareTo(b003);
//                try {
//                    amtTotalPct = totalAmt.divide(avgTopAmt, 2, RoundingMode.HALF_UP);
//                    amtNetVolPct = amtB2.subtract(amtS1).divide(avgTopAmt, 2, RoundingMode.HALF_UP);
//                } catch (ArithmeticException e) {
//                    e.printStackTrace();
//                }
//
//
//                //设置条件
//                // 成交量占比 分钟*0.1 top20 hour avg
//                boolean cv1 = amtTotalPct.compareTo(B0_1.multiply(BigDecimal.valueOf(mm))) > 0;
//                // 净成交量占比 > (0.1 / 2) top20 hour avg
//                boolean cv11 = amtNetVolPct.compareTo(B0_01.multiply(BigDecimal.valueOf(5)).multiply(BigDecimal.valueOf(mm))) > 0;
//                // 累计成交量 大于1000w
//                boolean cv2 = totalAmt.compareTo(BigDecimal.valueOf(10000000L).multiply(BigDecimal.valueOf(mm))) > 0;
//                //涨速 > 0.1
//                boolean cps = priceUpSpeedPct.compareTo(B0_01) > 0;
//                //涨幅 > 0.1 < 0.7
////                boolean cph = pricePct.compareTo(B0_01.multiply(BigDecimal.valueOf(7))) < 1;
//                boolean cpl = pricePct.compareTo(BigDecimal.ZERO) > 0;
//
//
////                if (cv1 && cv11 && cv2 && cps && cpl) {
//                if ((cv1 && cv11 && cv2 && cps && cpl) || detail) {
//                    StockEastVolEntity entity = new StockEastVolEntity();
//                    list.add(entity);
//
//                    entity.setCode(code);
//                    entity.setName(name);
//                    entity.setTs(new Date(detailTrans.getTs()));
//                    entity.setPeriod(mm);
//                    entity.setAmtS1(amtS1.doubleValue());
//                    entity.setAmtB2(amtB2.doubleValue());
//                    entity.setAmtTotal(totalAmt.doubleValue());
//                    entity.setAmtNet(amtB2.subtract(amtS1).doubleValue());
//                    entity.setPctTotalAmt(amtTotalPct.doubleValue());
//                    entity.setPctNetAmt(amtNetVolPct.doubleValue());
//                    entity.setPctPriceUpSpeed(priceUpSpeedPct.doubleValue());
//                    entity.setPrice(detailTrans.getPrice().doubleValue());
//                    entity.setPctPrice(pricePct.doubleValue());
//                    entity.setCreateTime(new Date());
//                }
//            }
//
//
//        }
//        return list;
//    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getMarket() {
        return market;
    }

    public void setMarket(int market) {
        this.market = market;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCt() {
        return ct;
    }

    public void setCt(int ct) {
        this.ct = ct;
    }

    public BigDecimal getClosePre() {
        return closePre;
    }

    public void setClosePre(BigDecimal closePre) {
        this.closePre = closePre;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public List<DetailTrans> getData() {
        return data;
    }

    public void setData(List<DetailTrans> data) {
        this.data = data;
    }


    public static EastGetStockFenShiTrans trans(EastGetStockFenShiVo vo, String date) {
        if (date == null) {
            return trans(vo);
        } else {

            EastGetStockFenShiTrans fs = new EastGetStockFenShiTrans();
            fs.setCode(vo.getC());
            fs.setName(vo.getN());

            fs.setMarket(vo.getM());
            fs.setCt(vo.getCt());

            String cps = String.valueOf(vo.getCp());
            if (cps.length() < 3) return null;


            fs.setClosePre(new BigDecimal(cps.substring(0, cps.length() - 3) + "." + cps.substring(cps.length() - 3)));
            fs.setTotalCount(vo.getTc());

            if (vo.getTc() > 0) {
                ArrayList<DetailTrans> list = new ArrayList<>(vo.getTc());
                for (Detail d : vo.getData()) {
                    DetailTrans detailTrans = new DetailTrans();

                    String sts = String.valueOf(d.getT());
//                    String ts = LocalDate.now().toString();
                    String ts = date;

                    if (sts.length() == 5) {//93809
                        ts += " 0" + sts.charAt(0) + ":" + sts.substring(1, 3) + ":" + sts.substring(3);
                    } else {
                        ts += " " + sts.substring(0, 2) + ":" + sts.substring(2, 4) + ":" + sts.substring(4);
                    }

                    LocalDateTime parse = LocalDateTime.parse(ts, DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss"));
                    detailTrans.setTs(parse.toInstant(ZoneOffset.ofHours(8)).toEpochMilli());
                    String ps = String.valueOf(d.getP());
                    detailTrans.setPrice(new BigDecimal(ps.substring(0, ps.length() - 3) + "." + ps.substring(ps.length() - 3)));

                    detailTrans.setVol(d.getV());
                    detailTrans.setBs(d.getBs());

                    list.add(detailTrans);
                }

                fs.setData(list);
            }

            return fs;
        }
    }

    public static EastGetStockFenShiTrans trans(EastGetStockFenShiVo vo) {
        /*
    private String code;//: "300144",
    private int market;//: 0,
    private String name;//: "宋城演艺",
    private int ct;//: 0,
    private BigDecimal closePre;//: 15270,
    private int totalCount;//: 4409,
    private List<DetailTrans> data;
         */

        EastGetStockFenShiTrans fs = new EastGetStockFenShiTrans();
        fs.setCode(vo.getC());
        fs.setName(vo.getN());

        fs.setMarket(vo.getM());
        fs.setCt(vo.getCt());

        String cps = String.valueOf(vo.getCp());
        if (cps.length() < 3) return null;


        fs.setClosePre(new BigDecimal(cps.substring(0, cps.length() - 3) + "." + cps.substring(cps.length() - 3)));
        fs.setTotalCount(vo.getTc());

        BigDecimal openPrice = BigDecimal.ZERO;
        BigDecimal openVol = BigDecimal.ZERO;
        BigDecimal openAmt = BigDecimal.ZERO;
        if (vo.getTc() > 0) {
            ArrayList<DetailTrans> list = new ArrayList<>(vo.getTc());
            for (Detail d : vo.getData()) {
                DetailTrans detailTrans = new DetailTrans();

                String sts = String.valueOf(d.getT());
                String ts = LocalDate.now().toString();
//                String ts = "2023-10-27";//LocalDate.now().toString();

                if (sts.length() == 5) {//93809
                    ts += " 0" + sts.charAt(0) + ":" + sts.substring(1, 3) + ":" + sts.substring(3);
                } else {
                    ts += " " + sts.substring(0, 2) + ":" + sts.substring(2, 4) + ":" + sts.substring(4);
                }


                LocalDateTime parse = LocalDateTime.parse(ts, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                detailTrans.setTs(parse.toInstant(ZoneOffset.ofHours(8)).toEpochMilli());
                String ps = String.valueOf(d.getP());
                detailTrans.setPrice(new BigDecimal(ps.substring(0, ps.length() - 3) + "." + ps.substring(ps.length() - 3)));

                detailTrans.setVol(d.getV());
                detailTrans.setBs(d.getBs());

                if (sts.startsWith("9250")){ //竞价解读
                    openPrice = detailTrans.getPrice();
                    openVol = BigDecimal.valueOf(d.getV());
                    openAmt = openPrice.multiply(openVol).multiply(BigDecimal.valueOf(100));
                    fs.setOpenPrice(openPrice);
                    fs.setOpenVol(openVol);
                    fs.setOpenAmt(openAmt);
                }

                list.add(detailTrans);
            }

            fs.setData(list);
        }

        return fs;
    }
}

/*
{
"bs": 4,
"p": 167450,
"t": 92448,
"v": 46515
},
 */
//class DetailTrans {
//    private long ts;
//    private BigDecimal price;
//    private long vol;
//
//    //内盘外盘是股市术语之一。
//    //内盘S（取英文 sell 卖出 的首字母S）表示，
//    //外盘B（取英文buy 买入 的首字母B）表示。
//    private int bs;
//    //1: (20w大单主动卖出) 绿色向下 青色的现手表示成交额大于20万的内盘分时成交
//    //2: (20w大单主动买入)         紫色的现手表示成交额大于20万的外盘分时成交
//    //4: 竞价阶段
//
//    public long getTs() {
//        return ts;
//    }
//
//    public void setTs(long ts) {
//        this.ts = ts;
//    }
//
//    public BigDecimal getPrice() {
//        return price;
//    }
//
//    public void setPrice(BigDecimal price) {
//        this.price = price;
//    }
//
//    public long getVol() {
//        return vol;
//    }
//
//    public void setVol(long vol) {
//        this.vol = vol;
//    }
//
//    public int getBs() {
//        return bs;
//    }
//
//    public void setBs(int bs) {
//        this.bs = bs;
//    }
//}

