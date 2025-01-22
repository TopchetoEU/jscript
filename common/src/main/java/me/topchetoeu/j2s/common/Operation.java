package me.topchetoeu.j2s.common;

import java.util.HashMap;

public enum Operation {
	TYPEOF(0x10, 1),
	INSTANCEOF(0x11, 2),
	IN(0x12, 2),

	MULTIPLY(0x20, 2),
	DIVIDE(0x21, 2),
	MODULO(0x22, 2),
	ADD(0x23, 2),
	SUBTRACT(0x24, 2),

	USHIFT_RIGHT(0x30, 2),
	SHIFT_RIGHT(0x31, 2),
	SHIFT_LEFT(0x32, 2),

	GREATER(0x40, 2),
	LESS(0x41, 2),
	GREATER_EQUALS(0x42, 2),
	LESS_EQUALS(0x43, 2),
	LOOSE_EQUALS(0x44, 2),
	LOOSE_NOT_EQUALS(0x45, 2),
	EQUALS(0x46, 2),
	NOT_EQUALS(0x47, 2),

	AND(0x50, 2),
	OR(0x51, 2),
	XOR(0x52, 2),

	NEG(0x60, 1),
	POS(0x61, 1),
	NOT(0x62, 1),
	INVERSE(0x63, 1);

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
