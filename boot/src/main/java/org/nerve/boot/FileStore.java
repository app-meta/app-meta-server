package org.nerve.boot;

import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class FileStore {

    //默认文件分组
    public static final String DEFAULT = "";
    public static final String TEMP = "TEMP";

    /*
    附件、文档存储路径
    默认为程序目录下 attach/
    格式为： attach/{group}/YYYY/MMdd/xxxx.txt
     */
    private String directory;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY/MMdd");

    public FileStore(){}
    public FileStore(String dir){
        directory = dir;
    }

    /**
     * 持久化 InputStream 到特定的文件，此方法不会关闭输入流，需要人工关闭
     * @param is
     * @param filename
     * @return
     * @throws IOException
     */
    public String store(InputStream is, String filename) throws IOException {
        return store(is, filename, DEFAULT);
    }

    /**
     * 持久化 InputStream 到特定的文件，此方法不会关闭输入流，需要人工关闭
     * @param is
     * @param filename
     * @param group
     * @return
     * @throws IOException
     */
    public String store(InputStream is, String filename, String group) throws IOException {
        Path fullFilePath = buildPath(filename, group);
        Files.createDirectories(fullFilePath.getParent());

        FileUtils.copyToFile(is, fullFilePath.toFile());
        return fullFilePath.toString();
    }

    /**
     * 持久化 InputStream 到特定的文件，此方法不会关闭输入流，需要人工关闭
     * @param is
     * @param filename
     * @param group
     * @return
     * @throws IOException
     */
    public String storeUnique(InputStream is, String filename, String group) throws IOException {
        Path fullFilePath = Paths.get(directory, group, dateFormat.format(new Date())).resolve(UUID.randomUUID() + "-" +  filename);
        Files.createDirectories(fullFilePath.getParent());
        FileUtils.copyToFile(is, fullFilePath.toFile());
        return fullFilePath.toString();
    }

    public Path buildPath(String name, String group){
        return Paths.get(directory, group, dateFormat.format(new Date())).resolve(name);
    }

    public Path buildPathWithoutDate(String name, String group){
        return Paths.get(directory, group).resolve(name);
    }

    public Path tempFile(String name) throws IOException {
        Path path = Paths.get(directory, TEMP);
        if(Files.notExists(path))
            Files.createDirectories(path);
        return path.resolve(name);
    }
}
