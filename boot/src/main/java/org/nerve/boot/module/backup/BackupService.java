package org.nerve.boot.module.backup;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.ibatis.session.SqlSession;
import org.nerve.boot.FileStore;
import org.nerve.boot.db.service.BaseService;
import org.nerve.boot.util.ClassNameUtil;
import org.nerve.boot.util.DateUtil;
import org.nerve.boot.util.Timing;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.nerve.boot.Const.COMMA;

@Service
@ConditionalOnProperty(value = "nerve.backup.enable", havingValue = "true" , matchIfMissing = true)
public class BackupService extends BaseService<BackupMapper, Backup> {
    public static final String BACKUP = "BACKUP";

    @Resource
    private BackupConfig config;

    @Resource
    private FileStore fileStore;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private  SqlSession sqlSession;

    /**
     * 执行备份
     * @param summary
     * @return
     */
    public Backup doBackup(String summary) throws Exception {
        Timing timing = new Timing();
        info("开始数据备份，是否删除单个数据表的备份文件 {}", config.isDeleteTableFile());

        Backup bk = new Backup();

        List<File> files = new ArrayList<>();
        Set<String> entities = new HashSet<>();
        if(config.getEntities() != null)        entities.addAll(config.getEntities());
        if(config.getDefaultEntities() != null) entities.addAll(config.getDefaultEntities());

        info("共需要备份 {} 则实体...", entities.size());
        Map<Object, Object> counterMap = new HashMap<>();
        for (String entityCls : entities) {
            try{
                Class clazz = Class.forName(entityCls);
                BackupBean bean = backupWithClass(clazz);

                files.add(bean.file);


                counterMap.put(ClassNameUtil.get(clazz, aClass -> aClass.getName().replace(".", "@")), bean.count);
                bk.setCounterMap(JSON.toJSONString(counterMap));
                bk.addCount(ClassNameUtil.get(clazz, aClass -> aClass.getName().replace(".", "@")), bean.count);
            }catch (ClassNotFoundException e) {
                logger.error("[备份失败] 待备份实体不存在：{}", e.getMessage());
            } catch (IOException e) {
                logger.error("[备份失败] 执行备份时出错 =.=", e);
            }
        }

        if(!files.isEmpty()){
            Path zipPath    = toZip(files);
            bk.path         = zipPath.toString();
            bk.fileSize     = Files.size(zipPath);

            if(config.isDeleteTableFile()){
                info("即将删除 {} 个临时备份 JSON 文件...", files.size());
                for (File file : files) {
                    FileUtils.deleteQuietly(file);
                }
            }
        }
        bk.human    = StringUtils.hasText(summary);
        bk.summary  = StringUtils.hasText(summary)? summary : String.format("%s 自动备份", DateUtil.getDateTime());
        bk.duration = timing.toMillSecond();
        save(bk);
        return bk;
    }

    public String doRestore(String id, Set<String> tables) throws IOException {
        Backup bk = getById(id);
        Assert.notNull(bk, String.format("备份记录（ID=%s）不存在", id));
        Assert.isTrue(StringUtils.hasText(bk.path), "字段“备份文件”为空，无法进行还原操作");

        //判断备份文件是否存在
        Path path = Paths.get(bk.path);
        Assert.isTrue(Files.exists(path), "备份文件不存在");


        Timing timing = new Timing();
        List<String> infos = new ArrayList<>();


        for (File file : zipToFiles(path.toFile())) {
            Stream<String> tableStream = CollectionUtils.isEmpty(tables)?
                    config.getEntities().stream()
                    :
                    tables.stream().map(n->n.replace("@", "."))
                    ;
            Optional<String> table = tableStream.filter(n-> file.getName().startsWith(FilenameUtils.getExtension(n))).findFirst();
            if(table.isPresent()){
                info("还原 {} （数据来源 {}）...", table.get(), file);

                try{
                    Class clazz = Class.forName(table.get());
                    Class<?> mapperClazz = Class.forName(table.get() + "Mapper");
                    Method insert = mapperClazz.getMethod("insert", Object.class);
                    Method delete = mapperClazz.getMethod("delete", Wrapper.class);
                    Object instance = Proxy.newProxyInstance(mapperClazz.getClassLoader(),
                            new Class[]{mapperClazz},
                            new MapperInvocationHandler(sqlSession.getMapper(mapperClazz)));
                    delete.invoke(instance, new QueryWrapper<>());
                    AtomicInteger count = new AtomicInteger();
                    Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8).lines().forEach(line->{
                        Object entity = JSON.parseObject(line, clazz);
                        try {
                            insert.invoke(instance, entity);
                        } catch (Exception e) {
                            logger.error("还原 {} 时出错...", table.get(), e);
                        }
                        count.getAndIncrement();
                    });

                    infos.add(String.format("还原%s %d 条", clazz.getSimpleName(), count.get()));

                    FileUtils.deleteQuietly(file);
                }catch (Exception e){
                    logger.error("还原 {} 时出错...", table.get(), e);
                }
            }
        }

        infos.add(String.format("耗时%s秒", timing.toSecondStr()));
        return org.apache.commons.lang3.StringUtils.join(infos, COMMA);
    }

    private class BackupBean{
        int count;
        File file;

        BackupBean(int count, File file){
            this.count = count;
            this.file = file;
        }
    }

    private BackupBean backupWithClass(Class clazz) throws IOException {
        info("开始备份 {}", clazz.getName());
        Path path = fileStore.buildPath(
                String.format("%s-%s.json", clazz.getSimpleName(), DateUtil.getDate("HHmmss")),
                BACKUP
        );
        Files.createDirectories(path.getParent());
        int count = 0;

        try(BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)){
            String tableName;
            TableName table = (TableName) clazz.getAnnotation(TableName.class);
            if (table!=null){
                tableName = table.value();
            }else {
                tableName = clazz.getSimpleName().toLowerCase();
            }
            List<Map<String, Object>> maps = jdbcTemplate.queryForList("select * from " + tableName);
            for (Object obj : maps){
                writer.write(JSON.toJSONString(obj));
                writer.newLine();
                count ++;
            }
        }

        info("完成 {} 的备份，共 {} 条...", clazz.getName(), count);
        return new BackupBean(count, path.toFile());
    }

    private void info(String msg, Object... ps) {
        logger.info("[数据备份] "+msg, ps);
    }

    private Path toZip(List<File> files) throws Exception{
        Path path = fileStore.buildPath(String.format("%s-%s.zip", BACKUP, DateUtil.getDate("yyyyMMddHHmmss")), BACKUP);

        try(
                FileOutputStream fos = new FileOutputStream(path.toFile());
                ZipOutputStream zos = new ZipOutputStream(fos)
        ){
            for (File file : files) {
                zos.putNextEntry(new ZipEntry(file.getName()));
                byte[] bytes = new byte[2048];
                FileInputStream fis = new FileInputStream(file);

                int len = fis.read(bytes);
                while (len != -1){
                    zos.write(bytes, 0, len);
                    len = fis.read(bytes);
                }
                zos.closeEntry();
                fis.close();
            }
        }
        return path;
    }

    private List<File> zipToFiles(File originFile) throws IOException {
        List<File> files = new ArrayList<>();
        try(ZipFile zipFile = new ZipFile(originFile, StandardCharsets.UTF_8)){
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()){
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();

                File file = Paths.get(originFile.getParent(), name).toFile();
                FileUtils.copyToFile(zipFile.getInputStream(entry), file);
                info("解压 {} 到 {} ...", name, file);

                files.add(file);
            }
        }
        info("解压完成，共 {} 个文件...", files.size());
        return files;
    }
}
