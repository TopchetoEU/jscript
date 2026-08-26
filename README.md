# J2S (Java-JavaScript or Java to JavaScript)

**NOTE: This project is no longer maintained, and I do not intend on maintaining it. Go use GraalJS**

**WARNING: Currently, this code is undocumented. Proceed with caution and a psychiatrist.**

J2S is an engine, capable of running EcmaScript 5, written entirely in Java. This engine has been developed with the goal of being easy to integrate with your preexisting codebase, **THE GOAL OF THIS ENGINE IS NOT PERFORMANCE**. My crude experiments show that this engine is 50x-100x slower than V8, which, although bad, is acceptable for most simple scripting purposes. A small REPL (`me.topchetoeu.j2s.repl.SimpleRepl`) library with an included simple debugger (`me.topchetoeu.j2s.repl.debug.SimpleDebugger`). These are more or less reference implementations. In the future, most of the primordials logic of `SimpleRepl` will be moved in the "lib" project, but for now, it will stay there.

## How to use?

Since this is mostly targeted for integration into other applications, here, examples for invoking JS code from Java will be shown.

### Setting up an event loop

First of all, you will want to create an event loop. While not required, 99% of the times you will want to have one.

```java
var engine = new Engine();
var thread = engine.start();
```

Now you have an event loop. The thread that was automatically created is a daemon thread, so it will end when the rest of the application ends. If you don't want to use the built-in thread, you can instead run it with `engine.run(untilEmpty)`. If you pass true (which you most likely need), the event loop will be run blocking-ly until it is empty. Otherwise, it will be run forever.

### Creating the execution environment

This is one of the other crucial parts of J2S's architecture - the environment. It contains the global scope, a reference to the event loop, the global scope, the debugger, source mappings and a lot more. To run JS code, you must create an environment:

```java
var env = Environment.empty();
env.add(EventLoop.KEY, engine); // Gives
env.add(DebugContext.KEY, new DebugContext()); // For source mappings
```

As you can see, the environment is nothing more than a simple map of objects that may be of interest to the JS code. Although you can do much more with the environment, we will leave it at that.

### Registering the compiler

The compiler is completely separate from the interpreter, and you could even roll your own. There is, however, an ES5 compiler included:

```java
env.add(Compiler.KEY, Compilers.jsCompiler());
```

You can even use popular transpilers, like typescript or babel (or even chain them). Here is how you use the built-in babel transpiler, for ESNext support:

```java
env.add(Compiler.KEY, Compilers.babelCompiler());
```

### Evaluating a piece of code on the event loop

This is what you really want to do: run code! You can do that in the following way:

```java
var result = engine.pushMsg(false, env, Filename.parse("my-app://test.js"), "return 10 + 5 / 3;", Value.UNDEFINED).get();
System.out.println(result.toReadable(env));
```

If all goes well, we will get "11.666..." as a result.
