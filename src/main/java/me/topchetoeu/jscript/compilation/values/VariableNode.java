package me.topchetoeu.jscript.compilation.values;

import java.util.function.IntFunction;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.JavaScript;
import me.topchetoeu.jscript.compilation.Node;
import me.topchetoeu.jscript.compilation.JavaScript.DeclarationType;
import me.topchetoeu.jscript.compilation.patterns.ChangeTarget;
import me.topchetoeu.jscript.compilation.patterns.Pattern;
import me.topchetoeu.jscript.compilation.scope.Variable;
import me.topchetoeu.jscript.runtime.exceptions.SyntaxException;

public class VariableNode extends Node implements Pattern, ChangeTarget {
    public final String name;

    public String assignName() { return name; }

    // @Override public void compileBeforeAssign(CompileResult target, boolean operator) {
    //     if (operator) {
    //         target.add(VariableNode.toGet(target, loc(), name));
    //     }
    // }
    // @Override public void compileAfterAssign(CompileResult target, boolean operator, boolean pollute) {
    //     target.add(VariableNode.toSet(target, loc(), name, pollute, false));
    // }

    @Override public void beforeChange(CompileResult target) {
        target.add(VariableNode.toGet(target, loc(), name));
    }

    // @Override public void destructArg(CompileResult target) {
    //     target.add(_i -> target.scope.define(new Variable(name, false), loc()).index().toSet(false));
    // }
    @Override public void destructDeclResolve(CompileResult target) {
        target.scope.define(new Variable(name, false), loc());
    }

    @Override public void afterAssign(CompileResult target, boolean pollute) {
        target.add(VariableNode.toSet(target, loc(), name, pollute, false));
    }

    @Override public void declare(CompileResult target, DeclarationType decl) {
        if (decl != null) {
            if (decl.strict) target.scope.defineStrict(new Variable(name, decl.readonly), loc());
            else target.scope.define(new Variable(name, decl.readonly), loc());
        }
        else target.add(_i -> {
            var i = target.scope.get(name, false);

            if (i == null) return Instruction.globDef(name);
            else return Instruction.nop();
        });
    }

    @Override public void destruct(CompileResult target, DeclarationType decl, boolean shouldDeclare) {
        if (!shouldDeclare || decl == null) {
            target.add(VariableNode.toSet(target, loc(), name, false, shouldDeclare));
        }
        else {
            if (decl == DeclarationType.VAR && target.scope.has(name, false)) throw new SyntaxException(loc(), "Duplicate parameter name not allowed");
            var v = target.scope.define(decl, name, loc());
            target.add(_i -> v.index().toSet(false));
        }
    }

    @Override public void compile(CompileResult target, boolean pollute) {
        var i = target.scope.get(name, false);

        if (i == null) {
            target.add(_i -> {
                if (target.scope.has(name, false)) return Instruction.throwSyntax(loc(), String.format("Cannot access '%s' before initialization", name));
                return Instruction.globGet(name, false);
            });

            if (!pollute) target.add(Instruction.discard());
        }
        else if (pollute) target.add(_i -> i.index().toGet());
    }

    public static IntFunction<Instruction> toGet(CompileResult target, Location loc, String name, boolean forceGet) {
        var i = target.scope.get(name, false);

        if (i == null) return _i -> {
            if (target.scope.has(name, false)) return Instruction.throwSyntax(loc, String.format("Cannot access '%s' before initialization", name));
            else return Instruction.globGet(name, forceGet);
        };
        else return _i -> i.index().toGet();
    }
    public static IntFunction<Instruction> toGet(CompileResult target, Location loc, String name) {
        return toGet(target, loc, name, false);
    }


    public static IntFunction<Instruction> toSet(CompileResult target, Location loc, String name, boolean keep, boolean define) {
        var i = target.scope.get(name, false);

        if (i == null) return _i -> {
            if (target.scope.has(name, false)) return Instruction.throwSyntax(loc, String.format("Cannot access '%s' before initialization", name));
            else return Instruction.globSet(name, keep, define);
        };
        else if (!define && i.readonly) return _i -> Instruction.throwSyntax(new SyntaxException(loc, "Assignment to constant variable"));
        else return _i -> i.index().toSet(keep);
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
