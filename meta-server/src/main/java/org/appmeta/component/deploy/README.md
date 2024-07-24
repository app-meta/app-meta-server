# 应用部署工具

目前支持的是本地 [pm2](https://pm2.keymetrics.io/docs/usage/quick-start) 部署 node.js 应用

## node 应用
> 假设要启动 node.js
> 
### 方式一：命令行

```shell
pm2 start node.js --name=name
```

### 方式二：配置文件

```js
//配置 xx.config.js
module.exports = {
	apps:[{
		name: "node",
		script:"node.js",
		exec_interpreter:"node",
		args:"--param01=A --param02=B"
	}]
}
```

`pm2 start xx.config.js` 即可

## java 应用
> 此处以允许 jar 为例

```js
//配置 xx.config.js
module.exports = {
	apps:[{
		name: "java",
		// 执行的命令
		script:"java",
		// 此处不填写，届时将显示 `none`
		exec_interpreter:"",
		// 启动命令（除去 java 命令后）
		args:"-server -jar spring-boot-demo-1.0.jar --server.port=9000"
	}]
}
```

## 其他应用（exe 或 二进制可执行文件）
> TODO
