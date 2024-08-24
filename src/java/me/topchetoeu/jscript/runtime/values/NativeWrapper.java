package me.topchetoeu.jscript.runtime.values;

import java.util.WeakHashMap;

import me.topchetoeu.jscript.runtime.environment.Environment;
import me.topchetoeu.jscript.runtime.environment.Key;
import me.topchetoeu.jscript.utils.interop.NativeWrapperProvider;

public class NativeWrapper extends ObjectValue {
    private static class MapKey {
        public final Object key;

        @Override
        public int hashCode() {
            return System.identityHashCode(key);
        }
        @Override
        public boolean equals(Object obj) {
            if (this == null || obj == null) return this == null && obj == null;
            if (!(obj instanceof MapKey)) return false;
            var other = (MapKey)obj;

            return other.key == key;
        }

        public MapKey(Object key) {
            this.key = key;
        }
    }

    private static final Key<WeakHashMap<MapKey, NativeWrapper>> WRAPPER_MAP = new Key<>();
    private static final Object NATIVE_PROTO = new Object();
    public final Object wrapped;

    @Override
    public ObjectValue getPrototype(Environment ext) {
        if (ext != null && prototype == NATIVE_PROTO) {
            var clazz = wrapped.getClass();
            var res = NativeWrapperProvider.get(ext).getProto(clazz);
            if (res != null) return res;
        }
        return super.getPrototype(ext);
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

    public static NativeWrapper of(Environment env, Object wrapped) {
        if (env == null) return new NativeWrapper(wrapped);

        var wrappers = env.get(WRAPPER_MAP);
        if (wrappers == null) return new NativeWrapper(wrapped);

        var key = new MapKey(wrapped);
        if (wrappers.containsKey(key)) return wrappers.get(key);

        var res = new NativeWrapper(wrapped);
        wrappers.put(key, res);

        return res;
    }
}
