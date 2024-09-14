package me.topchetoeu.jscript.compilation;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.SyntaxException;
import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.common.environment.Key;
import me.topchetoeu.jscript.common.parsing.Location;

public class LabelContext {
    public static final Key<LabelContext> BREAK_CTX = Key.of();
    public static final Key<LabelContext> CONTINUE_CTX = Key.of();

    private final LinkedList<IntSupplier> list = new LinkedList<>();
    private final HashMap<String, IntSupplier> map = new HashMap<>();

    public IntSupplier get() {
        return list.peekLast();
    }
    public IntSupplier get(String name) {
        return map.get(name);
    }

    public IntFunction<Instruction> getJump() {
        var res = get();
        if (res == null) return null;
        else return i -> Instruction.jmp(res.getAsInt() - i);
    }
    public IntFunction<Instruction> getJump(String name) {
        var res = get(name);
        if (res == null) return null;
        else return i -> Instruction.jmp(res.getAsInt() - i);
    }

    public void push(IntSupplier jumpTarget) {
        list.add(jumpTarget);
    }
    public void push(Location loc, String name, IntSupplier jumpTarget) {
        if (name == null) return;
        if (map.containsKey(name)) throw new SyntaxException(loc, String.format("Label '%s' has already been declared", name));
        map.put(name, jumpTarget);
    }

    public void pushLoop(Location loc, String name, IntSupplier jumpTarget) {
        push(jumpTarget);
        push(loc, name, jumpTarget);
    }

    public void pop() {
        list.removeLast();
    }
    public void pop(String name) {
        if (name == null) return;
        map.remove(name);
    }

    public void popLoop(String name) {
        pop();
        pop(name);
    }

    public static LabelContext getBreak(Environment env) {
        return env.initFrom(BREAK_CTX, () -> new LabelContext());
    }
    public static LabelContext getCont(Environment env) {
        return env.initFrom(CONTINUE_CTX, () -> new LabelContext());
    }

    public static void pushLoop(Environment env, Location loc, String name, IntSupplier breakTarget, int contTarget) {
        LabelContext.getBreak(env).pushLoop(loc, name, breakTarget);
        LabelContext.getCont(env).pushLoop(loc, name, () -> contTarget);
    }
    public static void pushLoop(Environment env, Location loc, String name, IntSupplier breakTarget, IntSupplier contTarget) {
        LabelContext.getBreak(env).pushLoop(loc, name, breakTarget);
        LabelContext.getCont(env).pushLoop(loc, name, contTarget);
    }
    public static void popLoop(Environment env, String name) {
        LabelContext.getBreak(env).popLoop(name);
        LabelContext.getCont(env).popLoop(name);
    }
}
