package me.topchetoeu.jscript.common;

import java.util.HashMap;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;

import me.topchetoeu.jscript.common.parsing.Location;

public class Instruction {
	public static enum Type {
		RETURN(0x00),
		NOP(0x01),
		THROW(0x02),
		THROW_SYNTAX(0x03),
		DELETE(0x04),
		TRY_START(0x05),
		TRY_END(0x06),

		CALL(0x10),
		CALL_NEW(0x12),
		JMP_IF(0x18),
		JMP_IFN(0x19),
		JMP(0x1A),

		PUSH_UNDEFINED(0x20),
		PUSH_NULL(0x21),
		PUSH_BOOL(0x22),
		PUSH_NUMBER(0x23),
		PUSH_STRING(0x24),
		DUP(0x25),
		DISCARD(0x26),

		LOAD_FUNC(0x30),
		LOAD_ARR(0x31),
		LOAD_OBJ(0x32),
		LOAD_REGEX(0x33),

		LOAD_GLOB(0x38),
		LOAD_INTRINSICS(0x39),
		LOAD_ARG(0x3A),
		LOAD_ARGS_N(0x3B),
		LOAD_ARGS(0x3C),
		LOAD_CALLED(0x3D),
		LOAD_THIS(0x3E),
		LOAD_ERROR(0x3F),

		LOAD_VAR(0x40),
		LOAD_MEMBER(0x41),
		LOAD_MEMBER_INT(0x42),
		LOAD_MEMBER_STR(0x43),

		STORE_VAR(0x48),
		STORE_MEMBER(0x49),
		STORE_MEMBER_INT(0x4A),
		STORE_MEMBER_STR(0x4B),

		DEF_PROP(0x50),
		DEF_FIELD(0x51),
		KEYS(0x52),
		TYPEOF(0x53),
		OPERATION(0x54),

		GLOB_GET(0x60),
		GLOB_SET(0x61),
		GLOB_DEF(0x62);

		private static final HashMap<Integer, Type> types = new HashMap<>();
		public final int numeric;

		static {
			for (var val : Type.values()) types.put(val.numeric, val);
		}

		private Type(int numeric) {
			this.numeric = numeric;
		}

		public static Type fromNumeric(int i) {
			return types.get(i);
		}
	}
	public static enum BreakpointType {
		/**
		 * A debugger should never stop at such instruction, unless a breakpoint has been set on it
		 */
		NONE,
		/**
		 * Debuggers should pause at instructions marked with this breakpoint type
		 * after any step command
		 */
		STEP_OVER,
		/**
		 * Debuggers should pause at instructions marked with this breakpoint type
		 * only after a step-in command
		 */
		STEP_IN;

		public boolean shouldStepIn() {
			return this != NONE;
		}
		public boolean shouldStepOver() {
			return this == STEP_OVER;
		}
	}

	public final Type type;
	public final Object[] params;

	@SuppressWarnings("unchecked")
	public <T> T get(int i) {
		if (i >= params.length || i < 0) return null;
		return (T)params[i];
	}

	private Instruction(Type type, Object ...params) {
		this.type = type;
		this.params = params;
	}

	/**
	 * Signals the start of a protected context
	 * @param catchStart The point to witch to jump if an error has been caught
	 * @param finallyStart The point to witch to jump after either the try or catch bodies have exited
	 * @param end The point to which to jump after exiting the whole protected context
	 */
	public static Instruction tryStart(int catchStart, int finallyStart, int end) {
		return new Instruction(Type.TRY_START, catchStart, finallyStart, end);
	}
	/**
	 * Signifies that the current protected section (try, catch or finally) has ended
	 */
	public static Instruction tryEnd() {
		return new Instruction(Type.TRY_END);
	}
	/**
	 * Throws the top stack value
	 */
	public static Instruction throwInstr() {
		return new Instruction(Type.THROW);
	}
	/**
	 * Converts the given exception to a runtime syntax error and throws it
	 */
	public static Instruction throwSyntax(SyntaxException err) {
		return new Instruction(Type.THROW_SYNTAX, err.getMessage());
	}
	/**
	 * Converts the given exception to a runtime syntax error and throws it
	 */
	public static Instruction throwSyntax(String err) {
		return new Instruction(Type.THROW_SYNTAX, err);
	}
	/**
	 * Converts the given exception to a runtime syntax error and throws it
	 */
	public static Instruction throwSyntax(Location loc, String err) {
		return new Instruction(Type.THROW_SYNTAX, new SyntaxException(loc, err).getMessage());
	}
	/**
	 * Performs a JS object property deletion.
	 * Operands:
	 * 1. Object to manipulate
	 * 2. Key to delete
	 */
	public static Instruction delete() {
		return new Instruction(Type.DELETE);
	}
	/**
	 * Returns the top stack value
	 */
	public static Instruction ret() {
		return new Instruction(Type.RETURN);
	}
	/**
	 * A special NOP instruction telling any debugger to pause
	 */
	public static Instruction debug() {
		return new Instruction(Type.NOP, "debug");
	}

	/**
	 * Does nothing. May be used for metadata or implementation-specific instructions that don't alter the behavior
	 */
	public static Instruction nop(Object ...params) {
		return new Instruction(Type.NOP, params);
	}

	public static Instruction call(int argn, boolean hasSelf) {
		return new Instruction(Type.CALL, argn, hasSelf);
	}
	public static Instruction callNew(int argn) {
		return new Instruction(Type.CALL_NEW, argn);
	}

	public static Instruction jmp(int offset) {
		return new Instruction(Type.JMP, offset);
	}
	public static Instruction jmpIf(int offset) {
		return new Instruction(Type.JMP_IF, offset);
	}
	public static Instruction jmpIfNot(int offset) {
		return new Instruction(Type.JMP_IFN, offset);
	}

	public static IntFunction<Instruction> jmp(IntSupplier pos) {
		return i -> new Instruction(Type.JMP, pos.getAsInt() - i);
	}
	public static IntFunction<Instruction> jmpIf(IntSupplier pos) {
		return i -> new Instruction(Type.JMP_IF, pos.getAsInt() - i);
	}
	public static IntFunction<Instruction> jmpIfNot(IntSupplier pos) {
		return i -> new Instruction(Type.JMP_IFN, pos.getAsInt() - i);
	}

	public static Instruction pushUndefined() {
		return new Instruction(Type.PUSH_UNDEFINED);
	}
	public static Instruction pushNull() {
		return new Instruction(Type.PUSH_NULL);
	}
	public static Instruction pushValue(boolean val) {
		return new Instruction(Type.PUSH_BOOL, val);
	}
	public static Instruction pushValue(double val) {
		return new Instruction(Type.PUSH_NUMBER, val);
	}
	public static Instruction pushValue(String val) {
		return new Instruction(Type.PUSH_STRING, val);
	}

	public static Instruction globDef(String name) {
		return new Instruction(Type.GLOB_DEF, name);
	}
	
	public static Instruction globGet(String name, boolean force) {
		return new Instruction(Type.GLOB_GET, name, force);
	}
	public static Instruction globSet(String name, boolean keep, boolean define) {
		return new Instruction(Type.GLOB_SET, name, keep, define);
	}

	public static Instruction loadVar(int i) {
		return new Instruction(Type.LOAD_VAR, i);
	}
	public static Instruction loadThis() {
		return new Instruction(Type.LOAD_THIS);
	}
	/**
	 * Loads the given argument
	 * @param i The index of the argument to load. If -1, will get the index from the stack instead
	 */
	public static Instruction loadArg(int i) {
		return new Instruction(Type.LOAD_ARG, i);
	}
	/**
	 * Pushes the amount of arguments to the stack
	 */
	public static Instruction loadArgsN() {
		return new Instruction(Type.LOAD_ARGS_N);
	}
	/**
	 * Pushes the arguments object to the stack
	 */
	public static Instruction loadArgs() {
		return new Instruction(Type.LOAD_ARGS);
	}
	/**
	 * Loads a reference to the function being called
	 */
	public static Instruction loadCalled() {
		return new Instruction(Type.LOAD_CALLED);
	}
	public static Instruction loadGlob() {
		return new Instruction(Type.LOAD_GLOB);
	}
	public static Instruction loadIntrinsics(String key) {
		return new Instruction(Type.LOAD_INTRINSICS, key);
	}
	public static Instruction loadError() {
		return new Instruction(Type.LOAD_ERROR);
	}
	public static Instruction loadMember() {
		return new Instruction(Type.LOAD_MEMBER);
	}
	public static Instruction loadMember(int member) {
		return new Instruction(Type.LOAD_MEMBER_INT, member);
	}
	public static Instruction loadMember(String member) {
		return new Instruction(Type.LOAD_MEMBER_STR, member);
	}

	public static Instruction loadRegex(String pattern, String flags) {
		return new Instruction(Type.LOAD_REGEX, pattern, flags);
	}
	// TODO: make this capturing a concern of the compiler
	public static Instruction loadFunc(int id, String name, int[] captures) {
		var args = new Object[2 + captures.length];
		args[0] = id;
		args[1] = name;
		for (var i = 0; i < captures.length; i++) args[i + 2] = captures[i];
		return new Instruction(Type.LOAD_FUNC, args);
	}
	public static Instruction loadObj() {
		return new Instruction(Type.LOAD_OBJ);
	}
	public static Instruction loadArr(int count) {
		return new Instruction(Type.LOAD_ARR, count);
	}
	public static Instruction dup() {
		return new Instruction(Type.DUP, 1, 0);
	}
	public static Instruction dup(int count, int offset) {
		return new Instruction(Type.DUP, count, offset);
	}

	public static Instruction storeVar(int i, boolean keep, boolean initialize) {
		return new Instruction(Type.STORE_VAR, i, keep, initialize);
	}

	public static Instruction storeMember() {
		return new Instruction(Type.STORE_MEMBER, false);
	}
	public static Instruction storeMember(boolean keep) {
		return new Instruction(Type.STORE_MEMBER, keep);
	}

	public static Instruction storeMember(String key) {
		return new Instruction(Type.STORE_MEMBER_STR, key, false);
	}
	public static Instruction storeMember(String key, boolean keep) {
		return new Instruction(Type.STORE_MEMBER_STR, key, keep);
	}

	public static Instruction storeMember(int key) {
		return new Instruction(Type.STORE_MEMBER_INT, key, false);
	}
	public static Instruction storeMember(int key, boolean keep) {
		return new Instruction(Type.STORE_MEMBER_INT, key, keep);
	}

	public static Instruction discard() {
		return new Instruction(Type.DISCARD);
	}

	public static Instruction typeof() {
		return new Instruction(Type.TYPEOF);
	}
	public static Instruction typeof(String varName) {
		return new Instruction(Type.TYPEOF, varName);
	}

	public static Instruction keys(boolean own, boolean onlyEnumerable) {
		return new Instruction(Type.KEYS, own, onlyEnumerable);
	}

	public static Instruction defProp(boolean setter) {
		return new Instruction(Type.DEF_PROP, setter);
	}
	public static Instruction defField() {
		return new Instruction(Type.DEF_FIELD);
	}

	public static Instruction operation(Operation op) {
		return new Instruction(Type.OPERATION, op);
	}

	@Override public String toString() {
		var res = type.toString();

		for (int i = 0; i < params.length; i++) {
			res += " " + params[i];
		}

		return res;
	}
}
