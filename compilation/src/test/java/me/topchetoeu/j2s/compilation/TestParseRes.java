package me.topchetoeu.j2s.compilation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import me.topchetoeu.j2s.common.Location;
import me.topchetoeu.j2s.compilation.parsing.ParseRes;

public class TestParseRes {
	@Test public void testCreateFailed() {
		var res = ParseRes.failed();
		assertEquals(false, res.isSuccess());
		assertEquals(true, res.isFailed());
		assertEquals(false, res.isError());
		assertEquals(0, res.n);
		assertEquals(null, res.result);
		assertEquals(null, res.errorLocation);
		assertEquals(null, res.error);
	}
	@Test public void testCreateError() {
		var res = ParseRes.error(Location.of("test:10:5"), "test");
		assertEquals(false, res.isSuccess());
		assertEquals(false, res.isFailed());
		assertEquals(true, res.isError());
		assertEquals(0, res.n);
		assertEquals(null, res.result);
		assertEquals(Location.of("test:10:5"), res.errorLocation);
		assertEquals("test", res.error);
	}
	@Test public void testCreateResult() {
		var res = ParseRes.res("test", 10);
		assertEquals(true, res.isSuccess());
		assertEquals(false, res.isFailed());
		assertEquals(false, res.isError());
		assertEquals(10, res.n);
		assertEquals("test", res.result);
		assertEquals(null, res.errorLocation);
		assertEquals(null, res.error);
	}

	@Test public void testChainFailed() {
		var a = ParseRes.<Integer>failed();

		var b1 = a.<String>chainError();
		assertEquals(a, b1);

		var b2 = a.<String>chainError(Location.of("test:1:2"), "test");
		assertEquals(true, b2.isError());
		assertEquals("test", b2.error);
	}
	@Test public void testChainError() {
		var a = ParseRes.<Integer>error(Location.of("test:1:2"), "test");

		var b1 = a.<String>chainError();
		assertEquals(a, b1);

		var b2 = a.<String>chainError(Location.of("test:1:2"), "test");
		assertEquals(a, b2);
	}
	@Test public void testChainResult() {
		var a = ParseRes.<Integer>res(10, 6);

		assertThrows(RuntimeException.class, () -> a.<String>chainError());

		var b1 = a.<String>chainError(Location.of("test:1:2"), "test");
		assertEquals(true, b1.isError());
		assertEquals("test", b1.error);
	}


	@Test public void testShouldAdd5() {
		var a = ParseRes.res("test", 6);
		var b = a.addN(5);

		assertEquals(11, b.n);
	}
	@Test public void testShouldSet5() {
		var a = ParseRes.res("test", 6);
		var b = a.setN(5);

		assertEquals(5, b.n);
	}

	@Test public void testShouldNotAdd() {
		var a = ParseRes.failed();
		var b = a.addN(5);

		assertEquals(0, b.n);
	}
	@Test public void testShouldNotSet() {
		var a = ParseRes.failed();
		var b = a.setN(5);

		assertEquals(0, b.n);
	}
}
