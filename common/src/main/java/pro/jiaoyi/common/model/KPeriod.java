package pro.jiaoyi.common.model;

public enum KPeriod {
    D1("day1", "日K");

    private String p;
    private String desc;

    KPeriod(String p, String desc) {
        this.p = p;
        this.desc = desc;
    }

    public String getP() {
        return p;
    }

    public String getDesc() {
        return desc;
    }

    public static KPeriod fromP(String p) {
        for (KPeriod kPeriod : KPeriod.values()) {
            if (kPeriod.getP().equals(p)) {
                return kPeriod;
            }
        }
        return null;
    }

}
