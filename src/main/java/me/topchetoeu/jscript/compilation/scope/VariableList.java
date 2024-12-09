package me.topchetoeu.jscript.compilation.scope;

import java.util.HashMap;
import java.util.Iterator;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import me.topchetoeu.jscript.compilation.scope.VariableIndex.IndexType;

public final class VariableList implements Iterable<Variable> {
	private final class VariableNode implements Supplier<VariableIndex> {
		public Variable var;
		public VariableNode next;
		public VariableNode prev;
		public int index;
		public int indexIteration = -1;

		public VariableList list() { return VariableList.this; }

		private int getIndex() {
			if (this.indexIteration != VariableList.this.indexIteration) {
				this.indexIteration = VariableList.this.indexIteration;

				if (prev == null) this.index = 0;
				else this.index = prev.getIndex() + 1;
			}

			return this.index;
		}

		@Override public VariableIndex get() {
			if (offset == null) return new VariableIndex(indexType, this.getIndex());
			else return new VariableIndex(indexType, offset.getAsInt() + this.getIndex());
		}

		public VariableNode(Variable var, VariableNode next, VariableNode prev) {
			this.var = var;
			this.next = next;
			this.prev = prev;
		}
	}

	private VariableNode first, last;

	private HashMap<Variable, VariableNode> varMap = new HashMap<>();

	private final IntSupplier offset;
	/**
	 * Increased when indices need recalculation. VariableNode will check if
	 * its internal indexIteration is up to date with this, and if not, will
	 * recalculate its index
	 */
	private int indexIteration = 0;

	public final VariableIndex.IndexType indexType;

	/**
	 * Adds the given variable to this list. If it already exists, does nothing
	 * @return val
	 */
	public Variable add(Variable val) {
		if (this.varMap.containsKey(val)) return val;
		this.indexIteration++;

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

	/**
	 * If the variable is not in the list, does nothing. Otherwise, removes the variable from the list
	 * @return null if nothing was done, else the deleted variable (should be var)
	 */
	public Variable remove(Variable var) {
		if (var == null) return null;

		var node = varMap.get(var);
		if (node == null) return null;

		this.indexIteration++;

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
		node.var.setIndexSupplier(null);

		return node.var;
	}

	/**
	 * Checks if the list has the given variable
	 */
	public boolean has(Variable var) {
		return varMap.containsKey(var);
	}

	/**
	 * Returns an indexer for the given variable
	 */
	public Supplier<VariableIndex> indexer(Variable var) {
		return varMap.get(var);
	}

	public int size() {
		return varMap.size();
	}

	public Iterator<Variable> iterator() {
		return new Iterator<Variable>() {
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

	/**
	 * @param offset Will offset the indices by the given amount from the supplier
	 */
	public VariableList(IndexType type, IntSupplier offset) {
		this.indexType = type;
		this.offset = offset;
	}
	/**
	 * @param offset Will offset the indices by the given amount
	 */
	public VariableList(IndexType type, int offset) {
		this.indexType = type;
		this.offset = () -> offset;
	}
	/**
	 * @param offset Will offset the indices by the size of the given list
	 */
	public VariableList(IndexType type, VariableList prev) {
		this.indexType = type;
		this.offset = () -> {
			if (prev.offset != null) return prev.offset.getAsInt() + prev.size();
			else return prev.size();
		};
	}
	public VariableList(IndexType type) {
		this.indexType = type;
		this.offset = null;
	}
}
