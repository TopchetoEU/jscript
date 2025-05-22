package me.topchetoeu.j2s.repl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
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
import me.topchetoeu.j2s.lib.debug.DebugServer;
import me.topchetoeu.j2s.lib.debug.Debugger;
import me.topchetoeu.j2s.lib.debug.SimpleDebugHandler;
import me.topchetoeu.j2s.lib.debug.SimpleDebugger;
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
	static String[] files;
	static boolean inspect = false;
	static int inspectPort = 9229;

	private static void reader() {
		try {
			if (inspect) {
				server = new DebugServer();
				debugTask = server.start(new InetSocketAddress("127.0.0.1", inspectPort), true);
				server.targets.put("default", (socket, req) -> new SimpleDebugger(socket)
					.attach((SimpleDebugHandler)DebugHandler.get(environment))
				);
				System.out.println("Debug server started at localhost:" + inspectPort);
			}

			System.out.println(String.format("Running %s v%s by %s", Metadata.name(), Metadata.version(), Metadata.author()));

			if (files.length > 0) {
				for (var arg : files) {
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
			}
			else {
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

	private static Environment createESEnv(String compiler) {
		var env = StdLib.apply(null);
		env.add(EventLoop.KEY, engine);
		env.add(DebugHandler.KEY, new SimpleDebugHandler());

		switch (compiler) {
			case "typescript":
			case "ts":
				env.add(Compiler.KEY, Compilers.chainTranspilers(Compilers.jsCompiler(), env, Compilers::typescriptCompiler));
				break;
			case "coffeescript":
			case "cs":
				env.add(Compiler.KEY, Compilers.chainTranspilers(Compilers.jsCompiler(), env, Compilers::babelCompiler, Compilers::coffeescriptCompiler));
				break;
			case "babel":
			case "es6":
			case "esnext":
				env.add(Compiler.KEY, Compilers.chainTranspilers(Compilers.jsCompiler(), env, Compilers::babelCompiler));
				break;
			default:
			case "js":
				env.add(Compiler.KEY, Compilers.jsCompiler());
				break;
		}

		var glob = Value.global(env);

		glob.defineOwnField(null, "exit", new NativeFunction("exit", args -> {
			Thread.currentThread().interrupt();
			throw new CancellationException();
		}));
		glob.defineOwnField(null, "dofile", new NativeFunction("dofile", args -> {
			var file = args.get(0).toString(args.env);
			var filename = new Filename("file", new File(file).getAbsolutePath());

			try {
				var src = Reading.streamToString(new FileInputStream(file));
				return Compiler.get(args.env).compile(args.env, filename, src, v -> v).apply(args.env, Value.UNDEFINED);
			}
			catch (FileNotFoundException e) {
				throw EngineException.ofError("IOException", e.getMessage());
			}
		}));

		return env;
	}

	private static void initEngine() {
		engineTask = engine.start();
	}

	public static void main(String args[]) throws InterruptedException {
		var compiler = "js";
		var files = new ArrayList<String>();

		for (String arg : args) {
			if (arg.startsWith("--lang=")) {
				compiler = arg.substring(7);
			}
			else if (arg.equals("--inspect")) {
				inspect = true;
			}
			else if (arg.startsWith("--inspect=")) {
				inspect = true;
				inspectPort = Integer.parseInt(arg.substring(10));
			}
			else {
				files.add(arg);
			}
		}

		SimpleRepl.files = files.toArray(new String[0]);
		var reader = new Thread(SimpleRepl::reader);

		environment = createESEnv(compiler);

		initEngine();

		reader.setDaemon(true);
		reader.setName("STD Reader");
		reader.start();

		reader.join();
		engineTask.interrupt();
		if (debugTask != null) debugTask.interrupt();
	}
}
