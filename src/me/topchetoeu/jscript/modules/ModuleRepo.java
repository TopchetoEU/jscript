package me.topchetoeu.jscript.modules;

import java.util.HashMap;

import me.topchetoeu.jscript.Filename;
import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.filesystem.Filesystem;
import me.topchetoeu.jscript.filesystem.Mode;

public interface ModuleRepo {
    public Module getModule(Context ctx, String cwd, String name);

    public static ModuleRepo ofFilesystem(Filesystem fs) {
        var modules = new HashMap<String, Module>();

        return (ctx, cwd, name) -> {
            name = fs.normalize(cwd, name);
            var filename = Filename.parse(name);
            var src = fs.open(name, Mode.READ).readToString();

            if (modules.containsKey(name)) return modules.get(name);

            var env = ctx.environment().child();
            env.moduleCwd = fs.normalize(name, "..");

            var mod = new SourceModule(filename, src, env);
            modules.put(name, mod);

            return mod;
        };
    }
}
