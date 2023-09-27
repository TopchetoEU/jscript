package me.topchetoeu.jscript.polyfills;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.interop.Native;
import me.topchetoeu.jscript.interop.NativeConstructor;

public class ErrorPolyfill {
    public static class SyntaxErrorPolyfill extends ErrorPolyfill {
        @NativeConstructor(thisArg = true) public static ObjectValue constructor(Context ctx, Object thisArg, Object message) throws InterruptedException {
            var target = ErrorPolyfill.constructor(ctx, thisArg, message);
            target.defineProperty(ctx, "name", "SyntaxError");
            return target;
        }
    }
    public static class TypeErrorPolyfill extends ErrorPolyfill {
        @NativeConstructor(thisArg = true) public static ObjectValue constructor(Context ctx, Object thisArg, Object message) throws InterruptedException {
            var target = ErrorPolyfill.constructor(ctx, thisArg, message);
            target.defineProperty(ctx, "name", "TypeError");
            return target;
        }
    }
    public static class RangeErrorPolyfill extends ErrorPolyfill {
        @NativeConstructor(thisArg = true) public static ObjectValue constructor(Context ctx, Object thisArg, Object message) throws InterruptedException {
            var target = ErrorPolyfill.constructor(ctx, thisArg, message);
            target.defineProperty(ctx, "name", "RangeError");
            return target;
        }
    }

    private static String toString(Context ctx, Object cause, Object name, Object message, ArrayValue stack) throws InterruptedException {
        if (name == null) name = "";
        else name = Values.toString(ctx, name).trim();
        if (message == null) message = "";
        else message = Values.toString(ctx, message).trim();
        StringBuilder res = new StringBuilder();

        if (!name.equals("")) res.append(name);
        if (!message.equals("") && !name.equals("")) res.append(": ");
        if (!message.equals("")) res.append(message);

        if (stack != null) {
            for (var el : stack) {
                var str = Values.toString(ctx, el).trim();
                if (!str.equals("")) res.append("\n    ").append(el);
            }
        }

        if (cause instanceof ObjectValue) res.append(toString(ctx, cause));

        return res.toString();
    }

    @Native(thisArg = true) public static String toString(Context ctx, Object thisArg) throws InterruptedException {
        if (thisArg instanceof ObjectValue) {
            var stack = Values.getMember(ctx, thisArg, "stack");
            if (!(stack instanceof ArrayValue)) stack = null;
            return toString(ctx,
                Values.getMember(ctx, thisArg, "cause"),
                Values.getMember(ctx, thisArg, "name"),
                Values.getMember(ctx, thisArg, "message"),
                (ArrayValue)stack
            );
        }
        else return "[Invalid error]";
    }

    @NativeConstructor(thisArg = true) public static ObjectValue constructor(Context ctx, Object thisArg, Object message) throws InterruptedException {
        var target = new ObjectValue();
        if (thisArg instanceof ObjectValue) target = (ObjectValue)thisArg;

        target.defineProperty(ctx, "stack", new ArrayValue(ctx, ctx.message.stackTrace().toArray()));
        target.defineProperty(ctx, "name", "Error");
        if (message == null) target.defineProperty(ctx, "message", "");
        else target.defineProperty(ctx, "message", Values.toString(ctx, message));

        return target;
    }
}
