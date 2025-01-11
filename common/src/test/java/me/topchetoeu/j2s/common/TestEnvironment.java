package me.topchetoeu.j2s.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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
		assertEquals(env.get(FOO), null);
		assertEquals(env.has(FOO), false);
	}

	@Test public void testShouldAdd() {
		var env = new Environment();
		env.add(FOO, "test");
		assertEquals(env.get(FOO), "test");
	}

	@Test public void testShouldGetFromParent() {
		var parent = new Environment();
		parent.add(FOO, "test");
		var child = parent.child();
		assertEquals(child.get(FOO), "test");
		assertEquals(child.has(FOO), true);
	}
	@Test public void testShouldHideParent() {
		var parent = new Environment();
		parent.add(FOO, "test");
		var child = parent.child();
		child.remove(FOO);
		assertEquals(child.get(FOO), null);
		assertEquals(child.has(FOO), false);
	}

	@Test public void testShouldAddMarker() {
		var env = new Environment();
		env.add(MARKER);
		assertEquals(env.has(MARKER), true);
		assertEquals(env.hasNotNull(MARKER), false);
	}

	@Test public void testShouldInitOnce() {
		var env = new Environment();
		assertEquals(env.init(FOO, "a"), "a");
		assertEquals(env.init(FOO, "b"), "a");
		assertEquals(env.get(FOO), "a");
	}
	@Test public void testShouldInitOnceFrom() {
		var env = new Environment();
		assertEquals(env.initFrom(FOO, () -> "a"), "a");
		assertEquals(env.initFrom(FOO, () -> "b"), "a");
		assertEquals(env.get(FOO), "a");
	}

	@Test public void testShouldWrap() {
		var env = new Environment();
		assertEquals(Environment.wrap(env), env);
		assertNotEquals(Environment.wrap(null), null);
	}
}
