package me.topchetoeu.jscript.compilation.patterns;

import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.JavaScript.DeclarationType;
import me.topchetoeu.jscript.compilation.values.VariableNode;

/**
 * Represents all nodes that can be a destructors (note that all destructors are assign targets, too)
 */
public interface Pattern extends AssignTarget {
    Location loc();

    /**
     * Called when the destructor has to declare 
     * @param target
     */
    void destructDeclResolve(CompileResult target);

    /**
     * Called when a declaration-like is being destructed
     * @param decl The variable type the destructor must declare, if it is a named pne
     */
    void destruct(CompileResult target, DeclarationType decl, boolean shouldDeclare);

    /**
     * Run when destructing a declaration without an initializer
     */
    void declare(CompileResult target, DeclarationType decl, boolean lateInitializer);

    public default void destructArg(CompileResult target, DeclarationType decl) {
        destruct(target, decl, false);
    }
    public default void destructVar(CompileResult target, DeclarationType decl, boolean hasInitializer) {
        if (hasInitializer) {
            if (decl == null || !decl.strict) destruct(target, null, true);
            else destruct(target, decl, true);
        }
        else {
            if (decl == null || !decl.strict) declare(target, null, false);
            else declare(target, decl, false);
        }
    }

    public static ParseRes<Pattern> parse(Source src, int i, boolean withDefault) {
        return withDefault ?
            ParseRes.first(src, i,
                AssignPattern::parse,
                ObjectPattern::parse,
                VariableNode::parse
            ) :
            ParseRes.first(src, i,
                ObjectPattern::parse,
                VariableNode::parse
            );
    }
}
