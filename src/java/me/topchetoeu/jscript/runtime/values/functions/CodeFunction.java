package me.topchetoeu.jscript.runtime.values.functions;

import me.topchetoeu.jscript.common.FunctionBody;
import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.runtime.Frame;
import me.topchetoeu.jscript.runtime.values.Value;

public class CodeFunction extends FunctionValue {
    public final FunctionBody body;
    public final Value[][] captures;
    public Environment env;

    @Override public Value onCall(Environment env, boolean isNew, String name, Value thisArg, Value ...args) {
        var frame = new Frame(env, isNew, thisArg, args, this);
        frame.onPush();

        try {
            while (true) {
                var res = frame.next();
                if (res != null) return res;
            }
        }
        finally {
            frame.onPop();
        }
    }

    public CodeFunction(Environment env, String name, FunctionBody body, Value[][] captures) {
        super(name, body.argsN);
        this.captures = captures;
        this.env = env;
        this.body = body;
    }
}
