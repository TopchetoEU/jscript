package me.topchetoeu.j2s.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TestFunctionMap {
	@Test public void createEmpty() {
		FunctionMap.builder().build(null, null, null);
		FunctionMap.builder().build();
	}
	@Test public void startOfEmpty() {
		var empty = FunctionMap.EMPTY;

		assertEquals(null, empty.start());
		assertEquals(null, empty.end());
	}
}
