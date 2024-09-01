package me.topchetoeu.jscript.compilation.values;

import java.util.function.IntFunction;
import java.util.function.Supplier;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Operation;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.AssignableNode;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.JavaScript;
import me.topchetoeu.jscript.compilation.Node;
import me.topchetoeu.jscript.compilation.values.operations.VariableAssignNode;
import me.topchetoeu.jscript.runtime.exceptions.SyntaxException;

public class VariableNode extends Node implements AssignableNode {
    public final String name;

    // @Override public EvalResult evaluate(CompileResult target) {
    //     var i = target.scope.getKey(name);

    //     if (i instanceof String) return EvalResult.NONE;
    //     else return EvalResult.UNKNOWN;
    // }

    @Override public Node toAssign(Node val, Operation operation) {
        return new VariableAssignNode(loc(), name, val, operation);
    }

    @Override public void compile(CompileResult target, boolean pollute) {
        var i = target.scope.get(name, true);

        if (i == null) {
            target.add(_i -> {
                if (target.scope.has(name)) throw new SyntaxException(loc(), String.format("Cannot access '%s' before initialization", name));
                return Instruction.globGet(name);
            });

            if (!pollute) target.add(Instruction.discard());
        }
        else if (pollute) {
            target.add(Instruction.loadVar(i.index()));
        }
    }

    public static IntFunction<Instruction> toGet(CompileResult target, Location loc, String name, Supplier<Instruction> onGlobal) {
        var i = target.scope.get(name, true);

        if (i == null) return _i -> {
            if (target.scope.has(name)) throw new SyntaxException(loc, String.format("Cannot access '%s' before initialization", name));
            else return onGlobal.get();
        };
        else return _i -> Instruction.loadVar(i.index());
    }
    public static IntFunction<Instruction> toGet(CompileResult target, Location loc, String name) {
        return toGet(target, loc, name, () -> Instruction.globGet(name));
    }


    public static IntFunction<Instruction> toSet(CompileResult target, Location loc, String name, boolean keep, boolean define) {
        var i = target.scope.get(name, true);

        if (i == null) return _i -> {
            if (target.scope.has(name)) throw new SyntaxException(loc, String.format("Cannot access '%s' before initialization", name));
            else return Instruction.globSet(name, keep, define);
        };
        else return _i -> Instruction.storeVar(i.index(), keep);

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
            if (literal.result.equals("await")) return ParseRes.error(src.loc(i + n), "'await' expressions are not supported.");
            if (literal.result.equals("const")) return ParseRes.error(src.loc(i + n), "'const' declarations are not supported.");
            if (literal.result.equals("let")) return ParseRes.error(src.loc(i + n), "'let' declarations are not supported.");
            return ParseRes.error(src.loc(i + n), String.format("Unexpected keyword '%s'.", literal.result));
        }

        return ParseRes.res(new VariableNode(loc, literal.result), n);
    }
}
