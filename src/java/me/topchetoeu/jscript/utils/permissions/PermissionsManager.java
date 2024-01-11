package me.topchetoeu.jscript.utils.permissions;

import java.util.ArrayList;

public class PermissionsManager implements PermissionsProvider {
    public static final PermissionsProvider ALL_PERMS = new PermissionsManager().add(new Permission("**"));

    public final ArrayList<Permission> allowed = new ArrayList<>();
    public final ArrayList<Permission> denied = new ArrayList<>();

    public PermissionsProvider add(Permission perm) {
        allowed.add(perm);
        return this;
    }
    public PermissionsProvider add(String perm) {
        allowed.add(new Permission(perm));
        return this;
    }

    @Override
    public boolean hasPermission(Permission perm, char delim) {
        for (var el : denied) if (el.match(perm, delim)) return false;
        for (var el : allowed) if (el.match(perm, delim)) return true;

        return false;
    }
    @Override
    public boolean hasPermission(Permission perm) {
        for (var el : denied) if (el.match(perm)) return false;
        for (var el : allowed) if (el.match(perm)) return true;

        return false;
    }
}
