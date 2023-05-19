package pro.jiaoyi.common.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtil {
    public static LocalDateTime strTo(String str, String pattern) {

        LocalDateTime localDateTime = LocalDateTime.parse(str, DateTimeFormatter.ofPattern(pattern));
        return localDateTime;
    }
}
