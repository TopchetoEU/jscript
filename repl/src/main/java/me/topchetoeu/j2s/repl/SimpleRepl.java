package me.topchetoeu.j2s.repl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import me.topchetoeu.j2s.common.Environment;
import me.topchetoeu.j2s.common.Filename;
import me.topchetoeu.j2s.common.Key;
import me.topchetoeu.j2s.common.Metadata;
import me.topchetoeu.j2s.common.Reading;
import me.topchetoeu.j2s.common.SyntaxException;
import me.topchetoeu.j2s.lib.Compilers;
import me.topchetoeu.j2s.lib.StdLib;
import me.topchetoeu.j2s.repl.debug.SimpleDebugHandler;
import me.topchetoeu.j2s.repl.debug.DebugServer;
import me.topchetoeu.j2s.repl.debug.Debugger;
import me.topchetoeu.j2s.repl.debug.SimpleDebugger;
import me.topchetoeu.j2s.runtime.Compiler;
import me.topchetoeu.j2s.runtime.Engine;
import me.topchetoeu.j2s.runtime.EventLoop;
import me.topchetoeu.j2s.runtime.debug.DebugHandler;
import me.topchetoeu.j2s.runtime.exceptions.EngineException;
import me.topchetoeu.j2s.runtime.values.Value;
import me.topchetoeu.j2s.runtime.values.functions.NativeFunction;

public class SimpleRepl {
	static Thread engineTask, debugTask;
	static Engine engine = new Engine();
	static Environment environment = Environment.empty();
	static DebugServer server;
	static Debugger debugger;
	static Key<OutputStream> STDOUT = new Key<>();
	static Key<OutputStream> STDERR = new Key<>();
	static Key<InputStream> STDIN = new Key<>();

	static int j = 0;
	static String[] args;

	private static void reader() {
		try {
			server = new DebugServer();
			debugTask = server.start(new InetSocketAddress("127.0.0.1", 9229), true);
			server.targets.put("default", (socket, req) -> new SimpleDebugger(socket)
				.attach((SimpleDebugHandler)DebugHandler.get(environment))
			);

			System.out.println(String.format("Running %s v%s by %s", Metadata.name(), Metadata.version(), Metadata.author()));

			for (var arg : args) {
				var file = new File(arg);
				var raw = Reading.streamToString(new FileInputStream(file));

				try {
					try {
						var res = engine.pushMsg(
							false, environment,
							Filename.fromFile(file), raw, null
						).get();

						System.err.println(res.toReadable(environment));
					}
					catch (ExecutionException e) { throw e.getCause(); }
				}
				catch (EngineException | SyntaxException e) { System.err.println(Value.errorToReadable(environment, e, null)); }

			}

			for (var i = 0; ; i++) {
				var raw = Reading.readline();

				if (raw == null) break;

				try {
					try {
						var res = engine.pushMsg(
							false, environment,
							new Filename(Metadata.name(), "repl/" + i + ".js"), raw,
							Value.UNDEFINED
						).get();
						System.err.println(res.toReadable(environment));
					}
					catch (ExecutionException e) { throw e.getCause(); }
				}
				catch (EngineException | SyntaxException e) { System.err.println(Value.errorToReadable(environment, e, null)); }

			}
		}
		catch (EngineException | SyntaxException e) { System.err.println(Value.errorToReadable(environment, e, null)); }
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

	private static Environment createESEnv() {
		var env = new Environment();
		env.add(EventLoop.KEY, engine);
		env.add(DebugHandler.KEY, new SimpleDebugHandler());
		env.add(Compiler.KEY, Compilers.chainTranspilers(environment, Compilers.jsCompiler(), Compilers::babelCompiler, Compilers::coffeescriptCompiler));
		StdLib.apply(env);

		var glob = Value.global(env);

		glob.defineOwnField(null, "exit", new NativeFunction("exit", args -> {
			Thread.currentThread().interrupt();
			throw new CancellationException();
		}));

		return env;
	}

	private static void initEngine() {
		engineTask = engine.start();
	}

	public static void main(String args[]) throws InterruptedException {
		SimpleRepl.args = args;
		var reader = new Thread(SimpleRepl::reader);

		environment = createESEnv();

		initEngine();

		reader.setDaemon(true);
		reader.setName("STD Reader");
		reader.start();

		reader.join();
		engineTask.interrupt();
		debugTask.interrupt();
	}
}
