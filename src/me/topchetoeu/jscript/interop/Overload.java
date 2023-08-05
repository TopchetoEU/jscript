package me.topchetoeu.jscript.interop;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import me.topchetoeu.jscript.engine.CallContext;

public class Overload {
    public static interface OverloadRunner {
        Object run(CallContext ctx, Object thisArg, Object[] args) throws
            InterruptedException,
            ReflectiveOperationException,
            IllegalArgumentException;
    }

    public final Overload.OverloadRunner runner;
    public final boolean variadic;
    public final Class<?> thisArg;
    public final Class<?>[] params;

    public static Overload fromMethod(Method method) {
        return new Overload(
            (ctx, th, args) -> method.invoke(th, args),
            method.isVarArgs(),
            Modifier.isStatic(method.getModifiers()) ? null : method.getDeclaringClass(),
            method.getParameterTypes()
        );
    }
    public static Overload fromConstructor(Constructor<?> method) {
        return new Overload(
            (ctx, th, args) -> method.newInstance(args),
            method.isVarArgs(),
            Modifier.isStatic(method.getModifiers()) ? null : method.getDeclaringClass(),
            method.getParameterTypes()
        );
    }
    public static Overload getterFromField(Field field) {
        return new Overload(
            (ctx, th, args) -> field.get(th), false,
            Modifier.isStatic(field.getModifiers()) ? null : field.getDeclaringClass(),
            new Class[0]
        );
    }
    public static Overload setterFromField(Field field) {
        if (Modifier.isFinal(field.getModifiers())) return null;
        return new Overload(
            (ctx, th, args) -> { field.set(th, args[0]); return null; }, false,
            Modifier.isStatic(field.getModifiers()) ? null : field.getDeclaringClass(),
            new Class[0]
        );
    }


    public Overload(Overload.OverloadRunner runner, boolean variadic, Class<?> thisArg, Class<?> args[]) {
        this.runner = runner;
        this.variadic = variadic;
        this.thisArg = thisArg;
        this.params = args;
    }
}