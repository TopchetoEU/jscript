package me.topchetoeu.jscript.engine.frame;

import java.util.Collections;

import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.engine.CallContext;
import me.topchetoeu.jscript.engine.DebugCommand;
import me.topchetoeu.jscript.engine.Operation;
import me.topchetoeu.jscript.engine.scope.ValueVariable;
import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.CodeFunction;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.SignalValue;
import me.topchetoeu.jscript.engine.values.Symbol;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.exceptions.EngineException;

public class Runners {
    public static final Object NO_RETURN = new Object();

    public static Object execReturn(CallContext ctx, Instruction instr, CodeFrame frame) {
        frame.codePtr++;
        return frame.pop();
    }
    public static Object execSignal(CallContext ctx, Instruction instr, CodeFrame frame) {
        frame.codePtr++;
        return new SignalValue(instr.get(0));
    }
    public static Object execThrow(CallContext ctx, Instruction instr, CodeFrame frame) {
        throw new EngineException(frame.pop());
    }
    public static Object execThrowSyntax(CallContext ctx, Instruction instr, CodeFrame frame) {
        throw EngineException.ofSyntax((String)instr.get(0));
    }

    private static Object call(CallContext ctx, DebugCommand state, Object func, Object thisArg, Object... args) throws InterruptedException {
        ctx.setData(CodeFrame.STOP_AT_START_KEY, state == DebugCommand.STEP_INTO);
        return Values.call(ctx, func, thisArg, args);
    }

    public static Object execCall(CallContext ctx, DebugCommand state, Instruction instr, CodeFrame frame) throws InterruptedException {
        var callArgs = frame.take(instr.get(0));
        var func = frame.pop();
        var thisArg = frame.pop();

        frame.push(ctx, call(ctx, state, func, thisArg, callArgs));

        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execCallNew(CallContext ctx, DebugCommand state, Instruction instr, CodeFrame frame) throws InterruptedException {
        var callArgs = frame.take(instr.get(0));
        var funcObj = frame.pop();

        if (Values.isFunction(funcObj) && Values.function(funcObj).special) {
            frame.push(ctx, call(ctx, state, funcObj, null, callArgs));
        }
        else {
            var proto = Values.getMember(ctx, funcObj, "prototype");
            var obj = new ObjectValue();
            obj.setPrototype(ctx, proto);
            call(ctx, state, funcObj, obj, callArgs);
            frame.push(ctx, obj);
        }

        frame.codePtr++;
        return NO_RETURN;
    }

    public static Object execMakeVar(CallContext ctx, Instruction instr, CodeFrame frame) throws InterruptedException {
        var name = (String)instr.get(0);
        frame.function.globals.define(name);
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execDefProp(CallContext ctx, Instruction instr, CodeFrame frame) throws InterruptedException {
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
    public static Object execInstanceof(CallContext ctx, Instruction instr, CodeFrame frame) throws InterruptedException {
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
    public static Object execKeys(CallContext ctx, Instruction instr, CodeFrame frame) throws InterruptedException {
        var val = frame.pop();

        var arr = new ObjectValue();
        var i = 0;

        var members = Values.getMembers(ctx, val, false, false);
        Collections.reverse(members);
        for (var el : members) {
            if (el instanceof Symbol) continue;
            arr.defineProperty(ctx, i++, el);
        }

        arr.defineProperty(ctx, "length", i);

        frame.push(ctx, arr);
        frame.codePtr++;
        return NO_RETURN;
    }

    public static Object execTry(CallContext ctx, Instruction instr, CodeFrame frame) throws InterruptedException {
        frame.addTry(instr.get(0), instr.get(1), instr.get(2));
        frame.codePtr++;
        return NO_RETURN;
    }

    public static Object execDup(CallContext ctx, Instruction instr, CodeFrame frame) {
        int offset = instr.get(0), count = instr.get(1);

        for (var i = 0; i < count; i++) {
            frame.push(ctx, frame.peek(offset + count - 1));
        }

        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execMove(CallContext ctx, Instruction instr, CodeFrame frame) {
        int offset = instr.get(0), count = instr.get(1);

        var tmp = frame.take(offset);
        var res = frame.take(count);

        for (var i = 0; i < offset; i++) frame.push(ctx, tmp[i]);
        for (var i = 0; i < count; i++) frame.push(ctx, res[i]);

        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execLoadUndefined(CallContext ctx, Instruction instr, CodeFrame frame) {
        frame.push(ctx, null);
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execLoadValue(CallContext ctx, Instruction instr, CodeFrame frame) {
        frame.push(ctx, instr.get(0));
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execLoadVar(CallContext ctx, Instruction instr, CodeFrame frame) throws InterruptedException {
        var i = instr.get(0);

        if (i instanceof String) frame.push(ctx, frame.function.globals.get(ctx, (String)i));
        else frame.push(ctx, frame.scope.get((int)i).get(ctx));

        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execLoadObj(CallContext ctx, Instruction instr, CodeFrame frame) {
        frame.push(ctx, new ObjectValue());
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execLoadGlob(CallContext ctx, Instruction instr, CodeFrame frame) {
        frame.push(ctx, frame.function.globals.obj);
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execLoadArr(CallContext ctx, Instruction instr, CodeFrame frame) {
        var res = new ArrayValue();
        res.setSize(instr.get(0));
        frame.push(ctx, res);
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execLoadFunc(CallContext ctx, Instruction instr, CodeFrame frame) {
        int n = (Integer)instr.get(0);
        int localsN = (Integer)instr.get(1);
        int len = (Integer)instr.get(2);
        var captures = new ValueVariable[instr.params.length - 3];

        for (var i = 3; i < instr.params.length; i++) {
            captures[i - 3] = frame.scope.get(instr.get(i));
        }

        var start = frame.codePtr + 1;
        var end = start + n - 1;
        var body = new Instruction[end - start];
        System.arraycopy(frame.function.body, start, body, 0, end - start);

        var func = new CodeFunction("", localsN, len, frame.function.globals, captures, body);
        frame.push(ctx, func);

        frame.codePtr += n;
        return NO_RETURN;
    }
    public static Object execLoadMember(CallContext ctx, DebugCommand state, Instruction instr, CodeFrame frame) throws InterruptedException {
        var key = frame.pop();
        var obj = frame.pop();

        try {
            ctx.setData(CodeFrame.STOP_AT_START_KEY, state == DebugCommand.STEP_INTO);
            frame.push(ctx, Values.getMember(ctx, obj, key));
        }
        catch (IllegalArgumentException e) {
            throw EngineException.ofType(e.getMessage());
        }
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execLoadKeyMember(CallContext ctx, DebugCommand state, Instruction instr, CodeFrame frame) throws InterruptedException {
        frame.push(ctx, instr.get(0));
        return execLoadMember(ctx, state, instr, frame);
    }
    public static Object execLoadRegEx(CallContext ctx, Instruction instr, CodeFrame frame) throws InterruptedException {
        frame.push(ctx, ctx.engine().makeRegex(instr.get(0), instr.get(1)));
        frame.codePtr++;
        return NO_RETURN;
    }

    public static Object execDiscard(CallContext ctx, Instruction instr, CodeFrame frame) {
        frame.pop();
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execStoreMember(CallContext ctx, DebugCommand state, Instruction instr, CodeFrame frame) throws InterruptedException {
        var val = frame.pop();
        var key = frame.pop();
        var obj = frame.pop();

        ctx.setData(CodeFrame.STOP_AT_START_KEY, state == DebugCommand.STEP_INTO);
        if (!Values.setMember(ctx, obj, key, val)) throw EngineException.ofSyntax("Can't set member '" + key + "'.");
        if ((boolean)instr.get(0)) frame.push(ctx, val);
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execStoreVar(CallContext ctx, Instruction instr, CodeFrame frame) throws InterruptedException {
        var val = (boolean)instr.get(1) ? frame.peek() : frame.pop();
        var i = instr.get(0);

        if (i instanceof String) frame.function.globals.set(ctx, (String)i, val);
        else frame.scope.get((int)i).set(ctx, val);

        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execStoreSelfFunc(CallContext ctx, Instruction instr, CodeFrame frame) {
        frame.scope.locals[(int)instr.get(0)].set(ctx, frame.function);
        frame.codePtr++;
        return NO_RETURN;
    }
    
    public static Object execJmp(CallContext ctx, Instruction instr, CodeFrame frame) {
        frame.codePtr += (int)instr.get(0);
        frame.jumpFlag = true;
        return NO_RETURN;
    }
    public static Object execJmpIf(CallContext ctx, Instruction instr, CodeFrame frame) throws InterruptedException {
        if (Values.toBoolean(frame.pop())) {
            frame.codePtr += (int)instr.get(0);
            frame.jumpFlag = true;
        }
        else frame.codePtr ++;
        return NO_RETURN;
    }
    public static Object execJmpIfNot(CallContext ctx, Instruction instr, CodeFrame frame) throws InterruptedException {
        if (Values.not(frame.pop())) {
            frame.codePtr += (int)instr.get(0);
            frame.jumpFlag = true;
        }
        else frame.codePtr ++;
        return NO_RETURN;
    }

    public static Object execIn(CallContext ctx, Instruction instr, CodeFrame frame) throws InterruptedException {
        var obj = frame.pop();
        var index = frame.pop();

        frame.push(ctx, Values.hasMember(ctx, obj, index, false));
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execTypeof(CallContext ctx, Instruction instr, CodeFrame frame) throws InterruptedException {
        String name = instr.get(0);
        Object obj;

        if (name != null) {
            if (frame.function.globals.has(ctx, name)) {
                obj = frame.function.globals.get(ctx, name);
            }
            else obj = null;
        }
        else obj = frame.pop();

        frame.push(ctx, Values.type(obj));

        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execNop(CallContext ctx, Instruction instr, CodeFrame frame) {
        if (instr.is(0, "dbg_names")) {
            var names = new String[instr.params.length - 1];
            for (var i = 0; i < instr.params.length - 1; i++) {
                if (!(instr.params[i + 1] instanceof String)) throw EngineException.ofSyntax("NOP dbg_names instruction must specify only string parameters.");
                names[i] = (String)instr.params[i + 1];
            }
            frame.scope.setNames(names);
        }

        frame.codePtr++;
        return NO_RETURN;
    }

    public static Object execDelete(CallContext ctx, Instruction instr, CodeFrame frame) throws InterruptedException {
        var key = frame.pop();
        var val = frame.pop();

        if (!Values.deleteMember(ctx, val, key)) throw EngineException.ofSyntax("Can't delete member '" + key + "'.");
        frame.push(ctx, true);
        frame.codePtr++;
        return NO_RETURN;
    }

    public static Object execOperation(CallContext ctx, Instruction instr, CodeFrame frame) throws InterruptedException {
        Operation op = instr.get(0);
        var args = new Object[op.operands];

        for (var i = op.operands - 1; i >= 0; i--) args[i] = frame.pop();

        frame.push(ctx, Values.operation(ctx, op, args));
        frame.codePtr++;
        return NO_RETURN;
    }

    public static Object exec(CallContext ctx, DebugCommand state, Instruction instr, CodeFrame frame) throws InterruptedException {
        // System.out.println(instr + "@" + instr.location);
        switch (instr.type) {
            case NOP: return execNop(ctx, instr, frame);
            case RETURN: return execReturn(ctx, instr, frame);
            case SIGNAL: return execSignal(ctx, instr, frame);
            case THROW: return execThrow(ctx, instr, frame);
            case THROW_SYNTAX: return execThrowSyntax(ctx, instr, frame);
            case CALL: return execCall(ctx, state, instr, frame);
            case CALL_NEW: return execCallNew(ctx, state, instr, frame);
            case TRY: return execTry(ctx, instr, frame);

            case DUP: return execDup(ctx, instr, frame);
            case MOVE: return execMove(ctx, instr, frame);
            case LOAD_VALUE: return execLoadValue(ctx, instr, frame);
            case LOAD_VAR: return execLoadVar(ctx, instr, frame);
            case LOAD_OBJ: return execLoadObj(ctx, instr, frame);
            case LOAD_ARR: return execLoadArr(ctx, instr, frame);
            case LOAD_FUNC: return execLoadFunc(ctx, instr, frame);
            case LOAD_MEMBER: return execLoadMember(ctx, state, instr, frame);
            case LOAD_VAL_MEMBER: return execLoadKeyMember(ctx, state, instr, frame);
            case LOAD_REGEX: return execLoadRegEx(ctx, instr, frame);
            case LOAD_GLOB: return execLoadGlob(ctx, instr, frame);

            case DISCARD: return execDiscard(ctx, instr, frame);
            case STORE_MEMBER: return execStoreMember(ctx, state, instr, frame);
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
