package pro.jiaoyi.eastm.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class EmDataKline {

    private String code;
    private int market;
    private String name;
    private int decimal;
    private int dktotal;
    private BigDecimal preKPrice;
    private List<String> klines;

    // getters and setters
}
