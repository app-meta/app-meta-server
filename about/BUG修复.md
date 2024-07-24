# BUG/问题修复记录

## 应用

1. [2024-01-22] 部署后端服务文件名不支持中文
> 报错行 var entry: ZipEntry? = zipIs.nextEntry

```log
java.lang.IllegalArgumentException: malformed input off : 0, length : 1
	at java.base/java.lang.String.throwMalformed(String.java:1279)
	at java.base/java.lang.String.decodeUTF8_UTF16(String.java:1235)
	at java.base/java.lang.String.newStringUTF8NoRepl(String.java:759)
	at java.base/java.lang.System$2.newStringUTF8NoRepl(System.java:2486)
	at java.base/java.util.zip.ZipCoder$UTF8ZipCoder.toString(ZipCoder.java:270)
	at java.base/java.util.zip.ZipCoder.toString(ZipCoder.java:91)
	at java.base/java.util.zip.ZipInputStream.readLOC(ZipInputStream.java:519)
	at java.base/java.util.zip.ZipInputStream.getNextEntry(ZipInputStream.java:153)
	at org.appmeta.tool.FileTool.unzip(FileTool.kt:68)
```

**原因分析**：由于 JDK 自带的 ZipEntry 默认按照 utf-8 编码识别文件名，碰到 GBK 编码就报错

**处理方案**：改用 Apache 的 `ArchiveStreamFactory().createArchiveInputStream(BufferedInputStream(FileInputStream(zipFile)))`

## 平台