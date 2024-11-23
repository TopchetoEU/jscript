package me.topchetoeu.jscript.common.environment;

public final class Key<T> {
    public static <T> Key<T> of() {
        return new Key<>();
    }
}
