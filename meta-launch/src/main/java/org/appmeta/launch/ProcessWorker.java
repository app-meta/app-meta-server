package org.appmeta.launch;
/*
 * @project app-meta-server
 * @file    org.appmeta.launch.ProcessWorker
 * CREATE   2023年11月30日 09:50 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;

public class ProcessWorker implements Runnable, WatchHandler {
    private static final Logger logger = LoggerFactory.getLogger(ProcessWorker.class);

    private List<String> cmds;
    private File root;
    private Process process;
    private long pid;

    public ProcessWorker(List<String> cmds, Path root){
        this.cmds = cmds;
        this.root = root.toFile();

        //退出主进程时，关闭子进程
        Runtime.getRuntime().addShutdownHook(new Thread(()-> stop()));
    }

    @Override
    public void run() {
        while (process == null){
            try {
                process = new ProcessBuilder(cmds)
                        .directory(root)
                        .redirectErrorStream(true)
                        .start();
                pid = process.pid();
                logger.info("启动服务 PID={}", pid);

                new Thread(()->{
                    try(BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))){
                        while (reader.readLine() != null) {}
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).start();

                process.waitFor();
                logger.info("进程结束（CODE={} 被终止或报错）", process.exitValue());

                Thread.sleep(C.delay * 1000L);
                process = null;
            } catch (IOException e) {
                logger.error("启动服务失败", e);
            } catch (InterruptedException e) {
                logger.error("执行服务出错", e);
            }
        }
    }

    public void stop(){
        if(process!=null){
            process.destroy();
            logger.info("停止进程，返回码={}", process.exitValue());
        }
    }

    @Override
    public void onChange(Path path) {
        logger.info("应用文件发生变化，尝试重启服务...");
        stop();
    }
}
