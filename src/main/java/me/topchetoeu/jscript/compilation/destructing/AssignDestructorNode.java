package me.topchetoeu.jscript.compilation.destructing;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Operation;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.DeferredIntSupplier;
import me.topchetoeu.jscript.compilation.JavaScript;
import me.topchetoeu.jscript.compilation.Node;
import me.topchetoeu.jscript.compilation.JavaScript.DeclarationType;
import me.topchetoeu.jscript.compilation.scope.Variable;
import me.topchetoeu.jscript.compilation.values.VariableNode;

public class AssignDestructorNode extends Node implements Destructor {
    public final String name;
    public final Node value;

    @Override public void destructDeclResolve(CompileResult target) {
        target.scope.define(new Variable(name, false), loc());
    }
    @Override public void destructArg(CompileResult target) {
        var v = target.scope.define(new Variable(name, false), loc());
        destructAssign(target);
        target.add(_i -> v.index().toSet(false));
    }
    @Override public void afterAssign(CompileResult target, DeclarationType decl) {
        if (decl != null && decl.strict) target.scope.define(new Variable(name, decl.readonly), loc());
        var end = new DeferredIntSupplier();

        target.add(Instruction.dup());
        target.add(Instruction.pushUndefined());
        target.add(Instruction.operation(Operation.EQUALS));
        target.add(Instruction.jmpIfNot(end));
        target.add(Instruction.discard());
        value.compile(target, true);

        end.set(target.size());

        target.add(VariableNode.toSet(target, loc(), name, false, decl != null));
    }

    public AssignDestructorNode(Location loc, String name, Node value) {
        super(loc);
        this.name = name;
        this.value = value;
    }

    public static ParseRes<AssignDestructorNode> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        var name = Parsing.parseIdentifier(src, i);
        if (!JavaScript.checkVarName(null)) return ParseRes.error(src.loc(i + n), String.format("Unexpected keyword '%s'", name.result));
        n += name.n;
        n += Parsing.skipEmpty(src, i + n);

        if (!src.is(i, "=")) return ParseRes.failed();
        n++;

        var value = JavaScript.parseExpression(src, i, 2);
        if (value.isError()) return ParseRes.error(src.loc(i + n), "Expected a value after '='");
        n += value.n;

        return ParseRes.res(new AssignDestructorNode(loc, name.result, value.result), n);
    }
}
