package me.topchetoeu.jscript.common;

import me.topchetoeu.jscript.core.scope.LocalScopeRecord;

public interface ScopeRecord {
    public Object getKey(String name);
    public Object define(String name);
    public LocalScopeRecord child();
}
