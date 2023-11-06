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
    static Thread engineTask, debugTask;
    static Engine engine;
    static Environment env;
    static int j = 0;

    private static Observer<Object> valuePrinter = new Observer<Object>() {
        public void next(Object data) {
            Values.printValue(null, data);
            System.out.println();
        }

        public void error(RuntimeException err) {
            Values.printError(err, null);
        }

        @Override
        public void finish() {
            engineTask.interrupt();
        }
    };

    public static void main(String args[]) {
        System.out.println(String.format("Running %s v%s by %s", Metadata.NAME, Metadata.VERSION, Metadata.AUTHOR));
        engine = new Engine(true);

        var exited = new boolean[1];
        var server = new DebugServer();
        server.targets.put("target", (ws, req) -> new SimpleDebugger(ws, engine));
        
        env = Internals.apply(new Environment(null, null, null));

        env.global.define("exit", _ctx -> {
            exited[0] = true;
            throw new InterruptException();
        });
        env.global.define("go", _ctx -> {
            try {
                var f = Path.of("do.js");
                var func = _ctx.compile(new Filename("do", "do/" + j++ + ".js"), new String(Files.readAllBytes(f)));
                return func.call(_ctx);
            }
            catch (IOException e) {
                throw new EngineException("Couldn't open do.js");
            }
        });

        engineTask = engine.start();
        debugTask = server.start(new InetSocketAddress("127.0.0.1", 9229), true);

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
                tsEnv.global.get(ctx, "ts"), env, new ArrayValue(null, Reading.resourceToString("js/lib.d.ts"))
            ).await();
        }
        catch (EngineException e) {
            Values.printError(e, "(while initializing TS)");
        }

        var reader = new Thread(() -> {
            try {
                for (var arg : args) {
                    try {
                        var file = Path.of(arg);
                        var raw = Files.readString(file);
                        valuePrinter.next(engine.pushMsg(false, new Context(engine).pushEnv(env), Filename.fromFile(file.toFile()), raw, null).await());
                    }
                    catch (EngineException e) { Values.printError(e, ""); }
                }
                for (var i = 0; ; i++) {
                    try {
                        var raw = Reading.read();

                        if (raw == null) break;
                        valuePrinter.next(engine.pushMsg(false, new Context(engine).pushEnv(env), new Filename("jscript", "repl/" + i + ".js"), raw, null).await());
                    }
                    catch (EngineException e) { Values.printError(e, ""); }
                }
            }
            catch (IOException e) { exited[0] = true; }
            catch (SyntaxException ex) {
                if (exited[0]) return;
                System.out.println("Syntax error:" + ex.msg);
            }
            catch (RuntimeException ex) {
                if (!exited[0]) {
                    System.out.println("Internal error ocurred:");
                    ex.printStackTrace();
                }
            }
            if (exited[0]) debugTask.interrupt();
        });
        reader.setDaemon(true);
        reader.setName("STD Reader");
        reader.start();
    }
}
