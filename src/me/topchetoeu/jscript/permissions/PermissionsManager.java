package me.topchetoeu.jscript.permissions;

public interface PermissionsManager {
    public static final PermissionsManager ALL_PERMS = perm -> true;
    public static final PermissionsManager NO_PERMS = perm -> false;

    boolean hasPermissions(String perm);
}
