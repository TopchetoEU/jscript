package me.topchetoeu.jscript.runtime.values;

import me.topchetoeu.jscript.common.FunctionBody;
import me.topchetoeu.jscript.runtime.Context;
import me.topchetoeu.jscript.runtime.Extensions;
import me.topchetoeu.jscript.runtime.Frame;
import me.topchetoeu.jscript.runtime.scope.ValueVariable;

public class CodeFunction extends FunctionValue {
    public final FunctionBody body;
    public final ValueVariable[] captures;
    public Extensions extensions;

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
    public Object call(Extensions ext, Object thisArg, Object ...args) {
        var frame = new Frame(Context.of(ext), thisArg, args, this);

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

    public CodeFunction(Extensions extensions, String name, FunctionBody body, ValueVariable[] captures) {
        super(name, body.argsN);
        this.captures = captures;
        this.extensions = extensions;
        this.body = body;
    }
}
