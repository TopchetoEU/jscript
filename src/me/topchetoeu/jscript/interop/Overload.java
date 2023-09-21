package me.topchetoeu.jscript.interop;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import me.topchetoeu.jscript.engine.Context;

public class Overload {
    public static interface OverloadRunner {
        Object run(Context ctx, Object thisArg, Object[] args) throws
            InterruptedException,
            ReflectiveOperationException,
            IllegalArgumentException;
    }

    public final OverloadRunner runner;
    public final boolean variadic;
    public final boolean passThis;
    public final Class<?> thisArg;
    public final Class<?>[] params;

    public static Overload fromMethod(Method method, boolean passThis) {
        return new Overload(
            (ctx, th, args) -> method.invoke(th, args),
            method.isVarArgs(), passThis,
            Modifier.isStatic(method.getModifiers()) ? null : method.getDeclaringClass(),
            method.getParameterTypes()
        );
    }
    public static Overload fromConstructor(Constructor<?> method, boolean passThis) {
        return new Overload(
            (ctx, th, args) -> method.newInstance(args),
            method.isVarArgs(), passThis,
            Modifier.isStatic(method.getModifiers()) ? null : method.getDeclaringClass(),
            method.getParameterTypes()
        );
    }
    public static Overload getterFromField(Field field) {
        return new Overload(
            (ctx, th, args) -> field.get(th), false, false,
            Modifier.isStatic(field.getModifiers()) ? null : field.getDeclaringClass(),
            new Class[0]
        );
    }
    public static Overload setterFromField(Field field) {
        if (Modifier.isFinal(field.getModifiers())) return null;
        return new Overload(
            (ctx, th, args) -> { field.set(th, args[0]); return null; }, false, false,
            Modifier.isStatic(field.getModifiers()) ? null : field.getDeclaringClass(),
            new Class[0]
        );
    }

    public static Overload getter(Class<?> thisArg, OverloadRunner runner, boolean passThis) {
        return new Overload(
            (ctx, th, args) -> runner.run(ctx, th, args), false, passThis,
            thisArg,
            new Class[0]
        );
    }

    public Overload(OverloadRunner runner, boolean variadic, boolean passThis, Class<?> thisArg, Class<?> args[]) {
        this.runner = runner;
        this.variadic = variadic;
        this.passThis = passThis;
        this.thisArg = thisArg;
        this.params = args;
    }
}