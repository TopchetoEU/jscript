package me.topchetoeu.jscript.interop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ExposeField {
    public String value() default "";
    public ExposeTarget target() default ExposeTarget.MEMBER;
}
