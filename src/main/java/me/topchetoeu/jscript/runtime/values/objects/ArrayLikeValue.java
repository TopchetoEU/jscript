package me.topchetoeu.jscript.runtime.values.objects;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.values.KeyCache;
import me.topchetoeu.jscript.runtime.values.Member;
import me.topchetoeu.jscript.runtime.values.Member.FieldMember;
import me.topchetoeu.jscript.runtime.values.primitives.numbers.NumberValue;
import me.topchetoeu.jscript.runtime.values.Value;

public abstract class ArrayLikeValue extends ObjectValue {
	private static class IndexField extends FieldMember {
		private int i;
		private ArrayLikeValue arr;

		@Override public Value get(Environment env, Value self) {
			return arr.get(i);
		}
		@Override public boolean set(Environment env, Value val, Value self) {
			return arr.set(env, i, val);
		}
		public IndexField(int i, ArrayLikeValue arr) {
			super(arr, true, true, true);
			this.arr = arr;
			this.i = i;
		}
	}

	private final FieldMember lengthField = new FieldMember(this, false, false, true) {
		@Override public Value get(Environment env, Value self) {
			return NumberValue.of(size());
		}
		@Override public boolean set(Environment env, Value val, Value self) {
			var num = val.toNumber(env);
			if (!num.isInt()) throw EngineException.ofRange("Invalid array length");

			var i = num.getInt();
			if (i < 0) throw EngineException.ofRange("Invalid array length");

			return setSize(i);
		}
	};

	public abstract int size();
	public abstract boolean setSize(int val);

	public abstract Value get(int i);
	public abstract boolean set(Environment env, int i, Value val);
	public abstract boolean has(int i);
	public abstract boolean remove(int i);

	@Override public Member getOwnMember(Environment env, KeyCache key) {
		var res = super.getOwnMember(env, key);
		if (res != null) return res;
		if (key.isSymbol()) return null;

		var num = key.toNumber(env);
		var i = key.toInt(env);

		if (i == num && i >= 0 && i < size() && has(i)) return new IndexField(i, this);
		else if (!key.isSymbol() && key.toString(env).equals("length")) return lengthField;
		else return null;
	}
	@Override public boolean defineOwnField(
		Environment env, KeyCache key, Value val,
		Boolean writable, Boolean enumerable, Boolean configurable
	) {
		if (!getState().writable) return false;

		if (!key.isSymbol()) {
			var num = key.toNumber(env);
			var i = key.toInt(env);

			if (i == num) {
				if (writable == null) writable = true;
				if (configurable == null) configurable = true;
				if (enumerable == null) enumerable = true;

				if (writable && configurable && enumerable) {
					if (!getState().extendable && !has(i)) return false;
					if (set(env, i, val)) return true;
				}
			}
		}

		return super.defineOwnField(env, key, val, writable, enumerable, configurable);
	}
	@Override public boolean deleteOwnMember(Environment env, KeyCache key) {
		if (!super.deleteOwnMember(env, key)) return false;
		if (key.isSymbol()) return true;

		var num = key.toNumber(env);
		var i = key.toInt(env);

		if (i == num && i >= 0 && i < size()) return remove(i);
		else return true;
	}

	@Override public Set<String> getOwnMembers(Environment env, boolean onlyEnumerable) {
		var res = new LinkedHashSet<String>();

		res.addAll(super.getOwnMembers(env, onlyEnumerable));

		for (var i = 0; i < size(); i++) {
			if (has(i)) res.add(i + "");
		}

		if (!onlyEnumerable) res.add("length");

		return res;
	}

	private LinkedList<String> toReadableBase(Environment env, HashSet<ObjectValue> passed, HashSet<String> ignoredKeys) {
		var stringified = new LinkedList<LinkedList<String>>();

		passed.add(this);

		var emptyN = 0;

		for (int i = 0; i < size(); i++) {
			if (has(i)) {
				String emptyStr = null;

				if (emptyN == 1) emptyStr = "<empty>";
				else if (emptyN > 1) emptyStr = "<empty x " + emptyN + ">";

				if (emptyStr != null) stringified.add(new LinkedList<>(Arrays.asList(emptyStr + ",")));
				emptyN = 0;

				stringified.add(new LinkedList<>(get(i).toReadableLines(env, passed)));
				ignoredKeys.add(i + "");

				var entry = stringified.getLast();
				entry.set(entry.size() - 1, entry.getLast() + ",");
			}
			else {
				emptyN++;
			}
		}

		String emptyStr = null;

		if (emptyN == 1) emptyStr = "<empty>";
		else if (emptyN > 1) emptyStr = "<empty x " + emptyN + ">";

		if (emptyStr != null) stringified.add(new LinkedList<>(Arrays.asList(emptyStr)));
		else if (stringified.size() > 0) {
			var lastEntry = stringified.getLast();
			lastEntry.set(lastEntry.size() - 1, lastEntry.getLast().substring(0, lastEntry.getLast().length() - 1));
		}


		passed.remove(this);

		if (stringified.size() == 0) return new LinkedList<>(Arrays.asList("[]"));
		var concat = new StringBuilder();
		for (var entry : stringified) {
			// We make a one-liner only when all members are one-liners
			if (entry.size() != 1) {
				concat = null;
				break;
			}

			if (concat.length() != 0) concat.append(" ");
			concat.append(entry.get(0));
		}

		// We don't want too long one-liners
		if (concat != null && concat.length() < 160) return new LinkedList<>(Arrays.asList("[" + concat.toString() + "]"));

		var res = new LinkedList<String>();

		res.add("[");

		for (var entry : stringified) {
			for (var line : entry) {
				res.add("    " + line);
			}
		}
		res.set(res.size() - 1, res.getLast().substring(0, res.getLast().length() - 1));
		res.add("]");

		return res;
	}

	@Override public List<String> toReadableLines(Environment env, HashSet<ObjectValue> passed) {
		var ignored = new HashSet<String>();
		var lines = toReadableBase(env, passed, ignored);

		var superLines = new LinkedList<String>(super.toReadableLines(env, passed, ignored));
		if (superLines.size() == 1 && superLines.getFirst().equals("{}")) return lines;

		lines.set(lines.size() - 1, lines.getLast() + " " + superLines.getFirst());
		lines.addAll(superLines.subList(1, superLines.size()));

		return lines;
	}
}
