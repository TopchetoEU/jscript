package me.topchetoeu.jscript.compilation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Function;

public final class NodeChildren implements Iterable<Node> {
    public static final class Slot {
        private Node node;
        private final Function<Node, Node> replacer;

        public final void replace(Node node) {
            this.node = this.replacer.apply(node);
        }

        public Slot(Node nodes, Function<Node, Node> replacer) {
            this.node = nodes;
            this.replacer = replacer;
        }
    }

    private final Slot[] slots;

    private NodeChildren(Slot[] slots) {
        this.slots = slots;
    }

    @Override public Iterator<Node> iterator() {
        return new Iterator<Node>() {
            private int i = 0;
            private Slot[] arr = slots;

            @Override public boolean hasNext() {
                if (arr == null) return false;
                else if (i >= arr.length) {
                    arr = null;
                    return false;
                }
                else return true;
            }
            @Override public Node next() {
                if (!hasNext()) return null;
                return arr[i++].node;
            }
        };
    }
    public Iterable<Slot> slots() {
        return () -> new Iterator<Slot>() {
            private int i = 0;
            private Slot[] arr = slots;

            @Override public boolean hasNext() {
                if (arr == null) return false;
                else if (i >= arr.length) {
                    arr = null;
                    return false;
                }
                else return true;
            }
            @Override public Slot next() {
                if (!hasNext()) return null;
                return arr[i++];
            }
        };
    }

    public static final class Builder {
        private final ArrayList<Slot> slots = new ArrayList<>();

        public final Builder add(Slot ...children) {
            for (var child : children) {
                this.slots.add(child);
            }

            return this;
        }
        public final Builder add(Iterable<Slot> children) {
            for (var child : children) {
                this.slots.add(child);
            }

            return this;
        }
        public final Builder add(Node child, Function<Node, Node> replacer) {
            slots.add(new Slot(child, replacer));
            return this;
        }

        public final NodeChildren build() {
            return new NodeChildren(slots.toArray(new Slot[0]));
        }
    }
}