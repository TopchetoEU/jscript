# JScript

**NOTE: This had nothing to do with Microsoft's dialect of EcmaScript**

**WARNING: Currently, this code is undocumented. Proceed with caution and a psychiatrist.**

JScript is an engine, capable of running EcmaScript 5, written entirely in Java. This engine has been developed with the goal of being easy to integrate with your preexisting codebase, **THE GOAL OF THIS ENGINE IS NOT PERFORMANCE**. My crude experiments show that this engine is 50x-100x slower than V8, which, although bad, is acceptable for most simple scripting purposes. Note that although the codebase has a Main class, this isn't meant to be a standalone program, but instead a library for running JavaScript code.

## Example

The following is going to execute a simple javascript statement:

```java
var engine = new Engine();
// Initialize a standard environment, with implementations of most basic standard libraries (Object, Array, Symbol, etc.)
var env = Internals.apply(new Environment());

// Queue code to load internal libraries and start engine
var awaitable = engine.pushMsg(false, env, new Filename("tmp", "eval"), "10 + Math.sqrt(5 / 3)", null);
// Run the engine on the same thread, until the event loop runs empty
engine.run(true);

// Get our result
System.out.println(awaitable.await());
```
