package me.topchetoeu.jscript.repl.debug;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import me.topchetoeu.jscript.common.FunctionBody;
import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.Instruction.Type;
import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.common.json.JSON;
import me.topchetoeu.jscript.common.json.JSONElement;
import me.topchetoeu.jscript.common.json.JSONList;
import me.topchetoeu.jscript.common.json.JSONMap;
import me.topchetoeu.jscript.common.mapping.FunctionMap;
import me.topchetoeu.jscript.common.parsing.Filename;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.runtime.Compiler;
import me.topchetoeu.jscript.runtime.Engine;
import me.topchetoeu.jscript.runtime.EventLoop;
import me.topchetoeu.jscript.runtime.Frame;
import me.topchetoeu.jscript.runtime.JSONConverter;
import me.topchetoeu.jscript.runtime.debug.DebugContext;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.Member.FieldMember;
import me.topchetoeu.jscript.runtime.values.Member.PropertyMember;
import me.topchetoeu.jscript.runtime.values.functions.FunctionValue;
import me.topchetoeu.jscript.runtime.values.objects.ArrayValue;
import me.topchetoeu.jscript.runtime.values.objects.ObjectValue;
import me.topchetoeu.jscript.runtime.values.primitives.BoolValue;
import me.topchetoeu.jscript.runtime.values.primitives.StringValue;
import me.topchetoeu.jscript.runtime.values.primitives.SymbolValue;
import me.topchetoeu.jscript.runtime.values.primitives.numbers.NumberValue;

// very simple indeed
public class SimpleDebugger implements Debugger {
	public static final Set<String> VSCODE_EMPTY = new HashSet<>(Arrays.asList(
		"function(...runtimeArgs){\n    let t = 1024; let e = null;\n    if(e)try{let r=\"<<default preview>>\",i=e.call(this,r);if(i!==r)return String(i)}catch(r){return`<<indescribable>>${JSON.stringify([String(r),\"object\"])}`}if(typeof this==\"object\"&&this){let r;for(let i of[Symbol.for(\"debug.description\"),Symbol.for(\"nodejs.util.inspect.custom\")])try{r=this[i]();break}catch{}if(!r&&!String(this.toString).includes(\"[native code]\")&&(r=String(this)),r&&!r.startsWith(\"[object \"))return r.length>=t?r.slice(0,t)+\"\\u2026\":r}\n  ;\n\n}",
		"function(...runtimeArgs){\n    let r = 1024; let e = null;\n    if(e)try{let t=\"<<default preview>>\",n=e.call(this,t);if(n!==t)return String(n)}catch(t){return`<<indescribable>>${JSON.stringify([String(t),\"object\"])}`}if(typeof this==\"object\"&&this){let t;for(let n of[Symbol.for(\"debug.description\"),Symbol.for(\"nodejs.util.inspect.custom\")])if(typeof this[n]==\"function\")try{t=this[n]();break}catch{}if(!t&&!String(this.toString).includes(\"[native code]\")&&(t=String(this)),t&&!t.startsWith(\"[object\"))return t.length>=r?t.slice(0,r)+\"\\u2026\":t};}",
		"function(...runtimeArgs){\n    let r = 1024; let e = null;\n    if(e)try{let t=\"<<default preview>>\",n=e.call(this,t);if(n!==t)return String(n)}catch(t){return`<<indescribable>>${JSON.stringify([String(t),\"object\"])}`}if(typeof this==\"object\"&&this){let t;for(let n of[Symbol.for(\"debug.description\"),Symbol.for(\"nodejs.util.inspect.custom\")])if(typeof this[n]==\"function\")try{t=this[n](2);break}catch{}if(!t&&!String(this.toString).includes(\"[native code]\")&&(t=String(this)),t&&!t.startsWith(\"[object \"))return t.length>=r?t.slice(0,r)+\"\\u2026\":t};}",
		"function(...runtimeArgs){\n    let t = 1024; let e = null;\n    let r={},i=\"<<default preview>>\";if(typeof this!=\"object\"||!this)return r;for(let[n,s]of Object.entries(this)){if(e)try{let o=e.call(s,i);if(o!==i){r[n]=String(o);continue}}catch(o){r[n]=`<<indescribable>>${JSON.stringify([String(o),n])}`;continue}if(typeof s==\"object\"&&s){let o;for(let a of runtimeArgs[0])try{o=s[a]();break}catch{}!o&&!String(s.toString).includes(\"[native code]\")&&(o=String(s)),o&&!o.startsWith(\"[object \")&&(r[n]=o.length>=t?o.slice(0,t)+\"\\u2026\":o)}}return r\n  ;\n\n}",
		"function(...runtimeArgs){\n    let r = 1024; let e = null;\n    let t={},n=\"<<default preview>>\";if(typeof this!=\"object\"||!this)return t;for(let[i,o]of Object.entries(this)){if(e)try{let s=e.call(o,n);if(s!==n){t[i]=String(s);continue}}catch(s){t[i]=`<<indescribable>>${JSON.stringify([String(s),i])}`;continue}if(typeof o==\"object\"&&o){let s;for(let a of runtimeArgs[0])if(typeof o[a]==\"function\")try{s=o[a]();break}catch{}!s&&!String(o.toString).includes(\"[native code]\")&&(s=String(o)),s&&!s.startsWith(\"[object \")&&(t[i]=s.length>=r?s.slice(0,r)+\"\\u2026\":s)}}return t\n  ;\n\n}",
		"function(...runtimeArgs){\n    let r = 1024; let e = null;\n    let t={},n=\"<<default preview>>\";if(typeof this!=\"object\"||!this)return t;for(let[i,o]of Object.entries(this)){if(e)try{let s=e.call(o,n);if(s!==n){t[i]=String(s);continue}}catch(s){t[i]=`<<indescribable>>${JSON.stringify([String(s),i])}`;continue}if(typeof o==\"object\"&&o){let s;for(let a of runtimeArgs[0])if(typeof o[a]==\"function\")try{s=o[a](2);break}catch{}!s&&!String(o.toString).includes(\"[native code]\")&&(s=String(o)),s&&!s.startsWith(\"[object \")&&(t[i]=s.length>=r?s.slice(0,r)+\"\\u2026\":s)}}return t\n  ;\n\n}",
		"function(){let t={__proto__:this.__proto__\n},e=Object.getOwnPropertyNames(this);for(let r=0;r<e.length;++r){let i=e[r],n=i>>>0;if(String(n>>>0)===i&&n>>>0!==4294967295)continue;let s=Object.getOwnPropertyDescriptor(this,i);s&&Object.defineProperty(t,i,s)}return t}",
		"function(){return[Symbol.for(\"debug.description\"),Symbol.for(\"nodejs.util.inspect.custom\")]\n}"
	));
	public static final Set<String> VSCODE_SELF = new HashSet<>(Arrays.asList(
		"function(t,e){let r={\n},i=t===-1?0:t,n=e===-1?this.length:t+e;for(let s=i;s<n&&s<this.length;++s){let o=Object.getOwnPropertyDescriptor(this,s);o&&Object.defineProperty(r,s,o)}return r}"
	));

	public static final String CHROME_GET_PROP_FUNC = "function s(e){let t=this;const n=JSON.parse(e);for(let e=0,i=n.length;e<i;++e)t=t[n[e]];return t}";
	public static final String CHROME_GET_PROP_FUNC_2 = "function invokeGetter(getter) { return Reflect.apply(getter, this, []);}";
	public static final String VSCODE_CALL = "function(t){return t.call(this)\n}";
	public static final String VSCODE_AUTOCOMPLETE = "function(t,e,r){let n=r?\"variable\":\"property\",i=(l,p,f)=>{if(p!==\"function\")return n;if(l===\"constructor\")return\"class\";let m=String(f);return m.startsWith(\"class \")||m.includes(\"[native code]\")&&/^[A-Z]/.test(l)?\"class\":r?\"function\":\"method\"\n},o=l=>{switch(typeof l){case\"number\":case\"boolean\":return`${l}`;case\"object\":return l===null?\"null\":l.constructor.name||\"object\";case\"function\":return`fn(${new Array(l.length).fill(\"?\").join(\", \")})`;default:return typeof l}},s=[],a=new Set,u=\"~\",c=t===void 0?this:t;for(;c!=null;c=c.__proto__){u+=\"~\";let l=Object.getOwnPropertyNames(c).filter(p=>p.startsWith(e)&&!p.match(/^\\d+$/));for(let p of l){if(a.has(p))continue;a.add(p);let f=Object.getOwnPropertyDescriptor(c,p),m=n,h;try{let H=c[p];m=i(p,typeof f?.value,H),h=o(H)}catch{}s.push({label:p,sortText:u+p.replace(/^_+/,H=>\"{\".repeat(H.length)),type:m,detail:h})}r=!1}return{result:s,isArray:this instanceof Array}}";

	private static enum State {
		RESUMED,
		STEPPING_IN,
		STEPPING_OUT,
		STEPPING_OVER,
		PAUSED_NORMAL,
		PAUSED_EXCEPTION,
	}
	private static enum CatchType {
		NONE,
		UNCAUGHT,
		ALL,
	}
	private static class DebugSource {
		public final int id;
		public final Filename filename;
		public final String source;

		public DebugSource(int id, Filename filename, String source) {
			this.id = id;
			this.filename = filename;
			this.source = source;
		}
	}

	private class Breakpoint {
		public final int id;
		public final String condition;
		public final Pattern pattern;
		public final int line, start;
		public final long locNum;
		public final HashMap<Filename, Location> resolvedLocations = new HashMap<>();
		public final HashMap<Filename, Long> resolvedDistances = new HashMap<>();

		public Breakpoint(int id, Pattern pattern, int line, int start, String condition) {
			this.id = id;
			this.condition = condition;
			this.pattern = pattern;
			this.line = line;
			this.start = start;
			this.locNum = start | ((long)line << 32);

			if (condition != null && condition.trim().equals("")) condition = null;
		}

		// TODO: Figure out how to unload a breakpoint
		// TODO: Do location resolution with function boundaries
		public void addFunc(FunctionBody body, FunctionMap map) {
			try {
				for (var loc : map.correctBreakpoint(pattern, line, start)) {
					var currNum = loc.start() + ((long)loc.line() << 32);
					long currDist = 0;
					if (currNum > locNum) currDist = currNum - locNum;
					else currDist = locNum - currNum;

					if (currDist > resolvedDistances.getOrDefault(loc.filename(), Long.MAX_VALUE)) continue;

					resolvedLocations.put(loc.filename(), loc);
					resolvedDistances.put(loc.filename(), currDist);
				}

				for (var loc : resolvedLocations.values()) {
					ws.send(new V8Event("Debugger.breakpointResolved", new JSONMap()
						.set("breakpointId", id)
						.set("location", serializeLocation(loc))
					));
				}

				updateBreakpoints();
			}
			catch (IOException e) {
				ws.close();
				close();
			}
		}
	}
	private class DebugFrame {
		public final Frame frame;
		public final int id;
		public final ScopeObject variables;
		public final ScopeObject locals, capturables, captures;
		public final StackObject valstack;
		public final Value globals;
		public Location location;

		public void updateLoc(Location loc) {
			if (loc == null) return;
			this.location = loc;
		}

		public JSONMap serialize(Value returnValue) {
			var chain = new JSONList();

			if (returnValue != null) {
				locals.add("Return Value", returnValue);
			}
			else {
				locals.remove("Return Value");
			}

			chain.add(new JSONMap()
				.set("type", "local")
				.set("name", "Locals")
				.set("object", serializeObj(frame.env, locals))
			);
			chain.add(new JSONMap()
				.set("type", "local")
				.set("name", "Capturables")
				.set("object", serializeObj(frame.env, capturables))
			);
			chain.add(new JSONMap()
				.set("type", "closure")
				.set("name", "Captures")
				.set("object", serializeObj(frame.env, captures))
			);
			chain.add(new JSONMap()
				.set("type", "global")
				.set("name", "Globals")
				.set("object", serializeObj(frame.env, globals))
			);
			chain.add(new JSONMap()
				.set("type", "other")
				.set("name", "Stack")
				.set("object", serializeObj(frame.env, valstack))
			);

			return new JSONMap()
				.set("callFrameId", id + "")
				.set("functionName", frame.function.name)
				.set("location", serializeLocation(location))
				.set("scopeChain", chain);
		}

		public DebugFrame(Frame frame, int id) {
			this.frame = frame;
			this.id = id;

			var map = DebugContext.get(frame.env).getMap(frame.function);
			this.globals = Value.global(frame.env);
			this.locals = ScopeObject.locals(frame, map.localNames);
			this.capturables = ScopeObject.capturables(frame, map.capturableNames);
			this.captures = ScopeObject.captures(frame, map.captureNames);
			this.variables = ScopeObject.combine((ObjectValue)this.globals, locals, capturables, captures);
			this.valstack = new StackObject(frame);
		}
	}
	private class ObjRef {
		public final Value obj;
		public final Environment env;
		public final HashSet<String> heldGroups = new HashSet<>();
		public boolean held = true;

		public boolean shouldRelease() {
			return !held && heldGroups.size() == 0;
		}

		public ObjRef(Environment env, Value obj) {
			this.env = env;
			this.obj = obj;
		}
	}

	private static class RunResult {
		public final Environment ext;
		public final Value result;
		public final EngineException error;

		public RunResult(Environment ext, Value result, EngineException error) {
			this.ext = ext;
			this.result = result;
			this.error = error;
		}
	}

	public boolean enabled = true;
	public CatchType execptionType = CatchType.NONE;
	public State state = State.RESUMED;

	public final WebSocket ws;

	private ObjectValue emptyObject = new ObjectValue();

	private WeakHashMap<DebugContext, DebugContext> contexts = new WeakHashMap<>();
	private WeakHashMap<FunctionBody, FunctionMap> mappings = new WeakHashMap<>();
	private HashMap<Location, HashSet<Breakpoint>> bpLocs = new HashMap<>();

	private HashMap<Integer, Breakpoint> idToBreakpoint = new HashMap<>();

	private HashMap<Filename, Integer> filenameToId = new HashMap<>();
	private HashMap<Integer, DebugSource> idToSource = new HashMap<>();
	private ArrayList<DebugSource> pendingSources = new ArrayList<>();

	private HashMap<Integer, DebugFrame> idToFrame = new HashMap<>();
	private HashMap<Frame, DebugFrame> codeFrameToFrame = new HashMap<>();

	private HashMap<Integer, ObjRef> idToObject = new HashMap<>();
	private HashMap<Value, Integer> objectToId = new HashMap<>();
	private HashMap<String, ArrayList<ObjRef>> objectGroups = new HashMap<>();

	private Object updateNotifier = new Object();
	private boolean pendingPause = false;

	private int nextId = 0;
	private DebugFrame stepOutFrame = null, currFrame = null;
	private int stepOutPtr = 0;

	private boolean compare(String src, String target) {
		src = src.replaceAll("\\s", "");
		target = target.replaceAll("\\s", "");
		if (src.length() != target.length()) return false;
		var diff = 0;
		var all = 0;

		for (var i = 0; i < src.length(); i++) {
			var a = src.charAt(i);
			var b = target.charAt(i);
			var letter = Character.isLetter(a) && Character.isLetter(b);

			if (a != b) {
				if (letter) diff++;
				else return false;
			}

			if (letter) all++;
		}

		return diff / (float)all < .5f;
	}
	private boolean compare(String src, Set<String> target) {
		for (var el : target) {
			if (compare(src, el)) return true;
		}
		return false;
	}

	private int nextId() {
		return nextId++;
	}

	private synchronized DebugFrame getFrame(Frame frame) {
		if (!codeFrameToFrame.containsKey(frame)) {
			var id = nextId();
			var fr = new DebugFrame(frame, id);

			idToFrame.put(id, fr);
			codeFrameToFrame.put(frame, fr);

			return fr;
		}
		else return codeFrameToFrame.get(frame);
	}
	private synchronized void updateFrames(Environment env, int skipN) {
		var frame = Frame.get(env, skipN);
		if (frame == null) return;

		currFrame = getFrame(frame);
	}
	private JSONList serializeFrames(Environment env, Value returnValue) {
		var res = new JSONList();
		var frames = Frame.get(env);
		for (var i = frames.size() - 1; i >= 0; i--) {
			var el = frames.get(i);

			var frame = getFrame(el);
			if (frame.location == null) continue;

			res.add(frame.serialize(returnValue));
		}

		return res;
	}

	private void updateBreakpoints() {
		bpLocs.clear();

		for (var bp : idToBreakpoint.values()) {
			for (var loc : bp.resolvedLocations.values()) {
				bpLocs.putIfAbsent(loc, new HashSet<>());
				var set = bpLocs.get(loc);

				set.add(bp);
			}
		}
	}

	private Location deserializeLocation(JSONElement el) {
		if (!el.isMap()) throw new RuntimeException("Expected location to be a map.");
		var id = Integer.parseInt(el.map().string("scriptId"));
		var line = (int)el.map().number("lineNumber");
		var column = (int)el.map().number("columnNumber");

		if (!idToSource.containsKey(id)) throw new RuntimeException(String.format("The specified source %s doesn't exist.", id));

		var res = Location.of(idToSource.get(id).filename, line, column);
		return res;
	}
	private JSONMap serializeLocation(Location loc) {
		var source = filenameToId.get(loc.filename());
		return new JSONMap()
			.set("scriptId", source + "")
			.set("lineNumber", loc.line())
			.set("columnNumber", loc.start());
	}

	private JSONMap serializeObj(Environment env, Value val, boolean byValue) {
		if (val == Value.NULL) {
			return new JSONMap()
				.set("type", "object")
				.set("subtype", "null")
				.setNull("value")
				.set("description", "null");
		}

		var type = val.type().value;

		if (type.equals("object") || type.equals("function")) {
			int id;

			if (objectToId.containsKey(val)) id = objectToId.get(val);
			else {
				id = nextId();
				var ref = new ObjRef(env, val);
				objectToId.put(val, id);
				idToObject.put(id, ref);
			}

			String subtype = null;
			String className = null;

			if (val instanceof ArrayValue) subtype = "array";

			try { className = val.getMemberPath(env, "constructor", "name").toString(env); }
			catch (Exception e) { }

			var res = new JSONMap()
				.set("type", type)
				.set("objectId", id + "");

			if (subtype != null) res.set("subtype", subtype);
			if (className != null) {
				res.set("className", className);
				res.set("description", className);
			}

			if (val instanceof ArrayValue arr) res.set("description", "Array(" + arr.size() + ")");
			else if (val instanceof FunctionValue) res.set("description", val.toString());
			else {
				var defaultToString = false;

				try {
					defaultToString =
						val.getMember(env, "toString") ==
						env.get(Value.OBJECT_PROTO).getMember(env, "toString");
				}
				catch (Exception e) { }

				try { res.set("description", className + (defaultToString ? "" : " { " + val.toString(env) + " }")); }
				catch (Exception e) { }
			}


			if (byValue) try { res.put("value", JSONConverter.fromJs(env, val)); }
			catch (Exception e) { }

			return res;
		}

		if (val == Value.UNDEFINED) return new JSONMap().set("type", "undefined");
		if (val instanceof StringValue str) return new JSONMap().set("type", "string").set("value", str.value);
		if (val instanceof BoolValue bool) return new JSONMap().set("type", "boolean").set("value", bool.value);
		if (val instanceof SymbolValue symbol) return new JSONMap().set("type", "symbol").set("description", symbol.value);
		if (val instanceof NumberValue numVal) {
			var num = numVal.getDouble();
			var res = new JSONMap().set("type", "number");

			if (Double.POSITIVE_INFINITY == num) res.set("unserializableValue", "Infinity");
			else if (Double.NEGATIVE_INFINITY == num) res.set("unserializableValue", "-Infinity");
			else if (Double.doubleToRawLongBits(num) == Double.doubleToRawLongBits(-0d)) res.set("unserializableValue", "-0");
			else if (Double.doubleToRawLongBits(num) == Double.doubleToRawLongBits(0d)) res.set("unserializableValue", "0");
			else if (Double.isNaN(num)) res.set("unserializableValue", "NaN");
			else res.set("value", num);

			return res;
		}

		throw new IllegalArgumentException("Unexpected JS object.");
	}
	private JSONMap serializeObj(Environment env, Value val) {
		return serializeObj(env, val, false);
	}
	private void addObjectGroup(String name, Value val) {
		var id = objectToId.getOrDefault(val, -1);
		if (id < 0) return;

		var ref = idToObject.get(id);

		if (objectGroups.containsKey(name)) objectGroups.get(name).add(ref);
		else objectGroups.put(name, new ArrayList<>(Arrays.asList(ref)));

		ref.heldGroups.add(name);
	}
	private void releaseGroup(String name) {
		var objs = objectGroups.remove(name);

		if (objs != null) for (var obj : objs) {
			if (obj.heldGroups.remove(name) && obj.shouldRelease()) {
				var id = objectToId.remove(obj.obj);
				if (id != null) idToObject.remove(id);
			}
		}
	}
	private Value deserializeArgument(JSONMap val) {
		if (val.isString("objectId")) return idToObject.get(Integer.parseInt(val.string("objectId"))).obj;
		else if (val.isString("unserializableValue")) switch (val.string("unserializableValue")) {
			case "NaN": return NumberValue.NAN;
			case "-Infinity": return NumberValue.of(Double.NEGATIVE_INFINITY);
			case "Infinity": return NumberValue.of(Double.POSITIVE_INFINITY);
			case "-0": return NumberValue.of(-0.);
		}
		var res = val.get("value");
		if (res == null) return null;
		else return JSONConverter.toJs(res);
	}

	private JSONMap serializeException(Environment env, EngineException err) {
		String text = null;

		try {
			text = err.value.toString(env);
		}
		catch (EngineException e) {
			text = "[error while stringifying]";
		}

		var res = new JSONMap()
			.set("exceptionId", nextId())
			.set("exception", serializeObj(env, err.value))
			.set("text", text);

		return res;
	}

	private void resume(State state) {
		try {
			this.state = state;
			ws.send(new V8Event("Debugger.resumed", new JSONMap()));
			synchronized (updateNotifier) {
				updateNotifier.notifyAll();
			}
		}
		catch (IOException e) {
			ws.close();
			close();
		}
	}
	private void pauseDebug(Environment env, Breakpoint bp) {
		try {
			state = State.PAUSED_NORMAL;
			var map = new JSONMap()
				.set("callFrames", serializeFrames(env, null))
				.set("reason", "debugCommand");

			if (bp != null) map.set("hitBreakpoints", new JSONList().add(bp.id + ""));
			ws.send(new V8Event("Debugger.paused", map));
		}
		catch (IOException e) {
			ws.close();
			close();
		}
	}
	private void pauseException(Environment env, EngineException exception) {
		try {
			state = State.PAUSED_EXCEPTION;
			var map = new JSONMap()
				.set("callFrames", serializeFrames(env, null))
				.set("data", serializeObj(env, exception.value))
				.set("reason", "exception");

			ws.send(new V8Event("Debugger.paused", map));
		}
		catch (IOException e) {
			ws.close();
			close();
		}
	}
	private void pauseReturn(Environment env, Value value) {
		try {
			state = State.PAUSED_NORMAL;
			var map = new JSONMap()
				.set("callFrames", serializeFrames(env, value))
				.set("reason", "debugCommand");

			ws.send(new V8Event("Debugger.paused", map));
		}
		catch (IOException e) {
			ws.close();
			close();
		}
	}

	private void sendSource(DebugSource src) {
		try {
			ws.send(new V8Event("Debugger.scriptParsed", new JSONMap()
				.set("scriptId", src.id + "")
				.set("hash", src.source.hashCode())
				.set("url", src.filename + "")
			));
		}
		catch (IOException e) {
			ws.close();
			close();
		}
	}

	// private Environment sanitizeEnvironment(Environment env) {
	// 	var res = env.child();

	// 	res.remove(EventLoop.KEY);
	// 	res.remove(DebugContext.KEY);
	// 	res.add(DebugContext.IGNORE);

	// 	return res;
	// }

	private RunResult run(DebugFrame codeFrame, String code) {
		if (codeFrame == null) return new RunResult(null, null, EngineException.ofError("Invalid code frame!"));
		var engine = new Engine();
		var env = codeFrame.frame.env.child();

		env.remove(DebugContext.KEY);
		env.remove(EventLoop.KEY);
		env.remove(Value.GLOBAL);
		env.add(Compiler.KEY, Compiler.DEFAULT);
		env.add(EventLoop.KEY, engine);
		env.add(Value.GLOBAL, codeFrame.variables);

		var awaiter = engine.pushMsg(false, env, new Filename("jscript", "eval"), code, codeFrame.frame.self, codeFrame.frame.args);

		try {
			engine.run(true);
			try {
				return new RunResult(env, awaiter.get(), null);
			}
			catch (ExecutionException e) {
				if (e.getCause() instanceof RuntimeException runtime) throw runtime;
				else throw new RuntimeException(e.getCause());
			}
		}
		catch (EngineException e) { return new RunResult(env, null, e); }
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return new RunResult(env, null, EngineException.ofError("Thread interrupted!"));
		}
	}

	private ObjectValue vscodeAutoSuggest(Environment env, Value target, String query, boolean variable) {
		var res = new ArrayValue();
		var passed = new HashSet<String>();
		var tildas = "~";
		if (target == null) target = Value.global(env);

		for (var proto = target; proto != null && proto != Value.NULL; proto = proto.getPrototype(env)) {
			for (var key : proto.getMembers(env, true, true)) {
				if (passed.contains(key)) continue;
				passed.add(key);

				var member = proto.getOwnMember(env, key);
				var val = member instanceof FieldMember field ? field.get(env, target) : Value.UNDEFINED;
				var desc = new ObjectValue();
				var sortText = "";

				if (key.startsWith(query)) sortText += "0@";
				else if (key.toLowerCase().startsWith(query.toLowerCase())) sortText += "1@";
				else if (key.contains(query)) sortText += "2@";
				else if (key.toLowerCase().contains(query.toLowerCase())) sortText += "3@";
				else sortText += "4@";

				sortText += tildas + key;

				desc.defineOwnField(env, "label", StringValue.of(key));
				desc.defineOwnField(env, "sortText", StringValue.of(sortText));

				if (val instanceof FunctionValue) {
					if (key.equals("constructor")) desc.defineOwnField(env, "type", StringValue.of("name"));
					else desc.defineOwnField(env, "type", StringValue.of(variable ? "function" : "method"));
				}
				else desc.defineOwnField(env, "type", StringValue.of(variable ? "variable" : "property"));

				switch (val.type().toString()) {
					case "number":
					case "boolean":
						desc.defineOwnField(env, "detail", StringValue.of(val.toString(env)));
						break;
					case "object":
						if (val == Value.NULL) desc.defineOwnField(env, "detail", StringValue.of("null"));
						else try {
							desc.defineOwnField(env, "detail", target.getMemberPath(env, "constructor", "name"));
						}
						catch (IllegalArgumentException e) {
							desc.defineOwnField(env, "detail", StringValue.of("object"));
						}
						break;
					case "function": {
						var type = "fn(";
						for (var i = 0; i < ((FunctionValue)val).length; i++) {
							if (i != 0) type += ",";
							type += "?";
						}
						type += ")";
						desc.defineOwnField(env, "detail", StringValue.of(type));
						break;
					}
					default:
						desc.defineOwnField(env, "type", val.type());
						break;
				}

				res.set(env, res.size(), desc);
			}

			tildas += "~";
			variable = true;
		}

		var resObj = new ObjectValue();
		resObj.defineOwnField(env, "result", res);
		resObj.defineOwnField(env, "isArray", BoolValue.of(target instanceof ArrayValue));
		return resObj;
	}

	@Override public synchronized void enable(V8Message msg) throws IOException {
		enabled = true;
		ws.send(msg.respond());

		for (var el : pendingSources) sendSource(el);
		pendingSources.clear();

		synchronized (updateNotifier) {
			updateNotifier.notifyAll();
		}
	}
	@Override public synchronized void disable(V8Message msg) throws IOException {
		close();
		ws.send(msg.respond());
	}
	@Override public synchronized void close() {
		if (state != State.RESUMED) {
			try {
				resume(State.RESUMED);
			}
			catch (Throwable e) {
				// don't care, didn't ask
			}
		}

		enabled = false;
		execptionType = CatchType.NONE;
		state = State.RESUMED;

		mappings.clear();
		bpLocs.clear();

		idToBreakpoint.clear();

		filenameToId.clear();
		idToSource.clear();
		pendingSources.clear();

		idToFrame.clear();
		codeFrameToFrame.clear();

		idToObject.clear();
		objectToId.clear();
		objectGroups.clear();

		pendingPause = false;

		stepOutFrame = currFrame = null;
		stepOutPtr = 0;

		for (var ctx : contexts.keySet()) ctx.detachDebugger(this);
		contexts.clear();

		synchronized (updateNotifier) {
			updateNotifier.notifyAll();
		}
	}

	@Override public synchronized void getScriptSource(V8Message msg) throws IOException {
		int id = Integer.parseInt(msg.params.string("scriptId"));
		ws.send(msg.respond(new JSONMap().set("scriptSource", idToSource.get(id).source)));
	}
	@Override public synchronized void getPossibleBreakpoints(V8Message msg) throws IOException {
		var start = deserializeLocation(msg.params.get("start"));
		var end = msg.params.isMap("end") ? deserializeLocation(msg.params.get("end")) : null;
		var res = new JSONList();

		for (var el : mappings.values()) {
			for (var bp : el.breakpoints(start, end)) {
				res.add(serializeLocation(bp));
			}
		}

		ws.send(msg.respond(new JSONMap().set("locations", res)));
	}

	@Override public synchronized void pause(V8Message msg) throws IOException {
		pendingPause = true;
		ws.send(msg.respond());
	}
	@Override public synchronized void resume(V8Message msg) throws IOException {
		resume(State.RESUMED);
		ws.send(msg.respond(new JSONMap()));
	}

	@Override public synchronized void setBreakpointByUrl(V8Message msg) throws IOException {
		var line = (int)msg.params.number("lineNumber");
		var col = (int)msg.params.number("columnNumber", 0);
		var cond = msg.params.string("condition", "").trim();

		if (cond.equals("")) cond = null;
		if (cond != null) cond  = "(" + cond + ")";

		Pattern regex;

		if (msg.params.isString("url")) regex = Pattern.compile(Pattern.quote(msg.params.string("url")));
		else if (msg.params.isString("urlRegex")) regex = Pattern.compile(msg.params.string("urlRegex"));
		else {
			ws.send(msg.respond(new JSONMap()
				.set("breakpointId", "john-doe")
				.set("locations", new JSONList())
			));
			return;
		}

		var bpt = new Breakpoint(nextId(), regex, line, col, cond);
		idToBreakpoint.put(bpt.id, bpt);


		for (var el : mappings.entrySet()) {
			bpt.addFunc(el.getKey(), el.getValue());
		}

		var locs = new JSONList();

		for (var loc : bpt.resolvedLocations.values()) {
			locs.add(serializeLocation(loc));
		}

		ws.send(msg.respond(new JSONMap()
			.set("breakpointId", bpt.id + "")
			.set("locations", locs)
		));
	}
	@Override public synchronized void removeBreakpoint(V8Message msg) throws IOException {
		var id = Integer.parseInt(msg.params.string("breakpointId"));

		idToBreakpoint.remove(id);
		updateBreakpoints();
		ws.send(msg.respond());
	}
	@Override public synchronized void continueToLocation(V8Message msg) throws IOException {
		// TODO: Figure out if we need this

		// var loc = correctLocation(deserializeLocation(msg.params.get("location")));

		// tmpBreakpts.add(loc);

		// resume(State.RESUMED);
		// ws.send(msg.respond());
	}

	@Override public synchronized void setPauseOnExceptions(V8Message msg) throws IOException {
		switch (msg.params.string("state")) {
			case "none": execptionType = CatchType.NONE; break;
			case "all": execptionType = CatchType.ALL; break;
			case "uncaught": execptionType = CatchType.UNCAUGHT; break;
			default:
				ws.send(new V8Error("Invalid exception pause type."));
				return;
		}

		ws.send(msg.respond());
	}

	@Override public synchronized void stepInto(V8Message msg) throws IOException {
		if (state == State.RESUMED) ws.send(new V8Error("Debugger is resumed."));
		else {
			stepOutFrame = currFrame;
			stepOutPtr = currFrame.frame.codePtr;
			resume(State.STEPPING_IN);
			ws.send(msg.respond());
		}
	}
	@Override public synchronized void stepOut(V8Message msg) throws IOException {
		if (state == State.RESUMED) ws.send(new V8Error("Debugger is resumed."));
		else {
			stepOutFrame = currFrame;
			stepOutPtr = currFrame.frame.codePtr;
			resume(State.STEPPING_OUT);
			ws.send(msg.respond());
		}
	}
	@Override public synchronized void stepOver(V8Message msg) throws IOException {
		if (state == State.RESUMED) ws.send(new V8Error("Debugger is resumed."));
		else {
			stepOutFrame = currFrame;
			stepOutPtr = currFrame.frame.codePtr;
			resume(State.STEPPING_OVER);
			ws.send(msg.respond());
		}
	}

	@Override public synchronized void evaluateOnCallFrame(V8Message msg) throws IOException {
		var cfId = Integer.parseInt(msg.params.string("callFrameId"));
		var expr = msg.params.string("expression");
		var group = msg.params.string("objectGroup", null);

		var cf = idToFrame.get(cfId);
		var res = run(cf, expr);

		if (group != null) addObjectGroup(group, res.result);

		if (res.error != null) ws.send(msg.respond(new JSONMap().set("exceptionDetails", serializeException(res.ext, res.error))));
		else ws.send(msg.respond(new JSONMap().set("result", serializeObj(res.ext, res.result))));
	}

	@Override public synchronized void releaseObjectGroup(V8Message msg) throws IOException {
		var group = msg.params.string("objectGroup");
		releaseGroup(group);
		ws.send(msg.respond());
	}
	@Override public synchronized void releaseObject(V8Message msg) throws IOException {
		var id = Integer.parseInt(msg.params.string("objectId"));
		var ref = idToObject.get(id);
		ref.held = false;

		if (ref.shouldRelease()) {
			objectToId.remove(ref.obj);
			idToObject.remove(id);
		}

		ws.send(msg.respond());
	}
	@Override public synchronized void getProperties(V8Message msg) throws IOException {
		var ref = idToObject.get(Integer.parseInt(msg.params.string("objectId")));
		var obj = ref.obj;
		var env = ref.env;
		var res = new JSONList();
		var own = true;

		if (obj != emptyObject && obj != null) {
			while (obj != null) {
				for (var key : obj.getMembers(env, true, false)) {
					var propDesc = new JSONMap();

					var member = obj.getOwnMember(env, key);

					if (member instanceof PropertyMember prop) {
						propDesc.set("name", key);
						if (prop.getter != null) propDesc.set("get", serializeObj(env, prop.getter));
						if (prop.setter != null) propDesc.set("set", serializeObj(env, prop.setter));
						propDesc.set("enumerable", member.enumerable());
						propDesc.set("configurable", member.configurable());
						propDesc.set("isOwn", true);
						res.add(propDesc);
					}
					else {
						propDesc.set("name", key);
						propDesc.set("value", serializeObj(env, member.get(env, obj)));
						propDesc.set("writable", member instanceof FieldMember field ? field.writable() : false);
						propDesc.set("enumerable", member.enumerable());
						propDesc.set("configurable", member.configurable());
						propDesc.set("isOwn", own);
						res.add(propDesc);
					}
				}

				var proto = obj.getPrototype(env);

				if (own) {
					var protoDesc = new JSONMap();
					protoDesc.set("name", "[[Prototype]]");
					protoDesc.set("value", serializeObj(env, proto == null ? Value.NULL : proto));
					protoDesc.set("writable", false);
					protoDesc.set("enumerable", false);
					protoDesc.set("configurable", false);
					protoDesc.set("isOwn", own);
					res.add(protoDesc);
				}

				obj = proto;
				own = false;
				break;
			}
		}

		ws.send(msg.respond(new JSONMap().set("result", res)));
	}
	@Override public synchronized void callFunctionOn(V8Message msg) throws IOException {
		var src = msg.params.string("functionDeclaration");
		var args = msg.params
			.list("arguments", new JSONList())
			.stream()
			.map(v -> v.map())
			.map(this::deserializeArgument)
			.collect(Collectors.toList());
		var byValue = msg.params.bool("returnByValue", false);

		var selfRef = idToObject.get(Integer.parseInt(msg.params.string("objectId")));
		var self = selfRef.obj;
		var env = selfRef.env;

		while (true) {
			var start = src.lastIndexOf("//# sourceURL=");
			if (start < 0) break;
			var end = src.indexOf("\n", start);
			if (end < 0) src = src.substring(0, start);
			else src = src.substring(0, start) + src.substring(end + 1);
		}

		try {
			Value res = null;
			if (compare(src, VSCODE_EMPTY)) res = emptyObject;
			else if (compare(src, VSCODE_SELF)) res = self;
			else if (compare(src, CHROME_GET_PROP_FUNC)) {
				res = self;
				for (var el : JSON.parse(null, args.get(0).toString(env)).list()) res = res.getMember(env, JSONConverter.toJs(el));
			}
			else if (compare(src, CHROME_GET_PROP_FUNC_2)) {
				res = args.get(0).apply(env, self);
			}
			else if (compare(src, VSCODE_CALL)) {
				var func = (FunctionValue)(args.size() < 1 ? null : args.get(0));
				ws.send(msg.respond(new JSONMap().set("result", serializeObj(env, func.apply(env, self)))));
			}
			else if (compare(src, VSCODE_AUTOCOMPLETE)) {
				var target = args.get(0);
				if (target == null) target = self;
				res = vscodeAutoSuggest(env, target, args.get(1).toString(env), args.get(2).toBoolean());
			}
			else {
				ws.send(new V8Error("Please use well-known functions with callFunctionOn"));
				return;
			}
			ws.send(msg.respond(new JSONMap().set("result", serializeObj(env, res, byValue))));
		}
		catch (EngineException e) { ws.send(msg.respond(new JSONMap().set("exceptionDetails", serializeException(env, e)))); }
	}

	@Override public synchronized void runtimeEnable(V8Message msg) throws IOException {
		ws.send(msg.respond());
	}

	@Override public synchronized void onSourceLoad(Filename filename, String source) {
		int id = nextId();
		var src = new DebugSource(id, filename, source);

		idToSource.put(id, src);
		filenameToId.put(filename, id);

		if (!enabled) pendingSources.add(src);
		else sendSource(src);
	}
	@Override public synchronized void onFunctionLoad(FunctionBody body, FunctionMap map) {
		for (var bpt : idToBreakpoint.values()) {
			bpt.addFunc(body, map);
		}
		mappings.put(body, map);
	}
	@Override public boolean onInstruction(Environment env, Frame cf, Instruction instruction, Value returnVal, EngineException error, boolean caught) {
		if (!enabled) return false;

		boolean isBreakpointable;
		Location loc;
		DebugFrame frame;
		BreakpointType bptType;

		synchronized (this) {
			frame = getFrame(cf);

			var map = DebugContext.get(env).getMap(frame.frame.function);

			frame.updateLoc(map.toLocation(frame.frame.codePtr));
			loc = frame.location;
			bptType = map.getBreakpoint(frame.frame.codePtr);
			isBreakpointable = loc != null && (bptType.shouldStepIn());

			if (error != null && (execptionType == CatchType.ALL || execptionType == CatchType.UNCAUGHT && !caught)) {
				pauseException(env, error);
			}
			else if (
				loc != null &&
				(state == State.STEPPING_IN || state == State.STEPPING_OVER) &&
				returnVal != null && stepOutFrame == frame
			) {
				pauseReturn(env, returnVal);
			}
			else if (isBreakpointable && bpLocs.containsKey(loc)) {
				for (var bp : bpLocs.get(loc)) {
					var ok = bp.condition == null ? true : run(currFrame, bp.condition).result.toBoolean();
					if (ok) pauseDebug(env, bp);
				}
			}
			// else if (isBreakpointable && tmpBreakpts.remove(loc)) pauseDebug(ctx, null);
			else if (isBreakpointable && pendingPause) {
				pauseDebug(env, null);
				pendingPause = false;
			}
			else if (
				instruction != null &&
				instruction.type == Type.NOP &&
				instruction.params.length == 1 &&
				instruction.get(0).equals("debug")
			) pauseDebug(env, null);
		}

		while (enabled) {
			synchronized (this) {
				switch (state) {
					case PAUSED_EXCEPTION:
					case PAUSED_NORMAL: break;

					case STEPPING_OUT:
					case RESUMED: return false;

					case STEPPING_IN:
					case STEPPING_OVER:
						if (stepOutFrame.frame == frame.frame) {
							if (returnVal != null || error != null) {
								state = State.STEPPING_OUT;
								continue;
							}
							else if (stepOutPtr != frame.frame.codePtr) {

								if (state == State.STEPPING_IN && bptType.shouldStepIn()) {
									pauseDebug(env, null);
									break;
								}
								else if (state == State.STEPPING_OVER && bptType.shouldStepOver()) {
									pauseDebug(env, null);
									break;
								}
							}
						}
						return false;
				}
			}

			try {
				synchronized (updateNotifier) {
					updateNotifier.wait();
				}
			}
			catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		}

		return false;
	}
	@Override public synchronized void onFramePush(Environment env, Frame frame) {
		var prevFrame = currFrame;
		updateFrames(env, 0);

		if (stepOutFrame != null && stepOutFrame.frame == prevFrame.frame && state == State.STEPPING_IN) {
			stepOutFrame = currFrame;
		}
	}
	@Override public synchronized void onFramePop(Environment env, Frame frame) {
		updateFrames(env, 1);

		try { idToFrame.remove(codeFrameToFrame.remove(frame).id); }
		catch (NullPointerException e) { }

		if (Frame.get(env).size() == 0) {
			if (state == State.PAUSED_EXCEPTION || state == State.PAUSED_NORMAL) resume(State.RESUMED);
		}
		else if (stepOutFrame != null && stepOutFrame.frame == frame && state == State.STEPPING_OUT) {
			state = State.STEPPING_IN;
			stepOutFrame = currFrame;
		}
	}

	public SimpleDebugger attach(DebugContext ctx) {
		ctx.attachDebugger(this);
		contexts.put(ctx, ctx);
		return this;
	}

	public SimpleDebugger(WebSocket ws) {
		this.ws = ws;
	}
}
