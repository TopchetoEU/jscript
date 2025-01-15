package me.topchetoeu.j2s.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestFilename {
	@Test public void testShouldParseFilePath() {
		var filename = Filename.parse("file://hello.world");
		assertEquals("file", filename.protocol);
		assertEquals("hello.world", filename.path);
	}
	@Test public void testShouldParseNoProtocolFilename() {
		var filename = Filename.parse("hello.world");
		assertEquals("file", filename.protocol);
		assertEquals("hello.world", filename.path);
	}
	@Test public void testShouldParseAdditionalSlashFilename() {
		var filename = Filename.parse("test:///hello.world");
		assertEquals("test", filename.protocol);
		assertEquals("/hello.world", filename.path);
	}
	@Test public void testShouldParseOneSlashFilename() {
		var filename = Filename.parse("test:/hello.world");
		assertEquals("file", filename.protocol);
		assertEquals("test:/hello.world", filename.path);
	}
	@Test public void testShouldParseMatroshkaFilename() {
		var a = Filename.parse("a://b://hello.world");
		assertEquals("a", a.protocol);
		assertEquals("b://hello.world", a.path);

		var b = Filename.parse(a.path);
		assertEquals("b", b.protocol);
		assertEquals("hello.world", b.path);
	}
}
