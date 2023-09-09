package me.topchetoeu.jscript.engine;

import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.parsing.Parsing;

public class Context {
    public final FunctionContext function;
    public final MessageContext message;

    public FunctionValue compile(String filename, String raw) throws InterruptedException {
        var res = Values.toString(this, function.compile.call(this, null, raw, filename));
        return Parsing.compile(function, filename, res);
    }

    public Context(FunctionContext funcCtx, MessageContext msgCtx) {
        this.function = funcCtx;
        this.message = msgCtx;
    }
}
