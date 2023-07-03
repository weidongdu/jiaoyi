package pro.jiaoyi.tradingview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import pro.jiaoyi.common.util.EmojiUtil;

@SpringBootApplication
@ComponentScan(basePackages = {"pro.jiaoyi.*"})
@EnableAsync
public class TradingviewApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradingviewApplication.class, args);

        {
            System.out.println(EmojiUtil.DOWN + "卖出: 成本价附近,昨日低点");
            System.out.println(EmojiUtil.DOWN + "卖出: 小幅整理期,震荡底部");
            System.out.println(EmojiUtil.DOWN + "卖出: 趋势未结束,底仓持有");
            System.out.println(EmojiUtil.DOWN + "卖出: 交易有盈利,浮动止盈");

            System.out.println(EmojiUtil.UP + "买入: 计划外的票,谨慎开仓");
            System.out.println(EmojiUtil.UP + "买入: 高开再新高,量要同步新高");
            System.out.println(EmojiUtil.UP + "买入: 仓位要控制,不要贪多");

            System.out.println(EmojiUtil.WARN + "选出之后: 1,看大盘, 打分 1-10");
            System.out.println(EmojiUtil.WARN + "选出之后: 2,看所属板块, 打分 1-10");
            System.out.println(EmojiUtil.WARN + "选出之后: 3,看关联个股, 打分 1-10");
            System.out.println(EmojiUtil.WARN + "选出之后: 4,看个股公告, 打分 1-10");
            System.out.println(EmojiUtil.WARN + "选出之后: 5,看个股位置, 打分 1-10");

        }
    }

}
