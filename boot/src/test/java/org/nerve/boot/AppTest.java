package org.nerve.boot;

import com.alibaba.fastjson2.JSON;
import org.springframework.boot.test.context.SpringBootTest;

import static com.alibaba.fastjson2.JSONWriter.Feature.FieldBased;
import static com.alibaba.fastjson2.JSONWriter.Feature.WriteMapNullValue;

/*
 * @project boot-starter
 * @file    org.nerve.boot.db.AppTest
 * CREATE   2023年01月03日 16:01 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */
@SpringBootTest
public class AppTest {

    protected void json(Object obj){
        System.out.println(JSON.toJSONString(obj, FieldBased, WriteMapNullValue));
    }
}
