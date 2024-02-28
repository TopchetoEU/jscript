package me.topchetoeu.jscript.utils.permissions;

import me.topchetoeu.jscript.core.Extensions;
import me.topchetoeu.jscript.core.Key;

public interface PermissionsProvider {
    public static final Key<PermissionsProvider> KEY = new Key<>();
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
            if (exts.hasNotNull(KEY)) return exts.get(KEY).hasPermission(perm);
            else return true;
        };
    }
}