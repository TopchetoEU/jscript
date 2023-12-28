package me.topchetoeu.jscript;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.Engine;
import me.topchetoeu.jscript.engine.Environment;
import me.topchetoeu.jscript.engine.debug.DebugContext;
import me.topchetoeu.jscript.engine.debug.DebugServer;
import me.topchetoeu.jscript.engine.debug.SimpleDebugger;
import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.NativeFunction;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.exceptions.EngineException;
import me.topchetoeu.jscript.exceptions.InterruptException;
import me.topchetoeu.jscript.exceptions.SyntaxException;
import me.topchetoeu.jscript.filesystem.Filesystem;
import me.topchetoeu.jscript.filesystem.MemoryFilesystem;
import me.topchetoeu.jscript.filesystem.Mode;
import me.topchetoeu.jscript.filesystem.PhysicalFilesystem;
import me.topchetoeu.jscript.filesystem.RootFilesystem;
import me.topchetoeu.jscript.lib.EnvironmentLib;
import me.topchetoeu.jscript.lib.Internals;
import me.topchetoeu.jscript.modules.ModuleRepo;
import me.topchetoeu.jscript.permissions.PermissionsManager;
import me.topchetoeu.jscript.permissions.PermissionsProvider;

public class Main {
    static Thread engineTask, debugTask;
    static Engine engine = new Engine();
    static DebugServer debugServer = new DebugServer();
    static Environment environment = new Environment();

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
                            false, environment,
                            Filename.fromFile(file.toFile()),
                            raw, null
                        ).await();
                        Values.printValue(null, res);
                        System.out.println();
                    }
                }
                catch (EngineException e) { Values.printError(e, null); }
            }
            for (var i = 0; ; i++) {
                try {
                    var raw = Reading.readline();

                    if (raw == null) break;
                    var res = engine.pushMsg(
                        false, environment,
                        new Filename("jscript", "repl/" + i + ".js"),
                        raw, null
                    ).await();
                    Values.printValue(null, res);
                    System.out.println();
                }
                catch (EngineException e) { Values.printError(e, null); }
                catch (SyntaxException e) { Values.printError(e, null); }
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

        environment.global.define(false, new NativeFunction("exit", args -> {
            exited = true;
            throw new InterruptException();
        }));
        environment.global.define(false, new NativeFunction("go", args -> {
            try {
                var f = Path.of("do.js");
                var func = args.ctx.compile(new Filename("do", "do/" + j++ + ".js"), new String(Files.readAllBytes(f)));
                return func.call(args.ctx);
            }
            catch (IOException e) {
                throw new EngineException("Couldn't open do.js");
            }
        }));

        var fs = new RootFilesystem(PermissionsProvider.get(environment));
        fs.protocols.put("temp", new MemoryFilesystem(Mode.READ_WRITE));
        fs.protocols.put("file", new PhysicalFilesystem("."));

        environment.add(PermissionsProvider.ENV_KEY, PermissionsManager.ALL_PERMS);
        environment.add(Filesystem.ENV_KEY, fs);
        environment.add(ModuleRepo.ENV_KEY, ModuleRepo.ofFilesystem(fs));
    }
    private static void initEngine() {
        var ctx = new DebugContext();
        engine.globalEnvironment.add(DebugContext.ENV_KEY, ctx);

        debugServer.targets.put("target", (ws, req) -> new SimpleDebugger(ws).attach(ctx));
        engineTask = engine.start();
        debugTask = debugServer.start(new InetSocketAddress("127.0.0.1", 9229), true);
    }
    private static void initTypescript() {
        try {
            var tsEnv = Internals.apply(new Environment());
            tsEnv.add(Environment.HIDE_STACK, true);
            tsEnv.global.define(null, "module", false, new ObjectValue());
            var bsEnv = Internals.apply(new Environment());
            bsEnv.add(Environment.HIDE_STACK, true);

            engine.pushMsg(
                false, tsEnv,
                new Filename("jscript", "ts.js"),
                Reading.resourceToString("assets/js/ts.js"), null
            ).await();
            System.out.println("Loaded typescript!");

            var typescript = tsEnv.global.get(new Context(engine, bsEnv), "ts");
            var libs = new ArrayValue(null, Reading.resourceToString("assets/js/lib.d.ts"));

            engine.pushMsg(
                false, bsEnv,
                new Filename("jscript", "bootstrap.js"), Reading.resourceToString("assets/js/bootstrap.js"), null,
                typescript, new EnvironmentLib(environment), libs
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
