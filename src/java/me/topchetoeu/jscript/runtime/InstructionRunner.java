package me.topchetoeu.jscript.runtime;

import java.util.Collections;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Operation;
import me.topchetoeu.jscript.runtime.environment.Environment;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.scope.GlobalScope;
import me.topchetoeu.jscript.runtime.scope.ValueVariable;
import me.topchetoeu.jscript.runtime.values.ArrayValue;
import me.topchetoeu.jscript.runtime.values.CodeFunction;
import me.topchetoeu.jscript.runtime.values.FunctionValue;
import me.topchetoeu.jscript.runtime.values.ObjectValue;
import me.topchetoeu.jscript.runtime.values.Symbol;
import me.topchetoeu.jscript.runtime.values.Values;

public class InstructionRunner {
    private static Object execReturn(Environment ext, Instruction instr, Frame frame) {
        return frame.pop();
    }
    private static Object execThrow(Environment ext, Instruction instr, Frame frame) {
        throw new EngineException(frame.pop());
    }
    private static Object execThrowSyntax(Environment ext, Instruction instr, Frame frame) {
        throw EngineException.ofSyntax((String)instr.get(0));
    }

    private static Object execCall(Environment ext, Instruction instr, Frame frame) {
        var callArgs = frame.take(instr.get(0));
        var func = frame.pop();
        var thisArg = frame.pop();

        frame.push(Values.call(ext, func, thisArg, callArgs));

        frame.codePtr++;
        return Values.NO_RETURN;
    }
    private static Object execCallNew(Environment ext, Instruction instr, Frame frame) {
        var callArgs = frame.take(instr.get(0));
        var funcObj = frame.pop();

        frame.push(Values.callNew(ext, funcObj, callArgs));

        frame.codePtr++;
        return Values.NO_RETURN;
    }

    private static Object execMakeVar(Environment ext, Instruction instr, Frame frame) {
        var name = (String)instr.get(0);
        GlobalScope.get(ext).define(ext, name);
        frame.codePtr++;
        return Values.NO_RETURN;
    }
    private static Object execDefProp(Environment ext, Instruction instr, Frame frame) {
        var setter = frame.pop();
        var getter = frame.pop();
        var name = frame.pop();
        var obj = frame.pop();

        if (getter != null && !(getter instanceof FunctionValue)) throw EngineException.ofType("Getter must be a function or undefined.");
        if (setter != null && !(setter instanceof FunctionValue)) throw EngineException.ofType("Setter must be a function or undefined.");
        if (!(obj instanceof ObjectValue)) throw EngineException.ofType("Property apply target must be an object.");
        ((ObjectValue)obj).defineProperty(ext, name, (FunctionValue)getter, (FunctionValue)setter, false, false);

        frame.push(obj);
        frame.codePtr++;
        return Values.NO_RETURN;
    }
    private static Object execKeys(Environment ext, Instruction instr, Frame frame) {
        var val = frame.pop();

        var members = Values.getMembers(ext, val, false, false);
        Collections.reverse(members);

        frame.push(null);

        for (var el : members) {
            if (el instanceof Symbol) continue;
            var obj = new ObjectValue();
            obj.defineProperty(ext, "value", el);
            frame.push(obj);
        }

        frame.codePtr++;
        return Values.NO_RETURN;
    }

    private static Object execTryStart(Environment ext, Instruction instr, Frame frame) {
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
    private static Object execTryEnd(Environment ext, Instruction instr, Frame frame) {
        frame.popTryFlag = true;
        return Values.NO_RETURN;
    }

    private static Object execDup(Environment ext, Instruction instr, Frame frame) {
        int count = instr.get(0);

        for (var i = 0; i < count; i++) {
            frame.push(frame.peek(count - 1));
        }

        frame.codePtr++;
        return Values.NO_RETURN;
    }
    private static Object execLoadValue(Environment ext, Instruction instr, Frame frame) {
        switch (instr.type) {
            case PUSH_UNDEFINED: frame.push(null); break;
            case PUSH_NULL: frame.push(Values.NULL); break;
            default: frame.push(instr.get(0)); break;
        }

        frame.codePtr++;
        return Values.NO_RETURN;
    }
    private static Object execLoadVar(Environment ext, Instruction instr, Frame frame) {
        var i = instr.get(0);

        if (i instanceof String) frame.push(GlobalScope.get(ext).get(ext, (String)i));
        else frame.push(frame.scope.get((int)i).get(ext));

        frame.codePtr++;
        return Values.NO_RETURN;
    }
    private static Object execLoadObj(Environment ext, Instruction instr, Frame frame) {
        frame.push(new ObjectValue());
        frame.codePtr++;
        return Values.NO_RETURN;
    }
    private static Object execLoadGlob(Environment ext, Instruction instr, Frame frame) {
        frame.push(GlobalScope.get(ext).obj);
        frame.codePtr++;
        return Values.NO_RETURN;
    }
    private static Object execLoadArr(Environment ext, Instruction instr, Frame frame) {
        var res = new ArrayValue();
        res.setSize(instr.get(0));
        frame.push(res);
        frame.codePtr++;
        return Values.NO_RETURN;
    }
    private static Object execLoadFunc(Environment ext, Instruction instr, Frame frame) {
        int id = instr.get(0);
        var captures = new ValueVariable[instr.params.length - 1];

        for (var i = 1; i < instr.params.length; i++) {
            captures[i - 1] = frame.scope.get(instr.get(i));
        }

        var func = new CodeFunction(ext, "", frame.function.body.children[id], captures);

        frame.push(func);

        frame.codePtr++;
        return Values.NO_RETURN;
    }
    private static Object execLoadMember(Environment ext, Instruction instr, Frame frame) {
        var key = frame.pop();
        var obj = frame.pop();

        try {
            frame.push(Values.getMember(ext, obj, key));
        }
        catch (IllegalArgumentException e) {
            throw EngineException.ofType(e.getMessage());
        }
        frame.codePtr++;
        return Values.NO_RETURN;
    }
    private static Object execLoadRegEx(Environment ext, Instruction instr, Frame frame) {
        if (ext.hasNotNull(Environment.REGEX_CONSTR)) {
            frame.push(Values.callNew(ext, ext.get(Environment.REGEX_CONSTR), instr.get(0), instr.get(1)));
        }
        else {
            throw EngineException.ofSyntax("Regex is not supported.");
        }
        frame.codePtr++;
        return Values.NO_RETURN;
    }

    private static Object execDiscard(Environment ext, Instruction instr, Frame frame) {
        frame.pop();
        frame.codePtr++;
        return Values.NO_RETURN;
    }
    private static Object execStoreMember(Environment ext, Instruction instr, Frame frame) {
        var val = frame.pop();
        var key = frame.pop();
        var obj = frame.pop();

        if (!Values.setMember(ext, obj, key, val)) throw EngineException.ofSyntax("Can't set member '" + key + "'.");
        if ((boolean)instr.get(0)) frame.push(val);
        frame.codePtr++;
        return Values.NO_RETURN;
    }
    private static Object execStoreVar(Environment ext, Instruction instr, Frame frame) {
        var val = (boolean)instr.get(1) ? frame.peek() : frame.pop();
        var i = instr.get(0);

        if (i instanceof String) GlobalScope.get(ext).set(ext, (String)i, val);
        else frame.scope.get((int)i).set(ext, val);

        frame.codePtr++;
        return Values.NO_RETURN;
    }
    private static Object execStoreSelfFunc(Environment ext, Instruction instr, Frame frame) {
        frame.scope.locals[(int)instr.get(0)].set(ext, frame.function);
        frame.codePtr++;
        return Values.NO_RETURN;
    }
    
    private static Object execJmp(Environment ext, Instruction instr, Frame frame) {
        frame.codePtr += (int)instr.get(0);
        frame.jumpFlag = true;
        return Values.NO_RETURN;
    }
    private static Object execJmpIf(Environment ext, Instruction instr, Frame frame) {
        if (Values.toBoolean(frame.pop())) {
            frame.codePtr += (int)instr.get(0);
            frame.jumpFlag = true;
        }
        else frame.codePtr ++;
        return Values.NO_RETURN;
    }
    private static Object execJmpIfNot(Environment ext, Instruction instr, Frame frame) {
        if (Values.not(frame.pop())) {
            frame.codePtr += (int)instr.get(0);
            frame.jumpFlag = true;
        }
        else frame.codePtr ++;
        return Values.NO_RETURN;
    }

    private static Object execTypeof(Environment ext, Instruction instr, Frame frame) {
        String name = instr.get(0);
        Object obj;

        if (name != null) {
            if (GlobalScope.get(ext).has(ext, name)) {
                obj = GlobalScope.get(ext).get(ext, name);
            }
            else obj = null;
        }
        else obj = frame.pop();

        frame.push(Values.type(obj));

        frame.codePtr++;
        return Values.NO_RETURN;
    }
    private static Object execNop(Environment ext, Instruction instr, Frame frame) {
        frame.codePtr++;
        return Values.NO_RETURN;
    }

    private static Object execDelete(Environment ext, Instruction instr, Frame frame) {
        var key = frame.pop();
        var val = frame.pop();

        if (!Values.deleteMember(ext, val, key)) throw EngineException.ofSyntax("Can't delete member '" + key + "'.");
        frame.codePtr++;
        return Values.NO_RETURN;
    }

    private static Object execOperation(Environment ext, Instruction instr, Frame frame) {
        Operation op = instr.get(0);
        var args = new Object[op.operands];

        for (var i = op.operands - 1; i >= 0; i--) args[i] = frame.pop();

        frame.push(Values.operation(ext, op, args));
        frame.codePtr++;
        return Values.NO_RETURN;
    }

    public static Object exec(Environment ext, Instruction instr, Frame frame) {
        switch (instr.type) {
            case NOP: return execNop(ext, instr, frame);
            case RETURN: return execReturn(ext, instr, frame);
            case THROW: return execThrow(ext, instr, frame);
            case THROW_SYNTAX: return execThrowSyntax(ext, instr, frame);
            case CALL: return execCall(ext, instr, frame);
            case CALL_NEW: return execCallNew(ext, instr, frame);
            case TRY_START: return execTryStart(ext, instr, frame);
            case TRY_END: return execTryEnd(ext, instr, frame);

            case DUP: return execDup(ext, instr, frame);
            case PUSH_UNDEFINED:
            case PUSH_NULL:
            case PUSH_STRING:
            case PUSH_NUMBER:
            case PUSH_BOOL:
                return execLoadValue(ext, instr, frame);
            case LOAD_VAR: return execLoadVar(ext, instr, frame);
            case LOAD_OBJ: return execLoadObj(ext, instr, frame);
            case LOAD_ARR: return execLoadArr(ext, instr, frame);
            case LOAD_FUNC: return execLoadFunc(ext, instr, frame);
            case LOAD_MEMBER: return execLoadMember(ext, instr, frame);
            case LOAD_REGEX: return execLoadRegEx(ext, instr, frame);
            case LOAD_GLOB: return execLoadGlob(ext, instr, frame);

            case DISCARD: return execDiscard(ext, instr, frame);
            case STORE_MEMBER: return execStoreMember(ext, instr, frame);
            case STORE_VAR: return execStoreVar(ext, instr, frame);
            case STORE_SELF_FUNC: return execStoreSelfFunc(ext, instr, frame);
            case MAKE_VAR: return execMakeVar(ext, instr, frame);

            case KEYS: return execKeys(ext, instr, frame);
            case DEF_PROP: return execDefProp(ext, instr, frame);
            case TYPEOF: return execTypeof(ext, instr, frame);
            case DELETE: return execDelete(ext, instr, frame);

            case JMP: return execJmp(ext, instr, frame);
            case JMP_IF: return execJmpIf(ext, instr, frame);
            case JMP_IFN: return execJmpIfNot(ext, instr, frame);

            case OPERATION: return execOperation(ext, instr, frame);

            default: throw EngineException.ofSyntax("Invalid instruction " + instr.type.name() + ".");
        }
    }
}
