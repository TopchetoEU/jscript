package me.topchetoeu.jscript.runtime.values;

import me.topchetoeu.jscript.runtime.Context;

public class NativeWrapper extends ObjectValue {
    private static final Object NATIVE_PROTO = new Object();
    public final Object wrapped;

    @Override
    public ObjectValue getPrototype(Context ctx) {
        if (ctx.environment != null && prototype == NATIVE_PROTO) return ctx.environment.wrappers.getProto(wrapped.getClass());
        else return super.getPrototype(ctx);
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

    public NativeWrapper(Object wrapped) {
        this.wrapped = wrapped;
        prototype = NATIVE_PROTO;
    }
}
