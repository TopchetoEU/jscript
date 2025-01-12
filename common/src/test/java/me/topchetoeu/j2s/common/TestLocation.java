package me.topchetoeu.j2s.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestLocation {
	@Test public void testShouldCreateLocation() {
		var loc = Location.of(new Filename("file", "test.txt"), 10, 5);
		assertEquals("test.txt", loc.filename().path);
		assertEquals(10, loc.line());
		assertEquals(5, loc.start());
	}

	@Test public void testShouldGetNextLineLocation() {
		var loc = Location.of("test.txt:10:5");
		var next = loc.nextLine();
		assertEquals(new Filename("file", "test.txt"), next.filename());
		assertEquals(10, next.line());
		assertEquals(0, next.start());
		assertEquals(new Filename("file", "test.txt"), loc.filename());
		assertEquals(9, loc.line());
		assertEquals(4, loc.start());
	}
	@Test public void testShouldGetNextNthLineLocation() {
		var loc = Location.of(new Filename("file", "test.txt"), 10, 5);
		var next = loc.nextLine(5);
		assertEquals(15, next.line());
		assertEquals(0, next.start());
		assertEquals(10, loc.line());
		assertEquals(5, loc.start());
	}

	@Test public void testShouldGetNextLocation() {
		var loc = Location.of("test:10:5");
		var next = loc.add(10);
		assertEquals(new Filename("file", "test"), next.filename());
		assertEquals(9, next.line());
		assertEquals(14, next.start());
		assertEquals(new Filename("file", "test"), loc.filename());
		assertEquals(9, loc.line());
		assertEquals(4, loc.start());
	}

	@Test public void testShouldParseLocation() {
		var loc = Location.of("test.txt:10:5");
		assertEquals(new Filename("file", "test.txt"), loc.filename());
		assertEquals(9, loc.line());
		assertEquals(4, loc.start());
	}
	@Test public void testShouldParseComplexFilenameLocation() {
		var loc = Location.of("testificate://test.txt:10:5");
		assertEquals(new Filename("testificate", "test.txt"), loc.filename());
		assertEquals(9, loc.line());
		assertEquals(4, loc.start());
	}
	@Test public void testShouldParseNoFilenameLocation() {
		var loc = Location.of("10:5");
		assertEquals(null, loc.filename());
		assertEquals(9, loc.line());
		assertEquals(4, loc.start());
	}
	@Test public void testShouldParseNoStartLocationA() {
		var loc = Location.of("file://10:5");
		assertEquals(new Filename("file", "10"), loc.filename());
		assertEquals(4, loc.line());
		assertEquals(-1, loc.start());
	}
	@Test public void testShouldParseNoStartLocationB() {
		var loc = Location.of("file:5");
		assertEquals(new Filename("file", "file"), loc.filename());
		assertEquals(4, loc.line());
		assertEquals(-1, loc.start());
	}
	@Test public void testShouldParseOnlyFilenameLocationA() {
		var loc = Location.of("http://example.org/test.txt");
		assertEquals(new Filename("http", "example.org/test.txt"), loc.filename());
		assertEquals(-1, loc.line());
		assertEquals(-1, loc.start());
	}
	@Test public void testShouldParseOnlyFilenameLocationB() {
		var loc = Location.of("test.txt");
		assertEquals(new Filename("file", "test.txt"), loc.filename());
		assertEquals(-1, loc.line());
		assertEquals(-1, loc.start());
	}
	@Test public void testShouldParseOnlyFilenameWithColonLocation() {
		var loc = Location.of("my-file:bad-file");
		assertEquals(new Filename("file", "my-file:bad-file"), loc.filename());
		assertEquals(-1, loc.line());
		assertEquals(-1, loc.start());
	}
	@Test public void testShouldParseOnlyFilenameWithTripleColonLocation() {
		var loc = Location.of("a:my-file:bad-file");
		assertEquals(new Filename("file", "a:my-file:bad-file"), loc.filename());
		assertEquals(-1, loc.line());
		assertEquals(-1, loc.start());
	}

	@Test public void testCompareEqualLoc() {
		var locA = Location.of("test:10:5");
		var locB = Location.of("test:10:5");

		assertEquals(0, locA.compareTo(locB));
		assertEquals(0, locB.compareTo(locA));
	}
	@Test public void testCompareNoFileLoc() {
		var locA = Location.of("10:5");
		var locB = Location.of("11:5");

		assertEquals(-1, locA.compareTo(locB));
		assertEquals(1, locB.compareTo(locA));
	}
	@Test public void testCompareOneNoFileLoc() {
		var locA = Location.of("10:5");
		var locB = Location.of("test:10:5");

		assertEquals(-1, locA.compareTo(locB));
		assertEquals(1, locB.compareTo(locA));
	}
	@Test public void testCompareDiffFileLoc() {
		var locA = Location.of("a:10:5");
		var locB = Location.of("b:10:5");

		assertEquals(-1, locA.compareTo(locB));
		assertEquals(1, locB.compareTo(locA));
	}
	@Test public void testCompareDiffLineLoc() {
		var locA = Location.of("test:10:5");
		var locB = Location.of("test:11:5");

		assertEquals(-1, locA.compareTo(locB));
		assertEquals(1, locB.compareTo(locA));
	}
	@Test public void testCompareDiffStartLoc() {
		var locA = Location.of("test:10:5");
		var locB = Location.of("test:10:10");

		assertEquals(-1, locA.compareTo(locB));
		assertEquals(1, locB.compareTo(locA));
	}

	@Test public void testToStringAll() {
		var locA = Location.of("test:10:5");
		assertEquals("file://test:10:5", locA.toString());
	}
	@Test public void testToStringNoFilename() {
		var locA = Location.of("10:5");
		assertEquals("10:5", locA.toString());
	}
	@Test public void testToStringNoStart() {
		var locA = Location.of("file:5");
		assertEquals("file://file:5", locA.toString());
	}
	@Test public void testToStringNoLoc() {
		var locA = Location.of("file");
		assertEquals("file://file", locA.toString());
	}
}
