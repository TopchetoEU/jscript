package me.topchetoeu.jscript.utils.modules;

import java.util.HashMap;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.runtime.Context;
import me.topchetoeu.jscript.runtime.Extensions;
import me.topchetoeu.jscript.runtime.Key;
import me.topchetoeu.jscript.runtime.scope.GlobalScope;
import me.topchetoeu.jscript.utils.filesystem.Filesystem;
import me.topchetoeu.jscript.utils.filesystem.Mode;

public interface ModuleRepo {
    public static final Key<ModuleRepo> KEY = new Key<>();
    public static final Key<String> CWD = new Key<>();

    public Module getModule(Context ctx, String cwd, String name);

    public static ModuleRepo ofFilesystem(Filesystem fs) {
        var modules = new HashMap<String, Module>();

        return (ctx, cwd, name) -> {
            name = fs.normalize(cwd, name);
            var filename = Filename.parse(name);
            var src = fs.open(name, Mode.READ).readToString();

            if (modules.containsKey(name)) return modules.get(name);

            var env = Context.clean(ctx.extensions).child();
            env.add(CWD, fs.normalize(name, ".."));
            var glob = env.get(GlobalScope.KEY);
            env.add(GlobalScope.KEY, glob.child());

            var mod = new SourceModule(filename, src, env);
            modules.put(name, mod);

            return mod;
        };
    }

    public static String cwd(Extensions exts) {
        return exts.init(CWD, "/");
    }
    public static ModuleRepo get(Extensions exts) {
        return exts.get(KEY);
    }
}
