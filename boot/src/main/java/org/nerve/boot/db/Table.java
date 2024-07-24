package org.nerve.boot.db;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target(ElementType.TYPE)
@Retention(RUNTIME)
public @interface Table {
    /**
     * (Optional) The name of the table.
     * <p> Defaults to the entity name.
     */
    String name() default "";
}
