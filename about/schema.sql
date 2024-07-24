-- `app-meta`.account definition

CREATE TABLE `account` (
  `id` varchar(100) NOT NULL,
  `name` varchar(20) NOT NULL,
  `did` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- `app-meta`.account_pwd definition

CREATE TABLE `account_pwd` (
  `id` varchar(100) NOT NULL,
  `value` varchar(250) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- `app-meta`.app definition

CREATE TABLE `app` (
  `id` varchar(30) NOT NULL COMMENT '应用唯一编号',
  `name` varchar(20) NOT NULL COMMENT '应用名称（2到15个字符间）',
  `abbr` varchar(3) DEFAULT NULL COMMENT '应用简称（1到3个字符）',
  `active` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否生效，0=不生效（前端不展示），1=生效',
  `category` tinyint(1) NOT NULL DEFAULT '0' COMMENT '应用类型，0=快应用，1=RPA，2=外置应用',
  `summary` text COMMENT '应用简介（支持 MD 语法）',
  `launch` int DEFAULT 0 COMMENT '应用执行次数',
  `mark` int DEFAULT 0 COMMENT '应用收藏次数';
  `thumb` int DEFAULT 0 COMMENT '应用点赞数';
  `author` varchar(15) DEFAULT NULL,
  `uid` varchar(12) DEFAULT NULL COMMENT '录入者ID',
  `uname` varchar(20) DEFAULT NULL COMMENT '录入者名称',
  `addOn` bigint DEFAULT NULL COMMENT '录入时间戳',
  `winFrame` tinyint NOT NULL DEFAULT '1' COMMENT '【窗口设置】是否显示边框',
  `winMax` tinyint NOT NULL DEFAULT '0' COMMENT '【窗口设置】自动最大化',
  `winWidth` int DEFAULT '920' COMMENT '【窗口设置】窗口宽度（px）',
  `winHeight` int DEFAULT '480' COMMENT '【窗口设置】窗口高度（px）',
  PRIMARY KEY (`id`),
  KEY `app_category_IDX` (`category`) USING BTREE,
  KEY `app_uid_IDX` (`uid`) USING BTREE,
  KEY `app_active_IDX` (`active`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE `app_property` (
  `id` varchar(30) NOT NULL COMMENT '应用唯一编号',
  `winFrame` tinyint NOT NULL DEFAULT '1' COMMENT '【窗口设置】是否显示边框',
  `winMax` tinyint NOT NULL DEFAULT '0' COMMENT '【窗口设置】自动最大化',
  `winWidth` int DEFAULT '920' COMMENT '【窗口设置】窗口宽度（px）',
  `winHeight` int DEFAULT '480' COMMENT '【窗口设置】窗口高度（px）',
  `native` int DEFAULT '0' COMMENT '是否需要在原生环境下运行，勾选后，如果在纯 WEB 环境下会报错',
  PRIMARY KEY (`id`),
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- `app-meta`.app_version definition

CREATE TABLE `app_version` (
  `id` int NOT NULL AUTO_INCREMENT,
  `aid` varchar(15) NOT NULL,
  `pid` varchar(100) DEFAULT NULL COMMENT '关联页面ID',
  `version` varchar(10) NOT NULL COMMENT 'Y.M.D 格式的版本号',
  `summary` text,
  `path` varchar(255) DEFAULT NULL COMMENT '文件保存地址（本地路径或者远程 url）',
  `size` bigint DEFAULT NULL COMMENT '文件大小',
  `addOn` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `app_version_aid_IDX` (`aid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- `app-meta`.app_role definition

CREATE TABLE `app_role` (
  `aid` varchar(30) NOT NULL,
  `uuid` varchar(100) NOT NULL,
  `name` varchar(100) DEFAULT NULL,
  `auth` text,
  `ip` varchar(255) DEFAULT NULL,
  `summary` varchar(255) DEFAULT NULL,
  `addOn` bigint DEFAULT NULL,
  PRIMARY KEY (`aid`,`uuid`),
  KEY `app_role_link_aid_IDX` (`aid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- `app-meta`.app_role_link definition

CREATE TABLE `app_role_link` (
  `aid` varchar(30) NOT NULL,
  `uid` varchar(30) NOT NULL,
  `role` varchar(255) DEFAULT NULL COMMENT '关联的角色ID，多个用英文逗号隔开',
  PRIMARY KEY (`aid`,`uid`),
  KEY `app_role_link_aid_IDX` (`aid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- `app-meta`.app_link definition

CREATE TABLE `app_link` (
  `id` int NOT NULL AUTO_INCREMENT,
  `aid` varchar(15) NOT NULL,
  `uid` varchar(15) NOT NULL,
  `type` tinyint DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `app_version_aid_IDX` (`aid`) USING BTREE,
  KEY `app_version_uid_IDX` (`uid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `page` (
  `id` int NOT NULL AUTO_INCREMENT,
  `aid` varchar(15) NOT NULL,
  `uid` varchar(15) NOT NULL,
  `name` varchar(200) DEFAULT '',
  `active` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否生效，0=不生效（前端不展示），1=生效',
  `main` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否标记为主页面（每个应用尽有一个主页面）',
  `search` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否可被检索',
  `serviceAuth` text COMMENT '访问授权，{标识，U为用户，D为部门}|{ID}',
  `editAuth` text COMMENT '编辑授权，{标识，U为用户，D为部门}|{ID}',
  `template` varchar(15) NOT NULL DEFAULT 'form',
  `launch` int DEFAULT 0 COMMENT '页面执行次数',
  `content` text,
  `updateOn` bigint NULL,
  `addOn` bigint NULL,
  PRIMARY KEY (`id`),
  KEY `aid_IDX` (`aid`) USING BTREE,
  KEY `uid_IDX` (`uid`) USING BTREE,
  KEY `search_IDX` (`search`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--ALTER TABLE `app-meta`.page ADD `search` tinyint(1) DEFAULT 1 NULL COMMENT '是否可被检索';
--ALTER TABLE `app-meta`.page CHANGE `search` `search` tinyint(1) DEFAULT 1 NULL COMMENT '是否可被检索' AFTER main;
--CREATE INDEX page_search_IDX USING BTREE ON `app-meta`.page (`search`);

CREATE TABLE `page_link` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `aid` varchar(100) NOT NULL COMMENT '关联应用ID',
  `pid` varchar(100) DEFAULT NULL COMMENT '关联表单ID',
  `uid` varchar(15) DEFAULT NULL,
  `name` varchar(200) DEFAULT '',
  `template` varchar(15) NOT NULL DEFAULT '',
  `active` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否生效，0=不生效（前端不展示），1=生效',
  `weight` int NOT NULL DEFAULT '0' COMMENT '排序值，越大越靠前',
  `addOn` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `data_active_IDX` (`active`) USING BTREE,
  KEY `data_uid_IDX` (`uid`) USING BTREE,
  KEY `data_pid_IDX` (`pid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `page_launch` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `aid` varchar(100) NOT NULL COMMENT '关联应用ID',
  `pid` varchar(100) DEFAULT NULL COMMENT '关联表单ID',
  `uid` varchar(15) DEFAULT NULL,
  `ip` varchar(100) DEFAULT '',
  `channel` varchar(20) DEFAULT NULL,
  `depart`  varchar(100) DEFAULT '',
  `addOn` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `uid_IDX` (`uid`) USING BTREE,
  KEY `channel_IDX` (`channel`) USING BTREE,
  KEY `pid_IDX` (`pid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE image (
	id int auto_increment NOT NULL,
	filename varchar(200) NOT NULL,
	`size` int NULL,
	ext varchar(10) NULL,
	`path` varchar(250) NOT NULL,
	uid varchar(10) NULL,
	addOn bigint NULL,
	PRIMARY KEY (id),
	KEY `form_uid_IDX` (`uid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `document` (
	`id` int auto_increment NOT NULL,
	`filename` varchar(200) NOT NULL,
	`size` int NULL,
	`ext` varchar(10) NULL,
	`path` varchar(250) NOT NULL,
	`uid` varchar(10) NULL,
	`aid` varchar(100) NOT NULL COMMENT '关联应用ID',
    `pid` varchar(100) DEFAULT NULL COMMENT '关联表单ID（预留，以后可能用得上）',
    `summary` varchar(255) NULL,
    `download` int DEFAULT 0 COMMENT '下载次数',
	addOn bigint NULL,
	PRIMARY KEY (id),
	KEY `form_uid_IDX` (`uid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE `app-meta`.department (
	id varchar(20) NOT NULL,
	name varchar(100) NOT NULL,
	CONSTRAINT department_pk PRIMARY KEY (id)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_0900_ai_ci;


-- `app-meta`.`data` definition

CREATE TABLE `data` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `aid` varchar(100) NOT NULL COMMENT '关联应用ID',
  `pid` varchar(100) DEFAULT NULL COMMENT '关联表单ID（预留，以后可能用得上）',
  `uid` varchar(15) DEFAULT NULL,
  `channel` varchar(20) DEFAULT NULL,
  `v` json DEFAULT NULL,
  `hide` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除，0=显示，1=删除',
  `hideOn` bigint DEFAULT NULL COMMENT '删除日期，格式为 yyyMMdd 的数字',
  `addOn` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `data_aid_IDX` (`aid`) USING BTREE,
  KEY `data_uid_IDX` (`uid`) USING BTREE,
  KEY `data_hide_IDX` (`hide`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- `app-meta`.`data_block` definition

CREATE TABLE `data_block` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `aid` varchar(100) NOT NULL COMMENT '关联应用ID',
  `uuid` varchar(200) NOT NULL COMMENT '数据块唯一ID',
  `text` TEXT NOT NULL,
  PRIMARY KEY (`id`),
  KEY `data_aid_IDX` (`aid`, `uuid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `data_batch` (
   `id` bigint NOT NULL AUTO_INCREMENT,
   `aid` varchar(100) NOT NULL COMMENT '关联应用ID',
   `pid` varchar(100) DEFAULT NULL COMMENT '关联表单ID（预留，以后可能用得上）',
   `uid` varchar(15) DEFAULT NULL,
   `batch` varchar(100) DEFAULT NULL,
   `size` int DEFAULT 0 COMMENT '数据量',
   `origin` varchar(200) DEFAULT NULL COMMENT '数据源头描述',
   `active` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否生效，0=已删除，1=有效',
   `addOn` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `data_batch_aid_IDX` (`aid`, `pid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 网页机器人执行结果记录

CREATE TABLE `data_robot` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `aid` varchar(100) NOT NULL COMMENT '关联应用ID',
  `pid` varchar(100) DEFAULT NULL COMMENT '关联表单ID（预留，以后可能用得上）',
  `uid` varchar(15) DEFAULT NULL,
  `ip` varchar(100) DEFAULT NULL,
  `startOn` bigint DEFAULT 0,
  `used` int DEFAULT 0 COMMENT '运行总时间，单位秒',
  `chrome` varchar(100) DEFAULT NULL,
  `os` varchar(100) DEFAULT NULL,
  `params` TEXT,
  `origin` TEXT,
  `logs` TEXT,
  `link` varchar(100),
  `addOn` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `data_aid_IDX` (`aid`) USING BTREE,
  KEY `data_uid_IDX` (`uid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE `notice` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `uid` varchar(15) DEFAULT NULL,
  `uname` varchar(200) DEFAULT '',
  `mode` varchar(10) DEFAULT 'notice' COMMENT '展示类型，notice=平铺显示、dialog=弹窗显示',
  `name` varchar(200) DEFAULT '',
  `serviceAuth` text,
  `fromDate` varchar(10) DEFAULT '',
  `toDate` varchar(10) DEFAULT '',
  `summary` text,
  `launch` int DEFAULT 0 COMMENT '投放次数',
  `addOn` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `notice_line` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `uid` varchar(15) DEFAULT NULL,
  `oid` bigint DEFAULT NULL,
  `doneOn` bigint DEFAULT NULL,
  `addOn` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `oid_IDX` (`oid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE `terminal_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `aid` varchar(100) NOT NULL COMMENT '关联应用ID',
  `uid` varchar(15) DEFAULT NULL,
  `method` varchar(10) DEFAULT '',
  `host` varchar(100) DEFAULT '',
  `url` varchar(255) DEFAULT '',
  `code` int DEFAULT 0,
  `summary` text,
  `used` bigint DEFAULT NULL,
  `channel` varchar(20) DEFAULT NULL,
  `addOn` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `aid_IDX` (`aid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `terminal_log_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `reqHeader` text COMMENT '请求头',
  `reqBody` text COMMENT '请求主体（字符串）',
  `resHeader` text COMMENT '响应头',
  `resBody` text COMMENT '响应主体（BASE64格式，需还原成字节，然后根据响应头转码）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;



CREATE TABLE `member` (
  `id` varchar(30) NOT NULL,
  `uuid` varchar(100) DEFAULT NULL,
  `name` varchar(100) DEFAULT NULL,
  `ids` varchar(255) NOT NULL COMMENT '授权用户ID',
  `category` varchar(10) NOT NULL DEFAULT '' COMMENT 'cli=命令行终端；worker=工作者；other=其他',
  `mode` tinyint NOT NULL DEFAULT '0',
  `secret` varchar(32) DEFAULT '',
  `pubKey` text,
  `priKey` text,
  `expire` int DEFAULT 0,
  `summary` text,
  `addOn` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `worker_task` (
  `id` varchar(100) NOT NULL,
  `uid` varchar(20) NOT NULL,
  `worker` varchar(100) NOT NULL,
  `method` varchar(100) NOT NULL,
  `params` json DEFAULT NULL,
  `status` varchar(10) NOT NULL DEFAULT 'PENDING',
  `response` text,
  `doneOn` bigint DEFAULT NULL,
  `addOn` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `worker_task_method_IDX` (`method`) USING BTREE,
  KEY `worker_task_worker_IDX` (`worker`) USING BTREE
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `dbm_source` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) DEFAULT NULL,
  `summary` text,
  `type` varchar(100) NOT NULL COMMENT '数据库类型',
  `host` varchar(100) DEFAULT '',
  `port` int DEFAULT -1,
  `username` varchar(100) DEFAULT '',
  `pwd` varchar(100) DEFAULT '',
  `db` varchar(100) DEFAULT '',
  `encoding` varchar(100) DEFAULT 'utf-8',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `dbm_auth` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) DEFAULT NULL,
  `uid` varchar(100) NOT NULL,
  `sourceId` int DEFAULT -1,
  `allow` varchar(100) DEFAULT '' COMMENT '授权详情，可选值：SQL、C、U、R、D，多个值用英文逗号隔开',
  `summary` text,
  PRIMARY KEY (`id`),
  UNIQUE KEY `dbm_auth_un` (`sourceId`,`uid`),
  KEY `uid_IDX` (`uid`) USING BTREE,
  KEY `sid_IDX` (`sourceId`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `dbm_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `uid` varchar(100) NOT NULL,
  `sourceId` int DEFAULT -1,
  `name` varchar(100) DEFAULT NULL,
  `action` varchar(100) DEFAULT NULL,
  `target` varchar(150) DEFAULT NULL,
  `ps` text COMMENT '参数（对于 action=sql 则是 SQL 语句）',
  `used` int DEFAULT NULL COMMENT '耗时，单位 ms',
  `summary` text,
  `addOn` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `uid_IDX` (`uid`) USING BTREE,
  KEY `sid_IDX` (`sourceId`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 系统功能表

-- sys_export_log definition
CREATE TABLE `sys_export_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `uid` varchar(100) DEFAULT NULL,
  `uname` varchar(100) DEFAULT NULL,
  `filename` varchar(100) DEFAULT NULL,
  `size` int DEFAULT NULL,
  `path` varchar(100) DEFAULT NULL,
  `data_count` int DEFAULT NULL,
  `entity` varchar(100) DEFAULT NULL,
  `summary` varchar(100) DEFAULT NULL,
  `add_date` varchar(100) DEFAULT NULL,
  `query` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`)
);

-- sys_import_log definition
CREATE TABLE `sys_import_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `uid` varchar(100) DEFAULT NULL,
  `uname` varchar(100) DEFAULT NULL,
  `filename` varchar(100) DEFAULT NULL,
  `size` int DEFAULT NULL,
  `path` varchar(100) DEFAULT NULL,
  `data_count` int DEFAULT NULL,
  `entity` varchar(100) DEFAULT NULL,
  `summary` varchar(100) DEFAULT NULL,
  `add_date` varchar(100) DEFAULT NULL,
  `insert_count` int DEFAULT NULL,
  `update_count` int DEFAULT NULL,
  PRIMARY KEY (`id`)
);

-- sys_operation definition
CREATE TABLE `sys_operation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cls` varchar(100) DEFAULT NULL COMMENT '对象Class',
  `uuid` varchar(100) DEFAULT NULL COMMENT '对象ID',
  `user` varchar(100) DEFAULT NULL COMMENT '用户信息',
  `ip` varchar(100) DEFAULT NULL,
  `type` int DEFAULT NULL,
  `summary` varchar(100) DEFAULT NULL,
  `addDate` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) AUTO_INCREMENT=15;

-- sys_role definition
CREATE TABLE `sys_role` (
  `id` varchar(100) NOT NULL,
  `name` varchar(100) NOT NULL,
  `summary` varchar(255) DEFAULT NULL,
  `admin` tinyint NOT NULL DEFAULT '0',
  `urls` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '',
  PRIMARY KEY (`id`)
);

-- sys_role_link definition
CREATE TABLE `sys_role_link` (
  `id` varchar(20) NOT NULL,
  `name` varchar(100) NOT NULL,
  `dept` varchar(100) DEFAULT NULL,
  `ip` varchar(200) DEFAULT NULL,
  `roles` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
);

-- sys_schedule_log definition
CREATE TABLE `sys_schedule_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category` varchar(200) DEFAULT NULL,
  `duration` bigint DEFAULT '0',
  `msg` text,
  `error` tinyint NOT NULL DEFAULT '0',
  `trace` text,
  `runOn` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`)
);

-- sys_setting definition
CREATE TABLE `sys_setting` (
  `id` varchar(100) NOT NULL,
  `title` varchar(100) NOT NULL,
  `summary` varchar(255) DEFAULT NULL,
  `defaultContent` varchar(255) DEFAULT NULL,
  `content` varchar(255) DEFAULT NULL,
  `form` varchar(15) NOT NULL DEFAULT 'TEXT',
  `formValue` varchar(255) DEFAULT NULL,
  `category` varchar(30) DEFAULT NULL,
  `sort` int DEFAULT '0' COMMENT '排序，越小越靠前',
  PRIMARY KEY (`id`)
);