package me.topchetoeu.jscript.core;

import java.util.Collections;

import me.topchetoeu.jscript.core.scope.ValueVariable;
import me.topchetoeu.jscript.core.values.ArrayValue;
import me.topchetoeu.jscript.core.values.CodeFunction;
import me.topchetoeu.jscript.core.values.FunctionValue;
import me.topchetoeu.jscript.core.values.ObjectValue;
import me.topchetoeu.jscript.core.values.Symbol;
import me.topchetoeu.jscript.core.values.Values;
import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Operation;
import me.topchetoeu.jscript.core.exceptions.EngineException;

public class InstructionRunner {
    private static Object execReturn(Context ctx, Instruction instr, Frame frame) {
        return frame.pop();
    }
    private static Object execThrow(Context ctx, Instruction instr, Frame frame) {
        throw new EngineException(frame.pop());
    }
    private static Object execThrowSyntax(Context ctx, Instruction instr, Frame frame) {
        throw EngineException.ofSyntax((String)instr.get(0));
    }

    private static Object execCall(Context ctx, Instruction instr, Frame frame) {
        var callArgs = frame.take(instr.get(0));
        var func = frame.pop();
        var thisArg = frame.pop();

        frame.push(Values.call(ctx, func, thisArg, callArgs));

        frame.codePtr++;
        return Values.NO_RETURN;
    }
    private static Object execCallNew(Context ctx, Instruction instr, Frame frame) {
        var callArgs = frame.take(instr.get(0));
        var funcObj = frame.pop();

        frame.push(Values.callNew(ctx, funcObj, callArgs));

        frame.codePtr++;
        return Values.NO_RETURN;
    }

    private static Object execMakeVar(Context ctx, Instruction instr, Frame frame) {
        var name = (String)instr.get(0);
        ctx.environment.global.define(ctx, name);
        frame.codePtr++;
        return Values.NO_RETURN;
    }
    private static Object execDefProp(Context ctx, Instruction instr, Frame frame) {
        var setter = frame.pop();
        var getter = frame.pop();
        var name = frame.pop();
        var obj = frame.pop();

        if (getter != null && !(getter instanceof FunctionValue)) throw EngineException.ofType("Getter must be a function or undefined.");
        if (setter != null && !(setter instanceof FunctionValue)) throw EngineException.ofType("Setter must be a function or undefined.");
        if (!(obj instanceof ObjectValue)) throw EngineException.ofType("Property apply target must be an object.");
        ((ObjectValue)obj).defineProperty(ctx, name, (FunctionValue)getter, (FunctionValue)setter, false, false);

        frame.push(obj);
        frame.codePtr++;
        return Values.NO_RETURN;
    }
    private static Object execKeys(Context ctx, Instruction instr, Frame frame) {
        var val = frame.pop();

        var members = Values.getMembers(ctx, val, false, false);
        Collections.reverse(members);

        frame.push(null);

        for (var el : members) {
            if (el instanceof Symbol) continue;
            var obj = new ObjectValue();
            obj.defineProperty(ctx, "value", el);
            frame.push(obj);
        }

        frame.codePtr++;
        return Values.NO_RETURN;
    }

    private static Object execTryStart(Context ctx, Instruction instr, Frame frame) {
        int start = frame.codePtr + 1;
        int catchStart = (int)instr.get(0);
        int finallyStart = (int)instr.get(1);
        if (finallyStart >= 0) finallyStart += start;
        if (catchStart >= 0) catchStart += start;
        int end = (int)instr.get(2) + start;
        frame.addTry(start, end, catchStart, finallyStart);
        frame.codePtr++;
        return Values.NO_RETURN;
    }
    private static Object execTryEnd(Context ctx, Instruction instr, Frame frame) {
        frame.popTryFlag = true;
        return Values.NO_RETURN;
    }

    private static Object execDup(Context ctx, Instruction instr, Frame frame) {
        int count = instr.get(0);

        for (var i = 0; i < count; i++) {
            frame.push(frame.peek(count - 1));
        }

        frame.codePtr++;
        return Values.NO_RETURN;
    }
    private static Object execLoadValue(Context ctx, Instruction instr, Frame frame) {
        switch (instr.type) {
            case PUSH_UNDEFINED: frame.push(null); break;
            case PUSH_NULL: frame.push(Values.NULL); break;
            default: frame.push(instr.get(0)); break;
        }

        frame.codePtr++;
        return Values.NO_RETURN;
    }
    private static Object execLoadVar(Context ctx, Instruction instr, Frame frame) {
        var i = instr.get(0);

        if (i instanceof String) frame.push(ctx.environment.global.get(ctx, (String)i));
        else frame.push(frame.scope.get((int)i).get(ctx));

        frame.codePtr++;
        return Values.NO_RETURN;
    }
    private static Object execLoadObj(Context ctx, Instruction instr, Frame frame) {
        frame.push(new ObjectValue());
        frame.codePtr++;
        return Values.NO_RETURN;
    }
    private static Object execLoadGlob(Context ctx, Instruction instr, Frame frame) {
        frame.push(ctx.environment.global.obj);
        frame.codePtr++;
        return Values.NO_RETURN;
    }
    private static Object execLoadArr(Context ctx, Instruction instr, Frame frame) {
        var res = new ArrayValue();
        res.setSize(instr.get(0));
        frame.push(res);
        frame.codePtr++;
        return Values.NO_RETURN;
    }
    private static Object execLoadFunc(Context ctx, Instruction instr, Frame frame) {
        int id = instr.get(0);
        var captures = new ValueVariable[instr.params.length - 1];

        for (var i = 1; i < instr.params.length; i++) {
            captures[i - 1] = frame.scope.get(instr.get(i));
        }

        var func = new CodeFunction(ctx.environment, "", frame.function.body.children[id], captures);

        frame.push(func);

        frame.codePtr++;
        return Values.NO_RETURN;
    }
    private static Object execLoadMember(Context ctx, Instruction instr, Frame frame) {
        var key = frame.pop();
        var obj = frame.pop();

        try {
            frame.push(Values.getMember(ctx, obj, key));
        }
        catch (IllegalArgumentException e) {
            throw EngineException.ofType(e.getMessage());
        }
        frame.codePtr++;
        return Values.NO_RETURN;
    }
    private static Object execLoadRegEx(Context ctx, Instruction instr, Frame frame) {
        if (ctx.hasNotNull(Environment.REGEX_CONSTR)) {
            frame.push(Values.callNew(ctx, ctx.get(Environment.REGEX_CONSTR), instr.get(0), instr.get(1)));
        }
        else {
            throw EngineException.ofSyntax("Regex is not supported.");
        }
        frame.codePtr++;
        return Values.NO_RETURN;
    }

    private static Object execDiscard(Context ctx, Instruction instr, Frame frame) {
        frame.pop();
        frame.codePtr++;
        return Values.NO_RETURN;
    }
    private static Object execStoreMember(Context ctx, Instruction instr, Frame frame) {
        var val = frame.pop();
        var key = frame.pop();
        var obj = frame.pop();

        if (!Values.setMember(ctx, obj, key, val)) throw EngineException.ofSyntax("Can't set member '" + key + "'.");
        if ((boolean)instr.get(0)) frame.push(val);
        frame.codePtr++;
        return Values.NO_RETURN;
    }
    private static Object execStoreVar(Context ctx, Instruction instr, Frame frame) {
        var val = (boolean)instr.get(1) ? frame.peek() : frame.pop();
        var i = instr.get(0);

        if (i instanceof String) ctx.environment.global.set(ctx, (String)i, val);
        else frame.scope.get((int)i).set(ctx, val);

        frame.codePtr++;
        return Values.NO_RETURN;
    }
    private static Object execStoreSelfFunc(Context ctx, Instruction instr, Frame frame) {
        frame.scope.locals[(int)instr.get(0)].set(ctx, frame.function);
        frame.codePtr++;
        return Values.NO_RETURN;
    }
    
    private static Object execJmp(Context ctx, Instruction instr, Frame frame) {
        frame.codePtr += (int)instr.get(0);
        frame.jumpFlag = true;
        return Values.NO_RETURN;
    }
    private static Object execJmpIf(Context ctx, Instruction instr, Frame frame) {
        if (Values.toBoolean(frame.pop())) {
            frame.codePtr += (int)instr.get(0);
            frame.jumpFlag = true;
        }
        else frame.codePtr ++;
        return Values.NO_RETURN;
    }
    private static Object execJmpIfNot(Context ctx, Instruction instr, Frame frame) {
        if (Values.not(frame.pop())) {
            frame.codePtr += (int)instr.get(0);
            frame.jumpFlag = true;
        }
        else frame.codePtr ++;
        return Values.NO_RETURN;
    }

    private static Object execTypeof(Context ctx, Instruction instr, Frame frame) {
        String name = instr.get(0);
        Object obj;

        if (name != null) {
            if (ctx.environment.global.has(ctx, name)) {
                obj = ctx.environment.global.get(ctx, name);
            }
            else obj = null;
        }
        else obj = frame.pop();

        frame.push(Values.type(obj));

        frame.codePtr++;
        return Values.NO_RETURN;
    }
    private static Object execNop(Context ctx, Instruction instr, Frame frame) {
        frame.codePtr++;
        return Values.NO_RETURN;
    }

    private static Object execDelete(Context ctx, Instruction instr, Frame frame) {
        var key = frame.pop();
        var val = frame.pop();

        if (!Values.deleteMember(ctx, val, key)) throw EngineException.ofSyntax("Can't delete member '" + key + "'.");
        frame.codePtr++;
        return Values.NO_RETURN;
    }

    private static Object execOperation(Context ctx, Instruction instr, Frame frame) {
        Operation op = instr.get(0);
        var args = new Object[op.operands];

        for (var i = op.operands - 1; i >= 0; i--) args[i] = frame.pop();

        frame.push(Values.operation(ctx, op, args));
        frame.codePtr++;
        return Values.NO_RETURN;
    }

    public static Object exec(Context ctx, Instruction instr, Frame frame) {
        switch (instr.type) {
            case NOP: return execNop(ctx, instr, frame);
            case RETURN: return execReturn(ctx, instr, frame);
            case THROW: return execThrow(ctx, instr, frame);
            case THROW_SYNTAX: return execThrowSyntax(ctx, instr, frame);
            case CALL: return execCall(ctx, instr, frame);
            case CALL_NEW: return execCallNew(ctx, instr, frame);
            case TRY_START: return execTryStart(ctx, instr, frame);
            case TRY_END: return execTryEnd(ctx, instr, frame);

            case DUP: return execDup(ctx, instr, frame);
            case PUSH_UNDEFINED:
            case PUSH_NULL:
            case PUSH_STRING:
            case PUSH_NUMBER:
            case PUSH_BOOL:
                return execLoadValue(ctx, instr, frame);
            case LOAD_VAR: return execLoadVar(ctx, instr, frame);
            case LOAD_OBJ: return execLoadObj(ctx, instr, frame);
            case LOAD_ARR: return execLoadArr(ctx, instr, frame);
            case LOAD_FUNC: return execLoadFunc(ctx, instr, frame);
            case LOAD_MEMBER: return execLoadMember(ctx, instr, frame);
            case LOAD_REGEX: return execLoadRegEx(ctx, instr, frame);
            case LOAD_GLOB: return execLoadGlob(ctx, instr, frame);

            case DISCARD: return execDiscard(ctx, instr, frame);
            case STORE_MEMBER: return execStoreMember(ctx, instr, frame);
            case STORE_VAR: return execStoreVar(ctx, instr, frame);
            case STORE_SELF_FUNC: return execStoreSelfFunc(ctx, instr, frame);
            case MAKE_VAR: return execMakeVar(ctx, instr, frame);

            case KEYS: return execKeys(ctx, instr, frame);
            case DEF_PROP: return execDefProp(ctx, instr, frame);
            case TYPEOF: return execTypeof(ctx, instr, frame);
            case DELETE: return execDelete(ctx, instr, frame);

            case JMP: return execJmp(ctx, instr, frame);
            case JMP_IF: return execJmpIf(ctx, instr, frame);
            case JMP_IFN: return execJmpIfNot(ctx, instr, frame);

            case OPERATION: return execOperation(ctx, instr, frame);

            default: throw EngineException.ofSyntax("Invalid instruction " + instr.type.name() + ".");
        }
    }
}
