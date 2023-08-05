package me.topchetoeu.jscript.interop;

import java.lang.reflect.Modifier;
import java.util.HashMap;

import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.NativeFunction;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.exceptions.EngineException;

public class NativeTypeRegister {
    private final HashMap<Class<?>, FunctionValue> constructors = new HashMap<>();
    private final HashMap<Class<?>, ObjectValue> prototypes = new HashMap<>();

    private static void applyMethods(boolean member, ObjectValue target, Class<?> clazz) {
        for (var method : clazz.getDeclaredMethods()) {
            if (!Modifier.isStatic(method.getModifiers()) != member) continue;

            var nat = method.getAnnotation(Native.class);
            var get = method.getAnnotation(NativeGetter.class);
            var set = method.getAnnotation(NativeSetter.class);

            if (nat != null) {
                var name = nat.value();
                var val = target.values.get(name);

                if (name.equals("")) name = method.getName();
                if (!(val instanceof OverloadFunction)) target.defineProperty(name, val = new OverloadFunction(name));

                ((OverloadFunction)val).overloads.add(Overload.fromMethod(method));
            }
            else {
                if (get != null) {
                    var name = get.value();
                    var prop = target.properties.get(name);
                    OverloadFunction getter = null;
                    var setter = prop == null ? null : prop.setter();

                    if (prop != null && prop.getter() instanceof OverloadFunction) getter = (OverloadFunction)prop.getter();
                    else getter = new OverloadFunction("get " + name);

                    getter.overloads.add(Overload.fromMethod(method));
                    target.defineProperty(name, getter, setter, true, true);
                }
                if (set != null) {
                    var name = set.value();
                    var prop = target.properties.get(name);
                    var getter = prop == null ? null : prop.getter();
                    OverloadFunction setter = null;

                    if (prop != null && prop.setter() instanceof OverloadFunction) setter = (OverloadFunction)prop.setter();
                    else setter = new OverloadFunction("set " + name);

                    setter.overloads.add(Overload.fromMethod(method));
                    target.defineProperty(name, getter, setter, true, true);
                }
            }
        }
    }
    private static void applyFields(boolean member, ObjectValue target, Class<?> clazz) {
        for (var field : clazz.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) != member) continue;
            var nat = field.getAnnotation(Native.class);

            if (nat != null) {
                var name = nat.value();
                if (name.equals("")) name = field.getName();
                var getter = new OverloadFunction("get " + name).add(Overload.getterFromField(field));
                var setter = new OverloadFunction("set " + name).add(Overload.setterFromField(field));
                target.defineProperty(name, getter, setter, true, false);
            }
        }
    }

    public static ObjectValue makeProto(Class<?> clazz) {
        var res = new ObjectValue();

        applyMethods(true, res, clazz);
        applyFields(true, res, clazz);

        return res;
    }
    public static FunctionValue makeConstructor(Class<?> clazz) {
        FunctionValue func = new OverloadFunction(clazz.getName());

        for (var overload : clazz.getConstructors()) {
            if (overload.getAnnotation(Native.class) == null) continue;
            ((OverloadFunction)func).add(Overload.fromConstructor(overload));
        }

        if (((OverloadFunction)func).overloads.size() == 0) {
            func = new NativeFunction(clazz.getName(), (a, b, c) -> { throw EngineException.ofError("This constructor is not invokable."); });
        }

        applyMethods(false, func, clazz);
        applyFields(false, func, clazz);

        func.special = true;

        return func;
    }
    public static ObjectValue makeNamespace(Class<?> clazz) {
        ObjectValue res = new ObjectValue();

        applyMethods(false, res, clazz);
        applyFields(false, res, clazz);

        return res;
    }

    private void initType(Class<?> clazz, FunctionValue constr, ObjectValue proto) {
        if (constr != null && proto != null) return;
        // i vomit
        if (
            clazz == Object.class ||
            clazz == Void.class ||
            clazz == Number.class || clazz == Double.class || clazz == Float.class ||
            clazz == Long.class || clazz == Integer.class || clazz == Short.class ||
            clazz == Character.class || clazz == Byte.class || clazz == Boolean.class ||
            clazz.isPrimitive() ||
            clazz.isArray() ||
            clazz.isAnonymousClass() ||
            clazz.isEnum() ||
            clazz.isInterface() ||
            clazz.isSynthetic()
        ) return;

        if (constr == null) constr = makeConstructor(clazz);
        if (proto == null) proto = makeProto(clazz);

        proto.values.put("constructor", constr);
        constr.values.put("prototype", proto);

        prototypes.put(clazz, proto);
        constructors.put(clazz, constr);

        var parent = clazz.getSuperclass();
        if (parent == null) return;

        var parentProto = getProto(parent);
        var parentConstr = getConstr(parent);

        if (parentProto != null) proto.setPrototype(null, parentProto);
        if (parentConstr != null) constr.setPrototype(null, parentConstr);
    }

    public ObjectValue getProto(Class<?> clazz) {
        initType(clazz, constructors.get(clazz), prototypes.get(clazz));
        return prototypes.get(clazz);
    }
    public FunctionValue getConstr(Class<?> clazz) {
        initType(clazz, constructors.get(clazz), prototypes.get(clazz));
        return constructors.get(clazz);
    }

    public void setProto(Class<?> clazz, ObjectValue value) {
        prototypes.put(clazz, value);
    }
    public void setConstr(Class<?> clazz, FunctionValue value) {
        constructors.put(clazz, value);
    }
}
