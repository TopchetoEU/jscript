package me.topchetoeu.jscript.runtime;

import java.util.HashMap;

import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.values.FunctionValue;
import me.topchetoeu.jscript.runtime.values.NativeFunction;
import me.topchetoeu.jscript.runtime.values.ObjectValue;

@SuppressWarnings("unchecked")
public class Environment implements Extensions {
    public static final Key<Compiler> COMPILE_FUNC = new Key<>();

    public static final Key<FunctionValue> REGEX_CONSTR = new Key<>();
    public static final Key<Integer> MAX_STACK_COUNT = new Key<>();
    public static final Key<Boolean> HIDE_STACK = new Key<>();

    public static final Key<ObjectValue> OBJECT_PROTO = new Key<>();
    public static final Key<ObjectValue> FUNCTION_PROTO = new Key<>();
    public static final Key<ObjectValue> ARRAY_PROTO = new Key<>();
    public static final Key<ObjectValue> BOOL_PROTO = new Key<>();
    public static final Key<ObjectValue> NUMBER_PROTO = new Key<>();
    public static final Key<ObjectValue> STRING_PROTO = new Key<>();
    public static final Key<ObjectValue> SYMBOL_PROTO = new Key<>();
    public static final Key<ObjectValue> ERROR_PROTO = new Key<>();
    public static final Key<ObjectValue> SYNTAX_ERR_PROTO = new Key<>();
    public static final Key<ObjectValue> TYPE_ERR_PROTO = new Key<>();
    public static final Key<ObjectValue> RANGE_ERR_PROTO = new Key<>();

    private HashMap<Key<?>, Object> data = new HashMap<>();

    @Override public <T> void add(Key<T> key, T obj) {
        data.put(key, obj);
    }
    @Override public <T> T get(Key<T> key) {
        return (T)data.get(key);
    }
    @Override public boolean remove(Key<?> key) {
        if (data.containsKey(key)) {
            data.remove(key);
            return true;
        }
        return false;
    }
    @Override public boolean has(Key<?> key) {
        return data.containsKey(key);
    }
    @Override public Iterable<Key<?>> keys() {
        return data.keySet();
    }

    public static FunctionValue regexConstructor(Extensions ext) {
        return ext.init(REGEX_CONSTR, new NativeFunction("RegExp", args -> {
            throw EngineException.ofError("Regular expressions not supported.").setExtensions(args.ctx);
        }));
    }

    public Context context() {
        return new Context(this);
    }
}
