package me.topchetoeu.jscript.compilation.scope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import java.util.stream.StreamSupport;

public final class VariableList {
    private final class Node implements IntSupplier {
        public Variable var;
        public Node next;
        public Node prev;
        public boolean frozen;
        public int index;

        @Override public int getAsInt() {
            if (frozen) {
                if (offset == null) {
                    return indexConverter == null ? index : indexConverter.applyAsInt(index);
                }
                else {
                    return indexConverter == null ?
                        index + offset.getAsInt() :
                        indexConverter.applyAsInt(index + offset.getAsInt());
                }
            }

            var res = 0;
            if (offset != null) res = offset.getAsInt();

            for (var it = prev; it != null; it = it.prev) {
                res++;
            }

            return indexConverter == null ? res : indexConverter.applyAsInt(res);
        }

        public void freeze() {
            if (frozen) return;
            this.frozen = true;
            this.next = null;
            this.var.freeze();
            if (prev == null) return;

            this.index = prev.index + 1;
            this.next = null;

            return;
        }

        public Node(Variable var, Node next, Node prev) {
            this.var = var;
            this.next = next;
            this.prev = prev;
        }
    }

    private Node first, last;

    private final HashMap<String, Node> map = new HashMap<>();
    private ArrayList<Node> frozenList = null;

    private final IntSupplier offset;
    private IntUnaryOperator indexConverter = null;

    public boolean frozen() {
        if (frozenList != null) {
            assert frozenList != null;
            assert map != null;
            assert first == null;
            assert last == null;

            return true;
        }
        else {
            assert frozenList == null;
            assert map != null;

            return false;
        }
    }

    private Variable add(Variable val, boolean overlay) {
        if (frozen()) throw new RuntimeException("The scope has been frozen");
        if (!overlay && map.containsKey(val.name)) {
            var node = this.map.get(val.name);
            val.setIndexSupplier(node);
            return node.var;
        }

        var node = new Node(val, null, last);

        if (last != null) {
            assert first != null;

            last.next = node;
            node.prev = last;

            last = node;
        }
        else {
            first = last = node;
        }

        map.put(val.name, node);
        val.setIndexSupplier(node);

        return val;
    }

    public Variable add(Variable val) {
        return this.add(val, false);
    }
    public Variable overlay(Variable val) {
        return this.add(val, true);
    }
    public Variable remove(String key) {
        if (frozen()) throw new RuntimeException("The scope has been frozen");

        var node = map.get(key);
        if (node == null) return null;

        if (node.prev != null) {
            assert node != first;
            node.prev.next = node.next;
        }
        else {
            assert node == first;
            first = first.next;
        }

        if (node.next != null) {
            assert node != last;
            node.next.prev = node.prev;
        }
        else {
            assert node == last;
            last = last.prev;
        }

        node.next = null;
        node.prev = null;

        return node.var;
    }

    public Variable get(String name) {
        var res = map.get(name);
        if (res != null) return res.var;
        else return null;
    }
    public int indexOfKey(String name) {
        return map.get(name).getAsInt();
    }

    public boolean has(String name) {
        return this.map.containsKey(name);
    }

    public int size() {
        if (frozen()) return frozenList.size();
        else return map.size();
    }

    public void freeze() {
        if (frozen()) return;

        frozenList = new ArrayList<>();

        for (var node = first; node != null; ) {
            frozenList.add(node);

            var tmp = node;
            node = node.next;
            tmp.freeze();
        }

        first = last = null;
    }

    public Iterable<Variable> all() {
        if (frozen()) return () -> frozenList.stream().map(v -> v.var).iterator();
        else return () -> new Iterator<Variable>() {
            private Node curr = first;

            @Override public boolean hasNext() {
                return curr != null;
            }
            @Override public Variable next() {
                if (curr == null) return null;

                var res = curr;
                curr = curr.next;
                return res.var;
            }
        };
    }
    public Iterable<String> keys() {
        return () -> StreamSupport.stream(all().spliterator(), false).map(v -> v.name).iterator();
    }

    public VariableList setIndexMap(IntUnaryOperator map) {
        indexConverter = map;
        return this;
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
