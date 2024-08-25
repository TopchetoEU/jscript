package me.topchetoeu.jscript.compilation.control;

import java.util.List;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.common.ParseRes;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.parsing.Operator;
import me.topchetoeu.jscript.compilation.parsing.Parsing;
import me.topchetoeu.jscript.compilation.parsing.Token;

public class IfStatement extends Statement {
    public final Statement condition, body, elseBody;

    @Override
    public void declare(CompileResult target) {
        body.declare(target);
        if (elseBody != null) elseBody.declare(target);
    }

    @Override public void compile(CompileResult target, boolean pollute, BreakpointType breakpoint) {
        condition.compile(target, true, breakpoint);

        if (elseBody == null) {
            int i = target.temp();
            body.compile(target, pollute, breakpoint);
            int endI = target.size();
            target.set(i, Instruction.jmpIfNot(endI - i));
        }
        else {
            int start = target.temp();
            body.compile(target, pollute, breakpoint);
            int mid = target.temp();
            elseBody.compile(target, pollute, breakpoint);
            int end = target.size();

            target.set(start, Instruction.jmpIfNot(mid - start + 1));
            target.set(mid, Instruction.jmp(end - mid));
        }
    }
    @Override public void compile(CompileResult target, boolean pollute) {
        compile(target, pollute, BreakpointType.STEP_IN);
    }

    public IfStatement(Location loc, Statement condition, Statement body, Statement elseBody) {
        super(loc);
        this.condition = condition;
        this.body = body;
        this.elseBody = elseBody;
    }

    public static ParseRes<IfStatement> parseTernary(Filename filename, List<Token> tokens, int i, Statement prev, int precedence) {
        var loc = Parsing.getLoc(filename, tokens, i);
        var n = 0;

        if (precedence > 2) return ParseRes.failed();
        if (!Parsing.isOperator(tokens, i + n++, Operator.QUESTION)) return ParseRes.failed();

        var a = Parsing.parseValue(filename, tokens, i + n, 2);
        if (!a.isSuccess()) return ParseRes.error(loc, "Expected a value after the ternary operator.", a);
        n += a.n;

        if (!Parsing.isOperator(tokens, i + n++, Operator.COLON)) return ParseRes.failed();

        var b = Parsing.parseValue(filename, tokens, i + n, 2);
        if (!b.isSuccess()) return ParseRes.error(loc, "Expected a second value after the ternary operator.", b);
        n += b.n;

        return ParseRes.res(new IfStatement(loc, prev, a.result, b.result), n);
    }
    public static ParseRes<IfStatement> parse(Filename filename, List<Token> tokens, int i) {
        var loc = Parsing.getLoc(filename, tokens, i);
        int n = 0;

        if (!Parsing.isIdentifier(tokens, i + n++, "if")) return ParseRes.failed();
        if (!Parsing.isOperator(tokens, i + n++, Operator.PAREN_OPEN)) return ParseRes.error(loc, "Expected a open paren after 'if'.");

        var condRes = Parsing.parseValue(filename, tokens, i + n, 0);
        if (!condRes.isSuccess()) return ParseRes.error(loc, "Expected an if condition.", condRes);
        n += condRes.n;

        if (!Parsing.isOperator(tokens, i + n++, Operator.PAREN_CLOSE)) return ParseRes.error(loc, "Expected a closing paren after if condition.");

        var res = Parsing.parseStatement(filename, tokens, i + n);
        if (!res.isSuccess()) return ParseRes.error(loc, "Expected an if body.", res);
        n += res.n;

        if (!Parsing.isIdentifier(tokens, i + n, "else")) return ParseRes.res(new IfStatement(loc, condRes.result, res.result, null), n);
        n++;

        var elseRes = Parsing.parseStatement(filename, tokens, i + n);
        if (!elseRes.isSuccess()) return ParseRes.error(loc, "Expected an else body.", elseRes);
        n += elseRes.n;

        return ParseRes.res(new IfStatement(loc, condRes.result, res.result, elseRes.result), n);
    }

}
