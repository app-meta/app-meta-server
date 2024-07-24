package org.nerve.boot.util;

import org.nerve.boot.annotation.CN;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class ClassNameUtil {
    private static Map<Class, String> nameMap = new HashMap<>();

    private static Function<Class, String> defaultCreator = Class::getSimpleName;
    /**
     * 查询类定义的 CN 名称（默认返回类名）
     * @param clazz
     * @return
     */
    public static String get(Class clazz){
        return get(clazz, defaultCreator);
    }

    public static String get(Class clazz, Function<Class, String> creator){
        if(nameMap.containsKey(clazz))  return nameMap.get(clazz);
        String className = creator.apply(clazz);
        if(clazz.isAnnotationPresent(CN.class)){
            CN cnDef = (CN) clazz.getDeclaredAnnotation(CN.class);
            if(StringUtils.hasText(cnDef.value()))
                className = cnDef.value();
        }
        nameMap.put(clazz, className);
        return className;
    }
}
