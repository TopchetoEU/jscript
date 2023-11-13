package me.topchetoeu.jscript;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.Engine;
import me.topchetoeu.jscript.engine.Environment;
import me.topchetoeu.jscript.engine.debug.DebugServer;
import me.topchetoeu.jscript.engine.debug.SimpleDebugger;
import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.events.Observer;
import me.topchetoeu.jscript.exceptions.EngineException;
import me.topchetoeu.jscript.exceptions.InterruptException;
import me.topchetoeu.jscript.exceptions.SyntaxException;
import me.topchetoeu.jscript.lib.Internals;

public class Main {   
    public static class Printer implements Observer<Object> {
        public void next(Object data) {
            Values.printValue(null, data);
            System.out.println();
        }

        public void error(RuntimeException err) {
            Values.printError(err, null);
        }

        public void finish() {
            engineTask.interrupt();
        }
    }

    static Thread engineTask, debugTask;
    static Engine engine = new Engine(true);
    static DebugServer debugServer = new DebugServer();
    static Environment environment = new Environment(null, null, null);

    static int j = 0;
    static boolean exited = false;
    static String[] args;

    private static void reader() {
        try {
            for (var arg : args) {
                try {
                    if (arg.equals("--ts")) initTypescript();
                    else {
                        var file = Path.of(arg);
                        var raw = Files.readString(file);
                        var res = engine.pushMsg(
                            false, new Context(engine, environment),
                            Filename.fromFile(file.toFile()),
                            raw, null
                        ).await();
                        Values.printValue(null, res);
                        System.out.println();
                    }
                }
                catch (EngineException e) { Values.printError(e, ""); }
            }
            for (var i = 0; ; i++) {
                try {
                    var raw = Reading.read();

                    if (raw == null) break;
                    var res = engine.pushMsg(
                        false, new Context(engine, environment),
                        new Filename("jscript", "repl/" + i + ".js"),
                        raw, null
                    ).await();
                    Values.printValue(null, res);
                    System.out.println();
                }
                catch (EngineException e) { Values.printError(e, ""); }
                catch (SyntaxException e) { Values.printError(e, ""); }
            }
        }
        catch (IOException e) {
            System.out.println(e.toString());
            exited = true;
        }
        catch (RuntimeException ex) {
            if (!exited) {
                System.out.println("Internal error ocurred:");
                ex.printStackTrace();
            }
        }
        if (exited) {
            debugTask.interrupt();
            engineTask.interrupt();
        }
    }

    private static void initEnv() {
        environment = Internals.apply(environment);

        environment.global.define("exit", _ctx -> {
            exited = true;
            throw new InterruptException();
        });
        environment.global.define("go", _ctx -> {
            try {
                var f = Path.of("do.js");
                var func = _ctx.compile(new Filename("do", "do/" + j++ + ".js"), new String(Files.readAllBytes(f)));
                return func.call(_ctx);
            }
            catch (IOException e) {
                throw new EngineException("Couldn't open do.js");
            }
        });
    }
    private static void initEngine() {
        debugServer.targets.put("target", (ws, req) -> new SimpleDebugger(ws, engine));
        engineTask = engine.start();
        debugTask = debugServer.start(new InetSocketAddress("127.0.0.1", 9229), true);
    }
    private static void initTypescript() {
        try {
            var tsEnv = Internals.apply(new Environment(null, null, null));
            var bsEnv = Internals.apply(new Environment(null, null, null));

            engine.pushMsg(
                false, new Context(engine, tsEnv),
                new Filename("jscript", "ts.js"),
                Reading.resourceToString("js/ts.js"), null
            ).await();
            System.out.println("Loaded typescript!");

            var ctx = new Context(engine, bsEnv);

            engine.pushMsg(
                false, ctx,
                new Filename("jscript", "internals/bootstrap.js"), Reading.resourceToString("js/bootstrap.js"), null,
                tsEnv.global.get(ctx, "ts"), environment, new ArrayValue(null, Reading.resourceToString("js/lib.d.ts"))
            ).await();
        }
        catch (EngineException e) {
            Values.printError(e, "(while initializing TS)");
        }
    }

    public static void main(String args[]) {
        System.out.println(String.format("Running %s v%s by %s", Metadata.name(), Metadata.version(), Metadata.author()));

        Main.args = args;
        var reader = new Thread(Main::reader);

        initEnv();
        initEngine();

        reader.setDaemon(true);
        reader.setName("STD Reader");
        reader.start();
    }
}
