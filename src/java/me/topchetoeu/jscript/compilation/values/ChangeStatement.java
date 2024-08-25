package me.topchetoeu.jscript.compilation.values;

import java.util.List;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.common.Operation;
import me.topchetoeu.jscript.common.ParseRes;
import me.topchetoeu.jscript.compilation.AssignableStatement;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.parsing.Operator;
import me.topchetoeu.jscript.compilation.parsing.Parsing;
import me.topchetoeu.jscript.compilation.parsing.Token;

public class ChangeStatement extends Statement {
    public final AssignableStatement value;
    public final double addAmount;
    public final boolean postfix;

    @Override public void compile(CompileResult target, boolean pollute) {
        value.toAssign(new ConstantStatement(loc(), -addAmount), Operation.SUBTRACT).compile(target, true);
        if (!pollute) target.add(Instruction.discard());
        else if (postfix) {
            target.add(Instruction.pushValue(addAmount));
            target.add(Instruction.operation(Operation.SUBTRACT));
        }
    }

    public ChangeStatement(Location loc, AssignableStatement value, double addAmount, boolean postfix) {
        super(loc);
        this.value = value;
        this.addAmount = addAmount;
        this.postfix = postfix;
    }

    public static ParseRes<ChangeStatement> parsePrefix(Filename filename, List<Token> tokens, int i) {
        var loc = Parsing.getLoc(filename, tokens, i);
        int n = 0;
    
        var opState = Parsing.parseOperator(tokens, i + n++);
        if (!opState.isSuccess()) return ParseRes.failed();
    
        int change = 0;
    
        if (opState.result == Operator.INCREASE) change = 1;
        else if (opState.result == Operator.DECREASE) change = -1;
        else return ParseRes.failed();
    
        var res = Parsing.parseValue(filename, tokens, i + n, 15);
        if (!(res.result instanceof AssignableStatement)) return ParseRes.error(loc, "Expected assignable value after prefix operator.");
        return ParseRes.res(new ChangeStatement(loc, (AssignableStatement)res.result, change, false), n + res.n);
    }
    public static ParseRes<ChangeStatement> parsePostfix(Filename filename, List<Token> tokens, int i, Statement prev, int precedence) {
        var loc = Parsing.getLoc(filename, tokens, i);
        int n = 0;
    
        if (precedence > 15) return ParseRes.failed();
    
        var opState = Parsing.parseOperator(tokens, i + n++);
        if (!opState.isSuccess()) return ParseRes.failed();
    
        int change = 0;
    
        if (opState.result == Operator.INCREASE) change = 1;
        else if (opState.result == Operator.DECREASE) change = -1;
        else return ParseRes.failed();
    
        if (!(prev instanceof AssignableStatement)) return ParseRes.error(loc, "Expected assignable value before suffix operator.");
        return ParseRes.res(new ChangeStatement(loc, (AssignableStatement)prev, change, true), n);
    }
}
