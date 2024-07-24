package org.appmeta.launch;
/*
 * @project app-meta-server
 * @file    org.appmeta.launch.WatchHandler
 * CREATE   2023年11月30日 11:00 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 *
 * 文件变化处理器
 */

import java.nio.file.Path;

public interface WatchHandler {

    void onChange(Path path);
}
