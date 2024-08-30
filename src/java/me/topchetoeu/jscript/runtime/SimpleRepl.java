package me.topchetoeu.jscript.runtime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import me.topchetoeu.jscript.common.Compiler;
import me.topchetoeu.jscript.common.Metadata;
import me.topchetoeu.jscript.common.Reading;
import me.topchetoeu.jscript.common.parsing.Filename;
import me.topchetoeu.jscript.runtime.debug.DebugContext;
import me.topchetoeu.jscript.runtime.environment.Environment;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.exceptions.InterruptException;
import me.topchetoeu.jscript.runtime.exceptions.SyntaxException;
import me.topchetoeu.jscript.runtime.scope.GlobalScope;
import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.functions.NativeFunction;
import me.topchetoeu.jscript.runtime.values.primitives.StringValue;
import me.topchetoeu.jscript.runtime.values.primitives.VoidValue;

public class SimpleRepl {
    static Thread engineTask;
    static Engine engine = new Engine();
    static Environment environment = Environment.empty();

    static int j = 0;
    static String[] args;

    private static void reader() {
        try {
            for (var arg : args) {
                try {
                    var file = Path.of(arg);
                    var raw = Files.readString(file);

                    try {
                        var res = engine.pushMsg(
                            false, environment,
                            Filename.fromFile(file.toFile()), raw, null
                        ).get();

                        System.err.println(res.toReadable(environment));
                    }
                    catch (ExecutionException e) { throw e.getCause(); }
                }
                catch (EngineException | SyntaxException e) { System.err.println(Value.errorToReadable(e, null)); }
            }

            for (var i = 0; ; i++) {
                try {
                    var raw = Reading.readline();

                    if (raw == null) break;

                    try {
                        var res = engine.pushMsg(
                            false, environment,
                            new Filename("jscript", "repl/" + i + ".js"), raw,
                            VoidValue.UNDEFINED
                        ).get();
                        System.err.println(res.toReadable(environment));
                    }
                    catch (ExecutionException e) { throw e.getCause(); }
                }
                catch (EngineException | SyntaxException e) { System.err.println(Value.errorToReadable(e, null)); }
            }
        }
        catch (IOException e) {
            System.out.println(e.toString());
            engine.thread().interrupt();
        }
        catch (CancellationException | InterruptedException e) { return; }
        catch (Throwable ex) {
            System.out.println("Internal error ocurred:");
            ex.printStackTrace();
        }
    }

    private static void initEnv() {
        environment.add(EventLoop.KEY, engine);
        environment.add(GlobalScope.KEY, new GlobalScope());
        environment.add(DebugContext.KEY, new DebugContext());
        environment.add(Compiler.KEY, Compiler.DEFAULT);

        var glob = GlobalScope.get(environment);

        glob.define(null, false, new NativeFunction("exit", args -> {
            Thread.currentThread().interrupt();
            throw new InterruptException();
        }));
        glob.define(null, false, new NativeFunction("log", args -> {
            for (var el : args.args) {
                if (el instanceof StringValue) System.out.print(((StringValue)el).value);
                else System.out.print(el.toReadable(args.env));
            }

            return null;
        }));
    }
    private static void initEngine() {
        engineTask = engine.start();
    }

    public static void main(String args[]) throws InterruptedException {
        System.out.println(String.format("Running %s v%s by %s", Metadata.name(), Metadata.version(), Metadata.author()));

        SimpleRepl.args = args;
        var reader = new Thread(SimpleRepl::reader);

        initEnv();
        initEngine();

        reader.setDaemon(true);
        reader.setName("STD Reader");
        reader.start();

        engine.thread().join();
        // debugTask.interrupt();
        engineTask.interrupt();
    }
}
