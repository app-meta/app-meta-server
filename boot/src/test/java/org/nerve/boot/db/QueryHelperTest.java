package org.nerve.boot.db;
/*
 * @project boot-starter
 * @file    org.nerve.boot.db.QueryHelperTest
 * CREATE   2023年01月03日 16:02 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.Test;
import org.nerve.boot.db.service.QueryHelper;

import java.util.HashMap;
import java.util.Map;

public class QueryHelperTest {

    private QueryHelper helper = new QueryHelper();

    @Test
    public void buildQuery(){
        Map<String, Object> map = new HashMap<>();
        // (id = ?)
        map.put("EQ_id", 1);
        // (age <= ? AND id = ?)
        map.put("LTE_age", 60);
        // (age <= ? AND (name LIKE ? OR name2 LIKE ?) AND id = ?)
        map.put("LIKE_name_name2", "张三");

        QueryWrapper q = helper.buildFromMap(map);
        System.out.println(q.getTargetSql());
    }
}
