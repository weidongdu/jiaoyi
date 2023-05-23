package pro.jiaoyi.tushare.model;

import lombok.Data;

import java.util.List;

@Data
public class TushareResult {
    private String request_id;
    private int code;
    private String msg;
    private DataResult data;

    @Data
    public static class DataResult {
        private List<String> fields;
        private boolean has_more;
        private List<List<String>> items;
    }
}

