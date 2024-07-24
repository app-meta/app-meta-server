package org.appmeta.tool


/*
 * @project app-meta-server
 * @file    org.appmeta.tool.CollectionTool
 * CREATE   2023年02月15日 10:57 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

class LimitMap<V>(val maxSize: Int = 100) : LinkedHashMap<String, V>() {

    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, V>) = size > maxSize
}