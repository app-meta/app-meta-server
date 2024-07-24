package org.nerve.boot.module.operation;

import com.baomidou.mybatisplus.annotation.TableName;
import org.nerve.boot.db.DateEntity;
import org.nerve.boot.domain.AuthUser;
import org.nerve.boot.domain.ID;

import java.util.Date;

@TableName("sys_operation")
public class Operation extends DateEntity {
    public static final int DEFAULT = 0;
    public static final int QUERY = 1;
    public static final int CREATE = 2;
    public static final int MODIFY = 3;
    public static final int DELETE = 4;
    public static final int IMPORT = 5;
    public static final int EXPORT = 6;

    String cls;             //对象类型（若有的话）
    String uuid;            //对象ID（若有的话）
    String user;            //用户名
    String ip;
    int type = DEFAULT;
    String summary;

    public Operation(){}
    public Operation(AuthUser authUser, String summary){
        if(authUser != null){
            user = authUser.getShowName();
            ip = authUser.getIp();
        }
        this.summary = summary;
        addDate = new Date();
    }
    public Operation(AuthUser authUser, ID entity, String summary){
        this(authUser, entity, DEFAULT, summary);
    }
    public Operation(AuthUser authUser, ID entity, int type, String summary){
        this(authUser, summary);

        if(entity != null){
            cls = entity.getClass().getSimpleName();
            uuid = String.valueOf(entity.id());
        }
        this.type = type;
    }
    public Operation(AuthUser authUser, Class<? extends ID> clazz, int type, String summary){
        this(authUser, summary);
        if(clazz != null){
            cls = clazz.getSimpleName();
        }
        this.type = type;
    }

    public String getCls() {
        return cls;
    }

    public Operation setCls(String cls) {
        this.cls = cls;
        return this;
    }

    public String getUuid() {
        return uuid;
    }

    public Operation setUuid(String uuid) {
        this.uuid = uuid;
        return this;
    }

    public String getUser() {
        return user;
    }

    public Operation setUser(String user) {
        this.user = user;
        return this;
    }

    public String getIp() {
        return ip;
    }

    public Operation setIp(String ip) {
        this.ip = ip;
        return this;
    }

    public int getType() {
        return type;
    }

    public Operation setType(int type) {
        this.type = type;
        return this;
    }

    public String getSummary() {
        return summary;
    }

    public Operation setSummary(String summary) {
        this.summary = summary;
        return this;
    }
}
