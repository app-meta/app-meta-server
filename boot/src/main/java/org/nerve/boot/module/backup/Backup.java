package org.nerve.boot.module.backup;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import org.nerve.boot.db.IdEntity;
import org.nerve.boot.util.DateUtil;

import java.util.HashMap;
import java.util.Map;

@TableName("sys_backup")
public class Backup extends IdEntity {
    String date;
    String path;
    long fileSize;              //文件大小
    String user;
    long duration;
    boolean disable;
    boolean human;              //是否为人工执行的操作
    String summary;
    String counterMap;  //备份数据量，key 为类名
    @TableField(exist = false)
    Map<String, Long> counter;


    public Backup(){
        date = DateUtil.getDate();
    }

    public Backup addCount(String keyName, long count){
        if(counter == null)
            counter = new HashMap<String, Long>();
        counter.put(keyName, count);
        return this;
    }

    public String getDate() {
        return date;
    }

    public Backup setDate(String date) {
        this.date = date;
        return this;
    }

    public String getPath() {
        return path;
    }

    public Backup setPath(String path) {
        this.path = path;
        return this;
    }

    public String getUser() {
        return user;
    }

    public Backup setUser(String user) {
        this.user = user;
        return this;
    }

    public long getDuration() {
        return duration;
    }

    public Backup setDuration(long duration) {
        this.duration = duration;
        return this;
    }

    public boolean isDisable() {
        return disable;
    }

    public Backup setDisable(boolean disable) {
        this.disable = disable;
        return this;
    }

    public boolean isHuman() {
        return human;
    }

    public Backup setHuman(boolean human) {
        this.human = human;
        return this;
    }

    public String getSummary() {
        return summary;
    }

    public Backup setSummary(String summary) {
        this.summary = summary;
        return this;
    }

    public void setCounter(Map<String, Long> counter) {
        this.counter = counter;
    }

    public Map<String, Long> getCounter() {
        return counter;
    }

    public String getCounterMap() {
        return counterMap;
    }

    public void setCounterMap(String counterMap) {
        this.counterMap = counterMap;
    }

    public long getFileSize() {
        return fileSize;
    }

    public Backup setFileSize(long fileSize) {
        this.fileSize = fileSize;
        return this;
    }
}
