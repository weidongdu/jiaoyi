package pro.jiaoyi.common.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtil {
    public static boolean fileCheck(String filePath) {
        Path path = Paths.get(filePath);
        return Files.exists(path);
    }

}
