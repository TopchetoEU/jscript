package me.topchetoeu.jscript.utils.interop;

public enum ExposeTarget {
    STATIC(true, true, false),
    MEMBER(false, false, true),
    NAMESPACE(false, true, false),
    CONSTRUCTOR(true, false, false),
    PROTOTYPE(false, false, true),
    ALL(true, true, true);

    public final boolean constructor;
    public final boolean namespace;
    public final boolean prototype;

    public boolean shouldApply(ExposeTarget other) {
        if (other.constructor && !constructor) return false;
        if (other.namespace && !namespace) return false;
        if (other.prototype && !prototype) return false;

        return true;
    }

    private ExposeTarget(boolean constructor, boolean namespace, boolean prototype) {
        this.constructor = constructor;
        this.namespace = namespace;
        this.prototype = prototype;
    }
}