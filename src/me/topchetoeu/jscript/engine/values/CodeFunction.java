package me.topchetoeu.jscript.engine.values;

import java.util.LinkedHashMap;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Instruction.Type;
import me.topchetoeu.jscript.engine.CallContext;
import me.topchetoeu.jscript.engine.Environment;
import me.topchetoeu.jscript.engine.frame.CodeFrame;
import me.topchetoeu.jscript.engine.scope.ValueVariable;

public class CodeFunction extends FunctionValue {
    public final int localsN;
    public final int length;
    public final Instruction[] body;
    public final LinkedHashMap<Location, Integer> breakableLocToIndex = new LinkedHashMap<>();
    public final LinkedHashMap<Integer, Location> breakableIndexToLoc = new LinkedHashMap<>();
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
    public Object call(CallContext ctx, Object thisArg, Object... args) throws InterruptedException {
        return new CodeFrame(ctx, thisArg, args, this).run(new CallContext(ctx, environment));
    }

    public CodeFunction(String name, int localsN, int length, Environment environment, ValueVariable[] captures, Instruction[] body) {
        super(name, length);
        this.captures = captures;
        this.environment = environment;
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
