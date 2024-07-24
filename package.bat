:: 打包 meta-server

@echo off
chcp 65001

call mvn package -pl meta-server -am -amd

echo 打包完成 ^.^