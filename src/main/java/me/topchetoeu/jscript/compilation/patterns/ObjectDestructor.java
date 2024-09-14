package me.topchetoeu.jscript.compilation.patterns;

import java.util.List;
import java.util.function.Consumer;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Node;
import me.topchetoeu.jscript.compilation.values.operations.IndexNode;

public abstract class ObjectDestructor<T> extends Node {
    public static final class Member<T> {
        public final Node key;
        public final T consumable;

        public Member(Node key, T consumer) {
            this.key = key;
            this.consumable = consumer;
        }
    }

    public final List<Member<T>> members;

    public void consume(Consumer<T> consumer) {
        for (var el : members) {
            consumer.accept(el.consumable);
        }
    }
    public void compile(CompileResult target, Consumer<T> consumer, boolean pollute) {
        for (var el : members) {
            target.add(Instruction.dup());
            IndexNode.indexLoad(target, el.key, true);
            consumer.accept(el.consumable);
        }

        if (!pollute) target.add(Instruction.discard());
    }

    public ObjectDestructor(Location loc, List<Member<T>> members) {
        super(loc);
        this.members = members;
    }
}
