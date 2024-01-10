package me.topchetoeu.jscript.core.engine.scope;

public interface ScopeRecord {
    public Object getKey(String name);
    public Object define(String name);
    public LocalScopeRecord child();
}
