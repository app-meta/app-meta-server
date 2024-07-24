package org.nerve.boot.module.export;

import com.baomidou.mybatisplus.annotation.TableName;
import org.nerve.boot.domain.AuthUser;

@TableName("sys_import_log")
public class ImportLog extends BasicExportBean {

    int insertCount = 0;
    int updateCount = 0;

    public ImportLog(){}

    public ImportLog(Class clazz){
        super(clazz);
    }

    public ImportLog(Class clazz, AuthUser user){
        super(clazz, user);
    }

    public int getInsertCount() {
        return insertCount;
    }

    public ImportLog setInsertCount(int insertCount) {
        this.insertCount = insertCount;
        return this;
    }

    public int getUpdateCount() {
        return updateCount;
    }

    public ImportLog setUpdateCount(int updateCount) {
        this.updateCount = updateCount;
        return this;
    }
}
