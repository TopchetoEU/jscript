package me.topchetoeu.jscript.runtime.values;

import java.util.List;

import me.topchetoeu.jscript.runtime.environment.Environment;

public abstract class FunctionValue extends ObjectValue {
    public String name = "";
    public int length;

    @Override
    public String toString() {
        return String.format("function %s(...)", name);
    }

    public abstract Object call(Environment ext, Object thisArg, Object ...args);
    public Object call(Environment ext) {
        return call(ext, null);
    }

    @Override
    protected Object getField(Environment ext, Object key) {
        if ("name".equals(key)) return name;
        if ("length".equals(key)) return length;
        return super.getField(ext, key);
    }
    @Override
    protected boolean setField(Environment ext, Object key, Object val) {
        if ("name".equals(key)) name = Values.toString(ext, val);
        else if ("length".equals(key)) length = (int)Values.toNumber(ext, val);
        else return super.setField(ext, key, val);
        return true;
    }
    @Override
    protected boolean hasField(Environment ext, Object key) {
        if ("name".equals(key)) return true;
        if ("length".equals(key)) return true;
        return super.hasField(ext, key);
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

