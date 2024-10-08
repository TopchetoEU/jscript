package me.topchetoeu.jscript.runtime.values.functions;

import me.topchetoeu.jscript.common.FunctionBody;
import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.runtime.Frame;
import me.topchetoeu.jscript.runtime.values.Value;

public final class CodeFunction extends FunctionValue {
    public final FunctionBody body;
    public final Value[][] captures;
    public Value self;
    public Value argsVal;
    public Environment env;

    private Value onCall(Frame frame) {
        frame.onPush();

        try {
            while (true) {
                var res = frame.next(null, null, null);
                if (res != null) return res;
            }
        }
        finally {
            frame.onPop();
        }
    }

    @Override public Value onCall(Environment env, boolean isNew, String name, Value thisArg, Value ...args) {
        var frame = new Frame(env, isNew, thisArg, args, this);
        if (argsVal != null) frame.fakeArgs = argsVal;
        if (self != null) frame.self = self;

        return onCall(frame);
    }

    public CodeFunction(Environment env, String name, FunctionBody body, Value[][] captures) {
        super(name, body.length);
        this.captures = captures;
        this.env = env;
        this.body = body;
    }
}
