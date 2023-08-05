package me.topchetoeu.jscript.events;

public interface Observer<T> {
    public void next(T data);
    public default void error(RuntimeException err) {}
    public default void finish() { }
}