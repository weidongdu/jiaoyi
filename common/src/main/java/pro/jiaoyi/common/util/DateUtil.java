package pro.jiaoyi.common.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateUtil {
    public static final String PATTERN_yyyyMMdd = "yyyyMMdd";
    public static final String PATTERN_yyyy_MM_dd = "yyyy-MM-dd";

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


}
