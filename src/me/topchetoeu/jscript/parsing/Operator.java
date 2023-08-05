package me.topchetoeu.jscript.parsing;

import java.util.HashMap;
import java.util.Map;

import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Instruction.Type;

public enum Operator {
    MULTIPLY("*", Type.MULTIPLY, 13),
    DIVIDE("/", Type.DIVIDE, 12),
    MODULO("%", Type.MODULO, 12),
    SUBTRACT("-", Type.SUBTRACT, 11),
    ADD("+", Type.ADD, 11),
    SHIFT_RIGHT(">>", Type.SHIFT_RIGHT, 10),
    SHIFT_LEFT("<<", Type.SHIFT_LEFT, 10),
    USHIFT_RIGHT(">>>", Type.USHIFT_RIGHT, 10),
    GREATER(">", Type.GREATER, 9),
    LESS("<", Type.LESS, 9),
    GREATER_EQUALS(">=", Type.GREATER_EQUALS, 9),
    LESS_EQUALS("<=", Type.LESS_EQUALS, 9),
    NOT_EQUALS("!=", Type.LOOSE_NOT_EQUALS, 8),
    LOOSE_NOT_EQUALS("!==", Type.NOT_EQUALS, 8),
    EQUALS("==", Type.LOOSE_EQUALS, 8),
    LOOSE_EQUALS("===", Type.EQUALS, 8),
    AND("&", Type.AND, 7),
    XOR("^", Type.XOR, 6),
    OR("|", Type.OR, 5),
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

    public final String value;
    public final Instruction.Type operation;
    public final int precedence;
    public final boolean reverse;
    private static final Map<String, Operator> ops = new HashMap<>();

    static {
        for (var el : Operator.values()) {
            ops.put(el.value, el);
        }
    }

    public boolean isAssign() { return precedence == 2; }

    public static Operator parse(String val) {
        return ops.get(val);
    }

    private Operator() {
        this.value = null;
        this.operation = null;
        this.precedence = -1;
        this.reverse = false;
    }
    private Operator(String value) {
        this. value = value;
        this.operation = null;
        this.precedence = -1;
        this.reverse = false;
    }
    private Operator(String value, int precedence) {
        this. value = value;
        this.operation = null;
        this.precedence = precedence;
        this.reverse = false;
    }
    private Operator(String value, int precedence, boolean reverse) {
        this. value = value;
        this.operation = null;
        this.precedence = precedence;
        this.reverse = reverse;
    }

    private Operator(String value, Instruction.Type funcName, int precedence) {
        this. value = value;
        this.operation = funcName;
        this.precedence = precedence;
        this.reverse = false;
    }
    private Operator(String value, Instruction.Type funcName, int precedence, boolean reverse) {
        this.value = value;
        this.operation = funcName;
        this.precedence = precedence;
        this.reverse = reverse;
    }
}