package me.topchetoeu.jscript.compilation.values.operations;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.AssignableNode;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.JavaScript;
import me.topchetoeu.jscript.compilation.Node;
import me.topchetoeu.jscript.compilation.values.constants.NumberNode;
import me.topchetoeu.jscript.compilation.values.constants.StringNode;

public class IndexNode extends Node implements AssignableNode {
    public final Node object;
    public final Node index;

    @Override public void compileBeforeAssign(CompileResult target, boolean op) {
        object.compile(target, true);

        if (index instanceof NumberNode num && (int)num.value == num.value) {
            if (op) {
                target.add(Instruction.dup());
                target.add(Instruction.loadMember((int)num.value));
            }
        }
        else if (index instanceof StringNode str) {
            if (op) {
                target.add(Instruction.dup());
                target.add(Instruction.loadMember(str.value));
            }
        }
        else {
            index.compile(target, true);

            if (op) {
                target.add(Instruction.dup(1, 1));
                target.add(Instruction.dup(1, 1));
                target.add(Instruction.loadMember());
            }
        }
    }
    @Override public void compileAfterAssign(CompileResult target, boolean op, boolean pollute) {
        if (index instanceof NumberNode num && (int)num.value == num.value) {
            target.add(Instruction.storeMember((int)num.value, pollute));
        }
        else if (index instanceof StringNode str) {
            target.add(Instruction.storeMember(str.value, pollute));
        }
        else {
            target.add(Instruction.storeMember(pollute));
        }
    }

    // @Override public Node toAssign(Node val, Operation operation) {
    //     return new IndexAssignNode(loc(), object, index, val, operation);
    // }
    public void compile(CompileResult target, boolean dupObj, boolean pollute) {
        object.compile(target, true);
        if (dupObj) target.add(Instruction.dup());

        if (index instanceof NumberNode num && (int)num.value == num.value) {
            target.add(Instruction.loadMember((int)num.value));
        }
        else if (index instanceof StringNode str) {
            target.add(Instruction.loadMember(str.value));
        }
        else {
            index.compile(target, true);
            target.add(Instruction.loadMember());
        }

        target.setLocationAndDebug(loc(), BreakpointType.STEP_IN);

        if (!pollute) target.add(Instruction.discard());
    }
    @Override public void compile(CompileResult target, boolean pollute) {
        compile(target, false, pollute);
    }

    public IndexNode(Location loc, Node object, Node index) {
        super(loc);
        this.object = object;
        this.index = index;
    }

    public static ParseRes<IndexNode> parseIndex(Source src, int i, Node prev, int precedence) {
        if (precedence > 18) return ParseRes.failed();

        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!src.is(i + n, "[")) return ParseRes.failed();
        n++;

        var valRes = JavaScript.parseExpression(src, i + n, 0);
        if (!valRes.isSuccess()) return valRes.chainError(src.loc(i + n), "Expected a value in index expression");
        n += valRes.n;
        n += Parsing.skipEmpty(src, i + n);

        if (!src.is(i + n, "]")) return ParseRes.error(src.loc(i + n), "Expected a closing bracket");
        n++;

        return ParseRes.res(new IndexNode(loc, prev, valRes.result), n);
    }
    public static ParseRes<IndexNode> parseMember(Source src, int i, Node prev, int precedence) {
        if (precedence > 18) return ParseRes.failed();

        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!src.is(i + n, ".")) return ParseRes.failed();
        n++;

        var literal = Parsing.parseIdentifier(src, i + n);
        if (!literal.isSuccess()) return literal.chainError(src.loc(i + n), "Expected an identifier after member access.");
        n += literal.n;

        return ParseRes.res(new IndexNode(loc, prev, new StringNode(loc, literal.result)), n);
    }
}
