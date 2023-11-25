package me.topchetoeu.jscript.permissions;

public interface PermissionsProvider {
    boolean hasPermission(Permission perm, char delim);
    boolean hasPermission(Permission perm);

    default boolean hasPermission(String perm, char delim) {
        return hasPermission(new Permission(perm), delim);
    }
    default boolean hasPermission(String perm) {
        return hasPermission(new Permission(perm));
    }
}