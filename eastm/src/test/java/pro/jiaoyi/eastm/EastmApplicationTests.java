package pro.jiaoyi.eastm;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pro.jiaoyi.eastm.api.EmClient;

import java.util.Map;

@SpringBootTest
class EastmApplicationTests {

    @Test
    void contextLoads() {
    }

    @Autowired
    private EmClient emClient;

    @Test
    public void test() {
//        System.out.println("hello world");

//        List<DailyK> dailyKs = emClient.getDailyKs("002422", LocalDate.now(), 500);
//
//        List<CList> lists = emClient.getClistDefault(false);
//
//        for (CList list : lists) {
//            System.out.println(list);
//        }

        Map<String, String> nameCodeMap = emClient.getNameCodeMap(false);
        System.out.println(nameCodeMap);

        nameCodeMap = emClient.getNameCodeMap(true);
        System.out.println(nameCodeMap);

        for (int i = 0; i < 10; i++) {
            long l = System.currentTimeMillis();
            nameCodeMap = emClient.getNameCodeMap(false);
            long l2 = System.currentTimeMillis();
            System.out.println("use ms = " + (l2 - l));

        }


    }

}
