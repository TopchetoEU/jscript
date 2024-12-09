package me.topchetoeu.jscript;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestHelloWorld {

	@Test
	public void testHelloWorld() {
		final String message = "Hello World!";
		assertEquals("Hello World!", message);
	}
}
