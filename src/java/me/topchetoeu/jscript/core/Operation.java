package me.topchetoeu.jscript.core;

import java.util.HashMap;

public enum Operation {
    INSTANCEOF(1, 2),
    IN(2, 2),

    MULTIPLY(3, 2),
    DIVIDE(4, 2),
    MODULO(5, 2),
    ADD(6, 2),
    SUBTRACT(7, 2),

    USHIFT_RIGHT(8, 2),
    SHIFT_RIGHT(9, 2),
    SHIFT_LEFT(10, 2),

    GREATER(11, 2),
    LESS(12, 2),
    GREATER_EQUALS(13, 2),
    LESS_EQUALS(14, 2),
    LOOSE_EQUALS(15, 2),
    LOOSE_NOT_EQUALS(16, 2),
    EQUALS(17, 2),
    NOT_EQUALS(18, 2),

    AND(19, 2),
    OR(20, 2),
    XOR(21, 2),

    NEG(23, 1),
    POS(24, 1),
    NOT(25, 1),
    INVERSE(26, 1);

    private static final HashMap<Integer, Operation> operations = new HashMap<>();

    static {
        for (var val : Operation.values()) operations.put(val.numeric, val);
    }

    public final int numeric;
    public final int operands;

    private Operation(int numeric, int n) {
        this.numeric = numeric;
        this.operands = n;
    }

    public static Operation fromNumeric(int i) {
        return operations.get(i);
    }
}
