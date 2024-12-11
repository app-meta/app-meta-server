# 数据修复 SQL 汇总
> 每次数据表结构变更，可能会导致数据缺失，此时需要用程序或者 SQL 进行调整

**变更应用数据块长度**
> on 2024-12-11

```sql
ALTER TABLE `app-meta`.data_block MODIFY COLUMN text MEDIUMTEXT;
```

**应用增加下架字段**
> on 2024-08-15

```sql
ALTER TABLE `app-meta`.app ADD offline tinyint DEFAULT 0 NULL COMMENT '是否下架' AFTER active;
CREATE INDEX app_offline_IDX USING BTREE ON `app-meta`.app (offline);
```

**页面增加更新日期字段**
> on 2024-04-19

```sql
ALTER TABLE page ADD updateOn BIGINT NULL AFTER content;
```

**Member表增加密钥对**
> on 2023-10-19

```sql
ALTER TABLE `app-meta`.`member` ADD uuid varchar(100) NULL AFTER id;
ALTER TABLE `app-meta`.`member` ADD category varchar(10) DEFAULT '' NOT NULL COMMENT 'cli=命令行终端；worker=工作者；other=其他' AFTER ids;
ALTER TABLE `app-meta`.`member` ADD pubKey text NULL AFTER secret;
ALTER TABLE `app-meta`.`member` ADD priKey text NULL AFTER secret;
ALTER TABLE `app-meta`.`member` ADD mode tinyint(1) NOT NULL DEFAULT '0' COMMENT '交互模式（0=默认，1=轮询）' AFTER category;
ALTER TABLE `app-meta`.data_robot ADD link varchar(100) NULL AFTER logs;
```

**Data表增加逻辑删除**
> on 2023-09-01

```sql
ALTER TABLE data ADD hide tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除，0=显示，1=删除' AFTER v;
ALTER TABLE data ADD `hideOn` bigint DEFAULT NULL COMMENT '删除日期，格式为 yyyMMdd 的数字' AFTER hide;
CREATE INDEX data_hide_IDX USING BTREE ON data (hide);
```

**增加请求转发详情持久化**
> add on 2023-08-23
 
```sql
ALTER TABLE terminal_log ADD host varchar(100) DEFAULT '' AFTER method;

CREATE TABLE `terminal_log_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `reqHeader` text COMMENT '请求头',
  `reqBody` text COMMENT '请求主体（字符串）',
  `resHeader` text COMMENT '响应头',
  `resBody` text COMMENT '响应主体（BASE64格式，需还原成字节，然后根据响应头转码）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```

**page_paunch 增加 depart 字段**
> add on 2023-08-16

```sql
update page_launch p set p.depart=(select concat(d.id,"-", d.name) from account a left join department d on a.did = d.id where a.id = p.uid) where p.depart=''
```