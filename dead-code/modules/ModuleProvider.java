package me.topchetoeu.jscript.engine.modules;

import java.io.File;

public interface ModuleProvider {
    Module getModule(File cwd, String name);
    String getRealName(File cwd, String name);
    default boolean hasModule(File cwd, String name) { return getRealName(cwd, name) != null; }
}