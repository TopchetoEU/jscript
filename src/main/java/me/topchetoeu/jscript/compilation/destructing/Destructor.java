package me.topchetoeu.jscript.compilation.destructing;

import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.JavaScript;
import me.topchetoeu.jscript.compilation.JavaScript.DeclarationType;
import me.topchetoeu.jscript.compilation.values.ObjectDestructorNode;
import me.topchetoeu.jscript.compilation.values.VariableNode;

public interface Destructor {
    void destructDeclResolve(CompileResult target);

    default void destructArg(CompileResult target) {
        beforeAssign(target, null);
        afterAssign(target, null);
    }
    default void beforeAssign(CompileResult target, DeclarationType decl) {}
    void afterAssign(CompileResult target, DeclarationType decl, boolean pollute);

    public static ParseRes<Destructor> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);

        ParseRes<Destructor> first = ParseRes.first(src, i + n,
            ObjectDestructorNode::parse,
            AssignDestructorNode::parse,
            VariableNode::parse
        );
        if (first.isSuccess()) return first.addN(n);

        var exp = JavaScript.parseExpression(src, i, 2);
        if (!(exp.result instanceof Destructor destructor)) return ParseRes.error(src.loc(i + n), "Expected a destructor expression");
        n += exp.n;

        return ParseRes.res(destructor, n);
    }
}
