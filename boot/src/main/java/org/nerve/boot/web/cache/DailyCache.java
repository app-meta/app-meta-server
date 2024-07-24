package org.nerve.boot.web.cache;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name ="nerve.cache.daily", havingValue = "true")
public class DailyCache {
    private Logger logger = LoggerFactory.getLogger(getClass());

    private Map<String, Set<Object>> data = new ConcurrentHashMap<>();


    @PostConstruct
    protected void init(){
        logger.info("[Cache] 初始化 DailyCache ( set nerve.cache.daily=false if you want to disable daily cache!)");
    }

    /**
     * 如果存在，则返回 false
     * 否则添加到记录（默认写入 0），返回 true
     *
     * @param category
     * @param key
     * @return
     */
    public boolean exist(String category, Object key){
        if(!data.containsKey(category))
            data.put(category, new HashSet<>());

        if(data.get(category).contains(key))
            return false;

        data.get(category).add(key);
        return true;
    }

    @Scheduled(cron = "0 0 0 * * *")
    private void clean(){
        synchronized (data){
            data.clear();
        }
        if(logger.isDebugEnabled()) logger.debug("刷新 Daily 内存键值...");
    }
}
