package me.topchetoeu.j2s.compilation.parsing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import me.topchetoeu.j2s.compilation.parsing.Parsing;
import me.topchetoeu.j2s.compilation.parsing.Source;

public class TestSkipWhite {
	@Test public void shBang() {
		var res1 = Parsing.skipEmpty(new Source("#!my-shbang\n10"), 0);
		assertEquals(12, res1);

		var res2 = Parsing.skipEmpty(new Source("#!fin"), 0);
		assertEquals(5, res2);
	}
	@Test public void simple() {
		var res1 = Parsing.skipEmpty(new Source("2134   45324"), 4);
		assertEquals(3, res1);

		var res2 = Parsing.skipEmpty(new Source("2134   "), 4);
		assertEquals(3, res2);
	}

	@Test public void nothing() {
		var res1 = Parsing.skipEmpty(new Source("12345678"), 4);
		assertEquals(0, res1);

		var res2 = Parsing.skipEmpty(new Source("1234"), 4);
		assertEquals(0, res2);
	}

	@Test public void singleLineComment() {
		var res1 = Parsing.skipEmpty(new Source("123// test\n54314214"), 3);
		assertEquals(8, res1);

		var res2 = Parsing.skipEmpty(new Source("123// test"), 3);
		assertEquals(7, res2);
	}
	@Test public void multilineComment() {
		var res1 = Parsing.skipEmpty(new Source("123/*test*/54314214"), 3);
		assertEquals(8, res1);

		var res2 = Parsing.skipEmpty(new Source("123/*test*/"), 3);
		assertEquals(8, res2);

		var res3 = Parsing.skipEmpty(new Source("123/*test"), 3);
		assertEquals(6, res3);
	}
}
