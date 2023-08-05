package me.topchetoeu.jscript.engine.values;

import java.util.List;

import me.topchetoeu.jscript.engine.CallContext;

public abstract class FunctionValue extends ObjectValue {
    public String name = "";
    public boolean special = false;
    public int length;

    @Override
    public String toString() {
        return "function(...) { ... }";
    }

    public abstract Object call(CallContext ctx, Object thisArg, Object... args) throws InterruptedException;
    public Object call(CallContext ctx) throws InterruptedException {
        return call(ctx, null);
    }

    @Override
    protected Object getField(CallContext ctx, Object key) throws InterruptedException {
        if (key.equals("name")) return name;
        if (key.equals("length")) return length;
        return super.getField(ctx, key);
    }
    @Override
    protected boolean setField(CallContext ctx, Object key, Object val) throws InterruptedException {
        if (key.equals("name")) name = Values.toString(ctx, val);
        else if (key.equals("length")) length = (int)Values.toNumber(ctx, val);
        else return super.setField(ctx, key, val);
        return true;
    }
    @Override
    protected boolean hasField(CallContext ctx, Object key) throws InterruptedException {
        if (key.equals("name")) return true;
        if (key.equals("length")) return true;
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
        proto.defineProperty("constructor", this, true, false, false);
        this.defineProperty("prototype", proto, true, false, false);
    }
}

