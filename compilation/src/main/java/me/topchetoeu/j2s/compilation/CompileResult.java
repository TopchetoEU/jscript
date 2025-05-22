package me.topchetoeu.j2s.compilation;

import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.Function;

import me.topchetoeu.j2s.common.Environment;
import me.topchetoeu.j2s.common.FunctionBody;
import me.topchetoeu.j2s.common.Instruction;
import me.topchetoeu.j2s.common.Key;
import me.topchetoeu.j2s.common.Location;
import me.topchetoeu.j2s.common.Instruction.BreakpointType;
import me.topchetoeu.j2s.compilation.CompilationFunctionMap.FunctionMapBuilder;
import me.topchetoeu.j2s.compilation.control.TryNode;
import me.topchetoeu.j2s.compilation.scope.FunctionScope;
import me.topchetoeu.j2s.compilation.scope.Variable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

public final class CompileResult {
	public static final Key<Void> DEBUG_LOG = new Key<>();

	private FunctionBody body;
	public final List<Instruction> instructions;
	public final List<CompileResult> children;
	public final Map<FunctionNode, CompileResult> childrenMap = new HashMap<>();
	public final Map<FunctionNode, Integer> childrenIndices = new HashMap<>();
	public final FunctionMapBuilder map;
	public final Environment env;
	public int length;
	public final FunctionScope scope;
	public final Map<TryNode, Variable> catchBindings = new HashMap<>();

	public int temp() {
		instructions.add(null);
		return instructions.size() - 1;
	}

	public CompileResult add(Instruction instr) {
		instructions.add(instr);
		return this;
	}
	public CompileResult set(int i, Instruction instr) {
		instructions.set(i, instr);
		return this;
	}

	public int size() { return instructions.size(); }

	public void setDebug(Location loc, BreakpointType type) {
		map.setDebug(loc, type);
	}
	public void setLocation(int i, Location loc) {
		map.setLocation(i, loc);
	}
	public void setLocationAndDebug(int i, Location loc, BreakpointType type) {
		map.setLocationAndDebug(i, loc, type);
	}
	public void setDebug(BreakpointType type) {
		setDebug(map.last(), type);
	}
	public void setLocation(Location type) {
		setLocation(instructions.size() - 1, type);
	}

	public void setLocationAndDebug(Location loc, BreakpointType type) {
		setLocationAndDebug(instructions.size() - 1, loc, type);
	}

	public Iterable<CompileResult> all() {
		var stack = new Stack<CompileResult>();
		stack.push(this);

		return () -> new Iterator<CompileResult>() {
			@Override public CompileResult next() {
				if (stack.empty()) return null;
				else {
					var res = stack.pop();
					for (var child : res.children) {
						stack.push(child);
					}
					return res;
				}
			}
			@Override public boolean hasNext() {
				return !stack.empty();
			}
		};
	}

	public CompileResult addChild(FunctionNode node, CompileResult res) {
		this.children.add(res);
		this.childrenMap.put(node, res);
		this.childrenIndices.put(node, this.children.size() - 1);
		return res;
	}

	public Instruction[] instructions() {
		return instructions.toArray(new Instruction[0]);
	}

	public CompilationFunctionMap map(Function<Location, Location> mapper) {
		return map.map(mapper).build(scope.localNames(), scope.capturableNames(), scope.captureNames());
	}
	public CompilationFunctionMap map() {
		return map.build(scope.localNames(), scope.capturableNames(), scope.captureNames());
	}
	public FunctionBody body() {
		if (body != null) return body;

		var builtChildren = new FunctionBody[children.size()];
		for (var i = 0; i < children.size(); i++) builtChildren[i] = children.get(i).body();

		var instrRes = instructions();

		if (env.has(DEBUG_LOG)) {
			System.out.println("================= BODY =================");
			System.out.println("LOCALS: " + scope.localsCount());
			System.out.println("CAPTURABLES: " + scope.capturablesCount());
			System.out.println("CAPTURES: " + scope.capturesCount());

			for (var instr : instrRes) System.out.println(instr);
		}

		return body = new FunctionBody(
			scope.localsCount(), scope.capturablesCount(), scope.capturesCount(),
			length, instrRes, builtChildren
		);
	}

	public CompileResult subtarget() {
		return new CompileResult(env, new FunctionScope(scope), this);
	}

	public CompileResult setEnvironment(Environment env) {
		return new CompileResult(env, scope, this);
	}
	/**
	 * Returns a compile result with a child of the environment that relates to the given key.
	 * In essence, this is used to create a compile result which is back at the root environment of the compilation
	 */
	public CompileResult rootEnvironment(Key<Environment> env) {
		return new CompileResult(this.env.get(env).child(), scope, this);
	}
	public CompileResult subEnvironment() {
		return new CompileResult(env.child(), scope, this);
	}

	public CompileResult(Environment env, FunctionScope scope, int length) {
		this.scope = scope;
		this.instructions = new ArrayList<>();
		this.children = new LinkedList<>();
		this.map = CompilationFunctionMap.builder();
		this.env = env;
		this.length = length;
	}
	private CompileResult(Environment env, FunctionScope scope, CompileResult parent) {
		this.scope = scope;
		this.instructions = parent.instructions;
		this.children = parent.children;
		this.map = parent.map;
		this.env = env;
	}
}
