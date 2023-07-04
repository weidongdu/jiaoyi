package pro.jiaoyi.common.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class FileUtil {
    public static boolean fileCheck(String filePath) {
        Path path = Paths.get(filePath);
        return Files.exists(path);
    }
//
//    public static void writeToFile(String s, String string) {
//
//    }


    public static void writeToFile(String fileName, String content) {
//        String fileName = "example.txt";
//        String content = "Hello, world!";

        try {



            // 创建文件对象
            File file = new File(fileName);
            // 如果文件名包含路径，则创建对应的目录
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            // 如果文件不存在，则创建文件
            Path path = Paths.get(fileName);
            if (!Files.exists(path)) {
                Files.createFile(path);
            }

            // 将字符串追加到文件末尾
            Files.write(path, content.getBytes(), StandardOpenOption.APPEND);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static String readFromFile(String fileName) {
//        Path path = Paths.get("path/to/file.txt");
        Path path = Paths.get(fileName);
        try (Stream<String> lines = Files.lines(path)) {
            StringBuilder stringBuilder = new StringBuilder();
            lines.forEach(stringBuilder::append);
            return stringBuilder.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    /*
     * 读取csv文件
     * String[] head = {"code", "name", "date", "high", "max", "min"};
     * String file = "/Users/dwd/dev/GitHub/jiaoyi/eastm/上影线20230627.csv";
     */
    public static List<String[]> getCsvFromFile(String[] head, String file) {
        Path path = Paths.get(file);
        try (Stream<String> lines = Files.lines(path)) {
            ArrayList<String[]> csv = new ArrayList<>();
            csv.add(head);

            lines.forEach(line -> {
                if (line.contains(head[0])) {
                    return;
                }
                String[] split = line.split(",");
                csv.add(split);
            });

            return csv;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }
}
