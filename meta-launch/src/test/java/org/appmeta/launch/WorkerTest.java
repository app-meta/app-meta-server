package org.appmeta.launch;
/*
 * @project app-meta-server
 * @file    org.appmeta.launch.WatchTest
 * CREATE   2023年11月30日 11:41 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class WorkerTest {

    @Test
    public void watch() throws IOException, InterruptedException {
        WatchWorker worker = new WatchWorker(
                Paths.get("E:\\workspace\\nerve\\app-meta-server\\meta-server\\target"),
                List.of("test.txt"),
                path -> System.out.println("变动:"+path)
        );

        new Thread(worker).start();
        Thread.sleep(100*1000);
    }

    @Test
    public void process() throws InterruptedException {
        ProcessWorker worker = new ProcessWorker(Arrays.asList("python", "-m", "http.server"), Paths.get("E:/"));
        new Thread(worker, "process").start();
        Thread.sleep(5*1000);
        worker.stop();
    }
}
