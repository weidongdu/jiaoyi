package pro.jiaoyi.search.scraper;

import lombok.Data;

import java.util.List;

@Data
public class SearchResult {

    private String doc;//原文

    private String keyword;//关键词
    private String source;//baidu zhihu
    private String plat;//pc mobile
    private String type;//dropdown related result

    private List<Item> items;//搜索结果

    private List<String> keywordRelated;//相关搜索
    private List<String> keywordDropdown;//下拉框

    public SearchResult(String keyword, String source, String plat, String type) {
        this.keyword = keyword;
        this.source = source;
        this.plat = plat;
        this.type = type;
    }

    public SearchResult() {
    }
    @Data
    public static class Item {
        private String title;
        private String content;
        private String url;
        private String realUrl;//真实url
        private int page;//搜索结果页数
        private int rank;//搜索结果排名
    }
}


