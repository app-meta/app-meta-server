package org.appmeta.launch;
/*
 * @project app-meta-server
 * @file    org.appmeta.launch.App
 * CREATE   2023年11月29日 11:44 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class LaunchApp {

    public static void main(String[] args) throws IOException {
        // 来源 http://patorjk.com/software/taag 字体 Small
        System.out.println(
                "  __  __ ___ _____ _     _      _  _   _ _  _  ___ _  _ ___ ___ \n" +
                " |  \\/  | __|_   _/_\\   | |    /_\\| | | | \\| |/ __| || | __| _ \\\n" +
                " | |\\/| | _|  | |/ _ \\  | |__ / _ \\ |_| | .` | (__| __ | _||   /\n" +
                " |_|  |_|___| |_/_/ \\_\\ |____/_/ \\_\\___/|_|\\_|\\___|_||_|___|_|_\\\n"
        );
        C.loadPropertyFromFile();

        ProcessWorker processWorker = new ProcessWorker(
                Arrays.stream(C.cmd.split(" ")).map(String::trim).toList(),
                Paths.get(C.dir)
        );

        //启动监听线程
        new Thread(new WatchWorker(Paths.get(C.dir), List.of(C.watch), processWorker), "watch").start();
        //启动应用线程
        new Thread(processWorker, "process").start();
    }
}
