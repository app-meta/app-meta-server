package org.appmeta.launch;
/*
 * @project app-meta-server
 * @file    org.appmeta.launch.WatchThread
 * CREATE   2023年11月29日 14:06 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

import com.sun.nio.file.SensitivityWatchEventModifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class WatchWorker implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(WatchWorker.class);

    private Path dir;
    private List<String> filenames;
    private WatchService watcher;
    private WatchHandler handler;

    private int duration    = 5;    //指定秒内仅触发一次
    private int delay       = 8;    //事件延迟发布

    private Map<String, Long> timeMap = new HashMap<>();

    public WatchWorker(Path dir, List<String> names, WatchHandler handler) throws IOException {
        this.dir = dir;
        this.filenames = names;
        this.watcher = FileSystems.getDefault().newWatchService();
        this.handler = handler;

        this.dir.register(
                watcher,
                new WatchEvent.Kind[]{ENTRY_CREATE, ENTRY_MODIFY},
                SensitivityWatchEventModifier.LOW
        );
        logger.info("开始监听目录 {}，目标{}", dir, filenames);
    }

    @Override
    public void run() {
        while (true) try {
            // 等待文件的变化（阻塞）
            WatchKey key = watcher.take();
            for (WatchEvent<?> event : key.pollEvents()) {
                if (event.kind() == StandardWatchEventKinds.OVERFLOW)   continue;

                Path target = (Path) event.context();
                if(filenames.contains(target.toString())){
                    //判断是否重复
                    long last = timeMap.getOrDefault(target.toString(), 0L);
                    if(System.currentTimeMillis() - last > duration * 1000L){
                        if(logger.isDebugEnabled()) logger.debug("监听到文件 {} 变动", target);

                        //等待指定时间后发布事件
                        if(delay > 0)   Thread.sleep(delay * 1000L);

                        if(handler != null) handler.onChange(target);
                    }
                    timeMap.put(target.toString(), System.currentTimeMillis());
                }
            }

            key.reset();
        } catch (Exception e) {
            logger.error("监听作业出错", e);
        }
    }

    public int getDuration() {
        return duration;
    }

    public WatchWorker setDuration(int duration) {
        this.duration = duration;
        return this;
    }

    public int getDelay() {
        return delay;
    }

    public WatchWorker setDelay(int delay) {
        this.delay = delay;
        return this;
    }
}
