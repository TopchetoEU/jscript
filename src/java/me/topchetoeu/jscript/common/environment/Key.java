package me.topchetoeu.jscript.common.environment;

public interface Key<T> {
    public static <T> Key<T> of() {
        return new Key<>() { };
    }
}
