package me.topchetoeu.jscript.engine;

import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.parsing.Parsing;

public class Context {
    public final Environment env;
    public final Message message;

    public FunctionValue compile(String filename, String raw) throws InterruptedException {
        var res = Values.toString(this, env.compile.call(this, null, raw, filename));
        return Parsing.compile(message.engine.functions, env, filename, res);
    }

    public Context setEnv(Environment env) {
        return new Context(env, message);
    }
    public Context setMsg(Message msg) {
        return new Context(env, msg);
    }

    public Context(Environment env, Message msg) {
        this.env = env;
        this.message = msg;
    }
}
