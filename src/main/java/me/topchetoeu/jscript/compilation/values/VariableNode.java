package me.topchetoeu.jscript.compilation.values;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.JavaScript;
import me.topchetoeu.jscript.compilation.Node;
import me.topchetoeu.jscript.compilation.patterns.ChangeTarget;

public class VariableNode extends Node implements ChangeTarget {
    public final String name;

    public String assignName() { return name; }

    @Override public void beforeChange(CompileResult target) {
        target.add(VariableNode.toGet(target, loc(), name));
    }

    @Override public void afterAssign(CompileResult target, boolean pollute) {
        target.add(VariableNode.toSet(target, loc(), name, pollute, false));
    }

    @Override public void compile(CompileResult target, boolean pollute) {
        target.add(toGet(target, loc(), name, true, false)).setLocation(loc());
    }

    public static Instruction toGet(CompileResult target, Location loc, String name, boolean keep, boolean forceGet) {
        var i = target.scope.get(name, false);

        if (i != null) {
            if (keep) return i.index().toGet();
            else return Instruction.nop();
        }
        else return Instruction.globGet(name, forceGet);
    }
    public static Instruction toGet(CompileResult target, Location loc, String name) {
        return toGet(target, loc, name, true, false);
    }

    public static Instruction toSet(CompileResult target, Location loc, String name, boolean keep, boolean init) {
        var i = target.scope.get(name, false);

        if (i != null) return i.index().toSet(keep);
        else return Instruction.globSet(name, keep, init);
    }

    public VariableNode(Location loc, String name) {
        super(loc);
        this.name = name;
    }

    public static ParseRes<VariableNode> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        var literal = Parsing.parseIdentifier(src, i);
        if (!literal.isSuccess()) return literal.chainError();
        n += literal.n;

        if (!JavaScript.checkVarName(literal.result)) {
            if (literal.result.equals("await")) return ParseRes.error(src.loc(i + n), "'await' expressions are not supported");
            return ParseRes.error(src.loc(i + n), String.format("Unexpected keyword '%s'", literal.result));
        }

        return ParseRes.res(new VariableNode(loc, literal.result), n);
    }
}
