package me.topchetoeu.jscript.permissions;

import java.util.ArrayList;

public class PermissionsManager {
    public static final PermissionsManager ALL_PERMS = new PermissionsManager().add(new Permission("**"));

    public final ArrayList<Permission> allowed = new ArrayList<>();
    public final ArrayList<Permission> denied = new ArrayList<>();

    public PermissionsManager add(Permission perm) {
        allowed.add(perm);
        return this;
    }
    public PermissionsManager add(String perm) {
        allowed.add(new Permission(perm));
        return this;
    }

    public boolean has(Permission perm, char delim) {
        for (var el : denied) if (el.match(perm, delim)) return false;
        for (var el : allowed) if (el.match(perm, delim)) return true;

        return false;
    }
    public boolean has(Permission perm) {
        for (var el : denied) if (el.match(perm)) return false;
        for (var el : allowed) if (el.match(perm)) return true;

        return false;
    }

    public boolean has(String perm, char delim) {
        return has(new Permission(perm), delim);
    }
    public boolean has(String perm) {
        return has(new Permission(perm));
    }
}
