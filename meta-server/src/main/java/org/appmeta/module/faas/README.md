# 脚本引擎
> 基于 graaljs 的 JavaScript 代码执行工具


## 附录

### 问题汇总

1、 集成 SpringBoot 打包后报错

```shell
# 在类包下找不到指定资源
Caused by: java.lang.NullPointerException: null
        at java.base/java.util.Objects.requireNonNull(Objects.java:233)
        at java.base/sun.nio.fs.WindowsFileSystem.getPath(WindowsFileSystem.java:215)
        at java.base/java.nio.file.Path.of(Path.java:148)
        at org.graalvm.polyglot.Engine$ClassPathIsolation.collectClassPathJars(Engine.java:1988)
        at org.graalvm.polyglot.Engine$ClassPathIsolation.createIsolatedTruffleModule(Engine.java:1783)
        at org.graalvm.polyglot.Engine$ClassPathIsolation.createIsolatedTruffle(Engine.java:1723)
        at org.graalvm.polyglot.Engine$1.searchServiceLoader(Engine.java:1682)
        at org.graalvm.polyglot.Engine$1.run(Engine.java:1668)
        at org.graalvm.polyglot.Engine$1.run(Engine.java:1663)
        at java.base/java.security.AccessController.doPrivileged(AccessController.java:319)
        at org.graalvm.polyglot.Engine.initEngineImpl(Engine.java:1663)
        at org.graalvm.polyglot.Engine$ImplHolder.<clinit>(Engine.java:186)
        ... 47 common frames omitted
```

**修复方式**：在启动脚本增加特定参数项目

```shell
java -D"polyglotimpl.DisableClassPathIsolation"=true -jar .\meta-server-1.0.jar
```

参考资料：

- [[23.1.0] Error creating a GraalJS Engine in a Spring Boot app](https://github.com/oracle/graaljs/issues/767)
- [Cannot run polyglot code with Spring Boot executable JAR and Java 21](https://github.com/oracle/graal/issues/7625)

2、打包后体积约增加**90MB**

主要来自：icu4j-23.1.1.jar(39MB)、js-language-23.1.1.jar(27MB)、truffle-api-23.1.1.jar(16MB)、regex-23.1.1.jar(3MB)、polyglot-23.1.1.jar(1MB)

### 参考

- [clever-graaljs](https://gitee.com/LiZhiW/clever-graaljs)：基于 graaljs 的高性能js脚本引擎，适合各种需要及时修改代码且立即生效的场景，如：ETL工具、动态定时任务、接口平台、工作流执行逻辑。 fast-api 就是基于clever-graaljs开发的接口平台，可以直接写js脚本开发Http接口，简单快速！
- [delight-graaljs-sandbox](https://github.com/javadelight/delight-graaljs-sandbox)：A sandbox for executing JavaScript with Graal in Java
- [Javet](https://github.com/caoccao/Javet)：It is an awesome way of embedding Node.js and V8 in Java.
- [Java表达式引擎选型调研分析]()(https://juejin.cn/post/7300562752422756361)