package pro.jiaoyi.eastm.model;

import lombok.Data;



@Data
public class EmResult<T> {
    private int rc;
    private int rt;
    private long svr;
    private int lt;
    private int full;
    private String dlmkts;
    private T data;

}
