<div align=center>
<h1>🎉 应用元宇宙 / APP META 🎉</h1>

![Language](https://img.shields.io/github/languages/top/0604hx/app-meta-server?logo=java&color=purple)
![License](https://img.shields.io/badge/License-MIT-green)
![LastCommit](https://img.shields.io/github/last-commit/0604hx/app-meta-server?color=blue&logo=github)

</div>

```text
 _______  _______  _______    __   __  _______  _______  _______ 
|   _   ||       ||       |  |  |_|  ||       ||       ||   _   |
|  |_|  ||    _  ||    _  |  |       ||    ___||_     _||  |_|  |
|       ||   |_| ||   |_| |  |       ||   |___   |   |  |       |
|       ||    ___||    ___|  |       ||    ___|  |   |  |       |
|   _   ||   |    |   |      | ||_|| ||   |___   |   |  |   _   |
|__| |__||___|    |___|      |_|   |_||_______|  |___|  |__| |__|
```

> 基于 [SpringBoot3](https://spring.io/projects/spring-boot) + [VUE3](https://cn.vuejs.org/) + [Naive UI](https://www.naiveui.com) + [Electron](https://www.electronjs.org) 应用快速开发、发布平台，旨在帮助使用者（包含但不限于开发人员、业务人员）快速响应业务需求，此仓库为后端，前端仓库详见[app-meta](https://github.com/0604hx/app-meta)。

[如何使用/二次开发](about/本地运行及二次开发.md)

## 附录

### 工具库

名称| 简介 |备注
---|---|---
[handlebars.java](https://github.com/jknack/handlebars.java)|Logic-less and semantic Mustache templates with Java（模版引擎）

### 备用库

名称| 简介 |备注
---|---|---
[sofa-ark](https://github.com/sofastack/sofa-ark)| SOFAArk 是一款基于 Java 实现的动态热部署和轻量级类隔离框架，由蚂蚁集团开源贡献，主要提供应用模块的动态热部署和类隔离能力。基于 Fat Jar 技术，可以将多个应用模块打包成一个自包含可运行的 Fat Jar，应用既可以是简单的单模块 Java 应用也可以是 SpringBoot/SOFABoot 应用 |用于动态部署 Java 类后端服务？
[NuProcess](https://github.com/brettwooldridge/NuProcess)| Low-overhead, non-blocking I/O, external Process implementation for Java |执行系统命令
[zt-exec](https://github.com/zeroturnaround/zt-exec)|ZeroTurnaround Process Executor|执行系统命令
[LiteFlow](https://github.com/dromara/liteflow)|一个轻量且强大的国产规则引擎框架，可用于复杂的组件化业务的编排领域，独有的DSL规则驱动整个复杂业务，并可实现平滑刷新热部署，支持多种脚本语言规则的嵌入