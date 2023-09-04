package me.topchetoeu.jscript.engine.modules;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import me.topchetoeu.jscript.engine.CallContext;

public class ModuleManager {
    private final List<ModuleProvider> providers = new ArrayList<>();
    private final HashMap<String, Module> cache = new HashMap<>();
    public final FileModuleProvider files;

    public void addProvider(ModuleProvider provider) {
        this.providers.add(provider);
    }

    public boolean isCached(File cwd, String name) {
        name = name.replace("\\", "/");

        // Absolute paths are forbidden
        if (name.startsWith("/")) return false;
        // Look for files if we have a relative path
        if (name.startsWith("../") || name.startsWith("./")) {
            var realName = files.getRealName(cwd, name);
            if (cache.containsKey(realName)) return true;
            else return false;
        }

        for (var provider : providers) {
            var realName = provider.getRealName(cwd, name);
            if (realName == null) continue;
            if (cache.containsKey(realName)) return true;
        }

        return false;
    }
    public Module tryLoad(CallContext ctx, String name) throws InterruptedException {
        name = name.replace('\\', '/');

        var pcwd = Path.of(".");

        if (ctx.hasData(Module.KEY)) {
            pcwd = Path.of(((Module)ctx.getData(Module.KEY)).filename).getParent();
            if (pcwd == null) pcwd = Path.of(".");
        }


        var cwd = pcwd.toFile();

        if (name.startsWith("/")) return null;
        if (name.startsWith("../") || name.startsWith("./")) {
            var realName = files.getRealName(cwd, name);
            if (realName == null) return null;
            if (cache.containsKey(realName)) return cache.get(realName);
            var mod = files.getModule(cwd, name);
            cache.put(mod.name(), mod);
            mod.execute(ctx);
            return mod;
        }

        for (var provider : providers) {
            var realName = provider.getRealName(cwd, name);
            if (realName == null) continue;
            if (cache.containsKey(realName)) return cache.get(realName);
            var mod = provider.getModule(cwd, name);
            cache.put(mod.name(), mod);
            mod.execute(ctx);
            return mod;
        }

        return null;
    }

    public ModuleManager(File root) {
        files = new FileModuleProvider(root, false);
    }
}
