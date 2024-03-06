package me.topchetoeu.jscript.runtime.values;

import me.topchetoeu.jscript.common.FunctionBody;
import me.topchetoeu.jscript.runtime.Context;
import me.topchetoeu.jscript.runtime.Environment;
import me.topchetoeu.jscript.runtime.Frame;
import me.topchetoeu.jscript.runtime.scope.ValueVariable;

public class CodeFunction extends FunctionValue {
    public final FunctionBody body;
    public final ValueVariable[] captures;
    public Environment environment;

    // public Location loc() {
    //     for (var instr : body.instructions) {
    //         if (instr.location != null) return instr.location;
    //     }
    //     return null;
    // }
    // public String readable() {
    //     var loc = loc();
    //     if (loc == null) return name;
    //     else if (name.equals("")) return loc.toString();
    //     else return name + "@" + loc;
    // }

    @Override
    public Object call(Context ctx, Object thisArg, Object ...args) {
        var frame = new Frame(ctx, thisArg, args, this);

        frame.onPush();

        try {
            while (true) {
                var res = frame.next(Values.NO_RETURN, Values.NO_RETURN, null);
                if (res != Values.NO_RETURN) return res;
            }
        }
        finally {
            frame.onPop();
        }
    }

    public CodeFunction(Environment environment, String name, FunctionBody body, ValueVariable[] captures) {
        super(name, body.argsN);
        this.captures = captures;
        this.environment = environment;
        this.body = body;
    }
}
