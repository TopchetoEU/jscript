package me.topchetoeu.jscript.interop;

import java.lang.reflect.Modifier;
import java.util.HashMap;

import me.topchetoeu.jscript.engine.Environment;
import me.topchetoeu.jscript.engine.WrappersProvider;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.NativeFunction;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.exceptions.EngineException;

public class NativeWrapperProvider implements WrappersProvider {
    private final HashMap<Class<?>, FunctionValue> constructors = new HashMap<>();
    private final HashMap<Class<?>, ObjectValue> prototypes = new HashMap<>();
    private final HashMap<Class<?>, ObjectValue> namespaces = new HashMap<>();
    private final Environment env;

    private static void applyMethods(Environment env, boolean member, ObjectValue target, Class<?> clazz) {
        for (var method : clazz.getDeclaredMethods()) {
            var nat = method.getAnnotation(Native.class);
            var get = method.getAnnotation(NativeGetter.class);
            var set = method.getAnnotation(NativeSetter.class);
            var memberMatch = !Modifier.isStatic(method.getModifiers()) == member;

            if (nat != null) {
                if (nat.thisArg() && !member || !nat.thisArg() && !memberMatch) continue;

                Object name = nat.value();
                if (((String)name).startsWith("@@")) name = env.symbol(((String)name).substring(2));
                else if (name.equals("")) name = method.getName();

                var val = target.values.get(name);

                if (!(val instanceof OverloadFunction)) target.defineProperty(null, name, val = new OverloadFunction(name.toString()));

                ((OverloadFunction)val).add(Overload.fromMethod(method, nat.thisArg()));
            }
            else {
                if (get != null) {
                    if (get.thisArg() && !member || !get.thisArg() && !memberMatch) continue;

                    Object name = get.value();
                    if (((String)name).startsWith("@@")) name = env.symbol(((String)name).substring(2));
                    else if (name.equals("")) name = method.getName();

                    var prop = target.properties.get(name);
                    OverloadFunction getter = null;
                    var setter = prop == null ? null : prop.setter;

                    if (prop != null && prop.getter instanceof OverloadFunction) getter = (OverloadFunction)prop.getter;
                    else getter = new OverloadFunction("get " + name);

                    getter.add(Overload.fromMethod(method, get.thisArg()));
                    target.defineProperty(null, name, getter, setter, true, true);
                }
                if (set != null) {
                    if (set.thisArg() && !member || !set.thisArg() && !memberMatch) continue;

                    Object name = set.value();
                    if (((String)name).startsWith("@@")) name = env.symbol(((String)name).substring(2));
                    else if (name.equals("")) name = method.getName();

                    var prop = target.properties.get(name);
                    var getter = prop == null ? null : prop.getter;
                    OverloadFunction setter = null;

                    if (prop != null && prop.setter instanceof OverloadFunction) setter = (OverloadFunction)prop.setter;
                    else setter = new OverloadFunction("set " + name);

                    setter.add(Overload.fromMethod(method, set.thisArg()));
                    target.defineProperty(null, name, getter, setter, true, true);
                }
            }
        }
    }
    private static void applyFields(Environment env, boolean member, ObjectValue target, Class<?> clazz) {
        for (var field : clazz.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) != member) continue;
            var nat = field.getAnnotation(Native.class);

            if (nat != null) {
                Object name = nat.value();
                if (((String)name).startsWith("@@")) name = env.symbol(((String)name).substring(2));
                else if (name.equals("")) name = field.getName();
    
                var getter = OverloadFunction.of("get " + name, Overload.getterFromField(field));
                var setter = OverloadFunction.of("set " + name, Overload.setterFromField(field));
                target.defineProperty(null, name, getter, setter, true, false);
            }
        }
    }
    private static void applyClasses(Environment env, boolean member, ObjectValue target, Class<?> clazz) {
        for (var cl : clazz.getDeclaredClasses()) {
            if (!Modifier.isStatic(cl.getModifiers()) != member) continue;
            var nat = cl.getAnnotation(Native.class);

            if (nat != null) {
                Object name = nat.value();
                if (((String)name).startsWith("@@")) name = env.symbol(((String)name).substring(2));
                else if (name.equals("")) name = cl.getName();

                var getter = new NativeFunction("get " + name, (ctx, thisArg, args) -> cl);

                target.defineProperty(null, name, getter, null, true, false);
            }
        }
    }

    /**
     * Generates a prototype for the given class.
     * The returned object will have appropriate wrappers for all instance members.
     * All accessors and methods will expect the this argument to be a native wrapper of the given class type.
     * @param clazz The class for which a prototype should be generated
     */
    public static ObjectValue makeProto(Environment ctx, Class<?> clazz) {
        var res = new ObjectValue();

        for (var overload : clazz.getDeclaredMethods()) {
            var init = overload.getAnnotation(NativeInit.class);
            if (init == null || init.value() != InitType.PROTOTYPE) continue;
            try { overload.invoke(null, ctx, res); }
            catch (ReflectiveOperationException e) { e.printStackTrace(); }
        }

        applyMethods(ctx, true, res, clazz);
        applyFields(ctx, true, res, clazz);
        applyClasses(ctx, true, res, clazz);

        return res;
    }
    /**
     * Generates a constructor for the given class.
     * The returned function will have appropriate wrappers for all static members.
     * When the function gets called, the underlying constructor will get called, unless the constructor is inaccessible.
     * @param clazz The class for which a constructor should be generated
     */
    public static FunctionValue makeConstructor(Environment ctx, Class<?> clazz) {
        FunctionValue func = new OverloadFunction(clazz.getName());

        for (var overload : clazz.getDeclaredConstructors()) {
            var nat = overload.getAnnotation(Native.class);
            if (nat == null) continue;
            ((OverloadFunction)func).add(Overload.fromConstructor(overload, nat.thisArg()));
        }
        for (var overload : clazz.getDeclaredMethods()) {
            var constr = overload.getAnnotation(NativeConstructor.class);
            if (constr == null) continue;
            ((OverloadFunction)func).add(Overload.fromMethod(overload, constr.thisArg()));
        }
        for (var overload : clazz.getDeclaredMethods()) {
            var init = overload.getAnnotation(NativeInit.class);
            if (init == null || init.value() != InitType.CONSTRUCTOR) continue;
            try { overload.invoke(null, ctx, func); }
            catch (ReflectiveOperationException e) { e.printStackTrace(); }
        }

        if (((OverloadFunction)func).overloads.size() == 0) {
            func = new NativeFunction(clazz.getName(), (a, b, c) -> { throw EngineException.ofError("This constructor is not invokable."); });
        }

        applyMethods(ctx, false, func, clazz);
        applyFields(ctx, false, func, clazz);
        applyClasses(ctx, false, func, clazz);

        func.special = true;

        return func;
    }
    /**
     * Generates a namespace for the given class.
     * The returned function will have appropriate wrappers for all static members.
     * This method behaves almost like {@link NativeWrapperProvider#makeConstructor}, but will return an object instead.
     * @param clazz The class for which a constructor should be generated
     */
    public static ObjectValue makeNamespace(Environment ctx, Class<?> clazz) {
        ObjectValue res = new ObjectValue();

        for (var overload : clazz.getDeclaredMethods()) {
            var init = overload.getAnnotation(NativeInit.class);
            if (init == null || init.value() != InitType.NAMESPACE) continue;
            try { overload.invoke(null, ctx, res); }
            catch (ReflectiveOperationException e) { e.printStackTrace(); }
        }

        applyMethods(ctx, false, res, clazz);
        applyFields(ctx, false, res, clazz);
        applyClasses(ctx, false, res, clazz);

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

        if (constr == null) constr = makeConstructor(env, clazz);
        if (proto == null) proto = makeProto(env, clazz);

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
    public ObjectValue getNamespace(Class<?> clazz) {
        if (!namespaces.containsKey(clazz)) namespaces.put(clazz, makeNamespace(env, clazz));
        return namespaces.get(clazz);
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

    public NativeWrapperProvider(Environment env) {
        this.env = env;
    }
}
