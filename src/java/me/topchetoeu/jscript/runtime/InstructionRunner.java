package me.topchetoeu.jscript.runtime;

import java.util.ArrayList;
import java.util.Collections;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Operation;
import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.values.Member.FieldMember;
import me.topchetoeu.jscript.runtime.values.Member.PropertyMember;
import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.functions.CodeFunction;
import me.topchetoeu.jscript.runtime.values.functions.FunctionValue;
import me.topchetoeu.jscript.runtime.values.objects.ArrayValue;
import me.topchetoeu.jscript.runtime.values.objects.ObjectValue;
import me.topchetoeu.jscript.runtime.values.primitives.BoolValue;
import me.topchetoeu.jscript.runtime.values.primitives.NumberValue;
import me.topchetoeu.jscript.runtime.values.primitives.StringValue;

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

        frame.push(func.call(env, false, instr.get(1), Value.UNDEFINED, callArgs));

        frame.codePtr++;
        return null;
    }
    private static Value execCallMember(Environment env, Instruction instr, Frame frame) {
        var callArgs = frame.take(instr.get(0));
        var index = frame.pop();
        var obj = frame.pop();

        frame.push(obj.getMember(env, index).call(env, false, instr.get(1), obj, callArgs));

        frame.codePtr++;
        return null;
    }
    private static Value execCallNew(Environment env, Instruction instr, Frame frame) {
        var callArgs = frame.take(instr.get(0));
        var funcObj = frame.pop();

        frame.push(funcObj.callNew(env, instr.get(1), callArgs));

        frame.codePtr++;
        return null;
    }

    private static Value execDefProp(Environment env, Instruction instr, Frame frame) {
        var setterVal = frame.pop();
        var getterVal = frame.pop();
        var key = frame.pop();
        var obj = frame.pop();

        FunctionValue getter, setter;

        if (getterVal == Value.UNDEFINED) getter = null;
        else if (getterVal instanceof FunctionValue) getter = (FunctionValue)getterVal;
        else throw EngineException.ofType("Getter must be a function or undefined.");

        if (setterVal == Value.UNDEFINED) setter = null;
        else if (setterVal instanceof FunctionValue) setter = (FunctionValue)setterVal;
        else throw EngineException.ofType("Setter must be a function or undefined.");

        obj.defineOwnMember(env, key, new PropertyMember(getter, setter, true, true));

        frame.push(obj);
        frame.codePtr++;
        return null;
    }
    private static Value execKeys(Environment env, Instruction instr, Frame frame) {
        var val = frame.pop();

        var members = new ArrayList<>(val.getMembers(env, false, true).keySet());
        Collections.reverse(members);

        frame.push(null);

        for (var el : members) {
            var obj = new ObjectValue();
            obj.defineOwnMember(env, "value", FieldMember.of(new StringValue(el)));
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

        for (var i = 0; i < count; i++) {
            frame.push(frame.peek(count - 1));
        }

        frame.codePtr++;
        return null;
    }
    private static Value execLoadValue(Environment env, Instruction instr, Frame frame) {
        switch (instr.type) {
            case PUSH_UNDEFINED: frame.push(Value.UNDEFINED); break;
            case PUSH_NULL: frame.push(Value.NULL); break;
            case PUSH_BOOL: frame.push(BoolValue.of(instr.get(0))); break;
            case PUSH_NUMBER: frame.push(new NumberValue(instr.get(0))); break;
            case PUSH_STRING: frame.push(new StringValue(instr.get(0))); break;
            default:
        }

        frame.codePtr++;
        return null;
    }
    private static Value execLoadVar(Environment env, Instruction instr, Frame frame) {
        int i = instr.get(0);

        frame.push(frame.getVar(i)[0]);
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
            captures[i - 2] = frame.getVar(instr.get(i));
        }

        var func = new CodeFunction(env, name, frame.function.body.children[id], captures);

        frame.push(func);

        frame.codePtr++;
        return null;
    }
    private static Value execLoadMember(Environment env, Instruction instr, Frame frame) {
        var key = frame.pop();
        var obj = frame.pop();

        try {
            frame.push(obj.getMember(env, key));
        }
        catch (IllegalArgumentException e) {
            throw EngineException.ofType(e.getMessage());
        }
        frame.codePtr++;
        return null;
    }
    private static Value execLoadRegEx(Environment env, Instruction instr, Frame frame) {
        if (env.hasNotNull(Value.REGEX_CONSTR)) {
            frame.push(env.get(Value.REGEX_CONSTR).callNew(env, instr.get(0), instr.get(1)));
        }
        else {
            throw EngineException.ofSyntax("Regex is not supported.");
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

        if (!obj.setMember(env, key, val)) throw EngineException.ofSyntax("Can't set member '" + key.toReadable(env) + "'.");
        if ((boolean)instr.get(0)) frame.push(val);
        frame.codePtr++;
        return null;
    }
    private static Value execStoreVar(Environment env, Instruction instr, Frame frame) {
        var val = (boolean)instr.get(1) ? frame.peek() : frame.pop();
        int i = instr.get(0);

        frame.getVar(i)[0] = val;
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

        if (!val.deleteMember(env, key)) throw EngineException.ofSyntax("Can't delete member '" + key.toReadable(env) + "'.");
        frame.codePtr++;
        return null;
    }

    private static Value execOperation(Environment env, Instruction instr, Frame frame) {
        Operation op = instr.get(0);
        var args = new Value[op.operands];

        for (var i = op.operands - 1; i >= 0; i--) args[i] = frame.pop();

        frame.push(Value.operation(env, op, args));
        frame.codePtr++;
        return null;
    }

    private static Value exexGlobDef(Environment env, Instruction instr, Frame frame) {
        var name = (String)instr.get(0);

        if (!Value.global(env).hasMember(env, name, false)) {
            if (!Value.global(env).defineOwnMember(env, name, Value.UNDEFINED)) throw EngineException.ofError("Couldn't define variable " + name);
        }

        frame.codePtr++;
        return null;
    }
    private static Value exexGlobGet(Environment env, Instruction instr, Frame frame) {
        var name = (String)instr.get(0);
        var res = Value.global(env).getMemberOrNull(env, name);

        if (res == null) throw EngineException.ofSyntax(name + " is not defined");
        else frame.push(res);

        frame.codePtr++;
        return null;
    }
    private static Value exexGlobSet(Environment env, Instruction instr, Frame frame) {
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

    private static Value execLoadArgs(Environment env, Instruction instr, Frame frame) {
        frame.push(frame.argsVal);
        frame.codePtr++;
        return null;
    }
    private static Value execLoadThis(Environment env, Instruction instr, Frame frame) {
        frame.push(frame.self);
        frame.codePtr++;
        return null;
    }

    private static Value execStackAlloc(Environment env, Instruction instr, Frame frame) {
        int n = instr.get(0);

        for (var i = 0; i < n; i++) frame.locals.add(new Value[] { Value.UNDEFINED });
        
        frame.codePtr++;
        return null;
    }
    private static Value execStackFree(Environment env, Instruction instr, Frame frame) {
        int n = instr.get(0);

        for (var i = 0; i < n; i++) {
            frame.locals.remove(frame.locals.size() - 1);
        }

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
            case CALL_MEMBER: return execCallMember(env, instr, frame);
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
            case LOAD_REGEX: return execLoadRegEx(env, instr, frame);
            case LOAD_GLOB: return execLoadGlob(env, instr, frame);
            case LOAD_INTRINSICS: return execLoadIntrinsics(env, instr, frame);
            case LOAD_ARGS: return execLoadArgs(env, instr, frame);
            case LOAD_THIS: return execLoadThis(env, instr, frame);

            case DISCARD: return execDiscard(env, instr, frame);
            case STORE_MEMBER: return execStoreMember(env, instr, frame);
            case STORE_VAR: return execStoreVar(env, instr, frame);

            case KEYS: return execKeys(env, instr, frame);
            case DEF_PROP: return execDefProp(env, instr, frame);
            case TYPEOF: return execTypeof(env, instr, frame);
            case DELETE: return execDelete(env, instr, frame);

            case JMP: return execJmp(env, instr, frame);
            case JMP_IF: return execJmpIf(env, instr, frame);
            case JMP_IFN: return execJmpIfNot(env, instr, frame);

            case OPERATION: return execOperation(env, instr, frame);

            case GLOB_DEF: return exexGlobDef(env, instr, frame);
            case GLOB_GET: return exexGlobGet(env, instr, frame);
            case GLOB_SET: return exexGlobSet(env, instr, frame);

            case STACK_ALLOC: return execStackAlloc(env, instr, frame);
            case STACK_FREE: return execStackFree(env, instr, frame);

            default: throw EngineException.ofSyntax("Invalid instruction " + instr.type.name() + ".");
        }
    }
}
