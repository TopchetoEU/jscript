# JScript

**NOTE: This had nothing to do with Microsoft's dialect of EcmaScript**

**WARNING: Currently, this code is mostly undocumented. Proceed with caution and a psychiatrist.**

JScript is an engine, capable of running EcmaScript 5, written entirely in Java. This engine has been developed with the goal of being easy to integrate with your preexisting codebase, **THE GOAL OF THIS ENGINE IS NOT PERFORMANCE**. My crude experiments show that this engine is 50x-100x slower than V8, which, although bad, is acceptable for most simple scripting purposes.

## Example

The following will create a REPL using the engine as a backend. Not that this won't properly log errors. I recommend checking out the implementation in `Main.main`:

```java
var engine = new PolyfillEngine(new File("."));
var in = new BufferedReader(new InputStreamReader(System.in));
engine.start();

while (true) {
    try {
        var raw = in.readLine();

        var res = engine.pushMsg(false, engine.global(), Map.of(), "<stdio>", raw, null).await();
        Values.printValue(engine.context(), res);
        System.out.println();
    }
    catch (EngineException e) {
        try {
            System.out.println("Uncaught " + e.toString(engine.context()));
        }
        catch (InterruptedException _e) {  return; }
    }
    catch (IOException | InterruptedException e) { return; }
}
```
