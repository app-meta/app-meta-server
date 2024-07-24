package org.nerve.boot.cache;

public interface CacheCreator<T> {

    T create() throws Exception;
}
