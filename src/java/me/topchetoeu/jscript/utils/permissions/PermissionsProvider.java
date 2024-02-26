package me.topchetoeu.jscript.utils.permissions;

import me.topchetoeu.jscript.core.Extensions;
import me.topchetoeu.jscript.core.values.Symbol;

public interface PermissionsProvider {
    public static final Symbol ENV_KEY = new Symbol("Environment.perms");
    public static final PermissionsProvider ALL_PERMS = (perm, value) -> true;

    boolean hasPermission(Permission perm, String value);

    default boolean hasPermission(Permission perm) {
        return hasPermission(perm, null);
    }

    default boolean hasPermission(String perm, String value, Matcher matcher) {
        return hasPermission(new Permission(perm, matcher), value);
    }
    default boolean hasPermission(String perm, Matcher matcher) {
        return hasPermission(new Permission(perm, matcher));
    }

    public static PermissionsProvider get(Extensions exts) {
        return (perm, value) -> {
            if (exts.hasNotNull(ENV_KEY)) return ((PermissionsProvider)exts.get(ENV_KEY)).hasPermission(perm);
            else return true;
        };
    }
}