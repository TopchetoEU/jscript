package me.topchetoeu.j2s.compilation.parsing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TestParseString {
	@Test public void notAString() {
		var res = Parsing.parseString(new Source("var a = 10"), 0);
		assertEquals(true, res.isFailed());
	}
	@Test public void simple() {
		var src = new Source("\"this is a test\"");
		var res = Parsing.parseString(src, 0);
		assertEquals(true, res.isSuccess());
		assertEquals("this is a test", res.result);
		assertEquals(16, res.n);
	}
	@Test public void simpleEscaped() {
		var src = new Source("\'this\\\\ is \\n some \\'\\\"\\\n escapes for you :\\)\'");
		var res = Parsing.parseString(src, 0);
		assertEquals(true, res.isSuccess());
		assertEquals("this\\ is \n some '\" escapes for you :)", res.result);
		assertEquals(46, res.n);
	}
	@Test public void allEscaped() {
		var src = new Source("'\\b\\t\\n\\f\\r\\0\\'\\x01\\u0123'");
		var res = Parsing.parseString(src, 0);
		assertEquals(true, res.isSuccess());
		assertEquals("\b\t\n\f\r\0'\u0001\u0123", res.result);
		assertEquals(26, res.n);
	}
	@Test public void shouldFailOctal() {
		var res1 = Parsing.parseString(new Source("'\\012'"), 0);
		assertEquals(true, res1.isError());

		var res2 = Parsing.parseString(new Source("'\\123'"), 0);
		assertEquals(true, res2.isError());
	}
	@Test public void shouldFailIncompleteHex() {
		var res1 = Parsing.parseString(new Source("'\\x"), 0);
		assertEquals(true, res1.isError());

		var res2 = Parsing.parseString(new Source("'\\x1turd"), 0);
		assertEquals(true, res2.isError());

		var res3 = Parsing.parseString(new Source("'\\xturd"), 0);
		assertEquals(true, res3.isError());
	}
	@Test public void shouldFailIncompleteUnicode() {
		var res1 = Parsing.parseString(new Source("'\\u"), 0);
		assertEquals(true, res1.isError());

		var res2 = Parsing.parseString(new Source("'\\u123turd"), 0);
		assertEquals(true, res2.isError());

		var res3 = Parsing.parseString(new Source("'\\uturd"), 0);
		assertEquals(true, res3.isError());
	}
	@Test public void unterminated() {
		var src = new Source("\"this is a test");
		var res = Parsing.parseString(src, 0);
		assertEquals(true, res.isError());
	}
}
