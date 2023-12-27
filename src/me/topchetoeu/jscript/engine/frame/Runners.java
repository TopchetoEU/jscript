package me.topchetoeu.jscript.engine.frame;

import java.util.Collections;

import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.Engine;
import me.topchetoeu.jscript.engine.Environment;
import me.topchetoeu.jscript.engine.Operation;
import me.topchetoeu.jscript.engine.scope.ValueVariable;
import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.CodeFunction;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.Symbol;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.exceptions.EngineException;

public class Runners {
    public static final Object NO_RETURN = new Object();

    public static Object execReturn(Context ctx, Instruction instr, CodeFrame frame) {
        return frame.pop();
    }
    public static Object execThrow(Context ctx, Instruction instr, CodeFrame frame) {
        throw new EngineException(frame.pop());
    }
    public static Object execThrowSyntax(Context ctx, Instruction instr, CodeFrame frame) {
        throw EngineException.ofSyntax((String)instr.get(0));
    }

    public static Object execCall(Context ctx, Instruction instr, CodeFrame frame) {
        var callArgs = frame.take(instr.get(0));
        var func = frame.pop();
        var thisArg = frame.pop();

        frame.push(ctx, Values.call(ctx, func, thisArg, callArgs));

        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execCallNew(Context ctx, Instruction instr, CodeFrame frame) {
        var callArgs = frame.take(instr.get(0));
        var funcObj = frame.pop();

        frame.push(ctx, Values.callNew(ctx, funcObj, callArgs));

        frame.codePtr++;
        return NO_RETURN;
    }

    public static Object execMakeVar(Context ctx, Instruction instr, CodeFrame frame) {
        var name = (String)instr.get(0);
        ctx.environment().global.define(name);
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execDefProp(Context ctx, Instruction instr, CodeFrame frame) {
        var setter = frame.pop();
        var getter = frame.pop();
        var name = frame.pop();
        var obj = frame.pop();

        if (getter != null && !Values.isFunction(getter)) throw EngineException.ofType("Getter must be a function or undefined.");
        if (setter != null && !Values.isFunction(setter)) throw EngineException.ofType("Setter must be a function or undefined.");
        if (!Values.isObject(obj)) throw EngineException.ofType("Property apply target must be an object.");
        Values.object(obj).defineProperty(ctx, name, Values.function(getter), Values.function(setter), false, false);

        frame.push(ctx, obj);
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execInstanceof(Context ctx, Instruction instr, CodeFrame frame) {
        var type = frame.pop();
        var obj = frame.pop();

        if (!Values.isPrimitive(type)) {
            var proto = Values.getMember(ctx, type, "prototype");
            frame.push(ctx, Values.isInstanceOf(ctx, obj, proto));
        }
        else {
            frame.push(ctx, false);
        }

        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execKeys(Context ctx, Instruction instr, CodeFrame frame) {
        var val = frame.pop();

        var members = Values.getMembers(ctx, val, false, false);
        Collections.reverse(members);

        frame.push(ctx, null);

        for (var el : members) {
            if (el instanceof Symbol) continue;
            var obj = new ObjectValue();
            obj.defineProperty(ctx, "value", el);
            frame.push(ctx, obj);
        }

        frame.codePtr++;
        return NO_RETURN;
    }

    public static Object execTryStart(Context ctx, Instruction instr, CodeFrame frame) {
        int start = frame.codePtr + 1;
        int catchStart = (int)instr.get(0);
        int finallyStart = (int)instr.get(1);
        if (finallyStart >= 0) finallyStart += start;
        if (catchStart >= 0) catchStart += start;
        int end = (int)instr.get(2) + start;
        frame.addTry(start, end, catchStart, finallyStart);
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execTryEnd(Context ctx, Instruction instr, CodeFrame frame) {
        frame.popTryFlag = true;
        return NO_RETURN;
    }

    public static Object execDup(Context ctx, Instruction instr, CodeFrame frame) {
        int count = instr.get(0);

        for (var i = 0; i < count; i++) {
            frame.push(ctx, frame.peek(count - 1));
        }

        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execLoadUndefined(Context ctx, Instruction instr, CodeFrame frame) {
        frame.push(ctx, null);
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execLoadValue(Context ctx, Instruction instr, CodeFrame frame) {
        frame.push(ctx, instr.get(0));
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execLoadVar(Context ctx, Instruction instr, CodeFrame frame) {
        var i = instr.get(0);

        if (i instanceof String) frame.push(ctx, ctx.environment().global.get(ctx, (String)i));
        else frame.push(ctx, frame.scope.get((int)i).get(ctx));

        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execLoadObj(Context ctx, Instruction instr, CodeFrame frame) {
        frame.push(ctx, new ObjectValue());
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execLoadGlob(Context ctx, Instruction instr, CodeFrame frame) {
        frame.push(ctx, ctx.environment().global.obj);
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execLoadArr(Context ctx, Instruction instr, CodeFrame frame) {
        var res = new ArrayValue();
        res.setSize(instr.get(0));
        frame.push(ctx, res);
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execLoadFunc(Context ctx, Instruction instr, CodeFrame frame) {
        long id = (Long)instr.get(0);
        var captures = new ValueVariable[instr.params.length - 1];

        for (var i = 1; i < instr.params.length; i++) {
            captures[i - 1] = frame.scope.get(instr.get(i));
        }

        var func = new CodeFunction(ctx.environment(), "", Engine.functions.get(id), captures);

        frame.push(ctx, func);

        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execLoadMember(Context ctx, Instruction instr, CodeFrame frame) {
        var key = frame.pop();
        var obj = frame.pop();

        try {
            frame.push(ctx, Values.getMember(ctx, obj, key));
        }
        catch (IllegalArgumentException e) {
            throw EngineException.ofType(e.getMessage());
        }
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execLoadKeyMember(Context ctx, Instruction instr, CodeFrame frame) {
        frame.push(ctx, instr.get(0));
        return execLoadMember(ctx, instr, frame);
    }
    public static Object execLoadRegEx(Context ctx, Instruction instr, CodeFrame frame) {
        if (ctx.has(Environment.REGEX_CONSTR)) {
            frame.push(ctx, Values.callNew(ctx, ctx.get(Environment.REGEX_CONSTR)));
        }
        else {
            throw EngineException.ofSyntax("Regex is not supported.");
        }
        frame.codePtr++;
        return NO_RETURN;
    }

    public static Object execDiscard(Context ctx, Instruction instr, CodeFrame frame) {
        frame.pop();
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execStoreMember(Context ctx, Instruction instr, CodeFrame frame) {
        var val = frame.pop();
        var key = frame.pop();
        var obj = frame.pop();

        if (!Values.setMember(ctx, obj, key, val)) throw EngineException.ofSyntax("Can't set member '" + key + "'.");
        if ((boolean)instr.get(0)) frame.push(ctx, val);
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execStoreVar(Context ctx, Instruction instr, CodeFrame frame) {
        var val = (boolean)instr.get(1) ? frame.peek() : frame.pop();
        var i = instr.get(0);

        if (i instanceof String) ctx.environment().global.set(ctx, (String)i, val);
        else frame.scope.get((int)i).set(ctx, val);

        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execStoreSelfFunc(Context ctx, Instruction instr, CodeFrame frame) {
        frame.scope.locals[(int)instr.get(0)].set(ctx, frame.function);
        frame.codePtr++;
        return NO_RETURN;
    }
    
    public static Object execJmp(Context ctx, Instruction instr, CodeFrame frame) {
        frame.codePtr += (int)instr.get(0);
        frame.jumpFlag = true;
        return NO_RETURN;
    }
    public static Object execJmpIf(Context ctx, Instruction instr, CodeFrame frame) {
        if (Values.toBoolean(frame.pop())) {
            frame.codePtr += (int)instr.get(0);
            frame.jumpFlag = true;
        }
        else frame.codePtr ++;
        return NO_RETURN;
    }
    public static Object execJmpIfNot(Context ctx, Instruction instr, CodeFrame frame) {
        if (Values.not(frame.pop())) {
            frame.codePtr += (int)instr.get(0);
            frame.jumpFlag = true;
        }
        else frame.codePtr ++;
        return NO_RETURN;
    }

    public static Object execIn(Context ctx, Instruction instr, CodeFrame frame) {
        var obj = frame.pop();
        var index = frame.pop();

        frame.push(ctx, Values.hasMember(ctx, obj, index, false));
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execTypeof(Context ctx, Instruction instr, CodeFrame frame) {
        String name = instr.get(0);
        Object obj;

        if (name != null) {
            if (ctx.environment().global.has(ctx, name)) {
                obj = ctx.environment().global.get(ctx, name);
            }
            else obj = null;
        }
        else obj = frame.pop();

        frame.push(ctx, Values.type(obj));

        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execNop(Context ctx, Instruction instr, CodeFrame frame) {
        frame.codePtr++;
        return NO_RETURN;
    }

    public static Object execDelete(Context ctx, Instruction instr, CodeFrame frame) {
        var key = frame.pop();
        var val = frame.pop();

        if (!Values.deleteMember(ctx, val, key)) throw EngineException.ofSyntax("Can't delete member '" + key + "'.");
        frame.codePtr++;
        return NO_RETURN;
    }

    public static Object execOperation(Context ctx, Instruction instr, CodeFrame frame) {
        Operation op = instr.get(0);
        var args = new Object[op.operands];

        for (var i = op.operands - 1; i >= 0; i--) args[i] = frame.pop();

        frame.push(ctx, Values.operation(ctx, op, args));
        frame.codePtr++;
        return NO_RETURN;
    }

    public static Object exec(Context ctx, Instruction instr, CodeFrame frame) {
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
            case LOAD_VALUE: return execLoadValue(ctx, instr, frame);
            case LOAD_VAR: return execLoadVar(ctx, instr, frame);
            case LOAD_OBJ: return execLoadObj(ctx, instr, frame);
            case LOAD_ARR: return execLoadArr(ctx, instr, frame);
            case LOAD_FUNC: return execLoadFunc(ctx, instr, frame);
            case LOAD_MEMBER: return execLoadMember(ctx, instr, frame);
            case LOAD_VAL_MEMBER: return execLoadKeyMember(ctx, instr, frame);
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
