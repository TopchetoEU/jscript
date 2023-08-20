package me.topchetoeu.jscript.engine.frame;

import java.util.Collections;

import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.engine.CallContext;
import me.topchetoeu.jscript.engine.DebugCommand;
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

    public static Object execReturn(Instruction instr, CodeFrame frame, CallContext ctx) {
        frame.codePtr++;
        return frame.pop();
    }
    public static Object execSignal(Instruction instr, CodeFrame frame, CallContext ctx) {
        frame.codePtr++;
        return new SignalValue(instr.get(0));
    }
    public static Object execThrow(Instruction instr, CodeFrame frame, CallContext ctx) {
        throw new EngineException(frame.pop());
    }
    public static Object execThrowSyntax(Instruction instr, CodeFrame frame, CallContext ctx) {
        throw EngineException.ofSyntax((String)instr.get(0));
    }

    private static Object call(DebugCommand state, CallContext ctx, Object func, Object thisArg, Object... args) throws InterruptedException {
        ctx.setData(CodeFrame.STOP_AT_START_KEY, state == DebugCommand.STEP_INTO);
        return Values.call(ctx, func, thisArg, args);
    }

    public static Object execCall(DebugCommand state, Instruction instr, CodeFrame frame, CallContext ctx) throws InterruptedException {
        int n = instr.get(0);

        var callArgs = new Object[n];
        for (var i = n - 1; i >= 0; i--) callArgs[i] = frame.pop();
        var func = frame.pop();
        var thisArg = frame.pop();

        frame.push(call(state, ctx, func, thisArg, callArgs));

        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execCallNew(DebugCommand state, Instruction instr, CodeFrame frame, CallContext ctx) throws InterruptedException {
        int n = instr.get(0);

        var callArgs = new Object[n];
        for (var i = n - 1; i >= 0; i--) callArgs[i] = frame.pop();
        var funcObj = frame.pop();

        if (Values.isFunction(funcObj) && Values.function(funcObj).special) {
            frame.push(call(state, ctx, funcObj, null, callArgs));
        }
        else {
            var proto = Values.getMember(ctx, funcObj, "prototype");
            var obj = new ObjectValue();
            obj.setPrototype(ctx, proto);
            call(state, ctx, funcObj, obj, callArgs);
            frame.push(obj);
        }

        frame.codePtr++;
        return NO_RETURN;
    }

    public static Object execMakeVar(Instruction instr, CodeFrame frame, CallContext ctx) throws InterruptedException {
        var name = (String)instr.get(0);
        frame.function.globals.define(name);
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execDefProp(Instruction instr, CodeFrame frame, CallContext ctx) throws InterruptedException {
        var setter = frame.pop();
        var getter = frame.pop();
        var name = frame.pop();
        var obj = frame.pop();

        if (getter != null && !Values.isFunction(getter)) throw EngineException.ofType("Getter must be a function or undefined.");
        if (setter != null && !Values.isFunction(setter)) throw EngineException.ofType("Setter must be a function or undefined.");
        if (!Values.isObject(obj)) throw EngineException.ofType("Property apply target must be an object.");
        Values.object(obj).defineProperty(name, Values.function(getter), Values.function(setter), false, false);

        frame.push(obj);
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execInstanceof(Instruction instr, CodeFrame frame, CallContext ctx) throws InterruptedException {
        var type = frame.pop();
        var obj = frame.pop();

        if (!Values.isPrimitive(type)) {
            var proto = Values.getMember(ctx, type, "prototype");
            frame.push(Values.isInstanceOf(ctx, obj, proto));
        }
        else {
            frame.push(false);
        }

        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execKeys(Instruction instr, CodeFrame frame, CallContext ctx) throws InterruptedException {
        var val = frame.pop();

        var arr = new ObjectValue();
        var i = 0;

        var members = Values.getMembers(ctx, val, false, false);
        Collections.reverse(members);
        for (var el : members) {
            if (el instanceof Symbol) continue;
            arr.defineProperty(i++, el);
        }

        arr.defineProperty("length", i);

        frame.push(arr);
        frame.codePtr++;
        return NO_RETURN;
    }

    public static Object execTry(DebugCommand state, Instruction instr, CodeFrame frame, CallContext ctx) throws InterruptedException {
        var finallyFunc = (boolean)instr.get(1) ? frame.pop() : null;
        var catchFunc = (boolean)instr.get(0) ? frame.pop() : null;
        var func = frame.pop();

        if (
            !Values.isFunction(func) ||
            catchFunc != null && !Values.isFunction(catchFunc) ||
            finallyFunc != null && !Values.isFunction(finallyFunc)
        ) throw EngineException.ofType("TRY instruction can be applied only upon functions.");

        Object res = new SignalValue("no_return");
        EngineException exception = null;

        Values.function(func).name = frame.function.name + "::try";
        if (catchFunc != null) Values.function(catchFunc).name = frame.function.name + "::catch";
        if (finallyFunc != null) Values.function(finallyFunc).name = frame.function.name + "::finally";

        try {
            ctx.setData(CodeFrame.STOP_AT_START_KEY, state != DebugCommand.NORMAL);
            res = Values.call(ctx, func, frame.thisArg);
        }
        catch (EngineException e) {
            exception = e.setCause(exception);
        }

        if (exception != null && catchFunc != null) {
            var exc = exception;
            exception = null;
            try {
                ctx.setData(CodeFrame.STOP_AT_START_KEY, state != DebugCommand.NORMAL);
                var _res = Values.call(ctx, catchFunc, frame.thisArg, exc);
                if (!SignalValue.isSignal(_res, "no_return")) res = _res;
            }
            catch (EngineException e) {
                exception = e.setCause(exc);
            }
        }

        if (finallyFunc != null) {
            try {
                ctx.setData(CodeFrame.STOP_AT_START_KEY, state != DebugCommand.NORMAL);
                var _res = Values.call(ctx, finallyFunc, frame.thisArg);
                if (!SignalValue.isSignal(_res, "no_return"))  {
                    res = _res;
                    exception = null;
                }
            }
            catch (EngineException e) {
                exception = e.setCause(exception);
            }
        }

        if (exception != null) throw exception;
        if (SignalValue.isSignal(res, "no_return")) {
            frame.codePtr++;
            return NO_RETURN;
        }
        else if (SignalValue.isSignal(res, "jmp_*")) {
            frame.codePtr += Integer.parseInt(((SignalValue)res).data.substring(4));
            return NO_RETURN;
        }
        else return res;
    }

    public static Object execDup(Instruction instr, CodeFrame frame, CallContext ctx) {
        var val = frame.peek(instr.get(1));
        for (int i = 0; i < (int)instr.get(0); i++) {
            frame.push(val);
        }
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execLoadUndefined(Instruction instr, CodeFrame frame, CallContext ctx) {
        frame.push(null);
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execLoadValue(Instruction instr, CodeFrame frame, CallContext ctx) {
        frame.push(instr.get(0));
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execLoadVar(Instruction instr, CodeFrame frame, CallContext ctx) throws InterruptedException {
        var i = instr.get(0);

        if (i instanceof String) frame.push(frame.function.globals.get(ctx, (String)i));
        else frame.push(frame.scope.get((int)i).get(ctx));

        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execLoadObj(Instruction instr, CodeFrame frame, CallContext ctx) {
        frame.push(new ObjectValue());
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execLoadGlob(Instruction instr, CodeFrame frame, CallContext ctx) {
        frame.push(frame.function.globals.obj);
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execLoadArr(Instruction instr, CodeFrame frame, CallContext ctx) {
        var res = new ArrayValue();
        res.setSize(instr.get(0));
        frame.push(res);
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execLoadFunc(Instruction instr, CodeFrame frame, CallContext ctx) {
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
        frame.push(func);

        frame.codePtr += n;
        return NO_RETURN;
    }
    public static Object execLoadMember(DebugCommand state, Instruction instr, CodeFrame frame, CallContext ctx) throws InterruptedException {
        var key = frame.pop();
        var obj = frame.pop();

        try {
            ctx.setData(CodeFrame.STOP_AT_START_KEY, state == DebugCommand.STEP_INTO);
            frame.push(Values.getMember(ctx, obj, key));
        }
        catch (IllegalArgumentException e) {
            throw EngineException.ofType(e.getMessage());
        }
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execLoadKeyMember(DebugCommand state, Instruction instr, CodeFrame frame, CallContext ctx) throws InterruptedException {
        frame.push(instr.get(0));
        return execLoadMember(state, instr, frame, ctx);
    }
    public static Object execLoadRegEx(Instruction instr, CodeFrame frame, CallContext ctx) throws InterruptedException {
        frame.push(ctx.engine().makeRegex(instr.get(0), instr.get(1)));
        frame.codePtr++;
        return NO_RETURN;
    }

    public static Object execDiscard(Instruction instr, CodeFrame frame, CallContext ctx) {
        frame.pop();
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execStoreMember(DebugCommand state, Instruction instr, CodeFrame frame, CallContext ctx) throws InterruptedException {
        var val = frame.pop();
        var key = frame.pop();
        var obj = frame.pop();

        ctx.setData(CodeFrame.STOP_AT_START_KEY, state == DebugCommand.STEP_INTO);
        if (!Values.setMember(ctx, obj, key, val)) throw EngineException.ofSyntax("Can't set member '" + key + "'.");
        if ((boolean)instr.get(0)) frame.push(val);
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execStoreVar(Instruction instr, CodeFrame frame, CallContext ctx) throws InterruptedException {
        var val = (boolean)instr.get(1) ? frame.peek() : frame.pop();
        var i = instr.get(0);

        if (i instanceof String) frame.function.globals.set(ctx, (String)i, val);
        else frame.scope.get((int)i).set(ctx, val);

        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execStoreSelfFunc(Instruction instr, CodeFrame frame, CallContext ctx) {
        frame.scope.locals[(int)instr.get(0)].set(ctx, frame.function);
        frame.codePtr++;
        return NO_RETURN;
    }
    
    public static Object execJmp(Instruction instr, CodeFrame frame, CallContext ctx) {
        frame.codePtr += (int)instr.get(0);
        return NO_RETURN;
    }
    public static Object execJmpIf(Instruction instr, CodeFrame frame, CallContext ctx) throws InterruptedException {
        frame.codePtr += Values.toBoolean(frame.pop()) ? (int)instr.get(0) : 1;
        return NO_RETURN;
    }
    public static Object execJmpIfNot(Instruction instr, CodeFrame frame, CallContext ctx) throws InterruptedException {
        frame.codePtr += Values.not(frame.pop()) ? (int)instr.get(0) : 1;
        return NO_RETURN;
    }

    public static Object execIn(Instruction instr, CodeFrame frame, CallContext ctx) throws InterruptedException {
        var obj = frame.pop();
        var index = frame.pop();

        frame.push(Values.hasMember(ctx, obj, index, false));
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execTypeof(Instruction instr, CodeFrame frame, CallContext ctx) throws InterruptedException {
        String name = instr.get(0);
        Object obj;

        if (name != null) {
            if (frame.function.globals.has(ctx, name)) {
                obj = frame.function.globals.get(ctx, name);
            }
            else obj = null;
        }
        else obj = frame.pop();

        frame.push(Values.type(obj));

        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execNop(Instruction instr, CodeFrame frame, CallContext ctx) {
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

    public static Object execDelete(Instruction instr, CodeFrame frame, CallContext ctx) throws InterruptedException {
        var key = frame.pop();
        var val = frame.pop();

        if (!Values.deleteMember(ctx, val, key)) throw EngineException.ofSyntax("Can't delete member '" + key + "'.");
        frame.push(true);
        frame.codePtr++;
        return NO_RETURN;
    }

    public static Object execAdd(Instruction instr, CodeFrame frame, CallContext ctx) throws InterruptedException {
        Object b = frame.pop(), a = frame.pop();
        frame.push(Values.add(ctx, a, b));
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execSubtract(Instruction instr, CodeFrame frame, CallContext ctx) throws InterruptedException {
        Object b = frame.pop(), a = frame.pop();
        frame.push(Values.subtract(ctx, a, b));
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execMultiply(Instruction instr, CodeFrame frame, CallContext ctx) throws InterruptedException {
        Object b = frame.pop(), a = frame.pop();
        frame.push(Values.multiply(ctx, a, b));
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execDivide(Instruction instr, CodeFrame frame, CallContext ctx) throws InterruptedException {
        Object b = frame.pop(), a = frame.pop();
        frame.push(Values.divide(ctx, a, b));
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execModulo(Instruction instr, CodeFrame frame, CallContext ctx) throws InterruptedException {
        Object b = frame.pop(), a = frame.pop();
        frame.push(Values.modulo(ctx, a, b));
        frame.codePtr++;
        return NO_RETURN;
    }

    public static Object execAnd(Instruction instr, CodeFrame frame, CallContext ctx) throws InterruptedException {
        Object b = frame.pop(), a = frame.pop();

        frame.push(Values.and(ctx, a, b));
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execOr(Instruction instr, CodeFrame frame, CallContext ctx) throws InterruptedException {
        Object b = frame.pop(), a = frame.pop();

        frame.push(Values.or(ctx, a, b));
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execXor(Instruction instr, CodeFrame frame, CallContext ctx) throws InterruptedException {
        Object b = frame.pop(), a = frame.pop();

        frame.push(Values.xor(ctx, a, b));
        frame.codePtr++;
        return NO_RETURN;
    }

    public static Object execLeftShift(Instruction instr, CodeFrame frame, CallContext ctx) throws InterruptedException {
        Object b = frame.pop(), a = frame.pop();

        frame.push(Values.shiftLeft(ctx, a, b));
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execRightShift(Instruction instr, CodeFrame frame, CallContext ctx) throws InterruptedException {
        Object b = frame.pop(), a = frame.pop();

        frame.push(Values.shiftRight(ctx, a, b));
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execUnsignedRightShift(Instruction instr, CodeFrame frame, CallContext ctx) throws InterruptedException {
        Object b = frame.pop(), a = frame.pop();

        frame.push(Values.unsignedShiftRight(ctx, a, b));
        frame.codePtr++;
        return NO_RETURN;
    }

    public static Object execNot(Instruction instr, CodeFrame frame, CallContext ctx) {
        frame.push(Values.not(frame.pop()));
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execNeg(Instruction instr, CodeFrame frame, CallContext ctx) throws InterruptedException {
        frame.push(Values.negative(ctx, frame.pop()));
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execPos(Instruction instr, CodeFrame frame, CallContext ctx) throws InterruptedException {
        frame.push(Values.toNumber(ctx, frame.pop()));
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execInverse(Instruction instr, CodeFrame frame, CallContext ctx) throws InterruptedException {
        frame.push(Values.bitwiseNot(ctx, frame.pop()));
        frame.codePtr++;
        return NO_RETURN;
    }

    public static Object execGreaterThan(Instruction instr, CodeFrame frame, CallContext ctx) throws InterruptedException {
        Object b = frame.pop(), a = frame.pop();

        frame.push(Values.compare(ctx, a, b) > 0);
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execLessThan(Instruction instr, CodeFrame frame, CallContext ctx) throws InterruptedException {
        Object b = frame.pop(), a = frame.pop();

        frame.push(Values.compare(ctx, a, b) < 0);
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execGreaterThanEquals(Instruction instr, CodeFrame frame, CallContext ctx) throws InterruptedException {
        Object b = frame.pop(), a = frame.pop();

        frame.push(Values.compare(ctx, a, b) >= 0);
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execLessThanEquals(Instruction instr, CodeFrame frame, CallContext ctx) throws InterruptedException {
        Object b = frame.pop(), a = frame.pop();

        frame.push(Values.compare(ctx, a, b) <= 0);
        frame.codePtr++;
        return NO_RETURN;
    }

    public static Object execLooseEquals(Instruction instr, CodeFrame frame, CallContext ctx) throws InterruptedException {
        Object b = frame.pop(), a = frame.pop();

        frame.push(Values.looseEqual(ctx, a, b));
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execLooseNotEquals(Instruction instr, CodeFrame frame, CallContext ctx) throws InterruptedException {
        Object b = frame.pop(), a = frame.pop();

        frame.push(!Values.looseEqual(ctx, a, b));
        frame.codePtr++;
        return NO_RETURN;
    }

    public static Object execEquals(Instruction instr, CodeFrame frame, CallContext ctx) throws InterruptedException {
        Object b = frame.pop(), a = frame.pop();

        frame.push(Values.strictEquals(a, b));
        frame.codePtr++;
        return NO_RETURN;
    }
    public static Object execNotEquals(Instruction instr, CodeFrame frame, CallContext ctx) throws InterruptedException {
        Object b = frame.pop(), a = frame.pop();

        frame.push(!Values.strictEquals(a, b));
        frame.codePtr++;
        return NO_RETURN;
    }

    public static Object exec(DebugCommand state, Instruction instr, CodeFrame frame, CallContext ctx) throws InterruptedException {
        // System.out.println(instr + "@" + instr.location);
        switch (instr.type) {
            case NOP: return execNop(instr, frame, ctx);
            case RETURN: return execReturn(instr, frame, ctx);
            case SIGNAL: return execSignal(instr, frame, ctx);
            case THROW: return execThrow(instr, frame, ctx);
            case THROW_SYNTAX: return execThrowSyntax(instr, frame, ctx);
            case CALL: return execCall(state, instr, frame, ctx);
            case CALL_NEW: return execCallNew(state, instr, frame, ctx);
            case TRY: return execTry(state, instr, frame, ctx);

            case DUP: return execDup(instr, frame, ctx);
            case LOAD_VALUE: return execLoadValue(instr, frame, ctx);
            case LOAD_VAR: return execLoadVar(instr, frame, ctx);
            case LOAD_OBJ: return execLoadObj(instr, frame, ctx);
            case LOAD_ARR: return execLoadArr(instr, frame, ctx);
            case LOAD_FUNC: return execLoadFunc(instr, frame, ctx);
            case LOAD_MEMBER: return execLoadMember(state, instr, frame, ctx);
            case LOAD_VAL_MEMBER: return execLoadKeyMember(state, instr, frame, ctx);
            case LOAD_REGEX: return execLoadRegEx(instr, frame, ctx);
            case LOAD_GLOB: return execLoadGlob(instr, frame, ctx);

            case DISCARD: return execDiscard(instr, frame, ctx);
            case STORE_MEMBER: return execStoreMember(state, instr, frame, ctx);
            case STORE_VAR: return execStoreVar(instr, frame, ctx);
            case STORE_SELF_FUNC: return execStoreSelfFunc(instr, frame, ctx);
            case MAKE_VAR: return execMakeVar(instr, frame, ctx);

            case IN: return execIn(instr, frame, ctx);
            case KEYS: return execKeys(instr, frame, ctx);
            case DEF_PROP: return execDefProp(instr, frame, ctx);
            case TYPEOF: return execTypeof(instr, frame, ctx);
            case DELETE: return execDelete(instr, frame, ctx);
            case INSTANCEOF: return execInstanceof(instr, frame, ctx);

            case JMP: return execJmp(instr, frame, ctx);
            case JMP_IF: return execJmpIf(instr, frame, ctx);
            case JMP_IFN: return execJmpIfNot(instr, frame, ctx);

            case ADD: return execAdd(instr, frame, ctx);
            case SUBTRACT: return execSubtract(instr, frame, ctx);
            case MULTIPLY: return execMultiply(instr, frame, ctx);
            case DIVIDE: return execDivide(instr, frame, ctx);
            case MODULO: return execModulo(instr, frame, ctx);

            case AND: return execAnd(instr, frame, ctx);
            case OR: return execOr(instr, frame, ctx);
            case XOR: return execXor(instr, frame, ctx);

            case SHIFT_LEFT: return execLeftShift(instr, frame, ctx);
            case SHIFT_RIGHT: return execRightShift(instr, frame, ctx);
            case USHIFT_RIGHT: return execUnsignedRightShift(instr, frame, ctx);

            case NOT: return execNot(instr, frame, ctx);
            case NEG: return execNeg(instr, frame, ctx);
            case POS: return execPos(instr, frame, ctx);
            case INVERSE: return execInverse(instr, frame, ctx);

            case GREATER: return execGreaterThan(instr, frame, ctx);
            case GREATER_EQUALS: return execGreaterThanEquals(instr, frame, ctx);
            case LESS: return execLessThan(instr, frame, ctx);
            case LESS_EQUALS: return execLessThanEquals(instr, frame, ctx);

            case LOOSE_EQUALS: return execLooseEquals(instr, frame, ctx);
            case LOOSE_NOT_EQUALS: return execLooseNotEquals(instr, frame, ctx);
            case EQUALS: return execEquals(instr, frame, ctx);
            case NOT_EQUALS: return execNotEquals(instr, frame, ctx);

            default: throw EngineException.ofSyntax("Invalid instruction " + instr.type.name() + ".");
        }
    }
}
