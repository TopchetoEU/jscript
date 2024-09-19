package me.topchetoeu.jscript.compilation.patterns;

import me.topchetoeu.jscript.common.SyntaxException;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.JavaScript;
import me.topchetoeu.jscript.compilation.JavaScript.DeclarationType;

public class Binding implements Pattern {
    public final Location loc;
    public final DeclarationType type;
    public final AssignTarget assignable;

    @Override public Location loc() { return loc; }

    @Override public void destructDeclResolve(CompileResult target) {
        if (type != null && !type.strict) {
            if (!(assignable instanceof Pattern p)) throw new SyntaxException(assignable.loc(), "Unexpected non-pattern in destruct context");
            p.destructDeclResolve(target);
        }
    }

    @Override public void destruct(CompileResult target, DeclarationType decl, boolean shouldDeclare) {
        if (!(assignable instanceof Pattern p)) throw new SyntaxException(assignable.loc(), "Unexpected non-pattern in destruct context");
        p.destruct(target, decl, shouldDeclare);
    }
    @Override public void declare(CompileResult target, DeclarationType decl, boolean lateInitializer) {
        if (!(assignable instanceof Pattern p)) throw new SyntaxException(assignable.loc(), "Unexpected non-pattern in destruct context");
        p.declare(target, decl, lateInitializer);
    }

    public void resolve(CompileResult target) {
        if (type != null) destructDeclResolve(target);
    }

    public void declare(CompileResult target, boolean hasInit) {
        if (type != null) destructVar(target, type, hasInit);
    }
    public void declareLateInit(CompileResult target) {
        if (type != null) declare(target, type, true);
    }

    @Override public void afterAssign(CompileResult target, boolean pollute) {
        assignable.assign(target, pollute);
    }

    public Binding(Location loc, DeclarationType type, AssignTarget assignable) {
        this.loc = loc;
        this.type = type;
        this.assignable = assignable;
    }

    public static ParseRes<Binding> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        var declType = JavaScript.parseDeclarationType(src, i + n);
        if (!declType.isSuccess()) {
            var res = JavaScript.parseExpression(src, i + n, 13);
            if (res.isSuccess() && res.result instanceof AssignTargetLike target) {
                n += res.n;
                return ParseRes.res(new Binding(loc, null, target.toAssignTarget()), n);
            }
            else return ParseRes.failed();
        }
        else {
            n += declType.n;
            n += Parsing.skipEmpty(src, i + n);

            var res = Pattern.parse(src, i + n, false);
            if (!res.isSuccess()) return ParseRes.failed();
            n += res.n;

            return ParseRes.res(new Binding(loc, declType.result, res.result), n);
        }
    }
}
