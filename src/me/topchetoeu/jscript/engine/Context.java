package me.topchetoeu.jscript.engine;

import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.parsing.Parsing;

public class Context {
    public final Environment env;
    public final MessageContext message;

    public FunctionValue compile(String filename, String raw) throws InterruptedException {
        var res = Values.toString(this, env.compile.call(this, null, raw, filename));
        return Parsing.compile(env, filename, res);
    }

    public Context(Environment funcCtx, MessageContext msgCtx) {
        this.env = funcCtx;
        this.message = msgCtx;
    }
}
