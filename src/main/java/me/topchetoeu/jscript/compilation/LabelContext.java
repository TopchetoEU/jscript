package me.topchetoeu.jscript.compilation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Stack;
import java.util.function.IntSupplier;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.SyntaxException;
import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.common.environment.Key;
import me.topchetoeu.jscript.common.parsing.Location;

public class LabelContext {
	public static final Key<LabelContext> BREAK_CTX = new Key<>();
	public static final Key<LabelContext> CONTINUE_CTX = new Key<>();

	private final LinkedList<IntSupplier> list = new LinkedList<>();
	private final HashMap<String, IntSupplier> map = new HashMap<>();

	private final Stack<ArrayList<Runnable>> deferredAdders = new Stack<>();

	public IntSupplier get() {
		return list.peekLast();
	}
	public IntSupplier get(String name) {
		return map.get(name);
	}

	public void flushAdders() {
		for (var adder : deferredAdders.peek()) {
			adder.run();
		}

		deferredAdders.pop();
	}

	public boolean jump(CompileResult target) {
		var res = get();
		if (res != null) {
			var tmp = target.temp();
			this.deferredAdders.peek().add(() -> target.set(tmp, Instruction.jmp(res.getAsInt() - tmp)));
			return true;
		}
		else return false;
	}
	public boolean jump(CompileResult target, String name) {
		var res = get(name);
		if (res != null) {
			var tmp = target.temp();
			this.deferredAdders.peek().add(() -> target.set(tmp, Instruction.jmp(res.getAsInt() - tmp)));
			return true;
		}
		else return false;
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
		deferredAdders.push(new ArrayList<>());
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
		flushAdders();
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
