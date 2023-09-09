package me.topchetoeu.jscript.engine.values;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.FunctionContext;
import me.topchetoeu.jscript.engine.frame.CodeFrame;
import me.topchetoeu.jscript.engine.scope.ValueVariable;

public class CodeFunction extends FunctionValue {
    public final int localsN;
    public final int length;
    public final Instruction[] body;
    public final ValueVariable[] captures;
    public FunctionContext environment;

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
    public Object call(Context ctx, Object thisArg, Object ...args) throws InterruptedException {
        return new CodeFrame(ctx, thisArg, args, this).run(new Context(environment, ctx.message));
    }

    public CodeFunction(FunctionContext environment, String name, int localsN, int length, ValueVariable[] captures, Instruction[] body) {
        super(name, length);
        this.captures = captures;
        this.environment = environment;
        this.localsN = localsN;
        this.length = length;
        this.body = body;
    }
}
