package me.topchetoeu.jscript.compilation;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.JavaScript.DeclarationType;

public class ClassStatementNode extends ClassNode {
    @Override public void compile(CompileResult target, boolean pollute, String name, BreakpointType bp) {
        super.compile(target, pollute, name, bp);
        var i = target.scope.define(DeclarationType.LET, name(), loc());
        target.add(_i -> i.index().toInit());
        if (pollute) target.add(Instruction.pushUndefined());
    }

    public ClassStatementNode(Location loc, Location end, String name, ClassBody body) {
        super(loc, end, name, body);
    }

    public static ParseRes<ClassStatementNode> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!Parsing.isIdentifier(src, i + n, "class")) return ParseRes.failed();
        n += 5;

        var name = Parsing.parseIdentifier(src, i + n);
        if (!name.isSuccess()) return name.chainError(src.loc(i + n), "Expected a class name");
        n += name.n;

        var body = parseBody(src, i + n);
        if (!body.isSuccess()) return body.chainError(src.loc(i + n), "Expected a class body");
        n += body.n;

        return ParseRes.res(new ClassStatementNode(loc, src.loc(i + n), name.result, body.result), n);
    }
}
