package me.topchetoeu.jscript.engine.scope;

import java.util.ArrayList;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.values.NativeFunction;
import me.topchetoeu.jscript.engine.values.ObjectValue;

public class LocalScope {
    private String[] names;
    public final ValueVariable[] captures;
    public final ValueVariable[] locals;
    public final ArrayList<ValueVariable> catchVars = new ArrayList<>();

    public ValueVariable get(int i) {
        if (i >= locals.length)
            return catchVars.get(i - locals.length);
        if (i >= 0) return locals[i];
        else return captures[~i];
    }

    public String[] getCaptureNames() {
        var res = new String[captures.length];

        for (int i = 0; i < captures.length; i++) {
            if (names == null || i >= names.length) res[i] = "capture_" + (i);
            else res[i] = names[i];
        }

        return res;
    }
    public String[] getLocalNames() {
        var res = new String[locals.length];

        for (int i = captures.length, j = 0; i < locals.length; i++, j++) {
            if (names == null || i >= names.length) {
                if (j == 0) res[j] = "this";
                else if (j == 1) res[j] = "arguments";
                else res[i] = "local_" + (j - 2);
            }
            else res[i] = names[i];
        }

        return res;
    }
    public void setNames(String[] names) {
        this.names = names;
    }

    public int size() {
        return captures.length + locals.length;
    }

    public void applyToObject(Context ctx, ObjectValue locals, ObjectValue captures, boolean props) {
        var localNames = getLocalNames();
        var captureNames = getCaptureNames();

        for (var i = 0; i < this.locals.length; i++) {
            var name = localNames[i];
            var _i = i;

            if (props) locals.defineProperty(ctx, name,
                new NativeFunction(name, (_ctx, thisArg, args) -> this.locals[_i].get(_ctx)),
                this.locals[i].readonly ? null :
                    new NativeFunction(name, (_ctx, thisArg, args) -> { this.locals[_i].set(_ctx, args.length < 1 ? null : args[0]); return null; }),
                true, true
            );
            else locals.defineProperty(ctx, name, this.locals[i].get(ctx));
        }
        for (var i = 0; i < this.captures.length; i++) {
            var name = captureNames[i];
            var _i = i;

            if (props) captures.defineProperty(ctx, name,
                new NativeFunction(name, (_ctx, thisArg, args) -> this.captures[_i].get(_ctx)),
                this.captures[i].readonly ? null :
                    new NativeFunction(name, (_ctx, thisArg, args) -> { this.captures[_i].set(_ctx, args.length < 1 ? null : args[0]); return null; }),
                true, true
            );
            else captures.defineProperty(ctx, name, this.captures[i].get(ctx));
        }

        captures.setPrototype(ctx, locals);
    }

    public LocalScope(int n, ValueVariable[] captures) {
        locals = new ValueVariable[n];
        this.captures = captures;

        for (int i = 0; i < n; i++) {
            locals[i] = new ValueVariable(false, null);
        }
    }
}
