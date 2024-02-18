package pro.jiaoyi.tradingview;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import pro.jiaoyi.common.util.EmojiUtil;

@SpringBootApplication
@ComponentScan(basePackages = {"pro.jiaoyi.*"})
@EnableAsync
public class TradingviewApplication implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        try {
            System.out.println("TradingviewApplication: run mysqlRL.sh...");

            String sh = "/Users/dwd/dev/GitHub/jiaoyi/eastm/mysqlRL.sh";
            Runtime.getRuntime().exec(sh);

            System.out.println("TradingviewApplication: run mysqlRL.sh over...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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


            System.out.println(EmojiUtil.WARN + "0,看图形, 打分 1-6 " +
                    "[1-毛刺少 2-庄股走势 3-突破持续性(次日表现) 4-相对位置不过高(防放量滞涨 宽幅震荡) 5-收敛度高 6-分时量持续] ");
            System.out.println(EmojiUtil.WARN + "1,看大盘, 打分 -1(下跌趋势) 0(整理) +1(上升 或 即将突破)");
            System.out.println(EmojiUtil.WARN + "2,看板块, 打分 -1(下降趋势) 0(整理) +1(上升 或 即将突破)");
            System.out.println(EmojiUtil.WARN + "3,看公告新闻, 打分 -1(利空公告) 0(利好,防高开过度) +1(无公告)");
            System.out.println(EmojiUtil.WARN + "4,看收益, 打分 -1(最近大涨) 0(近期大亏) +1(平稳)");
            System.out.println(EmojiUtil.WARN + "选出之后: 总分要求>=8分");
        }
    }

}
