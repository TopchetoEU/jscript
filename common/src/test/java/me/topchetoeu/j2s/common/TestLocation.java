package me.topchetoeu.j2s.common;

import org.junit.jupiter.api.Test;

import me.topchetoeu.j2s.common.parsing.Filename;
import me.topchetoeu.j2s.common.parsing.Location;

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
		assertEquals(next.filename(), new Filename("file", "test.txt"));
		assertEquals(next.line(), 10);
		assertEquals(next.start(), 0);
		assertEquals(loc.filename(), new Filename("file", "test.txt"));
		assertEquals(loc.line(), 9);
		assertEquals(loc.start(), 4);
	}
	@Test public void testShouldGetNextNthLineLocation() {
		var loc = Location.of(new Filename("file", "test.txt"), 10, 5);
		var next = loc.nextLine(5);
		assertEquals(next.line(), 15);
		assertEquals(next.start(), 0);
		assertEquals(loc.line(), 10);
		assertEquals(loc.start(), 5);
	}

	@Test public void testShouldGetNextLocation() {
		var loc = Location.of("test:10:5");
		var next = loc.add(10);
		assertEquals(next.filename(), new Filename("file", "test"));
		assertEquals(next.line(), 9);
		assertEquals(next.start(), 14);
		assertEquals(loc.filename(), new Filename("file", "test"));
		assertEquals(loc.line(), 9);
		assertEquals(loc.start(), 4);
	}

	@Test public void testShouldParseLocation() {
		var loc = Location.of("test.txt:10:5");
		assertEquals(loc.filename(), new Filename("file", "test.txt"));
		assertEquals(loc.line(), 9);
		assertEquals(loc.start(), 4);
	}
	@Test public void testShouldParseComplexFilenameLocation() {
		var loc = Location.of("testificate://test.txt:10:5");
		assertEquals(loc.filename(), new Filename("testificate", "test.txt"));
		assertEquals(loc.line(), 9);
		assertEquals(loc.start(), 4);
	}
	@Test public void testShouldParseNoFilenameLocation() {
		var loc = Location.of("10:5");
		assertEquals(loc.filename(), null);
		assertEquals(loc.line(), 9);
		assertEquals(loc.start(), 4);
	}
	@Test public void testShouldParseNoStartLocationA() {
		var loc = Location.of("file://10:5");
		assertEquals(loc.filename(), new Filename("file", "10"));
		assertEquals(loc.line(), 4);
		assertEquals(loc.start(), -1);
	}
	@Test public void testShouldParseNoStartLocationB() {
		var loc = Location.of("file:5");
		assertEquals(loc.filename(), new Filename("file", "file"));
		assertEquals(loc.line(), 4);
		assertEquals(loc.start(), -1);
	}
	@Test public void testShouldParseOnlyFilenameLocationA() {
		var loc = Location.of("http://example.org/test.txt");
		assertEquals(loc.filename(), new Filename("http", "example.org/test.txt"));
		assertEquals(loc.line(), -1);
		assertEquals(loc.start(), -1);
	}
	@Test public void testShouldParseOnlyFilenameLocationB() {
		var loc = Location.of("test.txt");
		assertEquals(loc.filename(), new Filename("file", "test.txt"));
		assertEquals(loc.line(), -1);
		assertEquals(loc.start(), -1);
	}
	@Test public void testShouldParseOnlyFilenameWithColonLocation() {
		var loc = Location.of("my-file:bad-file");
		assertEquals(loc.filename(), new Filename("file", "my-file:bad-file"));
		assertEquals(loc.line(), -1);
		assertEquals(loc.start(), -1);
	}
	@Test public void testShouldParseOnlyFilenameWithTripleColonLocation() {
		var loc = Location.of("a:my-file:bad-file");
		assertEquals(loc.filename(), new Filename("file", "a:my-file:bad-file"));
		assertEquals(loc.line(), -1);
		assertEquals(loc.start(), -1);
	}

	@Test public void testCompareEqualLoc() {
		var locA = Location.of("test:10:5");
		var locB = Location.of("test:10:5");

		assertEquals(locA.compareTo(locB), 0);
		assertEquals(locB.compareTo(locA), 0);
	}
	@Test public void testCompareNoFileLoc() {
		var locA = Location.of("10:5");
		var locB = Location.of("11:5");

		assertEquals(locA.compareTo(locB), -1);
		assertEquals(locB.compareTo(locA), 1);
	}
	@Test public void testCompareOneNoFileLoc() {
		var locA = Location.of("10:5");
		var locB = Location.of("test:10:5");

		assertEquals(locA.compareTo(locB), -1);
		assertEquals(locB.compareTo(locA), 1);
	}
	@Test public void testCompareDiffFileLoc() {
		var locA = Location.of("a:10:5");
		var locB = Location.of("b:10:5");

		assertEquals(locA.compareTo(locB), -1);
		assertEquals(locB.compareTo(locA), 1);
	}
	@Test public void testCompareDiffLineLoc() {
		var locA = Location.of("test:10:5");
		var locB = Location.of("test:11:5");

		assertEquals(locA.compareTo(locB), -1);
		assertEquals(locB.compareTo(locA), 1);
	}
	@Test public void testCompareDiffStartLoc() {
		var locA = Location.of("test:10:5");
		var locB = Location.of("test:10:10");

		assertEquals(locA.compareTo(locB), -1);
		assertEquals(locB.compareTo(locA), 1);
	}

	@Test public void testToStringAll() {
		var locA = Location.of("test:10:5");
		assertEquals(locA.toString(), "file://test:10:5");
	}
	@Test public void testToStringNoFilename() {
		var locA = Location.of("10:5");
		assertEquals(locA.toString(), "10:5");
	}
	@Test public void testToStringNoStart() {
		var locA = Location.of("file:5");
		assertEquals(locA.toString(), "file://file:5");
	}
	@Test public void testToStringNoLoc() {
		var locA = Location.of("file");
		assertEquals(locA.toString(), "file://file");
	}
}
