package org.nerve.boot.db.service;

/*
 * 适配 mybatis-plus 的查询辅助工具
 */

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.util.ObjectUtils;

import java.util.*;

import static org.nerve.boot.Const.UNDER;

public class QueryHelper<T> {

    private static String AT = "@";
    private static List<String> DESC = Arrays.asList("1", "DESC");

    public QueryWrapper<T> buildFromMap(Map<String, Object> params){
        QueryWrapper<T> q = Wrappers.query();

        if(params != null){
            params.forEach((k,v)-> {
                if (ObjectUtils.isEmpty(v))
                    return;
                String t[] = k.split(UNDER);
                boolean isSpecial = k.endsWith(UNDER) && !k.endsWith("__");      //是否需要特殊处理
                if (t.length < 2)
                    return;

                switch (t[0].toUpperCase()) {
                    case "LIKE":
                        if(t.length==2) q.like(t[1].replaceAll(AT,UNDER), v);
                        //多个字段 OR 模糊查询
                        else {
                            q.and(qw-> {
                                for (int i = 1; i < t.length; i++) {
                                    qw.or().like(t[i].replaceAll(AT, UNDER), v);
                                }
                            });
                        }
                        break;
                    case "EQ":
                        q.eq(t[1], v);
                        break;
                    case "IN":
                        List<Object> list = new ArrayList<>();

                        if(v instanceof List)           list.addAll((List) v);
                        else if(v instanceof Object[])  list.addAll(Arrays.asList((Object[]) v));
                        else                            list.add(v);

                        q.in(t[1], list);
                        break;
                    case "LT":      q.lt(t[1], isSpecial? Long.valueOf((String)v) :v); break;
                    case "LTE":     q.le(t[1], isSpecial? Long.valueOf((String)v) :v); break;
                    case "GT":      q.gt(t[1], isSpecial? Long.valueOf((String)v) :v); break;
                    case "GTE":     q.ge(t[1], isSpecial? Long.valueOf((String)v) :v); break;
                    case "NE":      q.ne(t[1], parseValue(v, isSpecial)); break;
                    case "SORT":
                        List<String> sortList = new ArrayList<>();
                        for (int i = 1; i < t.length; i++)
                            sortList.add(t[i]);

                        if(DESC.contains(v.toString()))
                            q.orderByDesc(sortList);
                        else
                            q.orderByAsc(sortList);
                        break;
                }
            });
        }

        return q;
    }

    /**
     * 在处理 EQ、NE 的操作，如果希望 value 是空字符串、null 时略显乏力
     * 目前方案是使用特殊的字符进行替换，详见 SEARCH_VALUES
     *
     * @param v
     * @param isSpecial
     * @return
     */
    private Object parseValue(Object v, boolean isSpecial){
        if(isSpecial)
            return Long.valueOf((String) v);
        if(SEARCH_VALUES.containsKey(v))
            return SEARCH_VALUES.get(v);
        return v;
    }

    private static final Map<String,Object> SEARCH_VALUES = new HashMap<>();
    static {
        SEARCH_VALUES.put("@@","");
        SEARCH_VALUES.put("@NULL@", null);
    }
}
