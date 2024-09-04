package me.topchetoeu.jscript.compilation.parsing;

import java.util.HashMap;
import java.util.Map;

import me.topchetoeu.jscript.common.Operation;

public enum Operator {
    MULTIPLY("*", Operation.MULTIPLY, 13),
    DIVIDE("/", Operation.DIVIDE, 12),
    MODULO("%", Operation.MODULO, 12),
    SUBTRACT("-", Operation.SUBTRACT, 11),
    ADD("+", Operation.ADD, 11),
    SHIFT_RIGHT(">>", Operation.SHIFT_RIGHT, 10),
    SHIFT_LEFT("<<", Operation.SHIFT_LEFT, 10),
    USHIFT_RIGHT(">>>", Operation.USHIFT_RIGHT, 10),
    GREATER(">", Operation.GREATER, 9),
    LESS("<", Operation.LESS, 9),
    GREATER_EQUALS(">=", Operation.GREATER_EQUALS, 9),
    LESS_EQUALS("<=", Operation.LESS_EQUALS, 9),
    NOT_EQUALS("!=", Operation.LOOSE_NOT_EQUALS, 8),
    LOOSE_NOT_EQUALS("!==", Operation.NOT_EQUALS, 8),
    EQUALS("==", Operation.LOOSE_EQUALS, 8),
    LOOSE_EQUALS("===", Operation.EQUALS, 8),
    AND("&", Operation.AND, 7),
    XOR("^", Operation.XOR, 6),
    OR("|", Operation.OR, 5),
    LAZY_AND("&&", 4),
    LAZY_OR("||", 3),
    ASSIGN_SHIFT_LEFT("<<=", 2, true),
    ASSIGN_SHIFT_RIGHT(">>=", 2, true),
    ASSIGN_USHIFT_RIGHT(">>>=", 2, true),
    ASSIGN_AND("&=", 2, true),
    ASSIGN_OR("|=", 2, true),
    ASSIGN_XOR("^=", 2, true),
    ASSIGN_MODULO("%=", 2, true),
    ASSIGN_DIVIDE("/=", 2, true),
    ASSIGN_MULTIPLY("*=", 2, true),
    ASSIGN_SUBTRACT("-=", 2, true),
    ASSIGN_ADD("+=", 2, true),
    ASSIGN("=", 2, true),
    SEMICOLON(";"),
    COLON(":"),
    PAREN_OPEN("("),
    PAREN_CLOSE(")"),
    BRACKET_OPEN("["),
    BRACKET_CLOSE("]"),
    BRACE_OPEN("{"),
    BRACE_CLOSE("}"),
    DOT("."),
    COMMA(","),
    NOT("!"),
    QUESTION("?"),
    INVERSE("~"),
    INCREASE("++"),
    DECREASE("--");

    public final String readable;
    public final Operation operation;
    public final int precedence;
    public final boolean reverse;
    private static final Map<String, Operator> ops = new HashMap<>();

    static {
        for (var el : Operator.values()) {
            ops.put(el.readable, el);
        }
    }

    public boolean isAssign() { return precedence == 2; }

    public static Operator parse(String val) {
        return ops.get(val);
    }

    private Operator() {
        this.readable = null;
        this.operation = null;
        this.precedence = -1;
        this.reverse = false;
    }
    private Operator(String value) {
        this.readable = value;
        this.operation = null;
        this.precedence = -1;
        this.reverse = false;
    }
    private Operator(String value, int precedence) {
        this.readable = value;
        this.operation = null;
        this.precedence = precedence;
        this.reverse = false;
    }
    private Operator(String value, int precedence, boolean reverse) {
        this.readable = value;
        this.operation = null;
        this.precedence = precedence;
        this.reverse = reverse;
    }

    private Operator(String value, Operation funcName, int precedence) {
        this.readable = value;
        this.operation = funcName;
        this.precedence = precedence;
        this.reverse = false;
    }
    private Operator(String value, Operation funcName, int precedence, boolean reverse) {
        this.readable = value;
        this.operation = funcName;
        this.precedence = precedence;
        this.reverse = reverse;
    }
}