package me.topchetoeu.jscript.runtime.values.functions;

import me.topchetoeu.jscript.common.FunctionBody;
import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.runtime.Frame;
import me.topchetoeu.jscript.runtime.values.Value;

public final class CodeFunction extends FunctionValue {
    public final FunctionBody body;
    public final Value[][] captures;
    public Environment env;

    public Value self;
    public Value argsVal;

    private Value onCall(Frame frame) {
        if (mustCallSuper) frame.self = null;
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

        var res = onCall(frame);

        if (isNew) return frame.self;
        else return res;
    }

    public CodeFunction(Environment env, String name, FunctionBody body, Value[][] captures) {
        super(name, body.length);
        this.captures = captures;
        this.env = env;
        this.body = body;
    }
}
