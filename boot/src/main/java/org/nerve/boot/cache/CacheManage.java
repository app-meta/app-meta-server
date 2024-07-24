package org.nerve.boot.cache;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public final class CacheManage {

    private final static ConcurrentHashMap<Object, Cache> caches = new ConcurrentHashMap<>();
    public final static int EXPIRE = 3600;     //缓存时效时间，单位秒，默认为 1 小时

    /**
     *
     * @param key
     * @param creator
     * @param <T>
     * @return
     * @throws Exception
     */
    public static <T> T get(Object key, CacheCreator<T> creator) throws Exception{
        return get(key, creator, EXPIRE);
    }

    public static <T> T get(Object key, CacheCreator<T> creator, int expire) throws Exception{
        if(caches.containsKey(key)){
            Cache cache = caches.get(key);
            if(!cache.isExpire()){
                cache.onHit();
                return (T) cache.getData();
            }
        }

        T data = creator.create();
        caches.put(key, new Cache(data, System.currentTimeMillis() + expire*1000));
        return data;
    }

    public static List<Cache> status(){
        List<Cache> cacheList = new ArrayList<>();
        caches.forEach((name, cache)-> cacheList.add(cache.snapshot(name.toString())));
        return cacheList;
    }

    public static boolean contains(Object key){
        return caches.containsKey(key);
    }

    public static void clear(Object key){
        caches.remove(key);
    }

    public static void clearWithPrefix(String prefix){
        Enumeration<Object> keys = caches.keys();
        while (keys.hasMoreElements()){
            Object keyObj = keys.nextElement();
            if(keyObj instanceof String && ((String) keyObj).startsWith(prefix))
                caches.remove(keyObj);
        }
    }

    public static Enumeration<Object> getKeys(){
        return caches.keys();
    }

    public static void main(String[] args) throws Exception {
        String key = "abc";

        for(int i=0;i<4; i++){
            System.out.println(CacheManage.get(key+i%2, ()->new Date().toString()));
            Thread.sleep(3000);

            status().forEach(cache -> System.out.println(cache.toString()));
        }

        clearWithPrefix(key);
        System.out.println(status().size());
    }
}
