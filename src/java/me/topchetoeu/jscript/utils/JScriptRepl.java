package me.topchetoeu.jscript.utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.common.Metadata;
import me.topchetoeu.jscript.common.Reading;
import me.topchetoeu.jscript.core.engine.Engine;
import me.topchetoeu.jscript.core.engine.Environment;
import me.topchetoeu.jscript.core.engine.debug.DebugContext;
import me.topchetoeu.jscript.core.engine.values.NativeFunction;
import me.topchetoeu.jscript.core.engine.values.Values;
import me.topchetoeu.jscript.core.exceptions.EngineException;
import me.topchetoeu.jscript.core.exceptions.InterruptException;
import me.topchetoeu.jscript.core.exceptions.SyntaxException;
import me.topchetoeu.jscript.lib.Internals;
import me.topchetoeu.jscript.utils.debug.DebugServer;
import me.topchetoeu.jscript.utils.debug.SimpleDebugger;
import me.topchetoeu.jscript.utils.filesystem.Filesystem;
import me.topchetoeu.jscript.utils.filesystem.MemoryFilesystem;
import me.topchetoeu.jscript.utils.filesystem.Mode;
import me.topchetoeu.jscript.utils.filesystem.PhysicalFilesystem;
import me.topchetoeu.jscript.utils.filesystem.RootFilesystem;
import me.topchetoeu.jscript.utils.modules.ModuleRepo;
import me.topchetoeu.jscript.utils.permissions.PermissionsManager;
import me.topchetoeu.jscript.utils.permissions.PermissionsProvider;

public class JScriptRepl {
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
        engine.add(DebugContext.ENV_KEY, ctx);

        debugServer.targets.put("target", (ws, req) -> new SimpleDebugger(ws).attach(ctx));
        engineTask = engine.start();
        debugTask = debugServer.start(new InetSocketAddress("127.0.0.1", 9229), true);
    }

    public static void main(String args[]) {
        System.out.println(String.format("Running %s v%s by %s", Metadata.name(), Metadata.version(), Metadata.author()));

        JScriptRepl.args = args;
        var reader = new Thread(JScriptRepl::reader);

        initEnv();
        initEngine();

        reader.setDaemon(true);
        reader.setName("STD Reader");
        reader.start();
    }
}
