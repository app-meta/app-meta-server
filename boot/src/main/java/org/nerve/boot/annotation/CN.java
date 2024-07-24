package org.nerve.boot.annotation;

import java.lang.annotation.*;

import static org.nerve.boot.Const.EMPTY;


@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
public @interface CN {
    String value() default EMPTY;
}
