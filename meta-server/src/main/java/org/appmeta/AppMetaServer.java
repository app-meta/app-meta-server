package org.appmeta;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.annotation.MapperScans;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableCaching
@EnableScheduling
@SpringBootApplication
@ComponentScan({"org.appmeta", "org.nerve"})
/*
update on 2023-11-08
通过 @MapperScans 配置不同的扫描方式
针对 org.appmeta 下只扫描 @Mapper 的接口

旧版配置方式为：
@MapperScan({"org.nerve.boot.module", "org.appmeta.domain", "org.appmeta.module"})
 */
@MapperScans({
        @MapperScan("org.nerve.boot.module"),
        @MapperScan(value = {"org.appmeta.domain", "org.appmeta.module"}, annotationClass = Mapper.class)
})
public class AppMetaServer {

    private static ConfigurableApplicationContext context;

    public static void main(String[] args) {
        context = SpringApplication.run(AppMetaServer.class, args);
    }

    /**
     * 重启上下文
     */
    public static void restart(){
        ApplicationArguments args = context.getBean(ApplicationArguments.class);
        Thread thread = new Thread(() -> {
            context.close();
            try {
                Thread.sleep(1000);
            }
            catch (Exception ignored){}
            context = SpringApplication.run(AppMetaServer.class, args.getSourceArgs());
        });
        // 设置非守护线程
        thread.setDaemon(false);
        thread.start();
    }
}
