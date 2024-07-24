package org.nerve.boot.web;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.nerve.boot.enums.Fields;
import org.nerve.boot.module.auth.RoleLinkMapper;
import org.nerve.boot.module.auth.RoleMapper;
import org.nerve.boot.module.setting.Setting;
import org.nerve.boot.module.setting.SettingService;
import org.nerve.boot.util.FileLoader;
import org.nerve.boot.util.Timing;
import org.nerve.boot.web.auth.Role;
import org.nerve.boot.web.auth.RoleLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.io.IOException;
import java.util.List;

import static org.nerve.boot.Const.SPACE;

@Configuration
@ConditionalOnProperty(value = "nerve.init", havingValue = "true" , matchIfMissing = true)
public class SystemInit {
    private Logger logger = LoggerFactory.getLogger(SystemInit.class);

    @Resource
    SettingService settingService;
    @Resource
    RoleMapper roleMapper;
    @Resource
    RoleLinkMapper roleLinkMapper;
    @Resource
    List<InitWorker> workers;

    @EventListener(ApplicationStartedEvent.class)
    public void initOnStart(){
        try{
            Class.forName("org.springframework.boot.test.context.SpringBootTest");
            logger.debug("[初始化] 检测到为测试环境，跳过...");
            return;
        }catch (ClassNotFoundException e){
            if(logger.isDebugEnabled()) logger.debug("[初始化] 程序即将初始化（包含但不限于超级管理员、附件目录）...");
        }

        Timing timing = new Timing();

        initSetting();
        initRole();
        initRoleLink();

        for (InitWorker worker : workers) {
            String msg = worker.get();
            logger.info("[初始化-{}] {}", worker.getClass().getSimpleName(), msg);
        }

        if(logger.isDebugEnabled()) logger.debug("[初始化] 作业完成，耗时 {} 秒 ^_^", timing.toSecondStr());
    }

    /**
     * user.txt 文件说明如下

     # 用户权限初始化
     # 格式说明：
     #   1、一行一个用户（# 开头或者空行为注释行）
     #   2、内容格式为：用户ID{单个空格}角色{单个空格}用户名
     #   3、多个角色用英文逗号隔开
     #   4、注意：用户ID跟角色直接用空格（单个）隔开
     #   6、如果数据库已经存在同 用户ID的权限配置，则跳过
     #   7、用户名是可选项
     #
     # 示例：
     #       0001 ADMIN
     #       0002 ADMIN
     #       0003 NORMAL,REVIEW 三把手

     */
    private void initRoleLink(){
        try{
            List<String> lines = FileLoader.loadLines("user.txt").stream()
                    .map(v->v.trim())
                    .filter(v-> !v.isEmpty() && !v.startsWith("#"))
                    .toList();
            if(lines.isEmpty()) return;

            if(logger.isDebugEnabled()) logger.debug("[初始化-用户] 读取到 {} 条规则", lines.size());

            List<Object> ids = roleLinkMapper.selectObjs(new QueryWrapper<RoleLink>().select(Fields.ID.value()));
            for (String line : lines) {
                String[] tmp = line.split(SPACE);
                if(tmp.length < 2)  continue;
                if(ids.contains(tmp[0])) continue;

                RoleLink link = new RoleLink();
                link.setId(tmp[0]);
                link.setRoles(tmp[1]);
                if(tmp.length>=3) link.setName(tmp[2]);

                roleLinkMapper.insert(link);
                logger.info("[初始化-用户] 用户 {} 分配角色 {}", link.id(), link.getRoles());
            }
        }catch (Exception e){
            if(logger.isDebugEnabled()) logger.debug("[初始化-用户] user.txt 文件不存在，跳过...");
        }
    }

    private void initRole(){
        try{
            List<Role> roles = JSON.parseArray(FileLoader.loadContent("roles.json"), Role.class);
            if(roles.isEmpty()) return;

            logger.info("[初始化-角色] 读取到 {} 个角色", roles.size());
            List<Object> ids = roleMapper.selectObjs(new QueryWrapper<Role>().select(Fields.ID.value()));
            for (Role role : roles) {
                if(ids.contains(role.id())) continue;

                logger.info("[初始化-角色] 检测到 {}/{} 角色不存在，即将创建...", role.id(), role.getName());
                roleMapper.insert(role);
            }
        }catch (Exception e){
            if(logger.isDebugEnabled()) logger.debug("[初始化-角色] roles.json 文件不存在，跳过...");
        }
    }

    /**
     * 初始化系统配置
     *
     * ① 判断是否存在 settings.json 文件
     * ②
     */
    private void initSetting(){
        try{
            String jsonStr = FileLoader.loadContent("settings.json");
            List<Setting> settings = JSON.parseArray(jsonStr, Setting.class);

            if(settings.isEmpty()) return;

            logger.info("[初始化-配置项] 读取到 {} 个配置信息", settings.size());

            List<Object> existIds = settingService.listObjs();

            for (int i = 0; i < settings.size(); i++) {
                Setting s = settings.get(i);
                if(s.getSort() == -1)
                    s.setSort(i);

                if(!existIds.contains(s.id())){
                    logger.info("[初始化-配置项] 检测到 {} 不存在，即将保存该项...", s.getId());

                    if(StringUtils.isEmpty(s.getContent()))
                        s.setContent(s.getDefaultContent());
                    else if(StringUtils.isEmpty(s.getDefaultContent()))
                        s.setDefaultContent(s.getContent());

                    settingService.save(s);
                }
            }
        }catch (IOException e){
            if(logger.isDebugEnabled()) logger.debug("[初始化-配置项] settings.json 文件不存在，跳过配置项...");
        }
    }
}
