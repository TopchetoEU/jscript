package me.topchetoeu.jscript.compilation.patterns;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Operation;
import me.topchetoeu.jscript.common.SyntaxException;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.JavaScript;
import me.topchetoeu.jscript.compilation.Node;
import me.topchetoeu.jscript.compilation.JavaScript.DeclarationType;

public class AssignPattern implements Pattern {
    public final Location loc;
    public final AssignTarget assignable;
    public final Node value;

    @Override public Location loc() { return loc; }

    @Override public void destructDeclResolve(CompileResult target) {
        if (!(assignable instanceof Pattern p)) throw new SyntaxException(assignable.loc(), "Unexpected non-pattern in destruct context");
        p.destructDeclResolve(target);
    }

    private void common(CompileResult target) {
        target.add(Instruction.dup());
        target.add(Instruction.pushUndefined());
        target.add(Instruction.operation(Operation.EQUALS));
        var start = target.temp();
        target.add(Instruction.discard());

        value.compile(target, true);

        target.set(start, Instruction.jmpIfNot(target.size() - start));
    }

    @Override public void declare(CompileResult target, DeclarationType decl, boolean lateInitializer) {
        if (lateInitializer) {
            if (assignable instanceof Pattern p) p.declare(target, decl, lateInitializer);
            else throw new SyntaxException(assignable.loc(), "Unexpected non-pattern in destruct context");
        }
        else throw new SyntaxException(loc(), "Expected an assignment value for destructor declaration");
    }
    @Override public void destruct(CompileResult target, DeclarationType decl, boolean shouldDeclare) {
        if (!(assignable instanceof Pattern p)) throw new SyntaxException(assignable.loc(), "Unexpected non-pattern in destruct context");
        common(target);
        p.destruct(target, decl, shouldDeclare);
    }

    @Override public void beforeAssign(CompileResult target) {
        assignable.beforeAssign(target);
    }
    @Override public void afterAssign(CompileResult target, boolean pollute) {
        common(target);
        assignable.afterAssign(target, false);
    }

    public AssignPattern(Location loc, Pattern assignable, Node value) {
        this.loc = loc;
        this.assignable = assignable;
        this.value = value;
    }

    public static ParseRes<AssignPattern> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        var pattern = Pattern.parse(src, i + n, false);
        if (!pattern.isSuccess()) return pattern.chainError();
        n += pattern.n;
        n += Parsing.skipEmpty(src, i + n);

        if (!src.is(i + n, "=")) return ParseRes.failed();
        n++;

        var value = JavaScript.parseExpression(src, i + n, 2);
        if (!value.isSuccess()) return value.chainError(src.loc(i + n), "Expected a default value");
        n += value.n;

        return ParseRes.res(new AssignPattern(loc, pattern.result, value.result), n);
    }
}
