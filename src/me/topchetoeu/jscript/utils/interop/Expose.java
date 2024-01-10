package me.topchetoeu.jscript.utils.interop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Expose {
    public String value() default "";
    public ExposeType type() default ExposeType.METHOD;
    public ExposeTarget target() default ExposeTarget.MEMBER;
}
