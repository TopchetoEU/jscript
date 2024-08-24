package me.topchetoeu.jscript.utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.common.Metadata;
import me.topchetoeu.jscript.common.Reading;
import me.topchetoeu.jscript.lib.Internals;
import me.topchetoeu.jscript.runtime.Compiler;
import me.topchetoeu.jscript.runtime.Engine;
import me.topchetoeu.jscript.runtime.EventLoop;
import me.topchetoeu.jscript.runtime.debug.DebugContext;
import me.topchetoeu.jscript.runtime.environment.Environment;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.exceptions.InterruptException;
import me.topchetoeu.jscript.runtime.exceptions.SyntaxException;
import me.topchetoeu.jscript.runtime.scope.GlobalScope;
import me.topchetoeu.jscript.runtime.values.NativeFunction;
import me.topchetoeu.jscript.runtime.values.Values;
import me.topchetoeu.jscript.utils.debug.DebugServer;
import me.topchetoeu.jscript.utils.debug.SimpleDebugger;
import me.topchetoeu.jscript.utils.filesystem.Filesystem;
import me.topchetoeu.jscript.utils.filesystem.MemoryFilesystem;
import me.topchetoeu.jscript.utils.filesystem.Mode;
import me.topchetoeu.jscript.utils.filesystem.PhysicalFilesystem;
import me.topchetoeu.jscript.utils.filesystem.RootFilesystem;
import me.topchetoeu.jscript.utils.filesystem.STDFilesystem;
import me.topchetoeu.jscript.utils.modules.ModuleRepo;
import me.topchetoeu.jscript.utils.permissions.PermissionsManager;
import me.topchetoeu.jscript.utils.permissions.PermissionsProvider;

public class JScriptRepl {
    static Thread engineTask, debugTask;
    static Engine engine = new Engine();
    static DebugServer debugServer = new DebugServer();
    static Environment environment = Environment.empty();

    static int j = 0;
    static String[] args;

    private static void reader() {
        try {
            for (var arg : args) {
                try {
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
                catch (EngineException e) { Values.printError(e, null); }
            }
            for (var i = 0; ; i++) {
                try {
                    var raw = Reading.readline();

                    if (raw == null) break;
                    var func = Compiler.compile(environment, new Filename("jscript", "repl/" + i + ".js"), raw);
                    var res = engine.pushMsg(false, environment, func, null).await();
                    Values.printValue(null, res);
                    System.out.println();
                }
                catch (EngineException e) { Values.printError(e, null); }
                catch (SyntaxException e) { Values.printError(e, null); }
            }
        }
        catch (IOException e) {
            System.out.println(e.toString());
            engine.thread().interrupt();
        }
        catch (RuntimeException ex) {
            if (ex instanceof InterruptException) return;
            else {
                System.out.println("Internal error ocurred:");
                ex.printStackTrace();
            }
        }
    }

    private static void initEnv() {
        environment = Internals.apply(environment);

        var glob = GlobalScope.get(environment);

        glob.define(null, false, new NativeFunction("exit", args -> {
            throw new InterruptException();
        }));
        glob.define(null, false, new NativeFunction("go", args -> {
            try {
                var f = Path.of("do.js");
                var func = Compiler.compile(args.env, new Filename("do", "do/" + j++ + ".js"), new String(Files.readAllBytes(f)));
                return func.call(args.env);
            }
            catch (IOException e) {
                throw new EngineException("Couldn't open do.js");
            }
        }));
        glob.define(null, false, new NativeFunction("log", args -> {
            for (var el : args.args) {
                Values.printValue(args.env, el);
            }

            return null;
        }));

        var fs = new RootFilesystem(PermissionsProvider.get(environment));
        fs.protocols.put("temp", new MemoryFilesystem(Mode.READ_WRITE));
        fs.protocols.put("file", new PhysicalFilesystem("."));
        fs.protocols.put("std", new STDFilesystem(System.in, System.out, System.err));

        environment.add(PermissionsProvider.KEY, PermissionsManager.ALL_PERMS);
        environment.add(Filesystem.KEY, fs);
        environment.add(ModuleRepo.KEY, ModuleRepo.ofFilesystem(fs));
        environment.add(Compiler.KEY, new JSCompiler(environment));
        environment.add(EventLoop.KEY, engine);
    }
    private static void initEngine() {
        var ctx = new DebugContext();
        environment.add(DebugContext.KEY, ctx);

        debugServer.targets.put("target", (ws, req) -> new SimpleDebugger(ws).attach(ctx));
        engineTask = engine.start();
        debugTask = debugServer.start(new InetSocketAddress("127.0.0.1", 9229), true);
    }

    public static void main(String args[]) throws InterruptedException {
        System.out.println(String.format("Running %s v%s by %s", Metadata.name(), Metadata.version(), Metadata.author()));

        JScriptRepl.args = args;
        var reader = new Thread(JScriptRepl::reader);

        initEnv();
        initEngine();

        reader.setDaemon(true);
        reader.setName("STD Reader");
        reader.start();

        engine.thread().join();
        debugTask.interrupt();
        engineTask.interrupt();
    }
}
