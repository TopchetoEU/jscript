package me.topchetoeu.jscript.compilation.control;

import java.util.List;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.common.ParseRes;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.Instruction.Type;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.parsing.Operator;
import me.topchetoeu.jscript.compilation.parsing.Parsing;
import me.topchetoeu.jscript.compilation.parsing.Token;

public class WhileStatement extends Statement {
    public final Statement condition, body;
    public final String label;

    @Override
    public void declare(CompileResult target) {
        body.declare(target);
    }
    @Override
    public void compile(CompileResult target, boolean pollute) {
        int start = target.size();
        condition.compile(target, true);
        int mid = target.temp();
        body.compile(target, false, BreakpointType.STEP_OVER);

        int end = target.size();

        replaceBreaks(target, label, mid + 1, end, start, end + 1);

        target.add(Instruction.jmp(start - end));
        target.set(mid, Instruction.jmpIfNot(end - mid + 1));
        if (pollute) target.add(Instruction.pushUndefined());
    }

    public static ParseRes<String> parseLabel(List<Token> tokens, int i) {
        int n = 0;
    
        var nameRes = Parsing.parseIdentifier(tokens, i + n++);
        if (!Parsing.isOperator(tokens, i + n++, Operator.COLON)) return ParseRes.failed();
    
        return ParseRes.res(nameRes.result, n);
    }
    public WhileStatement(Location loc, String label, Statement condition, Statement body) {
        super(loc);
        this.label = label;
        this.condition = condition;
        this.body = body;
    }

    public static void replaceBreaks(CompileResult target, String label, int start, int end, int continuePoint, int breakPoint) {
        for (int i = start; i < end; i++) {
            var instr = target.get(i);
            if (instr.type == Type.NOP && instr.is(0, "cont") && (instr.get(1) == null || instr.is(1, label))) {
                target.set(i, Instruction.jmp(continuePoint - i));
            }
            if (instr.type == Type.NOP && instr.is(0, "break") && (instr.get(1) == null || instr.is(1, label))) {
                target.set(i, Instruction.jmp(breakPoint - i));
            }
        }
    }

    public static ParseRes<WhileStatement> parseWhile(Filename filename, List<Token> tokens, int i) {
        var loc = Parsing.getLoc(filename, tokens, i);
        int n = 0;

        var labelRes = WhileStatement.parseLabel(tokens, i + n);
        n += labelRes.n;

        if (!Parsing.isIdentifier(tokens, i + n++, "while")) return ParseRes.failed();
        if (!Parsing.isOperator(tokens, i + n++, Operator.PAREN_OPEN)) return ParseRes.error(loc, "Expected a open paren after 'while'.");

        var condRes = Parsing.parseValue(filename, tokens, i + n, 0);
        if (!condRes.isSuccess()) return ParseRes.error(loc, "Expected a while condition.", condRes);
        n += condRes.n;

        if (!Parsing.isOperator(tokens, i + n++, Operator.PAREN_CLOSE)) return ParseRes.error(loc, "Expected a closing paren after while condition.");

        var res = Parsing.parseStatement(filename, tokens, i + n);
        if (!res.isSuccess()) return ParseRes.error(loc, "Expected a while body.", res);
        n += res.n;

        return ParseRes.res(new WhileStatement(loc, labelRes.result, condRes.result, res.result), n);
    }
}
