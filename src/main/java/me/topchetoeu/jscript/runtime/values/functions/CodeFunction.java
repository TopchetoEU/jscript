package me.topchetoeu.jscript.runtime.values.functions;

import me.topchetoeu.jscript.common.FunctionBody;
import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.runtime.Frame;
import me.topchetoeu.jscript.runtime.values.Value;

public final class CodeFunction extends FunctionValue {
    public final FunctionBody body;
    public final Value[][] captures;
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

    @Override public Value onCall(Environment env, boolean isNew, Value self, Value ...args) {
        var frame = new Frame(env, isNew, self, args, this);

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
