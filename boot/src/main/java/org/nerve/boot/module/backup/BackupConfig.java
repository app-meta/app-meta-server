package org.nerve.boot.module.backup;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
@ConfigurationProperties(prefix = "nerve.backup")
public class BackupConfig {
    private boolean enable;
    private boolean deleteTableFile = true;     //是否删除单个数据表的备份文件
    private int delay   = 7;                    //自动备份间隔（单位天），默认每周进行一次备份
    private int expire  = 15;                   //历史备份期限（单位天），超过则删除
    private Set<String> entities;              //需要备份的实体列表，完整类路径
    private Set<String> defaultEntities;        //默认需要备份的实体
    private String cron = "0 0 5 * * ?";        //默认每天早上5点执行备份

    public boolean isDeleteTableFile() {
        return deleteTableFile;
    }

    public BackupConfig setDeleteTableFile(boolean deleteTableFile) {
        this.deleteTableFile = deleteTableFile;
        return this;
    }

    public boolean isEnable() {
        return enable;
    }

    public BackupConfig setEnable(boolean enable) {
        this.enable = enable;
        return this;
    }

    public int getDelay() {
        return delay;
    }

    public BackupConfig setDelay(int delay) {
        this.delay = delay;
        return this;
    }

    public int getExpire() {
        return expire;
    }

    public BackupConfig setExpire(int expire) {
        this.expire = expire;
        return this;
    }

    public Set<String> getEntities() {
        return entities;
    }

    public BackupConfig setEntities(Set<String> entities) {
        this.entities = entities;
        return this;
    }

    public String getCron() {
        return cron;
    }

    public BackupConfig setCron(String cron) {
        this.cron = cron;
        return this;
    }

    public Set<String> getDefaultEntities() {
        return defaultEntities;
    }

    public BackupConfig setDefaultEntities(Set<String> defaultEntities) {
        this.defaultEntities = defaultEntities;
        return this;
    }
}
