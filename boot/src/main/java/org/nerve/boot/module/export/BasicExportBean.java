package org.nerve.boot.module.export;

import org.nerve.boot.db.DateEntity;
import org.nerve.boot.domain.AuthUser;

import java.util.Date;

public class BasicExportBean extends DateEntity {
    String  uid;
    String  uname;
    String  filename;
    long    size;
    String  path;
    int     dataCount;
    String  entity;
    String  summary;

    public BasicExportBean(){}

    public BasicExportBean(Class clazz){
        if(clazz != null)
            entity = clazz.getSimpleName();
        setAddDate(new Date());
    }

    public BasicExportBean(Class clazz, AuthUser user){
        this(clazz);

        if(user != null){
            uid     = user.getId();
            uname   = user.getShowName();
        }
    }

    public String getUid() {
        return uid;
    }

    public BasicExportBean setUid(String uid) {
        this.uid = uid;
        return this;
    }

    public String getUname() {
        return uname;
    }

    public BasicExportBean setUname(String uname) {
        this.uname = uname;
        return this;
    }

    public String getFilename() {
        return filename;
    }

    public BasicExportBean setFilename(String filename) {
        this.filename = filename;
        return this;
    }

    public long getSize() {
        return size;
    }

    public BasicExportBean setSize(long size) {
        this.size = size;
        return this;
    }

    public String getPath() {
        return path;
    }

    public BasicExportBean setPath(String path) {
        this.path = path;
        return this;
    }

    public int getDataCount() {
        return dataCount;
    }

    public BasicExportBean setDataCount(int dataCount) {
        this.dataCount = dataCount;
        return this;
    }

    public String getEntity() {
        return entity;
    }

    public BasicExportBean setEntity(String entity) {
        this.entity = entity;
        return this;
    }

    public String getSummary() {
        return summary;
    }

    public BasicExportBean setSummary(String summary) {
        this.summary = summary;
        return this;
    }
}
