package me.topchetoeu.jscript.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Operation;
import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.functions.CodeFunction;
import me.topchetoeu.jscript.runtime.values.functions.FunctionValue;
import me.topchetoeu.jscript.runtime.values.objects.ArrayValue;
import me.topchetoeu.jscript.runtime.values.objects.ObjectValue;
import me.topchetoeu.jscript.runtime.values.primitives.BoolValue;
import me.topchetoeu.jscript.runtime.values.primitives.StringValue;
import me.topchetoeu.jscript.runtime.values.primitives.numbers.NumberValue;

public class InstructionRunner {
	private static Value execReturn(Environment env, Instruction instr, Frame frame) {
		return frame.pop();
	}
	private static Value execThrow(Environment env, Instruction instr, Frame frame) {
		throw new EngineException(frame.pop());
	}
	private static Value execThrowSyntax(Environment env, Instruction instr, Frame frame) {
		throw EngineException.ofSyntax((String)instr.get(0));
	}

	private static Value execCall(Environment env, Instruction instr, Frame frame) {
		var callArgs = frame.take(instr.get(0));
		var func = frame.pop();
		var self = (boolean)instr.get(1) ? frame.pop() : Value.UNDEFINED;

		frame.push(func.apply(env, self, callArgs));

		frame.codePtr++;
		return null;
	}
	private static Value execCallNew(Environment env, Instruction instr, Frame frame) {
		var callArgs = frame.take(instr.get(0));
		var funcObj = frame.pop();

		frame.push(funcObj.constructNoSelf(env, callArgs));

		frame.codePtr++;
		return null;
	}

	private static Value execDefProp(Environment env, Instruction instr, Frame frame) {
		var val = frame.pop();
		var key = frame.pop();
		var obj = frame.pop();

		FunctionValue accessor;

		if (val == Value.UNDEFINED) accessor = null;
		else if (val instanceof FunctionValue func) accessor = func;
		else throw EngineException.ofType("Getter must be a function or undefined");

		if ((boolean)instr.get(0)) obj.defineOwnProperty(env, key, null, Optional.of(accessor), true, true);
		else obj.defineOwnProperty(env, key, Optional.of(accessor), null, true, true);

		frame.codePtr++;
		return null;
	}
	private static Value execDefField(Environment env, Instruction instr, Frame frame) {
		var val = frame.pop();
		var key = frame.pop();
		var obj = frame.pop();

		obj.defineOwnField(env, key, val, true, true, true);

		frame.codePtr++;
		return null;
	}
	private static Value execKeys(Environment env, Instruction instr, Frame frame) {
		var val = frame.pop();

		var members = new ArrayList<>(val.getMembers(env, instr.get(0), instr.get(1)));
		Collections.reverse(members);

		frame.push(Value.UNDEFINED);

		for (var el : members) {
			var obj = new ObjectValue();
			obj.defineOwnField(env, "value", StringValue.of(el));
			frame.push(obj);
		}

		frame.codePtr++;
		return null;
	}

	private static Value execTryStart(Environment env, Instruction instr, Frame frame) {
		int start = frame.codePtr + 1;
		int catchStart = (int)instr.get(0);
		int finallyStart = (int)instr.get(1);
		if (finallyStart >= 0) finallyStart += start;
		if (catchStart >= 0) catchStart += start;
		int end = (int)instr.get(2) + start;
		frame.addTry(start, end, catchStart, finallyStart);
		frame.codePtr++;
		return null;
	}
	private static Value execTryEnd(Environment env, Instruction instr, Frame frame) {
		frame.popTryFlag = true;
		return null;
	}

	private static Value execDup(Environment env, Instruction instr, Frame frame) {
		int count = instr.get(0);
		int offset = instr.get(1);

		var el = frame.stack[frame.stackPtr - offset - 1];

		for (var i = 0; i < count; i++) {
			frame.push(el);
		}

		frame.codePtr++;
		return null;
	}
	private static Value execLoadValue(Environment env, Instruction instr, Frame frame) {
		switch (instr.type) {
			case PUSH_UNDEFINED: frame.push(Value.UNDEFINED); break;
			case PUSH_NULL: frame.push(Value.NULL); break;
			case PUSH_BOOL: frame.push(BoolValue.of(instr.get(0))); break;
			case PUSH_NUMBER: frame.push(NumberValue.of((double)instr.get(0))); break;
			case PUSH_STRING: frame.push(StringValue.of(instr.get(0))); break;
			default:
		}

		frame.codePtr++;
		return null;
	}
	private static Value execLoadVar(Environment env, Instruction instr, Frame frame) {
		int i = instr.get(0);

		frame.push(frame.getVar(i));
		frame.codePtr++;

		return null;
	}
	private static Value execLoadObj(Environment env, Instruction instr, Frame frame) {
		var obj = new ObjectValue();
		obj.setPrototype(Value.OBJECT_PROTO);
		frame.push(obj);
		frame.codePtr++;
		return null;
	}
	private static Value execLoadGlob(Environment env, Instruction instr, Frame frame) {
		frame.push(Value.global(env));
		frame.codePtr++;
		return null;
	}
	private static Value execLoadIntrinsics(Environment env, Instruction instr, Frame frame) {
		frame.push(Value.intrinsics(env).get((String)instr.get(0)));
		frame.codePtr++;
		return null;
	}
	private static Value execLoadArr(Environment env, Instruction instr, Frame frame) {
		var res = new ArrayValue();
		res.setSize(instr.get(0));
		frame.push(res);
		frame.codePtr++;
		return null;
	}
	private static Value execLoadFunc(Environment env, Instruction instr, Frame frame) {
		int id = instr.get(0);
		String name = instr.get(1);

		var captures = new Value[instr.params.length - 2][];

		for (var i = 2; i < instr.params.length; i++) {
			captures[i - 2] = frame.captureVar(instr.get(i));
		}

		var func = new CodeFunction(env, name, frame.function.body.children[id], captures);

		frame.push(func);

		frame.codePtr++;
		return null;
	}
	private static Value execLoadMember(Environment env, Instruction instr, Frame frame) {
		var key = frame.pop();

		try {
			var top = frame.stackPtr - 1;
			frame.stack[top] = frame.stack[top].getMember(env, key);
		}
		catch (IllegalArgumentException e) {
			throw EngineException.ofType(e.getMessage());
		}
		frame.codePtr++;
		return null;
	}
	private static Value execLoadMemberInt(Environment env, Instruction instr, Frame frame) {
		try {
			var top = frame.stackPtr - 1;
			frame.stack[top] = frame.stack[top].getMember(env, (int)instr.get(0));
		}
		catch (IllegalArgumentException e) {
			throw EngineException.ofType(e.getMessage());
		}
		frame.codePtr++;
		return null;
	}
	private static Value execLoadMemberStr(Environment env, Instruction instr, Frame frame) {
		try {
			var top = frame.stackPtr - 1;
			frame.stack[top] = frame.stack[top].getMember(env, (String)instr.get(0));
		}
		catch (IllegalArgumentException e) {
			throw EngineException.ofType(e.getMessage());
		}
		frame.codePtr++;
		return null;
	}
	private static Value execLoadRegEx(Environment env, Instruction instr, Frame frame) {
		if (env.hasNotNull(Value.REGEX_CONSTR)) {
			frame.push(env.get(Value.REGEX_CONSTR).constructNoSelf(env,
				StringValue.of(instr.get(0)),
				StringValue.of(instr.get(1))
			));
		}
		else {
			throw EngineException.ofSyntax("Regex is not supported");
		}

		frame.codePtr++;
		return null;
	}

	private static Value execDiscard(Environment env, Instruction instr, Frame frame) {
		frame.pop();
		frame.codePtr++;
		return null;
	}
	private static Value execStoreMember(Environment env, Instruction instr, Frame frame) {
		var val = frame.pop();
		var key = frame.pop();
		var obj = frame.pop();

		if (!obj.setMember(env, key, val)) throw EngineException.ofSyntax("Can't set member '" + key.toReadable(env) + "'");
		if ((boolean)instr.get(0)) frame.push(val);
		frame.codePtr++;
		return null;
	}
	private static Value execStoreMemberStr(Environment env, Instruction instr, Frame frame) {
		var val = frame.pop();
		var obj = frame.pop();

		if (!obj.setMember(env, (String)instr.get(0), val)) throw EngineException.ofSyntax("Can't set member '" + instr.get(0) + "'");
		if ((boolean)instr.get(1)) frame.push(val);
		frame.codePtr++;
		return null;
	}
	private static Value execStoreMemberInt(Environment env, Instruction instr, Frame frame) {
		var val = frame.pop();
		var obj = frame.pop();

		if (!obj.setMember(env, (int)instr.get(0), val)) throw EngineException.ofSyntax("Can't set member '" + instr.get(0) + "'");
		if ((boolean)instr.get(1)) frame.push(val);
		frame.codePtr++;
		return null;
	}
	private static Value execStoreVar(Environment env, Instruction instr, Frame frame) {
		var val = (boolean)instr.get(1) ? frame.peek() : frame.pop();
		int i = instr.get(0);

		frame.setVar(i, val);
		frame.codePtr++;

		return null;
	}

	private static Value execJmp(Environment env, Instruction instr, Frame frame) {
		frame.codePtr += (int)instr.get(0);
		frame.jumpFlag = true;
		return null;
	}
	private static Value execJmpIf(Environment env, Instruction instr, Frame frame) {
		if (frame.pop().toBoolean()) {
			frame.codePtr += (int)instr.get(0);
			frame.jumpFlag = true;
		}
		else frame.codePtr ++;
		return null;
	}
	private static Value execJmpIfNot(Environment env, Instruction instr, Frame frame) {
		if (!frame.pop().toBoolean()) {
			frame.codePtr += (int)instr.get(0);
			frame.jumpFlag = true;
		}
		else frame.codePtr ++;
		return null;
	}

	private static Value execTypeof(Environment env, Instruction instr, Frame frame) {
		String name = instr.get(0);
		Value obj;

		if (name != null) obj = Value.global(env).getMember(env, name);
		else obj = frame.pop();

		frame.push(obj.type());

		frame.codePtr++;
		return null;
	}
	private static Value execNop(Environment env, Instruction instr, Frame frame) {
		frame.codePtr++;
		return null;
	}

	private static Value execDelete(Environment env, Instruction instr, Frame frame) {
		var key = frame.pop();
		var val = frame.pop();

		if (!val.deleteMember(env, key)) throw EngineException.ofSyntax("Can't delete member '" + key.toReadable(env) + "'");
		frame.codePtr++;
		return null;
	}

	private static Value execOperation(Environment env, Instruction instr, Frame frame) {
		Operation op = instr.get(0);
		Value res;
		var stack = frame.stack;

		frame.stackPtr -= 1;
		var ptr = frame.stackPtr;

		switch (op) {
			case ADD:
				res = Value.add(env, stack[ptr - 1], stack[ptr]);
				break;
			case SUBTRACT:
				res = Value.subtract(env, stack[ptr - 1], stack[ptr]);
				break;
			case DIVIDE:
				res = Value.divide(env, stack[ptr - 1], stack[ptr]);
				break;
			case MULTIPLY:
				res = Value.multiply(env, stack[ptr - 1], stack[ptr]);
				break;
			case MODULO:
				res = Value.modulo(env, stack[ptr - 1], stack[ptr]);
				break;

			case AND:
				res = Value.and(env, stack[ptr - 1], stack[ptr]);
				break;
			case OR:
				res = Value.or(env, stack[ptr - 1], stack[ptr]);
				break;
			case XOR:
				res = Value.xor(env, stack[ptr - 1], stack[ptr]);
				break;

			case EQUALS:
				res = BoolValue.of(stack[ptr - 1].equals(stack[ptr]));
				break;
			case NOT_EQUALS:
				res = BoolValue.of(!stack[ptr - 1].equals(stack[ptr]));
				break;
			case LOOSE_EQUALS:
				res = BoolValue.of(Value.looseEqual(env, stack[ptr - 1], stack[ptr]));
				break;
			case LOOSE_NOT_EQUALS:
				res = BoolValue.of(!Value.looseEqual(env, stack[ptr - 1], stack[ptr]));
				break;

			case GREATER:
				res = BoolValue.of(Value.greater(env, stack[ptr - 1], stack[ptr]));
				break;
			case GREATER_EQUALS:
				res = BoolValue.of(Value.greaterOrEqual(env, stack[ptr - 1], stack[ptr]));
				break;
			case LESS:
				res = BoolValue.of(Value.less(env, stack[ptr - 1], stack[ptr]));
				break;
			case LESS_EQUALS:
				res = BoolValue.of(Value.lessOrEqual(env, stack[ptr - 1], stack[ptr]));
				break;

			case INVERSE:
				res = Value.bitwiseNot(env, stack[ptr++]);
				frame.stackPtr++;
				break;
			case NOT:
				res = BoolValue.of(!stack[ptr++].toBoolean());
				frame.stackPtr++;
				break;
			case POS:
				res = stack[ptr++].toNumber(env);
				frame.stackPtr++;
				break;
			case NEG:
				res = Value.negative(env, stack[ptr++]);
				frame.stackPtr++;
				break;

			case SHIFT_LEFT:
				res = Value.shiftLeft(env, stack[ptr - 1], stack[ptr]);
				break;
			case SHIFT_RIGHT:
				res = Value.shiftRight(env, stack[ptr - 1], stack[ptr]);
				break;
			case USHIFT_RIGHT:
				res = Value.unsignedShiftRight(env, stack[ptr - 1], stack[ptr]);
				break;

			case IN:
				res = BoolValue.of(stack[ptr - 1].hasMember(env, stack[ptr], false));
				break;
			case INSTANCEOF:
				res = BoolValue.of(stack[ptr - 1].isInstanceOf(env, stack[ptr].getMember(env, StringValue.of("prototype"))));
				break;

			default: return null;
		}

		stack[ptr - 1] = res;
		frame.codePtr++;
		return null;
	}

	private static Value execGlobDef(Environment env, Instruction instr, Frame frame) {
		var name = (String)instr.get(0);

		if (!Value.global(env).hasMember(env, name, false)) {
			if (!Value.global(env).defineOwnField(env, name, Value.UNDEFINED)) throw EngineException.ofError("Couldn't define variable " + name);
		}

		frame.codePtr++;
		return null;
	}
	private static Value execGlobGet(Environment env, Instruction instr, Frame frame) {
		var name = (String)instr.get(0);
		if ((boolean)instr.get(1)) {
			frame.push(Value.global(env).getMember(env, name));
		}
		else {
			var res = Value.global(env).getMemberOrNull(env, name);

			if (res == null) throw EngineException.ofSyntax(name + " is not defined");
			else frame.push(res);
		}

		frame.codePtr++;
		return null;
	}
	private static Value execGlobSet(Environment env, Instruction instr, Frame frame) {
		var name = (String)instr.get(0);
		var keep = (boolean)instr.get(1);
		var define = (boolean)instr.get(2);

		var val = keep ? frame.peek() : frame.pop();
		var res = false;

		if (define) res = Value.global(env).setMember(env, name, val);
		else res = Value.global(env).setMemberIfExists(env, name, val);

		if (!res) throw EngineException.ofError("Couldn't set variable " + name);

		frame.codePtr++;
		return null;
	}

	private static Value execLoadArg(Environment env, Instruction instr, Frame frame) {
		int i = instr.get(0);
		if (i >= frame.args.length) frame.push(Value.UNDEFINED);
		else frame.push(frame.args[i]);
		frame.codePtr++;
		return null;
	}
	private static Value execLoadArgsN(Environment env, Instruction instr, Frame frame) {
		frame.push(frame.argsLen);
		frame.codePtr++;
		return null;
	}
	private static Value execLoadArgs(Environment env, Instruction instr, Frame frame) {
		frame.push(frame.argsVal);
		frame.codePtr++;
		return null;
	}
	private static Value execLoadCallee(Environment env, Instruction instr, Frame frame) {
		frame.push(frame.function);
		frame.codePtr++;
		return null;
	}
	private static Value execLoadThis(Environment env, Instruction instr, Frame frame) {
		if (frame.self == null) throw EngineException.ofError("Super constructor must be called before 'this' is accessed");
		frame.push(frame.self);
		frame.codePtr++;
		return null;
	}
	private static Value execLoadError(Environment env, Instruction instr, Frame frame) {
		frame.push(frame.tryStack.peek().error.value);
		frame.codePtr++;
		return null;
	}

	public static Value exec(Environment env, Instruction instr, Frame frame) {
		switch (instr.type) {
			case NOP: return execNop(env, instr, frame);
			case RETURN: return execReturn(env, instr, frame);
			case THROW: return execThrow(env, instr, frame);
			case THROW_SYNTAX: return execThrowSyntax(env, instr, frame);
			case CALL: return execCall(env, instr, frame);
			case CALL_NEW: return execCallNew(env, instr, frame);
			case TRY_START: return execTryStart(env, instr, frame);
			case TRY_END: return execTryEnd(env, instr, frame);

			case DUP: return execDup(env, instr, frame);
			case PUSH_UNDEFINED:
			case PUSH_NULL:
			case PUSH_STRING:
			case PUSH_NUMBER:
			case PUSH_BOOL:
				return execLoadValue(env, instr, frame);
			case LOAD_VAR: return execLoadVar(env, instr, frame);
			case LOAD_OBJ: return execLoadObj(env, instr, frame);
			case LOAD_ARR: return execLoadArr(env, instr, frame);
			case LOAD_FUNC: return execLoadFunc(env, instr, frame);
			case LOAD_MEMBER: return execLoadMember(env, instr, frame);
			case LOAD_MEMBER_INT: return execLoadMemberInt(env, instr, frame);
			case LOAD_MEMBER_STR: return execLoadMemberStr(env, instr, frame);
			case LOAD_REGEX: return execLoadRegEx(env, instr, frame);
			case LOAD_GLOB: return execLoadGlob(env, instr, frame);
			case LOAD_INTRINSICS: return execLoadIntrinsics(env, instr, frame);
			case LOAD_ERROR: return execLoadError(env, instr, frame);

			case LOAD_THIS: return execLoadThis(env, instr, frame);
			case LOAD_ARG: return execLoadArg(env, instr, frame);
			case LOAD_ARGS: return execLoadArgs(env, instr, frame);
			case LOAD_ARGS_N: return execLoadArgsN(env, instr, frame);
			case LOAD_CALLED: return execLoadCallee(env, instr, frame);

			case DISCARD: return execDiscard(env, instr, frame);
			case STORE_MEMBER: return execStoreMember(env, instr, frame);
			case STORE_MEMBER_STR: return execStoreMemberStr(env, instr, frame);
			case STORE_MEMBER_INT: return execStoreMemberInt(env, instr, frame);
			case STORE_VAR: return execStoreVar(env, instr, frame);

			case KEYS: return execKeys(env, instr, frame);
			case DEF_PROP: return execDefProp(env, instr, frame);
			case DEF_FIELD: return execDefField(env, instr, frame);
			case TYPEOF: return execTypeof(env, instr, frame);
			case DELETE: return execDelete(env, instr, frame);

			case JMP: return execJmp(env, instr, frame);
			case JMP_IF: return execJmpIf(env, instr, frame);
			case JMP_IFN: return execJmpIfNot(env, instr, frame);

			case OPERATION: return execOperation(env, instr, frame);

			case GLOB_DEF: return execGlobDef(env, instr, frame);
			case GLOB_GET: return execGlobGet(env, instr, frame);
			case GLOB_SET: return execGlobSet(env, instr, frame);

			default: throw EngineException.ofSyntax("Invalid instruction " + instr.type.name() + "");
		}
	}
}
