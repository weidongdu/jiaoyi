package pro.jiaoyi.common.meta;

public enum SideEnum {
    UP(1), DOWN(-1), NONE(0);

    private int value;

    SideEnum(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

}
