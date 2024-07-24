package org.nerve.boot.module.schedule;

/**
 * 基于 mybatis-plus 的定时任务抽象类
 */

import jakarta.annotation.Resource;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.nerve.boot.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSchedule {

    protected Logger logger = LoggerFactory.getLogger(getClass());
    protected static String NOT_SAVE_LOG = "...";

    @Resource
    protected ScheduleLogMapper mapper;

    /**
     * 定时任务类别名称，默认是当前类名
     * @return
     */
    protected String getCategoryName(){
        return getClass().getSimpleName();
    }

    protected String banner(){ return getCategoryName(); }

    protected void info(String msg, Object... ps){
        logger.info("[定时任务] ["+getCategoryName()+"] " + msg, ps);
    }

    protected void doWork(ScheduleWork worker){
        logger.info("[定时任务] 开始执行 {} ...", getCategoryName());
        long startOn = System.currentTimeMillis();

        ScheduleLog scheduleLog = new ScheduleLog(getCategoryName());

        try{
            String result = worker.get();
            if(NOT_SAVE_LOG.equals(result)) return;

            scheduleLog.setMsg(result);
        }catch (Exception e){
            scheduleLog.setError(true);
            scheduleLog.setMsg(ExceptionUtils.getMessage(e));
            scheduleLog.setTrace(ExceptionUtils.getStackTrace(e));

            logger.error("[定时任务] "+scheduleLog.getCategory()+" 出错", e);
        }

        //计算耗时
        scheduleLog.setDuration(System.currentTimeMillis() - startOn);
        scheduleLog.setRunOn(DateUtil.getDateTime());
        mapper.insert(scheduleLog);
        logger.info(
                "[定时任务] 完成执行 {}：ERROR = {}， 耗时 {} ms， {}",
                scheduleLog.getCategory(),
                scheduleLog.isError(),
                scheduleLog.getDuration(),
                scheduleLog.getMsg()
        );
    }

    @FunctionalInterface
    public interface ScheduleWork {

        /**
         * Gets a result.
         *
         * @return a result
         */
        String get() throws Exception;
    }
}