package me.topchetoeu.jscript.utils.modules;

import java.util.HashMap;

import me.topchetoeu.jscript.runtime.Context;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;

public class RootModuleRepo implements ModuleRepo {
    public final HashMap<String, ModuleRepo> repos = new HashMap<>();

    @Override
    public Module getModule(Context ctx, String cwd, String name) {
        var i = name.indexOf(":");
        String repoName, modName;

        if (i < 0) {
            repoName = "file";
            modName = name;
        }
        else {
            repoName = name.substring(0, i);
            modName = name.substring(i + 1);
        }

        var repo = repos.get(repoName);
        if (repo == null) throw EngineException.ofError("ModuleError", "Couldn't find module repo '" + repoName + "'.");

        return repo.getModule(ctx, cwd, modName);
    }
}
