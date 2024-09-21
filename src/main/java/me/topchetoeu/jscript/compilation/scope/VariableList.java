package me.topchetoeu.jscript.compilation.scope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public final class VariableList {
    private final class VariableNode implements Supplier<VariableIndex> {
        public Variable var;
        public VariableNode next;
        public VariableNode prev;
        public boolean frozen;
        public int index;

        public VariableList list() { return VariableList.this; }

        @Override public VariableIndex get() {
            if (frozen) {
                if (offset == null) return new VariableIndex(indexType, index);
                else new VariableIndex(indexType, index + offset.getAsInt());
            }

            var res = 0;
            if (offset != null) res = offset.getAsInt();

            for (var it = prev; it != null; it = it.prev) {
                res++;
            }

            return new VariableIndex(indexType, res);
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

        public VariableNode(Variable var, VariableNode next, VariableNode prev) {
            this.var = var;
            this.next = next;
            this.prev = prev;
        }
    }

    private VariableNode first, last;

    private ArrayList<VariableNode> frozenList = null;
    private HashMap<Variable, VariableNode> varMap = new HashMap<>();

    private final IntSupplier offset;

    public final VariableIndex.IndexType indexType;

    public boolean frozen() {
        if (frozenList != null) {
            assert frozenList != null;
            assert varMap != null;
            assert first == null;
            assert last == null;

            return true;
        }
        else {
            assert frozenList == null;
            assert varMap != null;

            return false;
        }
    }

    public Variable add(Variable val) {
        if (frozen()) throw new RuntimeException("The scope has been frozen");

        if (val.indexSupplier() instanceof VariableNode prevNode) {
            prevNode.list().remove(val);
        }

        var node = new VariableNode(val, null, last);

        if (last != null) {
            assert first != null;

            last.next = node;
            node.prev = last;

            last = node;
        }
        else {
            first = last = node;
        }

        varMap.put(val, node);
        val.setIndexSupplier(node);

        return val;
    }

    public Variable remove(Variable var) {
        if (frozen()) throw new RuntimeException("The scope has been frozen");

        if (var == null) return null;

        var node = varMap.get(var);
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

        varMap.remove(node.var);

        return node.var;
    }

    public boolean has(Variable var) {
        return varMap.containsKey(var);
    }

    public Supplier<VariableIndex> indexer(Variable var) {
        return varMap.get(var);
    }

    public int size() {
        if (frozen()) return frozenList.size();
        else return varMap.size();
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
            private VariableNode curr = first;

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

    public VariableList(VariableIndex.IndexType type, IntSupplier offset) {
        this.indexType = type;
        this.offset = offset;
    }
    public VariableList(VariableIndex.IndexType type, int offset) {
        this.indexType = type;
        this.offset = () -> offset;
    }
    public VariableList(VariableIndex.IndexType type, VariableList prev) {
        this.indexType = type;
        this.offset = prev::size;
    }
    public VariableList(VariableIndex.IndexType type) {
        this.indexType = type;
        this.offset = null;
    }
}
