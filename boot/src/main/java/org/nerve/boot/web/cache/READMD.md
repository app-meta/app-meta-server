# 集成 Caffeine 缓存

* `refreshAfterWrite`   缓存写入一定时间后刷新（指定时间后不删除 key，再次访问时异步更新，此时可能返回空值）
* `expireAfterWrite`    缓存写入一定时间后过期（指定时间后删除 key，再次访问时同步更新，返回最新的缓存值）
* `expireAfterAccess`   缓存最后一次访问一定时间后过期（期间如果一直有访问则不会触发刷新）

这里为了统一/简单，自定义缓存策略时不支持 refreshAfterWrite

```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

## 缓存定义

```yaml
# 统一配置 
spring:
  cache:
    caffeine:
      spec: maximumSize=100,expireAfterWrite=2h

# 配置特定缓存的策略
nerve:
  cache:
    enable: true
    # caches 为 Map<String, String> 对象
    caches:
      # 建议不使用 - 作为 key
      cacheName: maximumSize=100,expireAfterWrite=1m
      # 实在需要 key 包含 - 或者中文，则参考如下
      "[中文-又带有横杠]": maximumSize=100,expireAfterWrite=1h
```