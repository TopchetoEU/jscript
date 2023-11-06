# JScript

**NOTE: This had nothing to do with Microsoft's dialect of EcmaScript**

**WARNING: Currently, this code is mostly undocumented. Proceed with caution and a psychiatrist.**

JScript is an engine, capable of running EcmaScript 5, written entirely in Java. This engine has been developed with the goal of being easy to integrate with your preexisting codebase, **THE GOAL OF THIS ENGINE IS NOT PERFORMANCE**. My crude experiments show that this engine is 50x-100x slower than V8, which, although bad, is acceptable for most simple scripting purposes. Note that although the codebase has a Main class, this isn't meant to be a standalone program, but instead a library for running JavaScript code.

## Example

The following will create a REPL using the engine as a backend. Not that this won't properly log errors. I recommend checking out the implementation in `Main.main`:

```java
var engine = new Engine(true /* false if you dont want debugging */);
var env = new Environment(null, null, null);
var debugger = new DebugServer();

// Create one target for the engine and start debugging server
debugger.targets.put("target", (socket, req) -> new SimpleDebugger(socket, engine));
debugger.start(new InetSocketAddress("127.0.0.1", 9229), true);

// Queue code to load internal libraries and start engine
engine.pushMsg(false, null, new Internals().getApplier(env));
engine.start();

while (true) {
    try {
        var raw = Reading.read();
        if (raw == null) break;

        // Push a message to the engine with the raw REPL code
        var res = engine.pushMsg(
            false, new Context(engine).pushEnv(env),
            new Filename("jscript", "repl.js"), raw, null
        ).await();

        Values.printValue(null, res);
    }
    catch (EngineException e) { Values.printError(e, ""); }
    catch (SyntaxException ex) {
        System.out.println("Syntax error:" + ex.msg);
    }
    catch (IOException e) { }
}
```
