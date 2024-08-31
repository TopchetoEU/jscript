package me.topchetoeu.jscript.compilation.scope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.function.IntSupplier;

public class VariableList implements Iterable<VariableDescriptor> {
    private class ListVar extends VariableDescriptor {
        private ListVar next;
        private ListVar prev;

        @Override public int index() {
            throw new RuntimeException("The index of a variable may not be retrieved until the scope has been finalized");
            // var res = 0;
            // if (offset != null) res = offset.getAsInt();

            // for (var it = prev; it != null; it = it.prev) {
            //     res++;
            // }

            // return res;
        }

        public ListVar(String name, boolean readonly, ListVar next, ListVar prev) {
            super(name, readonly);

            this.next = next;
            this.prev = prev;
        }
    }

    private ListVar first, last;

    private HashMap<String, ListVar> map = new HashMap<>();

    private HashMap<String, VariableDescriptor> frozenMap = null;
    private ArrayList<VariableDescriptor> frozenList = null;

    private final IntSupplier offset;

    public boolean frozen() {
        if (frozenMap != null) {
            assert frozenList != null;
            assert frozenMap != null;
            assert map == null;
            assert first == null;
            assert last == null;

            return true;
        }
        else {
            assert frozenList == null;
            assert frozenMap == null;
            assert map != null;

            return false;
        }
    }

    public VariableDescriptor add(VariableDescriptor val) {
        return add(val.name, val.readonly);
    }
    public VariableDescriptor add(String name, boolean readonly) {
        if (frozen()) throw new RuntimeException("The scope has been frozen");
        if (map.containsKey(name)) return map.get(name);

        var res = new ListVar(name, readonly, null, last);
        last.next = res;
        last = res;
        map.put(name, res);

        return res;
    }
    public VariableDescriptor remove(String name) {
        if (frozen()) throw new RuntimeException("The scope has been frozen");

        var el = map.get(name);
        if (el == null) return null;

        el.prev.next = el.next;
        el.next.prev = el.prev;

        el.next = null;
        el.prev = null;

        return el;
    }

    public VariableDescriptor get(String name) {
        return map.get(name);
    }
    public VariableDescriptor get(int i) {
        if (frozen()) {
            if (i < 0 || i >= frozenList.size()) return null;
            return frozenList.get(i);
        }
        else {
            if (i < 0 || i >= map.size()) return null;

            if (i < map.size() / 2) {
                var it = first;
                for (var j = 0; j < i; it = it.next, j++);
                return it;
            }
            else {
                var it = last;
                for (var j = map.size() - 1; j >= i; it = it.prev, j--);
                return it;
            }
        }
    }

    public boolean has(String name) {
        return this.get(name) != null;
    }

    public int size() {
        if (frozen()) return frozenList.size();
        else return map.size();
    }

    public void freeze() {
        if (frozen()) return;

        frozenMap = new HashMap<>();
        frozenList = new ArrayList<>();

        var i = 0;
        if (offset != null) i = offset.getAsInt();

        for (var it = first; it != null; it = it.next) {
            frozenMap.put(it.name, VariableDescriptor.of(it.name, it.readonly, i++));
        }
    }

    @Override public Iterator<VariableDescriptor> iterator() {
        if (frozen()) return frozenList.iterator();
        else return new Iterator<VariableDescriptor>() {
            private ListVar curr = first;

            @Override public boolean hasNext() {
                return curr != null;
            }
            @Override public VariableDescriptor next() {
                if (curr == null) return null;

                var res = curr;
                curr = curr.next;
                return res;
            }
        };
    }

    public VariableDescriptor[] toArray() {
        var res = new VariableDescriptor[size()];
        var i = 0;

        for (var el : this) res[i++] = el;

        return res;
    }

    public VariableList(IntSupplier offset) {
        this.offset = offset;
    }
    public VariableList(int offset) {
        this.offset = () -> offset;
    }
    public VariableList(VariableList prev) {
        this.offset = prev::size;
    }
    public VariableList() {
        this.offset = null;
    }
}
