package me.topchetoeu.jscript.engine.values;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import me.topchetoeu.jscript.engine.Context;

public class ArrayValue extends ObjectValue {
    private static final Object EMPTY = new Object();
    private final ArrayList<Object> values = new ArrayList<>();

    public int size() { return values.size(); }
    public boolean setSize(int val) {
        if (val < 0) return false;
        while (size() > val) {
            values.remove(values.size() - 1);
        }
        while (size() < val) {
            values.add(EMPTY);
        }
        return true;
    }

    public Object get(int i) {
        if (i < 0 || i >= values.size()) return null;
        var res = values.get(i);
        if (res == EMPTY) return null;
        else return res;
    }
    public void set(Context ctx, int i, Object val) {
        if (i < 0) return;

        while (values.size() <= i) {
            values.add(EMPTY);
        }

        values.set(i, Values.normalize(ctx, val));
    }
    public boolean has(int i) {
        return i >= 0 && i < values.size() && values.get(i) != EMPTY;
    }
    public void remove(int i) {
        if (i < 0 || i >= values.size()) return;
        values.set(i, EMPTY);
    }
    public void shrink(int n) {
        if (n > values.size()) values.clear();
        else {
            for (int i = 0; i < n && values.size() > 0; i++) {
                values.remove(values.size() - 1);
            }
        }
    }

    public void sort(Comparator<Object> comparator) {
        values.sort((a, b) -> {
            var _a = 0;
            var _b = 0;

            if (a == null) _a = 1;
            if (a == EMPTY) _a = 2;

            if (b == null) _b = 1;
            if (b == EMPTY) _b = 2;

            if (Integer.compare(_a, _b) != 0) return Integer.compare(_a, _b);

            return comparator.compare(a, b);
        });
    }

    public Object[] toArray() {
        Object[] res = new Object[values.size()];

        for (var i = 0; i < values.size(); i++) {
            if (values.get(i) == EMPTY) res[i] = null;
            else res[i] = values.get(i);
        }

        return res;
    }

    @Override
    protected Object getField(Context ctx, Object key) throws InterruptedException {
        if (key.equals("length")) return values.size();
        if (key instanceof Number) {
            var i = ((Number)key).doubleValue();
            if (i >= 0 && i - Math.floor(i) == 0) {
                return get((int)i);
            }
        }

        return super.getField(ctx, key);
    }
    @Override
    protected boolean setField(Context ctx, Object key, Object val) throws InterruptedException {
        if (key.equals("length")) {
            return setSize((int)Values.toNumber(ctx, val));
        }
        if (key instanceof Number) {
            var i = Values.number(key);
            if (i >= 0 && i - Math.floor(i) == 0) {
                set(ctx, (int)i, val);
                return true;
            }
        }

        return super.setField(ctx, key, val);
    }
    @Override
    protected boolean hasField(Context ctx, Object key) throws InterruptedException {
        if (key.equals("length")) return true;
        if (key instanceof Number) {
            var i = Values.number(key);
            if (i >= 0 && i - Math.floor(i) == 0) {
                return has((int)i);
            }
        }

        return super.hasField(ctx, key);
    }
    @Override
    protected void deleteField(Context ctx, Object key) throws InterruptedException {
        if (key instanceof Number) {
            var i = Values.number(key);
            if (i >= 0 && i - Math.floor(i) == 0) {
                remove((int)i);
                return;
            }
        }

        super.deleteField(ctx, key);
    }

    @Override
    public List<Object> keys(boolean includeNonEnumerable) {
        var res = super.keys(includeNonEnumerable);
        for (var i = 0; i < size(); i++) {
            if (has(i)) res.add(i);
        }
        if (includeNonEnumerable) res.add("length");
        return res;
    }

    public ArrayValue() {
        super(PlaceholderProto.ARRAY);
        nonEnumerableSet.add("length");
        nonConfigurableSet.add("length");
    }
    public ArrayValue(Context ctx, Object ...values) {
        this();
        for (var i = 0; i < values.length; i++) this.values.add(Values.normalize(ctx, values[i]));
    }

    public static ArrayValue of(Context ctx, Collection<Object> values) {
        return new ArrayValue(ctx, values.toArray(Object[]::new));
    }
}
