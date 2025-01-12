package me.topchetoeu.j2s.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;

import me.topchetoeu.j2s.common.environment.Environment;
import me.topchetoeu.j2s.common.environment.Key;

public class TestEnvironment {
	private final Key<String> FOO = new Key<>();
	private final Key<Void> MARKER = new Key<>();

	@Test public void testShouldCreate() {
		new Environment();
		Environment.empty();
	}
	@Test public void testShouldNotExist() {
		var env = new Environment();
		assertEquals(null, env.get(FOO));
		assertEquals(false, env.has(FOO));
	}

	@Test public void testShouldAdd() {
		var env = new Environment();
		env.add(FOO, "test");
		assertEquals("test", env.get(FOO));
	}

	@Test public void testShouldGetFromParent() {
		var parent = new Environment();
		parent.add(FOO, "test");
		var child = parent.child();
		assertEquals("test", child.get(FOO));
		assertEquals(true, child.has(FOO));
	}
	@Test public void testShouldHideParent() {
		var parent = new Environment();
		parent.add(FOO, "test");
		var child = parent.child();
		child.remove(FOO);
		assertEquals(null, child.get(FOO));
		assertEquals(false, child.has(FOO));
	}

	@Test public void testShouldAddMarker() {
		var env = new Environment();
		env.add(MARKER);
		assertEquals(true, env.has(MARKER));
		assertEquals(false, env.hasNotNull(MARKER));
	}

	@Test public void testShouldInitOnce() {
		var env = new Environment();
		assertEquals("a", env.init(FOO, "a"));
		assertEquals("a", env.init(FOO, "b"));
		assertEquals("a", env.get(FOO));
	}
	@Test public void testShouldInitOnceFrom() {
		var env = new Environment();
		assertEquals("a", env.initFrom(FOO, () -> "a"));
		assertEquals("a", env.initFrom(FOO, () -> "b"));
		assertEquals("a", env.get(FOO));
	}

	@Test public void testShouldWrap() {
		var env = new Environment();
		assertEquals(env, Environment.wrap(env));
		assertInstanceOf(Environment.class, Environment.wrap(null));
	}
}
