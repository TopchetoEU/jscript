package me.topchetoeu.jscript;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.Engine;
import me.topchetoeu.jscript.engine.Environment;
import me.topchetoeu.jscript.engine.values.NativeFunction;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.events.Observer;
import me.topchetoeu.jscript.exceptions.EngineException;
import me.topchetoeu.jscript.exceptions.InterruptException;
import me.topchetoeu.jscript.exceptions.SyntaxException;
import me.topchetoeu.jscript.exceptions.UncheckedException;
import me.topchetoeu.jscript.lib.Internals;

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
        catch (Throwable e) { throw new UncheckedException(e); }
    }
    public static String resourceToString(String name) {
        var str = Main.class.getResourceAsStream("/me/topchetoeu/jscript/" + name);
        if (str == null) return null;
        return streamToString(str);
    }

    private static Observer<Object> valuePrinter = new Observer<Object>() {
        public void next(Object data) {
            Values.printValue(null, data);
            System.out.println();
        }

        public void error(RuntimeException err) {
            Values.printError(err, null);
        }
    };

    public static void main(String args[]) {
        System.out.println(String.format("Running %s v%s by %s", Metadata.NAME, Metadata.VERSION, Metadata.AUTHOR));
        var in = new BufferedReader(new InputStreamReader(System.in));
        engine = new Engine();

        env = new Environment(null, null, null);
        var exited = new boolean[1];

        engine.pushMsg(false, null, new NativeFunction((ctx, thisArg, _a) -> {
            new Internals().apply(env);

            env.global.define("exit", _ctx -> {
                exited[0] = true;
                throw new InterruptException();
            });
            env.global.define("go", _ctx -> {
                try {
                    var func = _ctx.compile("do.js", new String(Files.readAllBytes(Path.of("do.js"))));
                    return func.call(_ctx);
                }
                catch (IOException e) {
                    throw new EngineException("Couldn't open do.js");
                }
            });

            return null;
        }), null);

        task = engine.start();
        var reader = new Thread(() -> {
            try {
                while (true) {
                    try {
                        var raw = in.readLine();

                        if (raw == null) break;
                        engine.pushMsg(false, new Context(engine).pushEnv(env), "<stdio>", raw, null).toObservable().once(valuePrinter);
                    }
                    catch (EngineException e) { Values.printError(e, ""); }
                }
            }
            catch (InterruptException e) { return; }
            catch (SyntaxException ex) {
                if (exited[0]) return;
                System.out.println("Syntax error:" + ex.msg);
            }
            catch (RuntimeException ex) {
                if (exited[0]) return;
                System.out.println("Internal error ocurred:");
                ex.printStackTrace();
            }
            catch (Throwable e) { throw new UncheckedException(e); }
            if (exited[0]) return;
        });
        reader.setDaemon(true);
        reader.setName("STD Reader");
        reader.start();
    }
}
