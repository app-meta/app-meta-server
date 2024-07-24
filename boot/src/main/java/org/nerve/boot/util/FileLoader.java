package org.nerve.boot.util;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.nerve.boot.Const.NEW_LINE;


/**
 * Created by zengxm on 2017/9/1.
 */
public class FileLoader {

    /**
     *
     * @param name
     * @return
     * @throws IOException
     */
    public static InputStream load(String name) throws IOException {
        // 优先从从jar同级目录加载
        File file = new File(name);
        Resource resource = new FileSystemResource(file);
        //config目录下还是找不到，那就直接用classpath下的
        if (!resource.exists()) {
            resource = new ClassPathResource(name);
        }
        return resource.getInputStream();
    }

    public static String loadContent(String name) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(FileLoader.load(name), UTF_8))) {
            String str = null;
            while ((str = reader.readLine()) != null) {
                sb.append(str);
                sb.append(NEW_LINE);
            }
        }
        return sb.toString();
    }

    public static List<String> loadLines(String name) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(FileLoader.load(name), UTF_8))) {
            String str = null;
            while ((str = reader.readLine()) != null) {
                lines.add(str);
            }
        }
        return lines;
    }
}

