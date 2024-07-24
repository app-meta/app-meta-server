package org.nerve.boot.module.export;

import com.baomidou.mybatisplus.annotation.TableName;
import org.nerve.boot.domain.AuthUser;

@TableName("sys_export_log")
public class ExportLog extends BasicExportBean {
    String query;

    public ExportLog(){}

    public ExportLog(Class clazz){
        super(clazz);
    }

    public ExportLog(Class clazz, AuthUser user){
        super(clazz, user);
    }

    public String getQuery() {
        return query;
    }

    public ExportLog setQuery(String query) {
        this.query = query;
        return this;
    }
}
