package org.nerve.boot.module.backup;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.nerve.boot.module.schedule.AbstractSchedule;
import org.nerve.boot.util.DateUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(value = "nerve.backup.enable", havingValue = "true" , matchIfMissing = true)
public class BackupSchedule extends AbstractSchedule {
    @Resource
    BackupConfig config;
    @Resource
    BackupMapper backupMapper;
    @Resource
    BackupService service;

    @Override
    protected String getCategoryName() {
        return "数据自动备份";
    }

    @Scheduled(cron = "${nerve.backup.cron:0 0 5 * * ?}")
    public void workForBackup(){
        doWork(()->{
            List<String> results = new ArrayList<>();

            results.add(autoBackup());
            results.add(cleanBackup());

            return StringUtils.join(results, ";");
        });
    }

    private String autoBackup(){
        String latestDate = DateUtil.getDateAfter(-1 * config.getDelay());
        try{
            QueryWrapper<Backup> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("human", false);
            queryWrapper.eq("disable", false);
            queryWrapper.eq("date", latestDate);
            Long backupCount = backupMapper.selectCount(queryWrapper);

            if(backupCount > 0)
                return "无需自动备份";

            Backup bk = service.doBackup(null);
            info("系统备份完成：{}", bk.summary);

            return "备份完成，耗时 "+bk.duration+" ms";
        }catch (Exception e){
            Backup backup = new Backup();
            backup.disable = true;
            backup.summary = ExceptionUtils.getMessage(e);

            backupMapper.insert(backup);
            return "备份失败："+e.getMessage();
        }
    }

    private String cleanBackup(){
        String latestDate = DateUtil.getDateAfter(-1 * config.getExpire());

        int count = 0;
        QueryWrapper<Backup> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("disable", false);
        queryWrapper.eq("date", latestDate);
        List<Backup> toDels = backupMapper.selectList(queryWrapper);
        for (Backup backup : toDels) {
            info("即将删除 {} 的备份文件 {} ...", backup.date, backup.path);

            if(StringUtils.isNotEmpty(backup.path)){
                FileUtils.deleteQuietly(new File(backup.path));
                backup.disable=true;
                backup.summary=DateUtil.getDateTime()+" 超时删除";
                backupMapper.updateById(backup);
                count ++;
            }
        }
        return "自动移除 "+count+" 个备份数据";
    }
}
