package me.topchetoeu.jscript;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import me.topchetoeu.jscript.engine.Engine;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.events.Observer;
import me.topchetoeu.jscript.exceptions.EngineException;
import me.topchetoeu.jscript.exceptions.SyntaxException;

public class Main {
    static Thread task;
    static Engine engine;

    private static Observer<Object> valuePrinter = new Observer<Object>() {
        public void next(Object data) {
            try {
                Values.printValue(engine.context(), data);
            }
            catch (InterruptedException e) { }
            System.out.println();
        }

        public void error(RuntimeException err) {
            try {
                if (err instanceof EngineException) {
                    System.out.println("Uncaught " + ((EngineException)err).toString(engine.context()));
                }
                else if (err instanceof SyntaxException) {
                    System.out.println("Syntax error:" + ((SyntaxException)err).msg);
                }
                else if (err.getCause() instanceof InterruptedException) return;
                else {
                    System.out.println("Internal error ocurred:");
                    err.printStackTrace();
                }
            }
            catch (EngineException ex) {
                System.out.println("Uncaught [error while converting to string]");
            }
            catch (InterruptedException ex) {
                return;
            }
        }
    };

    public static void main(String args[]) {
        var in = new BufferedReader(new InputStreamReader(System.in));
        engine = new Engine();
        var scope = engine.global().globalChild();
        var exited = new boolean[1];

        scope.define("exit", ctx -> {
            exited[0] = true;
            task.interrupt();
            throw new InterruptedException();
        });
        scope.define("go", ctx -> {
            try {
                var func = engine.compile(scope, "do.js", new String(Files.readAllBytes(Path.of("do.js"))));
                return func.call(ctx);
            }
            catch (IOException e) {
                throw new EngineException("Couldn't open do.js");
            }
        });

        task = engine.start();
        var reader = new Thread(() -> {
            try {
                while (true) {
                    try {
                        var raw = in.readLine();

                        if (raw == null) break;
                        engine.pushMsg(false, scope, Map.of(), "<stdio>", raw, null).toObservable().once(valuePrinter);
                    }
                    catch (EngineException e) {
                        try {
                            System.out.println("Uncaught " + e.toString(engine.context()));
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
