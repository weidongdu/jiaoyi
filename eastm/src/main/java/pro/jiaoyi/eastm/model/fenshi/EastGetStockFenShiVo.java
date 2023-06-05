package pro.jiaoyi.eastm.model.fenshi;

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

public class EastGetStockFenShiVo {

    private String c;//: "300144",
    private int m;//: 0,
    private String n;//: "宋城演艺",
    private int ct;//: 0,
    private int cp;//: 15270,
    private int tc;//: 4409,
    private List<Detail> data;

    public String getC() {
        return c;
    }

    public void setC(String c) {
        this.c = c;
    }

    public int getM() {
        return m;
    }

    public void setM(int m) {
        this.m = m;
    }

    public String getN() {
        return n;
    }

    public void setN(String n) {
        this.n = n;
    }

    public int getCt() {
        return ct;
    }

    public void setCt(int ct) {
        this.ct = ct;
    }

    public int getCp() {
        return cp;
    }

    public void setCp(int cp) {
        this.cp = cp;
    }

    public int getTc() {
        return tc;
    }

    public void setTc(int tc) {
        this.tc = tc;
    }

    public List<Detail> getData() {
        return data;
    }

    public void setData(List<Detail> data) {
        this.data = data;
    }
}

class Detail {
    private int t;
    private int p;
    private int v;
    private int bs;

    public int getT() {
        return t;
    }

    public void setT(int t) {
        this.t = t;
    }

    public int getP() {
        return p;
    }

    public void setP(int p) {
        this.p = p;
    }

    public int getV() {
        return v;
    }

    public void setV(int v) {
        this.v = v;
    }

    public int getBs() {
        return bs;
    }

    public void setBs(int bs) {
        this.bs = bs;
    }
}