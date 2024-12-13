package me.topchetoeu.jscript.compilation.scope;

import java.util.ArrayList;
import java.util.HashMap;

public final class FunctionScope {
	protected final VariableList locals = new VariableList(VariableIndex.IndexType.LOCALS);
	protected final VariableList capturables = new VariableList(VariableIndex.IndexType.CAPTURABLES, this.locals);
	private final VariableList captures = new VariableList(VariableIndex.IndexType.CAPTURES);

	private final HashMap<String, Variable> localsMap = new HashMap<>();
	private final HashMap<String, Variable> capturesMap = new HashMap<>();
	private final ArrayList<Variable> catchesMap = new ArrayList<>();

	private final HashMap<Variable, Variable> childToParent = new HashMap<>();
	private final HashMap<Variable, Variable> parentToChild = new HashMap<>();

	public final FunctionScope parent;
	public final boolean passthrough;

	private Variable addCaptured(Variable var, boolean captured) {
		if (captured && !this.capturables.has(var) && !this.captures.has(var)) this.capturables.add(var);
		return var;
	}

	private Variable getCatchVar(String name) {
		for (var el : catchesMap) {
			if (el.name.equals(name)) return el;
		}

		return null;
	}

	/**
	 * @returns If a variable with the same name exists, the old variable. Otherwise, the given variable
	 */
	public Variable define(Variable var) {
		if (passthrough) return null;
		else {
			var catchVar = getCatchVar(var.name);
			if (catchVar != null) return catchVar;
			if (localsMap.containsKey(var.name)) return localsMap.get(var.name);
			if (capturesMap.containsKey(var.name)) throw new RuntimeException("HEY!");

			localsMap.put(var.name, var);
			return locals.add(var);
		}
	}

	/**
	 * @returns A variable with the given name, or null if a global variable
	 */
	public Variable define(String name) {
		return define(new Variable(name, false));
	}

	/**
	 * Creates a catch variable and returns it
	 */
	public Variable defineCatch(String name) {
		var var = new Variable(name, false);
		this.locals.add(var);
		this.catchesMap.add(var);
		return var;
	}
	/**
	 * Creates a catch variable, using a specific variable instance
	 * Used in the second pass
	 */
	public Variable defineCatch(String name, Variable var) {
		this.locals.add(var);
		this.catchesMap.add(var);
		return var;
	}
	/**
	 * Removes the last catch variable.
	 * NOTE: the variable is still in the internal list. It just won't be findable by its name
	 */
	public void undefineCatch() {
		this.catchesMap.remove(this.catchesMap.size() - 1);
	}

	/**
	 * Gets the index supplier of the given variable name, or null if it is a global
	 * 
	 * @param capture If true, the variable is being captured by a function
	 */
	public Variable get(String name, boolean capture) {
		var catchVar = getCatchVar(name);

		if (catchVar != null) return addCaptured(catchVar, capture);
		if (localsMap.containsKey(name)) return addCaptured(localsMap.get(name), capture);
		if (capturesMap.containsKey(name)) return addCaptured(capturesMap.get(name), capture);

		if (parent == null) return null;

		var parentVar = parent.get(name, true);
		if (parentVar == null) return null;

		var childVar = captures.add(parentVar.clone().setIndexSupplier(null));
		capturesMap.put(childVar.name, childVar);
		childToParent.put(childVar, parentVar);
		parentToChild.put(parentVar, childVar);

		return childVar;
	}

	/**
	 * If the variable given is contained in this function, just returns the variable itself.
	 * However, this function is important to handle cases in which you might want to access
	 * a captured variable. In such cases, this function will return a capture to the given variable.
	 *
	 * @param capture Whether or not to execute this capturing logic
	 */
	public Variable get(Variable var, boolean capture) {
		if (captures.has(var)) return addCaptured(var, capture);
		if (locals.has(var)) return addCaptured(var, capture);

		if (capture) {
			if (parentToChild.containsKey(var)) return addCaptured(parentToChild.get(var), capture);
	
			if (parent == null) return null;
	
			var parentVar = parent.get(var, true);
			if (parentVar == null) return null;
	
			var childVar = captures.add(parentVar.clone());
			childToParent.put(childVar, parentVar);
			parentToChild.put(parentVar, childVar);
	
			return childVar;
		}
		else return null;
	}

	/**
	 * Checks if the given variable name is accessible
	 * 
	 * @param capture If true, will check beyond this function's scope
	 */
	public boolean has(String name, boolean capture) {
		if (localsMap.containsKey(name)) return true;

		if (capture) {
			if (capturesMap.containsKey(name)) return true;
			if (parent != null) return parent.has(name, true);
		}

		return false;
	}

	public int localsCount() {
		return locals.size();
	}
	public int capturesCount() {
		return captures.size();
	}
	public int capturablesCount() {
		return capturables.size();
	}

	public int[] getCaptureIndices() {
		var res = new int[captures.size()];
		var i = 0;

		for (var el : captures) {
			assert childToParent.containsKey(el);
			res[i] = childToParent.get(el).index().toCaptureIndex();
			i++;
		}

		return res;
	}

	public Iterable<Variable> capturables() {
		return capturables;
	}
	public Iterable<Variable> locals() {
		return locals;
	}

	public String[] captureNames() {
		var res = new String[this.captures.size()];
		var i = 0;

		for (var el : this.captures) {
			res[i++] = el.name;
		}

		return res;
	}
	public String[] localNames() {
		var res = new String[this.locals.size() + this.capturables.size()];
		var i = 0;

		for (var el : this.locals) {
			res[i++] = el.name;
		}
		for (var el : this.capturables) {
			res[i++] = el.name;
		}

		return res;
	}

	public FunctionScope(FunctionScope parent) {
		this.parent = parent;
		this.passthrough = false;
	}
	public FunctionScope(boolean passthrough) {
		this.parent = null;
		this.passthrough = passthrough;
	}
}
