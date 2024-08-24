package me.topchetoeu.jscript.utils.modules;

import java.util.HashMap;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.runtime.environment.Environment;
import me.topchetoeu.jscript.runtime.environment.Key;
import me.topchetoeu.jscript.runtime.scope.GlobalScope;
import me.topchetoeu.jscript.utils.filesystem.Filesystem;
import me.topchetoeu.jscript.utils.filesystem.Mode;

public interface ModuleRepo {
    public static final Key<ModuleRepo> KEY = new Key<>();
    public static final Key<String> CWD = new Key<>();

    public Module getModule(Environment ctx, String cwd, String name);

    public static ModuleRepo ofFilesystem(Filesystem fs) {
        var modules = new HashMap<String, Module>();

        return (env, cwd, name) -> {
            name = fs.normalize(cwd, name);
            var filename = Filename.parse(name);
            var src = fs.open(name, Mode.READ).readToString();

            if (modules.containsKey(name)) return modules.get(name);

            var moduleEnv = env.child()
                .add(CWD, fs.normalize(name, ".."))
                .add(GlobalScope.KEY, env.hasNotNull(GlobalScope.KEY) ? env.get(GlobalScope.KEY).child() : new GlobalScope());

            var mod = new SourceModule(filename, src, moduleEnv);
            modules.put(name, mod);

            return mod;
        };
    }

    public static String cwd(Environment exts) {
        exts.init(CWD, "/");
        return "/";
    }
    public static ModuleRepo get(Environment exts) {
        return exts.get(KEY);
    }
}
