package me.topchetoeu.jscript.lib;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.Environment;
import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.interop.InitType;
import me.topchetoeu.jscript.interop.Native;
import me.topchetoeu.jscript.interop.NativeConstructor;
import me.topchetoeu.jscript.interop.NativeInit;

@Native("Error") public class ErrorLib {
    private static String toString(Context ctx, boolean rethrown, Object cause, Object name, Object message, ArrayValue stack) {
        if (name == null) name = "";
        else name = Values.toString(ctx, name).trim();
        if (message == null) message = "";
        else message = Values.toString(ctx, message).trim();
        StringBuilder res = new StringBuilder();

        if (!name.equals("")) res.append(name);
        if (!message.equals("") && !name.equals("")) res.append(": ");
        if (!message.equals("")) res.append(message);

        if (cause instanceof ObjectValue) {
            if (rethrown) res.append("\n    (rethrown)");
            else res.append("\nCaused by ").append(toString(ctx, cause));
        }

        return res.toString();
    }

    @Native(thisArg = true) public static String toString(Context ctx, Object thisArg) {
        if (thisArg instanceof ObjectValue) {
            var stack = Values.getMember(ctx, thisArg, "stack");
            if (!(stack instanceof ArrayValue)) stack = null;
            var cause = Values.getMember(ctx, thisArg, ctx.environment().symbol("Symbol.cause"));
            return toString(ctx,
                thisArg == cause,
                cause,
                Values.getMember(ctx, thisArg, "name"),
                Values.getMember(ctx, thisArg, "message"),
                (ArrayValue)stack
            );
        }
        else return "[Invalid error]";
    }

    @NativeConstructor(thisArg = true) public static ObjectValue constructor(Context ctx, Object thisArg, Object message) {
        var target = new ObjectValue();
        if (thisArg instanceof ObjectValue) target = (ObjectValue)thisArg;

        target.defineProperty(ctx, "stack", ArrayValue.of(ctx, ctx.stackTrace()));
        target.defineProperty(ctx, "name", "Error");
        if (message == null) target.defineProperty(ctx, "message", "");
        else target.defineProperty(ctx, "message", Values.toString(ctx, message));

        return target;
    }

    @NativeInit(InitType.PROTOTYPE) public static void init(Environment env, ObjectValue target) {
        target.defineProperty(null, "name", "Error");
    }
}
