package me.topchetoeu.jscript.compilation;

import java.util.function.Predicate;

public class Path<T extends Node> {
    public final Path<?> parent;
    public final Node node;

    public Path<?> getParent(Predicate<Path<?>> predicate) {
        for (Path<?> it = this; it != null; it = it.parent) {
            if (predicate.test(it)) return it;
        }

        return null;
    }

    public Path<?> getParent(Class<? extends Node> type, Predicate<Path<?>> predicate) {
        for (Path<?> it = this; it != null; it = it.parent) {
            if (type.isInstance(it.node) && predicate.test(it)) return it;
        }

        return null;
    }

    public Path(Path<?> parent, Node node) {
        this.parent = parent;
        this.node = node;
    }
}
