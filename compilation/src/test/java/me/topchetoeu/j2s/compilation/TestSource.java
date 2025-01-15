package me.topchetoeu.j2s.compilation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import me.topchetoeu.j2s.common.Environment;
import me.topchetoeu.j2s.common.Filename;
import me.topchetoeu.j2s.common.Location;
import me.topchetoeu.j2s.compilation.parsing.Source;

public class TestSource {
	private Source mkSource(String src) {
		return new Source(new Environment(), Filename.parse("test"), src);
	}

	@Test public void testShouldCreate() {
		new Source(new Environment(), Filename.parse("test"), "my little source :)");
		new Source(null, Filename.parse("test"), "my little source :)");
		new Source("my little source :)");
	}

	@Test public void testShouldGet() {
		var src = mkSource("1234567890");
		assertEquals('1', src.at(0));
		assertEquals('6', src.at(5));
	}
	@Test public void testShouldThrowOutOfRange() {
		var src = mkSource("1234567890");
		assertThrows(IndexOutOfBoundsException.class, () -> src.at(-1));
		assertThrows(IndexOutOfBoundsException.class, () -> src.at(10));
	}
	@Test public void testImmutableSrcLoc() {
		var src = mkSource("1234567890");
		var loc = src.loc(5);
		// kinda stupid
		for (var i = 0; i < 1000; i++) {
			assertEquals(5, loc.start());
			assertEquals(0, loc.line());
			assertEquals(Filename.parse("test"), loc.filename());
		}
	}
	@Test public void testSingleLineSourceLocation() {
		var src = mkSource("1234567890");
		assertEquals(Location.of("test:1:1"), src.loc(-5));
		assertEquals(Location.of("test:1:1"), src.loc(0));
		assertEquals(Location.of("test:1:10"), src.loc(9));
		assertEquals(Location.of("test:1:11"), src.loc(14));
	}
	@Test public void testMultilineSourceLocation() {
		var src = mkSource("123\n456\n\n789\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
		assertEquals(Location.of("test:1:1"), src.loc(-5));
		assertEquals(Location.of("test:1:1"), src.loc(0));
		assertEquals(Location.of("test:1:4"), src.loc(3));
		assertEquals(Location.of("test:2:1"), src.loc(4));
		assertEquals(Location.of("test:2:4"), src.loc(7));
		assertEquals(Location.of("test:3:1"), src.loc(8));
		assertEquals(Location.of("test:4:1"), src.loc(9));
		assertEquals(Location.of("test:4:2"), src.loc(10));
		assertEquals(Location.of("test:4:4"), src.loc(12));
		assertEquals(Location.of("test:5:1"), src.loc(13));
		assertEquals(Location.of("test:19:1"), src.loc(50));
	}
}
