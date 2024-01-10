package me.topchetoeu.jscript.core.engine.values;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.core.compilation.FunctionBody;
import me.topchetoeu.jscript.core.compilation.Instruction;
import me.topchetoeu.jscript.core.engine.Context;
import me.topchetoeu.jscript.core.engine.Environment;
import me.topchetoeu.jscript.core.engine.frame.CodeFrame;
import me.topchetoeu.jscript.core.engine.scope.ValueVariable;

public class CodeFunction extends FunctionValue {
    public final int localsN;
    public final Instruction[] body;
    public final String[] captureNames, localNames;
    public final ValueVariable[] captures;
    public Environment environment;

    public Location loc() {
        for (var instr : body) {
            if (instr.location != null) return instr.location;
        }
        return null;
    }
    public String readable() {
        var loc = loc();
        if (loc == null) return name;
        else if (name.equals("")) return loc.toString();
        else return name + "@" + loc;
    }

    @Override
    public Object call(Context ctx, Object thisArg, Object ...args) {
        var frame = new CodeFrame(ctx, thisArg, args, this);

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

    public CodeFunction(Environment environment, String name, FunctionBody body, ValueVariable... captures) {
        super(name, body.argsN);
        this.captures = captures;
        this.captureNames = body.captureNames;
        this.localNames = body.localNames;
        this.environment = environment;
        this.localsN = body.localsN;
        this.body = body.instructions;
    }
}
