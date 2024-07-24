package org.appmeta.launch;
/*
 * @project app-meta-server
 * @file    org.appmeta.launch.C
 * CREATE   2023年11月29日 15:02 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public final class C {
    private static final Logger logger = LoggerFactory.getLogger(C.class);

    public static int delay         = 8;
    public static String dir        = "";
    public static String watch      = "meta-server-1.0.jar";
    public static String cmd        = "java -jar meta-server-1.0.jar --spring.profiles.active=prod";

    /**
     * 读取同目录下的 config.properties 文件
     */
    public static void loadPropertyFromFile(){
        try{
            String CONFIG = "launch.properties";
            Path configPath = Paths.get(CONFIG);
            if(Files.exists(configPath)){
                Properties p = new Properties();
                p.load((Files.newBufferedReader(configPath)));

                logger.info("读取 {} 配置信息...", CONFIG);

                C.dir   = p.getProperty("dir", C.dir);
                C.watch = p.getProperty("watch", C.watch);
                C.cmd   = p.getProperty("cmd", C.cmd);
                logger.info("[配置] DIR={}", C.dir);
                logger.info("[配置] CMD={}", C.cmd);
            }

        } catch (IOException e) {
            logger.error("读取配置文件出错", e);
        }
    }
}
