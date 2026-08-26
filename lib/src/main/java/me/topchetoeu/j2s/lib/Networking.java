package me.topchetoeu.j2s.lib;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;

import me.topchetoeu.j2s.common.Environment;
import me.topchetoeu.j2s.common.Key;
import me.topchetoeu.j2s.runtime.EngineException;
import me.topchetoeu.j2s.runtime.EventLoop;
import me.topchetoeu.j2s.runtime.values.Value;
import me.topchetoeu.j2s.runtime.values.functions.NativeFunction;
import me.topchetoeu.j2s.runtime.values.objects.ObjectValue;
import me.topchetoeu.j2s.runtime.values.primitives.UserValue;

public class Networking {
	public static final Key<EventLoop> NET_POOL = new Key<>();

	public static ObjectValue socketPrimordials(Environment env) {
		var res = new ObjectValue();
		res.setPrototype(null, null);

		var prototype = new ObjectValue[1];
		NativeFunction serverConstructor = new NativeFunction(args -> {
			try {
				return UserValue.of(new ServerSocket());
			}
			catch (IOException e) {
				throw EngineException.ofError("IOException", e.getMessage());
			}
		});
		serverConstructor.prototype.defineOwnField(env, "bind", new NativeFunction(args -> {
			var socket = args.self(ServerSocket.class);
			var address = args.get(0).toString(args.env);

			URI uri;

			try {
				uri = new URI(null, address, null, null, null).parseServerAuthority();
				socket.bind(new InetSocketAddress(uri.getHost(), uri.getPort()));
				return Value.UNDEFINED;
			}
			catch (URISyntaxException e) { throw EngineException.ofSyntax(e.getMessage()); }
			catch (IOException e) { throw EngineException.ofError("IOException", e.getMessage()); }


		}));
		prototype[0] = (ObjectValue)serverConstructor.prototype;

		return serverConstructor;
	}
}
