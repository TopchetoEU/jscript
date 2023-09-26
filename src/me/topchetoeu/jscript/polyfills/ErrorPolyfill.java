package me.topchetoeu.jscript.polyfills;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.interop.Native;

public class ErrorPolyfill {
    public static class SyntaxErrorPolyfill extends ErrorPolyfill {
        @Native public SyntaxErrorPolyfill(Context ctx, Object message) throws InterruptedException {
            super(ctx, message);
            this.name = "SyntaxError";
        }
    }
    public static class TypeErrorPolyfill extends ErrorPolyfill {
        @Native public TypeErrorPolyfill(Context ctx, Object message) throws InterruptedException {
            super(ctx, message);
            this.name = "TypeError";
        }
    }
    public static class RangeErrorPolyfill extends ErrorPolyfill {
        @Native public RangeErrorPolyfill(Context ctx, Object message) throws InterruptedException {
            super(ctx, message);
            this.name = "RangeError";
        }
    }

    @Native public final ArrayValue stack;
    @Native public String message;
    @Native public String name = "Error";

    private static String toString(Context ctx, Object name, Object message, ArrayValue stack) throws InterruptedException {
        if (name == null) name = "";
        else name = Values.toString(ctx, name).trim();
        if (message == null) message = "";
        else message = Values.toString(ctx, message).trim();
        StringBuilder res = new StringBuilder();

        if (!name.equals("")) res.append(name);
        if (!message.equals("") && !name.equals("")) res.append(": ");
        if (!name.equals("")) res.append(message);

        if (stack != null) {
            for (var el : stack) {
                var str = Values.toString(ctx, el).trim();
                if (!str.equals("")) res.append("\n    ").append(el);
            }
        }

        return res.toString();
    }

    @Native(thisArg = true) public static String toString(Context ctx, Object thisArg) throws InterruptedException {
        if (thisArg instanceof ErrorPolyfill) {
            return toString(ctx, ((ErrorPolyfill)thisArg).name, ((ErrorPolyfill)thisArg).message, ((ErrorPolyfill)thisArg).stack);
        }
        else if (thisArg instanceof ObjectValue) {
            var stack = Values.getMember(ctx, thisArg, "stack");
            if (!(stack instanceof ArrayValue)) stack = null;
            return toString(ctx,
                Values.getMember(ctx, thisArg, "name"),
                Values.getMember(ctx, thisArg, "message"),
                (ArrayValue)stack
            );
        }
        else return "[Invalid error]";
    }

    @Native public ErrorPolyfill(Context ctx, Object message) throws InterruptedException {
        this.stack = new ArrayValue(ctx, ctx.message.stackTrace().toArray());
        if (message == null) this.message = "";
        else this.message = Values.toString(ctx, message);
    }
}
