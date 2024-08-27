package me.topchetoeu.jscript.compilation.parsing;

import java.util.Arrays;
import java.util.LinkedHashSet;

import me.topchetoeu.jscript.common.Operation;

public enum Operator {
    MULTIPLY("*", Operation.MULTIPLY, 13, true),
    DIVIDE("/", Operation.DIVIDE, 12, true),
    MODULO("%", Operation.MODULO, 12, true),
    SUBTRACT("-", Operation.SUBTRACT, 11, true),
    ADD("+", Operation.ADD, 11, true),
    SHIFT_RIGHT(">>", Operation.SHIFT_RIGHT, 10, true),
    SHIFT_LEFT("<<", Operation.SHIFT_LEFT, 10, true),
    USHIFT_RIGHT(">>>", Operation.USHIFT_RIGHT, 10, true),
    GREATER(">", Operation.GREATER, 9, false),
    LESS("<", Operation.LESS, 9, false),
    GREATER_EQUALS(">=", Operation.GREATER_EQUALS, 9, false),
    LESS_EQUALS("<=", Operation.LESS_EQUALS, 9, false),
    NOT_EQUALS("!=", Operation.LOOSE_NOT_EQUALS, 8, false),
    LOOSE_NOT_EQUALS("!==", Operation.NOT_EQUALS, 8, false),
    EQUALS("==", Operation.LOOSE_EQUALS, 8, false),
    LOOSE_EQUALS("===", Operation.EQUALS, 8, false),
    AND("&", Operation.AND, 7, true),
    XOR("^", Operation.XOR, 6, true),
    OR("|", Operation.OR, 5, true);

    public final String readable;
    public final Operation operation;
    public final int precedence;
    public final boolean assignable;

    public static final LinkedHashSet<Operator> opsByLength = new LinkedHashSet<Operator>();

    static {
        var vals = Operator.values();
        Arrays.sort(vals, (a, b) -> b.readable.length() - a.readable.length());
        for (var el : vals) opsByLength.add(el);
    }

    private Operator(String value, Operation funcName, int precedence, boolean assignable) {
        this.readable = value;
        this.operation = funcName;
        this.precedence = precedence;
        this.assignable = assignable;
    }
}