package me.topchetoeu.jscript.utils.permissions;

public class PermissionPredicate {
    public final String namespace;
    public final String value;
    public final boolean denies;

    public boolean match(Permission permission, String value) {
        if (!match(permission)) return false;
        if (this.value == null || value == null) return true;
        if (permission.matcher == null) return true;
        else return permission.matcher.match(this.value, value);
    }
    public boolean match(Permission permission) {
        return Matcher.namespaceWildcard().match(namespace, permission.namespace);
    }

    @Override
    public String toString() {
        if (value != null) return namespace + ":" + value;
        else return namespace;
    }

    public PermissionPredicate(String raw) {
        raw = raw.trim();

        if (raw.startsWith("!")) {
            denies = true;
            raw = raw.substring(1).trim();
        }
        else denies = false;

        var i = raw.indexOf(':');

        if (i > 0) {
            value = raw.substring(i + 1);
            namespace = raw.substring(0, i);
        }
        else {
            value = null;
            namespace = raw;
        }
    }
}
