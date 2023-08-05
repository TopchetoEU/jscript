package me.topchetoeu.jscript.filesystem;

import java.nio.file.Path;

public interface PermissionsProvider {
    Permissions perms(Path file);
}
