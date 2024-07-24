# 开发接口/OenAPI
> 管理员维护接口（主要针对数据库操作，以 SQL 执行方式），方便页面/第三方调用

## 简单例子
> 希望能够按照指定字段，统计页面的数量

```sql
# 按照类型
SELECT template, count(*) as count FROM page GROUP BY template;
# 按照用户ID
SELECT uid, count(*) as count FROM page GROUP BY uid;
```

此时可以新建一个名字`页面分组统计`的接口，接收参数`field`，执行语句：

```sql
SELECT {{ field }} as key, count(*) as count FROM page GROUP BY {{ field }};
```

得到的是二维数组，可按照需要转换为 `List<Map>`。

## 数据结构
> 详见 [数据字典.md](app-meta/docs/数据字典.md)


## 执行流程
> `/api/:id` 为接口的对外入口

1. 调用方发起请求，传递参数 `ps`
2. 后端识别对应的接口，并判断接口是否生效（false 则中断）
3. 判断当前用户是否授权使用（false 则中断）
4. 解析参数，判断参数是否合规（false 则中断）
5. 若设置了`sourceId`，需要进一步判断数据源的可用性（false 则中断）
6. 拼凑 SQL 并执行
7. 按照 `resultType` 封装结果并返回
8. `launch` +1 并记录日志

**参数**
> 参数通常包含以下属性

* `id` 参数键值
* `name` 中文名称
* `required` 是否必填
* `regex` 检验正则表达式
* `type` [可选]类型，String，Number

## GUI 流程

1. 管理员新建接口，然后再进行接口详情的编辑
2. 接口以`树形结构`展示