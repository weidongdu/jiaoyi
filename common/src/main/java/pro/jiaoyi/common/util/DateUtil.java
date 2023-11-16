package pro.jiaoyi.common.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class DateUtil {
    public static final String PATTERN_yyyyMMdd_HH_mm_ss = "yyyyMMdd_HH:mm:ss";
    public static final String PATTERN_yyyyMMdd = "yyyyMMdd";
    public static final String PATTERN_yyyyMMdd_HHmm = "yyyyMMdd_HHmm";
    public static final String PATTERN_yyyyMMdd_HHmmss = "yyyyMMdd_HHmmss";
    public static final String PATTERN_yyyy_MM_dd = "yyyy-MM-dd";
    public static final String PATTERN_HH_mm_ss = "HH:mm:ss";

    public static String td() {
        return LocalDate.now().toString().replace("-", "");
    }

    public static String tdPre(int i) {
        return LocalDate.now().minusDays(i).toString().replace("-", "");
    }

    public static LocalDateTime strToLocalDateTime(String str, String pattern) {
        return LocalDateTime.parse(str, DateTimeFormatter.ofPattern(pattern));
    }

    public static LocalDateTime toLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public static LocalDate strToLocalDate(String str, String pattern) {
        return LocalDate.parse(str, DateTimeFormatter.ofPattern(pattern));
    }

    public static Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static long toTimestamp(LocalDateTime localDateTime) {
        Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant(); // 转换为 Instant 类型
        return instant.toEpochMilli();
    }

    public static long toTimestamp(LocalDate localDate) {
        LocalTime localTime = LocalTime.of(0, 0, 0); // 定义当天的零点
        LocalDateTime localDateTime = LocalDateTime.of(localDate, localTime); // 获取当天的零点时间
        return toTimestamp(localDateTime);
    }

    public static String today() {
        return LocalDate.now().toString().replace("-", "");
    }

    public static String dateStr(LocalDate l) {
        return l.toString().replace("-", "");
    }

    public static String tradeDate() {
//        return "20230814";

        LocalTime localTimePre = LocalTime.of(9, 0, 0); // 定义当天的零点
//        LocalTime localTimeEnd = LocalTime.of(15, 0, 0); // 定义当天的零点

        LocalDateTime pre = LocalDateTime.of(LocalDate.now(), localTimePre);
//        LocalDateTime end = LocalDateTime.of(LocalDate.now(), localTimeEnd);
        String dateStr = DateUtil.today();
        if (LocalDateTime.now().isBefore(pre)) {
            dateStr = dateStr(LocalDate.now().minusDays(1));
        }


        if (LocalDate.now().getDayOfWeek().getValue() == 6) {
            dateStr = DateUtil.dateStr(LocalDate.now().minusDays(1));
        }
        if (LocalDate.now().getDayOfWeek().getValue() == 7) {
            dateStr = DateUtil.dateStr(LocalDate.now().minusDays(2));
        }
        return dateStr;
    }


    public static String tsToStr(long ts, String pattern) {
        /*
         * timestamp -> string pattern=pattern
         */
        Instant instant = Instant.ofEpochMilli(ts); // 将时间戳转为 Instant 对象
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern); // 定义格式化器
        return formatter.format(instant.atZone(ZoneId.systemDefault()));
    }

    public static LocalDate tsToLocalDate(long ts) {
        /*
         * timestamp -> LocalDate
         */
        String pattern = PATTERN_yyyy_MM_dd;
        return strToLocalDate(tsToStr(ts, pattern), pattern);
    }

    public static Integer daysDiff(LocalDateTime localDateTime) {
        return (int) ChronoUnit.DAYS.between(localDateTime, LocalDateTime.now());
    }

    public static void main(String[] args) {
        LocalDateTime now = LocalDateTime.now();
        System.out.println(now.toString());
        System.out.println(LocalDateTime.now().toString().substring(0, 16));

        System.out.println(tradeDate());
    }

}
