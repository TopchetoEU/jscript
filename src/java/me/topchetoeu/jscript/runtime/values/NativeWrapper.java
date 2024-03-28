package me.topchetoeu.jscript.runtime.values;

import java.util.WeakHashMap;

import me.topchetoeu.jscript.runtime.Context;
import me.topchetoeu.jscript.runtime.Extensions;
import me.topchetoeu.jscript.runtime.Key;

public class NativeWrapper extends ObjectValue {
    private static final Key<WeakHashMap<Object, NativeWrapper>> WRAPPERS = new Key<>();
    private static final Object NATIVE_PROTO = new Object();
    public final Object wrapped;

    @Override
    public ObjectValue getPrototype(Context ctx) {
        if (ctx.environment != null && prototype == NATIVE_PROTO) {
            var clazz = wrapped.getClass();
            var res = ctx.environment.wrappers.getProto(clazz);
            if (res != null) return res;
        }
        return super.getPrototype(ctx);
    }

    @Override
    public String toString() {
        return wrapped.toString();
    }
    @Override
    public boolean equals(Object obj) {
        return wrapped.equals(obj);
    }
    @Override
    public int hashCode() {
        return wrapped.hashCode();
    }

    private NativeWrapper(Object wrapped) {
        this.wrapped = wrapped;
        prototype = NATIVE_PROTO;
    }

    public static NativeWrapper of(Extensions exts, Object wrapped) {
        var wrappers = exts == null ? null : exts.get(WRAPPERS);

        if (wrappers == null) return new NativeWrapper(wrapped);
        if (wrappers.containsKey(wrapped)) return wrappers.get(wrapped);

        var res = new NativeWrapper(wrapped);
        wrappers.put(wrapped, res);

        return res;
    }
}
