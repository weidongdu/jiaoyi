package pro.jiaoyi.eastm.model.fenshi;

import java.math.BigDecimal;


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

/*
{
"bs": 4,
"p": 167450,
"t": 92448,
"v": 46515
},
 */
public class DetailTrans {
    private long ts;
    private BigDecimal price;
    private long vol;

    //内盘外盘是股市术语之一。
    //内盘S（取英文 sell 卖出 的首字母S）表示，
    //外盘B（取英文  buy 买入 的首字母B）表示。
    private int bs; // 1:内盘 2:外盘 3:中性盘 4:竞价
    //1: (20w大单主动卖出) 绿色向下 青色的现手表示成交额大于20万的内盘分时成交
    //2: (20w大单主动买入)         紫色的现手表示成交额大于20万的外盘分时成交
    //4: 竞价阶段

    public long getTs() {
        return ts;
    }

    public void setTs(long ts) {
        this.ts = ts;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public long getVol() {
        return vol;
    }

    public void setVol(long vol) {
        this.vol = vol;
    }

    public int getBs() {
        return bs;
    }

    public void setBs(int bs) {
        this.bs = bs;
    }


    public BigDecimal amt() {
        return price.multiply(new BigDecimal(vol).multiply(new BigDecimal(100)));
    }

}

