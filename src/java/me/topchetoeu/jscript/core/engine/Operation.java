package me.topchetoeu.jscript.core.engine;

public enum Operation {
    INSTANCEOF(2, false),
    IN(2, false),

    MULTIPLY(2, true),
    DIVIDE(2, true),
    MODULO(2, true),
    ADD(2, true),
    SUBTRACT(2, true),

    USHIFT_RIGHT(2, true),
    SHIFT_RIGHT(2, true),
    SHIFT_LEFT(2, true),

    GREATER(2, true),
    LESS(2, true),
    GREATER_EQUALS(2, true),
    LESS_EQUALS(2, true),
    LOOSE_EQUALS(2, true),
    LOOSE_NOT_EQUALS(2, true),
    EQUALS(2, true),
    NOT_EQUALS(2, true),

    AND(2, true),
    OR(2, true),
    XOR(2, true),

    NEG(1, true),
    POS(1, true),
    NOT(1, true),
    INVERSE(1, true);

    public final int operands;
    public final boolean optimizable;

    private Operation(int n, boolean opt) {
        this.operands = n;
        this.optimizable = opt;
    }
}
