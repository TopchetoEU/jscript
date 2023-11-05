package me.topchetoeu.jscript;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.Engine;
import me.topchetoeu.jscript.engine.Environment;
import me.topchetoeu.jscript.engine.StackData;
import me.topchetoeu.jscript.engine.debug.DebugServer;
import me.topchetoeu.jscript.engine.debug.SimpleDebugger;
import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.NativeFunction;
import me.topchetoeu.jscript.engine.values.ObjectValue;
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

        env = new Environment(null, null, null);
        var exited = new boolean[1];
        var server = new DebugServer();
        server.targets.put("target", (ws, req) -> new SimpleDebugger(ws, engine));

        engineTask = engine.start();
        debugTask = server.start(new InetSocketAddress("127.0.0.1", 9229), true);

        engine.pushMsg(false, null, new NativeFunction((ctx, thisArg, _a) -> {
            new Internals().apply(env);

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
            // TODO: make better API
            env.global.define(true, new NativeFunction("include", (_ctx, th, __args) -> {
                try {
                    var currFilename = StackData.peekFrame(_ctx).function.loc().filename();
                    var loc = Path.of("").toAbsolutePath();
                    if (currFilename.protocol.equals("file")) loc = Path.of(currFilename.path).getParent();
                    var path = loc.resolve(Path.of(__args.length >= 1 ? Values.toString(_ctx, __args[0]) : ""));
                    var src = Files.readString(path);
                    var func = _ctx.compile(Filename.fromFile(path.toFile()), src);
                    var callArgs = new ArrayValue();
                    if (__args.length >= 2 && __args[1] instanceof ArrayValue) callArgs = (ArrayValue)__args[1];
                    return func.call(_ctx, null, callArgs);
                }
                catch (IOException e) { throw EngineException.ofError("IOError", "Couldn't open file."); }
            }));

            return null;
        }), null).await();

        try {
            var tsEnv = env.child();
            tsEnv.global.define(null, "module", false, new ObjectValue());
            engine.pushMsg(
                false, new Context(engine).pushEnv(tsEnv),
                new Filename("jscript", "ts.js"),
                Reading.resourceToString("js/ts.js"), null
            ).await();
            System.out.println("Loaded typescript!");

            var ctx = new Context(engine).pushEnv(env.child());

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
