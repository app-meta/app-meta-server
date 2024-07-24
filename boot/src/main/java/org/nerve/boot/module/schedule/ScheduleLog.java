package org.nerve.boot.module.schedule;

import com.baomidou.mybatisplus.annotation.TableName;
import org.nerve.boot.db.IdEntity;

@TableName("sys_schedule_log")
public class ScheduleLog extends IdEntity {
    private String category;
    private long duration;
    private String msg;
    private boolean error;
    private String trace;
    private String runOn;

    public ScheduleLog(){}
    public ScheduleLog(String category){
        this.category   = category;
    }

    public String getCategory() {
        return category;
    }

    public ScheduleLog setCategory(String category) {
        this.category = category;
        return this;
    }

    public long getDuration() {
        return duration;
    }

    public ScheduleLog setDuration(long duration) {
        this.duration = duration;
        return this;
    }

    public String getMsg() {
        return msg;
    }

    public ScheduleLog setMsg(String msg) {
        this.msg = msg;
        return this;
    }

    public boolean isError() {
        return error;
    }

    public ScheduleLog setError(boolean error) {
        this.error = error;
        return this;
    }

    public String getTrace() {
        return trace;
    }

    public ScheduleLog setTrace(String trace) {
        this.trace = trace;
        return this;
    }

    public String getRunOn() {
        return runOn;
    }

    public ScheduleLog setRunOn(String runOn) {
        this.runOn = runOn;
        return this;
    }
}
