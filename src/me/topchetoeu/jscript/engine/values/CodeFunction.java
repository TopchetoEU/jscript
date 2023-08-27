package me.topchetoeu.jscript.engine.values;

import java.util.LinkedHashMap;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Instruction.Type;
import me.topchetoeu.jscript.engine.CallContext;
import me.topchetoeu.jscript.engine.frame.CodeFrame;
import me.topchetoeu.jscript.engine.scope.GlobalScope;
import me.topchetoeu.jscript.engine.scope.ValueVariable;

public class CodeFunction extends FunctionValue {
    public final int localsN;
    public final int length;
    public final Instruction[] body;
    public final LinkedHashMap<Location, Integer> breakableLocToIndex = new LinkedHashMap<>();
    public final LinkedHashMap<Integer, Location> breakableIndexToLoc = new LinkedHashMap<>();
    public final ValueVariable[] captures;
    public final GlobalScope globals;

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
    public Object call(CallContext ctx, Object thisArg, Object... args) throws InterruptedException {
        return new CodeFrame(ctx, thisArg, args, this).run(ctx);
    }

    public CodeFunction(String name, int localsN, int length, GlobalScope globals, ValueVariable[] captures, Instruction[] body) {
        super(name, length);
        this.captures = captures;
        this.globals = globals;
        this.localsN = localsN;
        this.length = length;
        this.body = body;

        for (var i = 0; i < body.length; i++) {
            if (body[i].type == Type.LOAD_FUNC) i += (int)body[i].get(0);
            if (body[i].debugged && body[i].location != null) {
                breakableLocToIndex.put(body[i].location, i);
                breakableIndexToLoc.put(i, body[i].location);
            }
        }
    }
}
