package org.nerve.boot.annotation;

import java.lang.annotation.*;

/**
 * 此注解的 Controller 的接口不被 RequestMappingListener 扫描
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface Public {
}
