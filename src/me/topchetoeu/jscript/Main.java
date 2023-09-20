package me.topchetoeu.jscript;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

import me.topchetoeu.jscript.engine.MessageContext;
import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.Engine;
import me.topchetoeu.jscript.engine.Environment;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.events.Observer;
import me.topchetoeu.jscript.exceptions.EngineException;
import me.topchetoeu.jscript.exceptions.SyntaxException;
import me.topchetoeu.jscript.interop.NativeTypeRegister;
import me.topchetoeu.jscript.polyfills.Internals;

public class Main {
    static Thread task;
    static Engine engine;
    static Environment env;

    public static String streamToString(InputStream in) {
        try {
            StringBuilder out = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            for(var line = br.readLine(); line != null; line = br.readLine()) {
                out.append(line).append('\n');
            }

            br.close();
            return out.toString();
        }
        catch (IOException e) {
            return null;
        }
    }
    public static String resourceToString(String name) {
        var str = Main.class.getResourceAsStream("/me/topchetoeu/jscript/" + name);
        if (str == null) return null;
        return streamToString(str);
    }

    private static Observer<Object> valuePrinter = new Observer<Object>() {
        public void next(Object data) {
            try { Values.printValue(null, data); }
            catch (InterruptedException e) { }
            System.out.println();
        }

        public void error(RuntimeException err) {
            try { Values.printError(err, null); }
            catch (InterruptedException ex) { return; }
        }
    };

    public static void main(String args[]) {
        System.out.println(String.format("Running %s v%s by %s", Metadata.NAME, Metadata.VERSION, Metadata.AUTHOR));
        var in = new BufferedReader(new InputStreamReader(System.in));
        engine = new Engine();

        // TODO: Replace type register with safer accessor
        env = new Environment(null, new NativeTypeRegister(), null);
        var builderEnv = new Environment(null, new NativeTypeRegister(), null);
        var exited = new boolean[1];

        env.global.define("exit", ctx -> {
            exited[0] = true;
            task.interrupt();
            throw new InterruptedException();
        });
        env.global.define("go", ctx -> {
            try {
                var func = ctx.compile("do.js", new String(Files.readAllBytes(Path.of("do.js"))));
                return func.call(ctx);
            }
            catch (IOException e) {
                throw new EngineException("Couldn't open do.js");
            }
        });

        engine.pushMsg(false, new Context(builderEnv, new MessageContext(engine)), "core.js", resourceToString("js/core.js"), null, env, new Internals()).toObservable().on(valuePrinter);

        task = engine.start();
        var reader = new Thread(() -> {
            try {
                while (true) {
                    try {
                        var raw = in.readLine();

                        if (raw == null) break;
                        engine.pushMsg(false, new Context(env, new MessageContext(engine)), "<stdio>", raw, null).toObservable().once(valuePrinter);
                    }
                    catch (EngineException e) {
                        try {
                            System.out.println("Uncaught " + e.toString(null));
                        }
                        catch (EngineException ex) {
                            System.out.println("Uncaught [error while converting to string]");
                        }
                    }
                }
            }
            catch (IOException e) {
                e.printStackTrace();
                return;
            }
            catch (SyntaxException ex) {
                if (exited[0]) return;
                System.out.println("Syntax error:" + ex.msg);
            }
            catch (RuntimeException ex) {
                if (exited[0]) return;
                System.out.println("Internal error ocurred:");
                ex.printStackTrace();
            }
            catch (InterruptedException e) { return; }
            if (exited[0]) return;
        });
        reader.setDaemon(true);
        reader.setName("STD Reader");
        reader.start();
    }
}
