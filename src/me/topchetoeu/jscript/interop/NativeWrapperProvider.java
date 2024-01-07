package me.topchetoeu.jscript.interop;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.Environment;
import me.topchetoeu.jscript.engine.WrappersProvider;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.NativeFunction;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.Symbol;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.exceptions.EngineException;
import me.topchetoeu.jscript.exceptions.InterruptException;

public class NativeWrapperProvider implements WrappersProvider {
    private final HashMap<Class<?>, FunctionValue> constructors = new HashMap<>();
    private final HashMap<Class<?>, ObjectValue> prototypes = new HashMap<>();
    private final HashMap<Class<?>, ObjectValue> namespaces = new HashMap<>();
    private final Environment env;

    private static Object call(Context ctx, String name, Method method, Object thisArg, Object... args) {
        try {
            var realArgs = new Object[method.getParameterCount()];
            System.arraycopy(args, 0, realArgs, 0, realArgs.length);
            if (Modifier.isStatic(method.getModifiers())) thisArg = null;
            return Values.normalize(ctx, method.invoke(Values.convert(ctx, thisArg, method.getDeclaringClass()), realArgs));
        }
        catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof EngineException) {
                throw ((EngineException)e.getTargetException()).add(ctx, name, Location.INTERNAL);
            }
            else if (e.getTargetException() instanceof NullPointerException) {
                e.printStackTrace();
                throw EngineException.ofType("Unexpected value of 'undefined'.").add(ctx, name, Location.INTERNAL);
            }
            else if (e.getTargetException() instanceof InterruptException || e.getTargetException() instanceof InterruptedException) {
                throw new InterruptException();
            }
            else throw new EngineException(e.getTargetException()).add(ctx, name, Location.INTERNAL);
        }
        catch (ReflectiveOperationException e) {
            throw EngineException.ofError(e.getMessage()).add(ctx, name, Location.INTERNAL);
        }
    }
    private static FunctionValue create(String name, Method method) {
        return new NativeFunction(name, args -> call(args.ctx, name, method, args.self, args));
    }
    private static void checkSignature(Method method, boolean forceStatic, Class<?> ...params) {
        if (forceStatic && !Modifier.isStatic(method.getModifiers())) throw new IllegalArgumentException(String.format(
            "Method %s must be static.",
            method.getDeclaringClass().getName() + "." + method.getName()
        ));

        var actual = method.getParameterTypes();

        boolean failed = actual.length > params.length;

        if (!failed) for (var i = 0; i < actual.length; i++) {
            if (!actual[i].isAssignableFrom(params[i])) {
                failed = true;
                break;
            }
        }

        if (failed) throw new IllegalArgumentException(String.format(
            "Method %s was expected to have a signature of '%s', found '%s' instead.",
            method.getDeclaringClass().getName() + "." + method.getName(),
            String.join(", ", Arrays.stream(params).map(v -> v.getName()).collect(Collectors.toList())),
            String.join(", ", Arrays.stream(actual).map(v -> v.getName()).collect(Collectors.toList()))
        ));
    }
    private static String getName(Class<?> clazz) {
        var classNat = clazz.getAnnotation(WrapperName.class);
        if (classNat != null && !classNat.value().trim().equals("")) return classNat.value().trim();
        else return clazz.getSimpleName();
    }

    private static void checkUnderscore(Member member) {
        if (!member.getName().startsWith("__")) {
            System.out.println(String.format("WARNING: The name of the exposed member '%s.%s' doesn't start with '__'.",
                member.getDeclaringClass().getName(),
                member.getName()
            ));
        }
    }
    private static String getName(Member member, String overrideName) {
        if (overrideName == null) overrideName = "";
        if (overrideName.isBlank()) {
            var res = member.getName();
            if (res.startsWith("__")) res = res.substring(2);
            return res;
        }
        else return overrideName.trim();
    }
    private static Object getKey(String name) {
        if (name.startsWith("@@")) return Symbol.get(name.substring(2));
        else return name;
    }

    private static void apply(ObjectValue obj, Environment env, ExposeTarget target, Class<?> clazz) {
        var getters = new HashMap<Object, FunctionValue>();
        var setters = new HashMap<Object, FunctionValue>();
        var props = new HashSet<Object>();
        var nonProps = new HashSet<Object>();

        for (var method : clazz.getDeclaredMethods()) {
            for (var annotation : method.getAnnotationsByType(Expose.class)) {
                if (!annotation.target().shouldApply(target)) continue;

                checkUnderscore(method);
                var name = getName(method, annotation.value());
                var key = getKey(name);
                var repeat = false;

                switch (annotation.type()) {
                    case INIT:
                        checkSignature(method, true,
                            target == ExposeTarget.CONSTRUCTOR ? FunctionValue.class : ObjectValue.class,
                            Environment.class
                        );
                        call(null, null, method, obj, null, env);
                        break;
                    case METHOD:
                        if (props.contains(key) || nonProps.contains(key)) repeat = true;
                        else {
                            checkSignature(method, false, Arguments.class);
                            obj.defineProperty(null, key, create(name, method), true, true, false);
                            nonProps.add(key);
                        }
                        break;
                    case GETTER:
                        if (nonProps.contains(key) || getters.containsKey(key)) repeat = true;
                        else {
                            checkSignature(method, false, Arguments.class);
                            getters.put(key, create(name, method));
                            props.add(key);
                        }
                        break;
                    case SETTER:
                        if (nonProps.contains(key) || setters.containsKey(key)) repeat = true;
                        else {
                            checkSignature(method, false, Arguments.class);
                            setters.put(key, create(name, method));
                            props.add(key);
                        }
                        break;
                }

                if (repeat)
                    throw new IllegalArgumentException(String.format(
                    "A member '%s' in the wrapper for '%s' of type '%s' is already present.",
                    name, clazz.getName(), target.toString()
                ));
            }
            for (var annotation : method.getAnnotationsByType(ExposeField.class)) {
                if (!annotation.target().shouldApply(target)) continue;

                checkUnderscore(method);
                var name = getName(method, annotation.value());
                var key = getKey(name);
                var repeat = false;

                if (props.contains(key) || nonProps.contains(key)) repeat = true;
                else {
                    checkSignature(method, true, Environment.class);
                    obj.defineProperty(null, key, call(new Context(null, env), name, method, null, env), true, true, false);
                    nonProps.add(key);
                }

                if (repeat)
                    throw new IllegalArgumentException(String.format(
                    "A member '%s' in the wrapper for '%s' of type '%s' is already present.",
                    name, clazz.getName(), target.toString()
                ));
            }
        }
        for (var field : clazz.getDeclaredFields()) {
            for (var annotation : field.getAnnotationsByType(ExposeField.class)) {
                if (!annotation.target().shouldApply(target)) continue;

                checkUnderscore(field);
                var name = getName(field, annotation.value());
                var key = getKey(name);
                var repeat = false;

                if (props.contains(key) || nonProps.contains(key)) repeat = true;
                else {
                    try {
                        obj.defineProperty(null, key, Values.normalize(new Context(null, env), field.get(null)), true, true, false);
                        nonProps.add(key);
                    }
                    catch (IllegalArgumentException | IllegalAccessException e) { }
                }

                if (repeat)
                    throw new IllegalArgumentException(String.format(
                    "A member '%s' in the wrapper for '%s' of type '%s' is already present.",
                    name, clazz.getName(), target.toString()
                ));
            }
        }

        for (var key : props) obj.defineProperty(null, key, getters.get(key), setters.get(key), true, false);
    }

    private static Method getConstructor(Environment env, Class<?> clazz) {
        for (var method : clazz.getDeclaredMethods()) {
            if (!method.isAnnotationPresent(ExposeConstructor.class)) continue;
            checkSignature(method, true, Arguments.class);
            return method;
        }

        return null;
    }

    /**
     * Generates a prototype for the given class.
     * The returned object will have appropriate wrappers for all instance members.
     * All accessors and methods will expect the this argument to be a native wrapper of the given class type.
     * @param clazz The class for which a prototype should be generated
     */
    public static ObjectValue makeProto(Environment env, Class<?> clazz) {
        var res = new ObjectValue();
        res.defineProperty(null, Symbol.get("Symbol.typeName"), getName(clazz));
        apply(res, env, ExposeTarget.PROTOTYPE, clazz);
        return res;
    }
    /**
     * Generates a constructor for the given class.
     * The returned function will have appropriate wrappers for all static members.
     * When the function gets called, the underlying constructor will get called, unless the constructor is inaccessible.
     * @param clazz The class for which a constructor should be generated
     */
    public static FunctionValue makeConstructor(Environment ctx, Class<?> clazz) {
        var constr = getConstructor(ctx, clazz);

        FunctionValue res = constr == null ?
            new NativeFunction(getName(clazz), args -> { throw EngineException.ofError("This constructor is not invokable."); }) :
            create(getName(clazz), constr);

        res.special = true;

        apply(res, ctx, ExposeTarget.CONSTRUCTOR, clazz);

        return res;
    }
    /**
     * Generates a namespace for the given class.
     * The returned function will have appropriate wrappers for all static members.
     * This method behaves almost like {@link NativeWrapperProvider#makeConstructor}, but will return an object instead.
     * @param clazz The class for which a constructor should be generated
     */
    public static ObjectValue makeNamespace(Environment ctx, Class<?> clazz) {
        var res = new ObjectValue();
        res.defineProperty(null, Symbol.get("Symbol.typeName"), getName(clazz));
        apply(res, ctx, ExposeTarget.NAMESPACE, clazz);
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

        proto.defineProperty(null, "constructor", constr, true, false, false);
        constr.defineProperty(null, "prototype", proto, true, false, false);

        prototypes.put(clazz, proto);
        constructors.put(clazz, constr);

        var parent = clazz.getSuperclass();
        if (parent == null) return;

        var parentProto = getProto(parent);
        var parentConstr = getConstr(parent);

        if (parentProto != null) Values.setPrototype(Context.NULL, proto, parentProto);
        if (parentConstr != null) Values.setPrototype(Context.NULL, constr, parentConstr);
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

    @Override public WrappersProvider fork(Environment env) {
        return new NativeWrapperProvider(env);
    }

    public void setProto(Class<?> clazz, ObjectValue value) {
        prototypes.put(clazz, value);
    }
    public void setConstr(Class<?> clazz, FunctionValue value) {
        constructors.put(clazz, value);
    }

    private void initError() {
        var proto = new ObjectValue();
        proto.defineProperty(null, "message", new NativeFunction("message", args -> {
            if (args.self instanceof Throwable) return ((Throwable)args.self).getMessage();
            else return null;
        }));
        proto.defineProperty(null, "name", new NativeFunction("name", args -> getName(args.self.getClass())));
        proto.defineProperty(null, "toString", new NativeFunction("toString", args -> args.self.toString()));

        var constr = makeConstructor(null, Throwable.class);
        proto.defineProperty(null, "constructor", constr, true, false, false);
        constr.defineProperty(null, "prototype", proto, true, false, false);

        setProto(Throwable.class, proto);
        setConstr(Throwable.class, constr);
    }

    public NativeWrapperProvider(Environment env) {
        this.env = env;
        initError();
    }
}
