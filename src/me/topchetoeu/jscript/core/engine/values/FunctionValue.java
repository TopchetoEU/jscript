package me.topchetoeu.jscript.core.engine.values;

import java.util.List;

import me.topchetoeu.jscript.core.engine.Context;

public abstract class FunctionValue extends ObjectValue {
    public String name = "";
    public boolean special = false;
    public int length;

    @Override
    public String toString() {
        return String.format("function %s(...)", name);
    }

    public abstract Object call(Context ctx, Object thisArg, Object ...args);
    public Object call(Context ctx) {
        return call(ctx, null);
    }

    @Override
    protected Object getField(Context ctx, Object key) {
        if ("name".equals(key)) return name;
        if ("length".equals(key)) return length;
        return super.getField(ctx, key);
    }
    @Override
    protected boolean setField(Context ctx, Object key, Object val) {
        if ("name".equals(key)) name = Values.toString(ctx, val);
        else if ("length".equals(key)) length = (int)Values.toNumber(ctx, val);
        else return super.setField(ctx, key, val);
        return true;
    }
    @Override
    protected boolean hasField(Context ctx, Object key) {
        if ("name".equals(key)) return true;
        if ("length".equals(key)) return true;
        return super.hasField(ctx, key);
    }

    @Override
    public List<Object> keys(boolean includeNonEnumerable) {
        var res = super.keys(includeNonEnumerable);
        if (includeNonEnumerable) {
            res.add("name");
            res.add("length");
        }
        return res;
    }

    public FunctionValue(String name, int length) {
        super(PlaceholderProto.FUNCTION);

        if (name == null) name = "";
        this.length = length;
        this.name = name;

        nonConfigurableSet.add("name");
        nonEnumerableSet.add("name");
        nonWritableSet.add("length");
        nonConfigurableSet.add("length");
        nonEnumerableSet.add("length");

        var proto = new ObjectValue();
        proto.defineProperty(null, "constructor", this, true, false, false);
        this.defineProperty(null, "prototype", proto, true, false, false);
    }
}

