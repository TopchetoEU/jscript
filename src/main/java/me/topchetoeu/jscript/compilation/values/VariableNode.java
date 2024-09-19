package me.topchetoeu.jscript.compilation.values;

import java.util.function.IntFunction;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.SyntaxException;
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

public class VariableNode extends Node implements Pattern, ChangeTarget {
    public final String name;

    public String assignName() { return name; }

    @Override public void beforeChange(CompileResult target) {
        target.add(VariableNode.toGet(target, loc(), name));
    }

    @Override public void destructDeclResolve(CompileResult target) {
        var i = target.scope.define(new Variable(name, false), loc());
        if (i != null) target.add(_i -> i.index().toUndefinedInit(false));
    }

    @Override public void afterAssign(CompileResult target, boolean pollute) {
        target.add(VariableNode.toSet(target, loc(), name, pollute));
    }

    @Override public void declare(CompileResult target, DeclarationType decl, boolean lateInitializer) {
        if (decl != null) {
            var i = target.scope.define(decl, name, loc());
            target.add(_i -> i.index().toUndefinedInit(decl.strict));
        }
        else target.add(_i -> {
            var i = target.scope.get(name, false);

            if (i == null) return Instruction.globDef(name);
            else return Instruction.nop();
        });
    }

    @Override public void destruct(CompileResult target, DeclarationType decl, boolean shouldDeclare) {
        if (!shouldDeclare || decl == null) {
            if (shouldDeclare) target.add(VariableNode.toInit(target, loc(), name));
            else target.add(VariableNode.toInit(target, loc(), name));
        }
        else {
            if (decl == DeclarationType.VAR && target.scope.has(name, false)) throw new SyntaxException(loc(), "Duplicate parameter name not allowed");
            var v = target.scope.define(decl, name, loc());
            target.add(_i -> v.index().toInit());
        }
    }

    @Override public void compile(CompileResult target, boolean pollute) {
        target.add(toGet(target, loc(), name, true, false));
    }

    public static IntFunction<Instruction> toGet(CompileResult target, Location loc, String name, boolean keep, boolean forceGet) {
        var oldI = target.scope.get(name, true);

        if (oldI != null) {
            if (keep) return _i -> oldI.index().toGet();
            else return _i -> Instruction.nop();
        }
        else return _i -> {
            var newI = target.scope.get(name, false);

            if (newI == null) return Instruction.globGet(name, forceGet);
            else if (keep) return newI.index().toGet();
            else return Instruction.nop();
        };
    }
    public static IntFunction<Instruction> toGet(CompileResult target, Location loc, String name) {
        return toGet(target, loc, name, true, false);
    }

    public static IntFunction<Instruction> toInit(CompileResult target, Location loc, String name) {
        var oldI = target.scope.get(name, true);

        if (oldI != null) return _i -> oldI.index().toInit();
        else return _i -> {
            var i = target.scope.get(name, false);

            if (i == null) return Instruction.globSet(name, false, true);
            else return i.index().toInit();
        };
    }
    public static IntFunction<Instruction> toSet(CompileResult target, Location loc, String name, boolean keep) {
        var oldI = target.scope.get(name, true);

        if (oldI != null) return _i -> oldI.index().toSet(keep);
        else return _i -> {
            var i = target.scope.get(name, false);

            if (i == null) return Instruction.globSet(name, keep, false);
            else return i.index().toSet(keep);
        };
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
