package me.topchetoeu.jscript.engine.modules;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

import me.topchetoeu.jscript.polyfills.PolyfillEngine;

public class FileModuleProvider implements ModuleProvider {
    public File root;
    public final boolean allowOutside;

    private boolean checkInside(Path modFile) {
        return modFile.toAbsolutePath().startsWith(root.toPath().toAbsolutePath());
    }

    @Override
    public Module getModule(File cwd, String name) {
        var realName = getRealName(cwd, name);
        if (realName == null) return null;
        var path = Path.of(realName + ".js").normalize();

        try {
            var res = PolyfillEngine.streamToString(new FileInputStream(path.toFile()));
            return new Module(realName, path.toString(), res);
        }
        catch (IOException e) {
            return null;
        }
    }
    @Override
    public String getRealName(File cwd, String name) {
        var path = Path.of(".", Path.of(cwd.toString(), name).normalize().toString());
        var fileName = path.getFileName().toString();
        if (fileName == null) return null;
        if (!fileName.equals("index") && path.toFile().isDirectory()) return getRealName(cwd, name + "/index");
        path = Path.of(path.toString() + ".js");
        if (!allowOutside && !checkInside(path)) return null;
        if (!path.toFile().isFile() || !path.toFile().canRead()) return null;
        var res = path.toString().replace('\\', '/');
        var i = res.lastIndexOf('.');
        return res.substring(0, i);
    }

    public FileModuleProvider(File root, boolean allowOutside) {
        this.root = root.toPath().normalize().toFile();
        this.allowOutside = allowOutside;
    }
}
