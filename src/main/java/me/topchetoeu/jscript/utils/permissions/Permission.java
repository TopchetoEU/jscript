package me.topchetoeu.jscript.utils.permissions;


public class Permission {
    public final String namespace;
    public final Matcher matcher;

    @Override public String toString() {
        return namespace;
    }

    public Permission(String namespace, Matcher matcher) {
        this.namespace = namespace;
        this.matcher = matcher;
    }
    public Permission(String raw) {
        this(raw, null);
    }
}
