package me.topchetoeu.jscript.permissions;

import me.topchetoeu.jscript.engine.Extensions;
import me.topchetoeu.jscript.engine.values.Symbol;

public interface PermissionsProvider {
    public static final Symbol ENV_KEY = new Symbol("Environment.perms");

    boolean hasPermission(Permission perm, char delim);
    boolean hasPermission(Permission perm);

    default boolean hasPermission(String perm, char delim) {
        return hasPermission(new Permission(perm), delim);
    }
    default boolean hasPermission(String perm) {
        return hasPermission(new Permission(perm));
    }

    public static PermissionsProvider get(Extensions exts) {
        return new PermissionsProvider() {
            @Override public boolean hasPermission(Permission perm) {
                if (exts.hasNotNull(ENV_KEY)) return ((PermissionsProvider)exts.get(ENV_KEY)).hasPermission(perm);
                else return true;
            }
            @Override public boolean hasPermission(Permission perm, char delim) {
                if (exts.hasNotNull(ENV_KEY)) return ((PermissionsProvider)exts.get(ENV_KEY)).hasPermission(perm, delim);
                else return true;
            }
        };
    }
}