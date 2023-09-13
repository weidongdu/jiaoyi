package pro.jiaoyi.search;

import com.alibaba.fastjson.JSONArray;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import pro.jiaoyi.common.util.CollectionsUtil;
import pro.jiaoyi.search.dao.repo.KeywordsWaitToSearchRepo;
import pro.jiaoyi.search.dao.repo.SearchResultRepo;
import pro.jiaoyi.search.util.SimilarityUtil;
import pro.jiaoyi.search.util.Text2Keyword;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static pro.jiaoyi.search.config.CutModeEnum.cut_for_search;

@SpringBootTest
@Slf4j
class TDIDFTests {

    @Test
    void contextLoads() {
    }


    @Resource
    private Text2Keyword text2Keyword;

    @Resource
    private SearchResultRepo searchResultRepo;
    @Resource
    private KeywordsWaitToSearchRepo keywordsWaitToSearchRepo;

    public Set<String> keyword(String master){
//        String master = "路由器";
        List<String> keyword = searchResultRepo.findKeyword(master);

        HashSet<String> set = new HashSet<>(keyword);
        List<String> keywordRs = searchResultRepo.findKeywordRs(master);
        for (String keywordR : keywordRs) {
            List<String> list = JSONArray.parseArray(keywordR).toJavaList(String.class);
            set.addAll(list);
        }

        List<String> keywordBaidu = keywordsWaitToSearchRepo.findKeyword(master);
        set.addAll(keywordBaidu);

        return set;
    }

    @Test
    public void keywordAnalyse(){

        String master = "路由器";
        Set<String> keywords = keyword(master);
        HashMap<String, List<String>> word2KeywordListMap = new HashMap<>();
        //针对每个keyword 分词
        for (String keyword : keywords) {
            List<String> words = text2Keyword.text2KeywordList(keyword, cut_for_search);
            log.info("keyword: {}, words: {}", keyword, words);
            word2KeywordListMap.put(keyword, words);
        }

        BigDecimal sLimit = new BigDecimal("0.9");

        //计算相似度
        for (String keyword : keywords) {
            List<String> wordsSrc = word2KeywordListMap.get(keyword);

            HashMap<String, BigDecimal> sMap = new HashMap<>();

            for (String keyword1 : keywords) {
                if (keyword.equals(keyword1)){
                    continue;
                }
                List<String> wordsDst = word2KeywordListMap.get(keyword1);
                if (wordsDst == null || wordsDst.isEmpty()){
                    continue;
                }

                double similarity = SimilarityUtil.calculateCosineSimilarity(wordsSrc, wordsDst);
                BigDecimal v = null;
                try {
                    v = new BigDecimal(String.valueOf(similarity)).setScale(4, RoundingMode.HALF_UP);
                } catch (Exception e) {
                    log.error("keyword: {}, keyword1: {}, similarity: {}", keyword, keyword1, similarity);
                    v = BigDecimal.ZERO;
                }
                if (v.compareTo(sLimit) > 0){
                    sMap.put(keyword1, v);
                }
            }

            Map<String, BigDecimal> sortMap = CollectionsUtil.sortByValue(sMap, false);
            sortMap.forEach((k, v) -> {
                log.info("keyword: {}, keyword1: {}, similarity: {}", keyword, k, v);
            });
        }
    }


    public static List<String> genKeywords() {
        ArrayList<String> list = new ArrayList<>();


        list.add("咖啡师资格证怎么考");
        list.add("咖啡");
        list.add("咖啡的功效与作用及副作用");
        list.add("咖啡店加盟10大品牌排行");
        list.add("咖啡英文");
        list.add("咖啡的种类及口味");
        list.add("咖啡店加盟");
        list.add("咖啡品牌排行榜前十名");
        list.add("咖啡对身体有什么好处和坏处");
        list.add("咖啡arabica");
        list.add("咖啡app");
        list.add("咖啡a股上市公司有哪些");
        list.add("咖啡ag值是什么意思");
        list.add("咖啡app聊天软件下载");
        list.add("咖啡asd是什么意思");
        list.add("咖啡agf和ucc");
        list.add("咖啡杯");
        list.add("咖啡杯图片");
        list.add("咖啡不能和什么一起吃");
        list.add("咖啡伴侣");
        list.add("咖啡保质期一般多长时间");
        list.add("咖啡costa");
        list.add("咖啡coffee");
        list.add("咖啡促进新陈代谢");
        list.add("咖啡产地排名");
        list.add("咖啡冲泡方法");
        list.add("咖啡萃取");
        list.add("咖啡combo");
        list.add("咖啡creamer");
        list.add("咖啡产地");
        list.add("咖啡成分有哪些");
        list.add("咖啡dirty");
        list.add("咖啡的功效与作用");
        list.add("咖啡的危害");
        list.add("咖啡豆");
        list.add("咖啡的英文");
        list.add("咖啡driy");
        list.add("咖啡espresso");
        list.add("咖啡emoji");
        list.add("咖啡espresso什么意思");
        list.add("咖啡eco是什么意思");
        list.add("咖啡emoji表情");
        list.add("咖啡ebf是什么意思");
        list.add("咖啡esp是什么意思");
        list.add("咖啡二合一和三合一哪个好点");
        list.add("咖啡二次萃取");
        list.add("咖啡儿童喝了好不好");
        list.add("咖啡放一晚上第二天能喝吗");
        list.add("咖啡粉");
        list.add("咖啡粉如何冲泡咖啡");
        list.add("咖啡放冰箱第二天能喝吗");
        list.add("咖啡粉渣有啥用");
        list.add("咖啡副作用");
        list.add("咖啡发酸怎么回事");
        list.add("咖啡粉过期了还能喝吗");
        list.add("咖啡flat white");
        list.add("咖啡分类");
        list.add("咖啡过期还能喝吗?");
        list.add("咖啡过一夜能喝吗");
        list.add("咖啡公社");
        list.add("咖啡g1g2代表什么意思");
        list.add("咖啡灌肠的作用与功效");
        list.add("咖啡隔夜还能喝吗?");
        list.add("咖啡过敏的症状");
        list.add("咖啡馆");
        list.add("咖啡过期了但是密封没打开还能喝么");
        list.add("咖啡功效");
        list.add("咖啡喝多了对身体有什么影响吗");
        list.add("咖啡喝多了会怎么样");
        list.add("咖啡会不会长胖");
        list.add("咖啡和酒能不能一起喝");
        list.add("咖啡喝多了心慌怎么办");
        list.add("咖啡喝多了恶心想吐怎么办");
        list.add("咖啡和茶能一起喝吗");
        list.add("咖啡会上火吗");
        list.add("咖啡喝多了会不孕不育吗");
        list.add("咖啡喝多了有什么副作用");
        list.add("咖啡illy");
        list.add("咖啡ip形象设计案例");
        list.add("咖啡渣的用途");
        list.add("咖啡渍怎么去除");
        list.add("咖啡渍弄到白衣服上怎么办");
        list.add("咖啡渣怎么做肥料");
        list.add("咖啡深度烘焙 中度烘焙的区别");
        list.add("咖啡渣");
        list.add("咖啡减肥有效果吗");
        list.add("咖啡加牛奶");
        list.add("咖啡机");
        list.add("咖啡解药性吗");
        list.add("咖啡加蜂蜜");
        list.add("咖啡胶囊");
        list.add("咖啡机怎么操作");
        list.add("咖啡加奶叫什么");
        list.add("咖啡减肥是真的吗");
        list.add("咖啡结块还能不能喝");
        list.add("咖啡可以隔夜喝吗");
        list.add("咖啡可以空腹喝吗?");
        list.add("咖啡可以减肥吗");
        list.add("咖啡空腹喝好还是饭后喝好");
        list.add("咖啡可以天天喝吗?");
        list.add("咖啡可以消肿吗");
        list.add("咖啡可以用凉水冲么");
        list.add("咖啡可以加蜂蜜吗");
        list.add("咖啡可以放冰箱第二天喝吗");
        list.add("咖啡可以提高新陈代谢吗");
        list.add("咖啡利尿吗");
        list.add("咖啡luckin coffee");
        list.add("咖啡logo创意设计");
        list.add("咖啡拉花");
        list.add("咖啡logo");
        list.add("咖啡里面有什么成分");
        list.add("咖啡利尿作用强吗");
        list.add("咖啡logo设计");
        list.add("咖啡logo图片");
        list.add("咖啡拉花教学");
        list.add("咖啡manner");
        list.add("咖啡mstand");
        list.add("咖啡名字");
        list.add("咖啡猫");
        list.add("咖啡m是什么意思");
        list.add("咖啡名字创意大全");
        list.add("咖啡每天喝多少克合适");
        list.add("咖啡每天喝一杯对身体有伤害吗");
        list.add("咖啡沫子可以养花吗");
        list.add("咖啡磨豆机");
        list.add("咖啡能减肥吗");
        list.add("咖啡能隔夜喝吗");
        list.add("咖啡你冲不冲");
        list.add("咖啡弄到衣服用什么洗的掉");
        list.add("咖啡能提神几个小时");
        list.add("咖啡弄到白衣服能洗掉吗");
        list.add("咖啡能用冷水冲吗");
        list.add("咖啡能提高新陈代谢吗");
        list.add("咖啡浓缩是什么意思");
        list.add("咖啡能天天喝吗");
        list.add("咖啡o和咖啡c的区别");
        list.add("咖啡oz是什么意思");
        list.add("咖啡or tea");
        list.add("咖啡one shot");
        list.add("咖啡oz是什么单位");
        list.add("咖啡欧蕾");
        list.add("咖啡品牌");
        list.add("咖啡嘌呤高吗?");
        list.add("咖啡品种");
        list.add("咖啡ph值多少");
        list.add("咖啡ph值");
        list.add("咖啡配什么食物");
        list.add("咖啡pca证书");
        list.add("咖啡配纯牛奶会怎么样");
        list.add("咖啡ppt");
        list.add("咖啡q证是什么证");
        list.add("咖啡起源于哪个国家");
        list.add("咖啡q grader");
        list.add("咖啡去水肿吗");
        list.add("咖啡起源");
        list.add("咖啡浅烘和深烘区别");
        list.add("咖啡去湿气吗");
        list.add("咖啡祛湿吗");
        list.add("咖啡热量高吗");
        list.add("咖啡ristretto");
        list.add("咖啡热量多少大卡");
        list.add("咖啡热气上火吗");
        list.add("咖啡容易发胖吗?");
        list.add("咖啡瑞幸");
        list.add("咖啡raw");
        list.add("咖啡渣可以直接放在花盆里吗");
        list.add("咖啡热量排行");
        list.add("咖啡色分泌物是怎么回事");
        list.add("咖啡什么时候喝最好");
        list.add("咖啡色");
        list.add("咖啡sca是什么");
        list.add("咖啡酸片");
        list.add("咖啡树");
        list.add("咖啡色搭配什么颜色最佳");
        list.add("咖啡酸片的作用与功效");
        list.add("咖啡上火吗");
        list.add("咖啡tims");
        list.add("咖啡糖");
        list.add("咖啡厅");
        list.add("咖啡图片");
        list.add("咖啡天天喝,对身体有害吗");
        list.add("咖啡糖果的功效与作用及副作用");
        list.add("咖啡图片真实图片");
        list.add("咖啡图片唯美图片");
        list.add("咖啡推荐");
        list.add("咖啡通便吗");
        list.add("咖啡ucc");
        list.add("咖啡用热水还是冷水");
        list.add("咖啡ucc是什么意思");
        list.add("咖啡ucc和agf哪个好");
        list.add("咖啡v型是什么意思");
        list.add("咖啡vc可以一起喝吗");
        list.add("咖啡v60是什么意思");
        list.add("咖啡vietcheck在线查真伪");
        list.add("咖啡王子一号店");
        list.add("咖啡污渍怎么去除衣服");
        list.add("咖啡文案");
        list.add("咖啡文案短句干净治愈");
        list.add("咖啡为什么会酸");
        list.add("咖啡为什么提神");
        list.add("咖啡文案朋友圈");
        list.add("咖啡wbc是什么意思");
        list.add("咖啡wbc");
        list.add("咖啡消肿是真的嘛");
        list.add("咖啡小孩子能喝吗");
        list.add("咖啡消肿吗");
        list.add("咖啡洗的掉不");
        list.add("咖啡续命");
        list.add("咖啡性质是凉是热");
        list.add("咖啡续命的朋友圈怎么发");
        list.add("咖啡效果持续多久");
        list.add("咖啡英文名怎么写");
        list.add("咖啡y英语怎么说");
        list.add("咖啡有什么好处和坏处");
        list.add("咖啡渍怎么洗");
        list.add("咖啡怎么洗掉的小窍门");
        list.add("咖啡种类");
        list.add("咖啡渣可以吸甲醛吗");
        return list;
    }

}
