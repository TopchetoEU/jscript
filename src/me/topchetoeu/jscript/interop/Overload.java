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
    public final boolean raw;
    public final Class<?> thisArg;
    public final Class<?>[] params;

    public static Overload fromMethod(Method method, boolean raw) {
        return new Overload(
            (ctx, th, args) -> method.invoke(th, args),
            method.isVarArgs(), raw,
            Modifier.isStatic(method.getModifiers()) ? null : method.getDeclaringClass(),
            method.getParameterTypes()
        );
    }
    public static Overload fromConstructor(Constructor<?> method, boolean raw) {
        return new Overload(
            (ctx, th, args) -> method.newInstance(args),
            method.isVarArgs(), raw,
            Modifier.isStatic(method.getModifiers()) ? null : method.getDeclaringClass(),
            method.getParameterTypes()
        );
    }
    public static Overload getterFromField(Field field, boolean raw) {
        return new Overload(
            (ctx, th, args) -> field.get(th), false, raw,
            Modifier.isStatic(field.getModifiers()) ? null : field.getDeclaringClass(),
            new Class[0]
        );
    }
    public static Overload setterFromField(Field field, boolean raw) {
        if (Modifier.isFinal(field.getModifiers())) return null;
        return new Overload(
            (ctx, th, args) -> { field.set(th, args[0]); return null; }, false, raw,
            Modifier.isStatic(field.getModifiers()) ? null : field.getDeclaringClass(),
            new Class[0]
        );
    }

    public static Overload getter(Class<?> thisArg, OverloadRunner runner, boolean raw) {
        return new Overload(
            (ctx, th, args) -> runner.run(ctx, th, args), false, raw,
            thisArg,
            new Class[0]
        );
    }

    public Overload(OverloadRunner runner, boolean variadic, boolean raw, Class<?> thisArg, Class<?> args[]) {
        this.runner = runner;
        this.variadic = variadic;
        this.raw = raw;
        this.thisArg = thisArg;
        this.params = args;

        if (raw) {
            if (!(
                thisArg == null && (
                args.length == 3 && args[0] == Context.class && args[1] == Object.class && args[2] == Object[].class ||
                args.length == 2 && args[0] == Context.class && args[1] == Object[].class
            ))) throw new IllegalArgumentException("Invalid signature for raw method.");
        }
    }
}