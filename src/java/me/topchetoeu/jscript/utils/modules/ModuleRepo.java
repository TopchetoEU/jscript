package me.topchetoeu.jscript.utils.modules;

import java.util.HashMap;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.core.engine.Context;
import me.topchetoeu.jscript.core.engine.Extensions;
import me.topchetoeu.jscript.core.engine.values.Symbol;
import me.topchetoeu.jscript.utils.filesystem.Filesystem;
import me.topchetoeu.jscript.utils.filesystem.Mode;

public interface ModuleRepo {
    public static final Symbol ENV_KEY = Symbol.get("Environment.modules");
    public static final Symbol CWD = Symbol.get("Environment.moduleCwd");

    public Module getModule(Context ctx, String cwd, String name);

    public static ModuleRepo ofFilesystem(Filesystem fs) {
        var modules = new HashMap<String, Module>();

        return (ctx, cwd, name) -> {
            name = fs.normalize(cwd, name);
            var filename = Filename.parse(name);
            var src = fs.open(name, Mode.READ).readToString();

            if (modules.containsKey(name)) return modules.get(name);

            var env = ctx.environment.child();
            env.add(CWD, fs.normalize(name, ".."));

            var mod = new SourceModule(filename, src, env);
            modules.put(name, mod);

            return mod;
        };
    }

    public static String cwd(Extensions exts) {
        return exts.init(CWD, "/");
    }
    public static ModuleRepo get(Extensions exts) {
        return exts.get(ENV_KEY);
    }
}
