package me.topchetoeu.jscript.engine.values;

import me.topchetoeu.jscript.engine.Context;

public class NativeWrapper extends ObjectValue {
    private static final Object NATIVE_PROTO = new Object();
    public final Object wrapped;

    @Override
    public ObjectValue getPrototype(Context ctx) throws InterruptedException {
        if (prototype == NATIVE_PROTO) return ctx.function.wrappersProvider.getProto(wrapped.getClass());
        else return super.getPrototype(ctx);
    }

    public NativeWrapper(Object wrapped) {
        this.wrapped = wrapped;
        prototype = NATIVE_PROTO;
    }
}
