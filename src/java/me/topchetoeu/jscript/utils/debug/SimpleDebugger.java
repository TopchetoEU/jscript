package me.topchetoeu.jscript.utils.debug;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.common.FunctionBody;
import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.Instruction.Type;
import me.topchetoeu.jscript.common.events.Notifier;
import me.topchetoeu.jscript.common.json.JSON;
import me.topchetoeu.jscript.common.json.JSONElement;
import me.topchetoeu.jscript.common.json.JSONList;
import me.topchetoeu.jscript.common.json.JSONMap;
import me.topchetoeu.jscript.common.mapping.FunctionMap;
import me.topchetoeu.jscript.compilation.parsing.Parsing;
import me.topchetoeu.jscript.runtime.Context;
import me.topchetoeu.jscript.runtime.Engine;
import me.topchetoeu.jscript.runtime.Environment;
import me.topchetoeu.jscript.runtime.EventLoop;
import me.topchetoeu.jscript.runtime.Frame;
import me.topchetoeu.jscript.runtime.debug.DebugContext;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.scope.GlobalScope;
import me.topchetoeu.jscript.runtime.values.ArrayValue;
import me.topchetoeu.jscript.runtime.values.FunctionValue;
import me.topchetoeu.jscript.runtime.values.ObjectValue;
import me.topchetoeu.jscript.runtime.values.Symbol;
import me.topchetoeu.jscript.runtime.values.Values;

// very simple indeed
public class SimpleDebugger implements Debugger {
    public static final Set<String> VSCODE_EMPTY = Set.of(
        "function(...runtimeArgs){\n    let t = 1024; let e = null;\n    if(e)try{let r=\"<<default preview>>\",i=e.call(this,r);if(i!==r)return String(i)}catch(r){return`<<indescribable>>${JSON.stringify([String(r),\"object\"])}`}if(typeof this==\"object\"&&this){let r;for(let i of[Symbol.for(\"debug.description\"),Symbol.for(\"nodejs.util.inspect.custom\")])try{r=this[i]();break}catch{}if(!r&&!String(this.toString).includes(\"[native code]\")&&(r=String(this)),r&&!r.startsWith(\"[object \"))return r.length>=t?r.slice(0,t)+\"\\u2026\":r}\n  ;\n\n}",
        "function(...runtimeArgs){\n    let t = 1024; let e = null;\n    let r={},i=\"<<default preview>>\";if(typeof this!=\"object\"||!this)return r;for(let[n,s]of Object.entries(this)){if(e)try{let o=e.call(s,i);if(o!==i){r[n]=String(o);continue}}catch(o){r[n]=`<<indescribable>>${JSON.stringify([String(o),n])}`;continue}if(typeof s==\"object\"&&s){let o;for(let a of runtimeArgs[0])try{o=s[a]();break}catch{}!o&&!String(s.toString).includes(\"[native code]\")&&(o=String(s)),o&&!o.startsWith(\"[object \")&&(r[n]=o.length>=t?o.slice(0,t)+\"\\u2026\":o)}}return r\n  ;\n\n}",
        "function(){let t={__proto__:this.__proto__\n},e=Object.getOwnPropertyNames(this);for(let r=0;r<e.length;++r){let i=e[r],n=i>>>0;if(String(n>>>0)===i&&n>>>0!==4294967295)continue;let s=Object.getOwnPropertyDescriptor(this,i);s&&Object.defineProperty(t,i,s)}return t}",
        "function(){return[Symbol.for(\"debug.description\"),Symbol.for(\"nodejs.util.inspect.custom\")]\n}"
    );
    public static final Set<String> VSCODE_SELF = Set.of(
        "function(t,e){let r={\n},i=t===-1?0:t,n=e===-1?this.length:t+e;for(let s=i;s<n&&s<this.length;++s){let o=Object.getOwnPropertyDescriptor(this,s);o&&Object.defineProperty(r,s,o)}return r}"
    );

    public static final String CHROME_GET_PROP_FUNC = "function s(e){let t=this;const n=JSON.parse(e);for(let e=0,i=n.length;e<i;++e)t=t[n[e]];return t}";
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
        public final WeakHashMap<FunctionBody, Set<Location>> resolvedLocations = new WeakHashMap<>();

        public Breakpoint(int id, Pattern pattern, int line, int start, String condition) {
            this.id = id;
            this.condition = condition;
            this.pattern = pattern;
            this.line = line;
            this.start = start;

            if (condition != null && condition.trim().equals("")) condition = null;
        }

        // TODO: Figure out how to unload a breakpoint
        public void addFunc(FunctionBody body, FunctionMap map) {
            try {
                for (var loc : map.correctBreakpoint(pattern, line, start)) {
                    if (!resolvedLocations.containsKey(body)) resolvedLocations.put(body, new HashSet<>());
                    var set = resolvedLocations.get(body);
                    set.add(loc);

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
        public Frame frame;
        public int id;
        public ObjectValue local, capture, global, valstack;
        public JSONMap serialized;
        public Location location;

        public void updateLoc(Location loc) {
            if (loc == null) return;
            this.location = loc;
        }

        public DebugFrame(Frame frame, int id) {
            this.frame = frame;
            this.id = id;

            this.global = frame.function.environment.global.obj;
            this.local = frame.getLocalScope();
            this.capture = frame.getCaptureScope();
            Values.makePrototypeChain(frame.ctx, global, capture, local);
            this.valstack = frame.getValStackScope();

            this.serialized = new JSONMap()
                .set("callFrameId", id + "")
                .set("functionName", frame.function.name)
                .set("scopeChain", new JSONList()
                    .add(new JSONMap()
                        .set("type", "local")
                        .set("name", "Local Scope")
                        .set("object", serializeObj(frame.ctx, local))
                    )
                    .add(new JSONMap()
                        .set("type", "closure")
                        .set("name", "Closure")
                        .set("object", serializeObj(frame.ctx, capture))
                    )
                    .add(new JSONMap()
                        .set("type", "global")
                        .set("name", "Global Scope")
                        .set("object", serializeObj(frame.ctx, global))
                    )
                    .add(new JSONMap()
                        .set("type", "other")
                        .set("name", "Value Stack")
                        .set("object", serializeObj(frame.ctx, valstack))
                    )
                );
        }
    }
    private class ObjRef {
        public final ObjectValue obj;
        public final Context ctx;
        public final HashSet<String> heldGroups = new HashSet<>();
        public boolean held = true;

        public boolean shouldRelease() {
            return !held && heldGroups.size() == 0;
        }

        public ObjRef(Context ctx, ObjectValue obj) {
            this.ctx = ctx;
            this.obj = obj;
        }
    }

    private static class RunResult {
        public final Context ctx;
        public final Object result;
        public final EngineException error;

        public RunResult(Context ctx, Object result, EngineException error) {
            this.ctx = ctx;
            this.result = result;
            this.error = error;
        }
    }

    public boolean enabled = true;
    public CatchType execptionType = CatchType.NONE;
    public State state = State.RESUMED;

    public final WebSocket ws;

    private ObjectValue emptyObject = new ObjectValue();

    private WeakHashMap<FunctionBody, FunctionMap> mappings = new WeakHashMap<>();
    private WeakHashMap<FunctionBody, HashMap<Location, Breakpoint>> bpLocs = new WeakHashMap<>();

    private HashMap<Integer, Breakpoint> idToBreakpoint = new HashMap<>();

    private HashMap<Filename, Integer> filenameToId = new HashMap<>();
    private HashMap<Integer, DebugSource> idToSource = new HashMap<>();
    private ArrayList<DebugSource> pendingSources = new ArrayList<>();

    private HashMap<Integer, DebugFrame> idToFrame = new HashMap<>();
    private HashMap<Frame, DebugFrame> codeFrameToFrame = new HashMap<>();

    private HashMap<Integer, ObjRef> idToObject = new HashMap<>();
    private HashMap<ObjectValue, Integer> objectToId = new HashMap<>();
    private HashMap<String, ArrayList<ObjRef>> objectGroups = new HashMap<>();

    private Notifier updateNotifier = new Notifier();
    private boolean pendingPause = false;

    private int nextId = 0;
    private DebugFrame stepOutFrame = null, currFrame = null;
    private int stepOutPtr = 0;

    private boolean compare(String src, String target) {
        if (src.length() != target.length()) return false;
        var diff = 0;
        var all = 0;

        for (var i = 0; i < src.length(); i++) {
            var a = src.charAt(i);
            var b = target.charAt(i);
            var letter = Parsing.isLetter(a) && Parsing.isLetter(b);

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
    private synchronized void updateFrames(Context ctx) {
        var frame = ctx.frame;
        if (frame == null) return;

        currFrame = getFrame(frame);
    }
    private JSONList serializeFrames(Context ctx) {
        var res = new JSONList();

        for (var el : ctx.frames()) {
            var frame = getFrame(el);
            if (frame.location == null) continue;
            frame.serialized.set("location", serializeLocation(frame.location));
            if (frame.location != null) res.add(frame.serialized);
        }

        return res;
    }

    private void updateBreakpoints() {
        bpLocs.clear();

        for (var bp : idToBreakpoint.values()) {
            for (var el : bp.resolvedLocations.entrySet()) {
                if (!bpLocs.containsKey(el.getKey())) bpLocs.put(el.getKey(), new HashMap<>());
                var map = bpLocs.get(el.getKey());

                for (var loc : el.getValue()) {
                    map.put(loc, bp);
                }
            }
        }
    }

    private Location deserializeLocation(JSONElement el) {
        if (!el.isMap()) throw new RuntimeException("Expected location to be a map.");
        var id = Integer.parseInt(el.map().string("scriptId"));
        var line = (int)el.map().number("lineNumber") + 1;
        var column = (int)el.map().number("columnNumber") + 1;

        if (!idToSource.containsKey(id)) throw new RuntimeException(String.format("The specified source %s doesn't exist.", id));

        var res = new Location(line, column, idToSource.get(id).filename);
        return res;
    }
    private JSONMap serializeLocation(Location loc) {
        var source = filenameToId.get(loc.filename());
        return new JSONMap()
            .set("scriptId", source + "")
            .set("lineNumber", loc.line() - 1)
            .set("columnNumber", loc.start() - 1);
    }

    private Integer objectId(Context ctx, ObjectValue obj) {
        if (objectToId.containsKey(obj)) return objectToId.get(obj);
        else {
            int id = nextId();
            var ref = new ObjRef(ctx, obj);
            objectToId.put(obj, id);
            idToObject.put(id, ref);
            return id;
        }
    }
    private JSONMap serializeObj(Context ctx, Object val, boolean byValue) {
        val = Values.normalize(null, val);
        var newCtx = new Context();
        newCtx.addAll(ctx);
        newCtx.add(DebugContext.IGNORE);
        ctx = newCtx;

        if (val == Values.NULL) {
            return new JSONMap()
                .set("type", "object")
                .set("subtype", "null")
                .setNull("value")
                .set("description", "null");
        }

        if (val instanceof ObjectValue) {
            var obj = (ObjectValue)val;
            var id = objectId(ctx, obj);
            var type = "object";
            String subtype = null;
            String className = null;

            if (obj instanceof FunctionValue) type = "function";
            if (obj instanceof ArrayValue) subtype = "array";

            try { className = Values.toString(ctx, Values.getMemberPath(ctx, obj, "constructor", "name")); }
            catch (Exception e) { }

            var res = new JSONMap()
                .set("type", type)
                .set("objectId", id + "");

            if (subtype != null) res.set("subtype", subtype);
            if (className != null) {
                res.set("className", className);
            }

            if (obj instanceof ArrayValue) res.set("description", "Array(" + ((ArrayValue)obj).size() + ")");
            else if (obj instanceof FunctionValue) res.set("description", obj.toString());
            else {
                var defaultToString = false;

                try {
                    defaultToString =
                        Values.getMember(ctx, obj, "toString") ==
                        Values.getMember(ctx, ctx.get(Environment.OBJECT_PROTO), "toString");
                }
                catch (Exception e) { }

                try { res.set("description", className + (defaultToString ? "" : " { " + Values.toString(ctx, obj) + " }")); }
                catch (EngineException e) { res.set("description", className); }
            }


            if (byValue) try { res.put("value", JSON.fromJs(ctx, obj)); }
            catch (Exception e) { }

            return res;
        }

        if (val == null) return new JSONMap().set("type", "undefined");
        if (val instanceof String) return new JSONMap().set("type", "string").set("value", (String)val);
        if (val instanceof Boolean) return new JSONMap().set("type", "boolean").set("value", (Boolean)val);
        if (val instanceof Symbol) return new JSONMap().set("type", "symbol").set("description", val.toString());
        if (val instanceof Number) {
            var num = (double)(Number)val;
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
    private JSONMap serializeObj(Context ctx, Object val) {
        return serializeObj(ctx, val, false);
    }
    private void addObjectGroup(String name, Object val) {
        if (val instanceof ObjectValue) {
            var obj = (ObjectValue)val;
            var id = objectToId.getOrDefault(obj, -1);
            if (id < 0) return;

            var ref = idToObject.get(id);

            if (objectGroups.containsKey(name)) objectGroups.get(name).add(ref);
            else objectGroups.put(name, new ArrayList<>(List.of(ref)));

            ref.heldGroups.add(name);
        }
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
    private Object deserializeArgument(JSONMap val) {
        if (val.isString("objectId")) return idToObject.get(Integer.parseInt(val.string("objectId"))).obj;
        else if (val.isString("unserializableValue")) switch (val.string("unserializableValue")) {
            case "NaN": return Double.NaN;
            case "-Infinity": return Double.NEGATIVE_INFINITY;
            case "Infinity": return Double.POSITIVE_INFINITY;
            case "-0": return -0.;
        }
        var res = val.get("value");
        if (res == null) return null;
        else return JSON.toJs(res);
    }

    private JSONMap serializeException(Context ctx, EngineException err) {
        String text = null;

        try {
            text = Values.toString(ctx, err.value);
        }
        catch (EngineException e) {
            text = "[error while stringifying]";
        }

        var res = new JSONMap()
            .set("exceptionId", nextId())
            .set("exception", serializeObj(ctx, err.value))
            .set("text", text);

        return res;
    }

    private void resume(State state) {
        try {
            this.state = state;
            ws.send(new V8Event("Debugger.resumed", new JSONMap()));
            updateNotifier.next();
        }
        catch (IOException e) {
            ws.close();
            close();
        }
    }
    private void pauseDebug(Context ctx, Breakpoint bp) {
        try {
            state = State.PAUSED_NORMAL;
            var map = new JSONMap()
                .set("callFrames", serializeFrames(ctx))
                .set("reason", "debugCommand");

            if (bp != null) map.set("hitBreakpoints", new JSONList().add(bp.id + ""));
            ws.send(new V8Event("Debugger.paused", map));
        }
        catch (IOException e) {
            ws.close();
            close();
        }
    }
    private void pauseException(Context ctx) {
        try {
            state = State.PAUSED_EXCEPTION;
            var map = new JSONMap()
                .set("callFrames", serializeFrames(ctx))
                .set("reason", "exception");

            ws.send(new V8Event("Debugger.paused", map));
        }
        catch (IOException e) {
            ws.close();
            close();
        }
    }

    private void sendSource(DebugSource src){
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

    private RunResult run(DebugFrame codeFrame, String code) {
        if (codeFrame == null) return new RunResult(null, code, new EngineException("Invalid code frame!"));
        var engine = new Engine();
        var env = codeFrame.frame.function.environment.copy();

        env.global = new GlobalScope(codeFrame.local);
        env.remove(EventLoop.KEY);
        env.remove(DebugContext.KEY);
        env.add(EventLoop.KEY, engine);

        var ctx = new Context(env);
        var awaiter = engine.pushMsg(false, ctx.environment, new Filename("jscript", "eval"), code, codeFrame.frame.thisArg, codeFrame.frame.args);

        engine.run(true);

        try { return new RunResult(ctx, awaiter.await(), null); }
        catch (EngineException e) { return new RunResult(ctx, null, e); }
    }

    private ObjectValue vscodeAutoSuggest(Context ctx, Object target, String query, boolean variable) {
        var res = new ArrayValue();
        var passed = new HashSet<String>();
        var tildas = "~";
        if (target == null) target = ctx.environment.global;

        for (var proto = target; proto != null && proto != Values.NULL; proto = Values.getPrototype(ctx, proto)) {
            for (var el : Values.getMembers(ctx, proto, true, true)) {
                var strKey = Values.toString(ctx, el);
                if (passed.contains(strKey)) continue;
                passed.add(strKey);

                var val = Values.getMember(ctx, Values.getMemberDescriptor(ctx, proto, el), "value");
                var desc = new ObjectValue();
                var sortText = "";
                if (strKey.startsWith(query)) sortText += "0@";
                else if (strKey.toLowerCase().startsWith(query.toLowerCase())) sortText += "1@";
                else if (strKey.contains(query)) sortText += "2@";
                else if (strKey.toLowerCase().contains(query.toLowerCase())) sortText += "3@";
                else sortText += "4@";
                sortText += tildas + strKey;

                desc.defineProperty(ctx, "label", strKey);
                desc.defineProperty(ctx, "sortText", sortText);

                if (val instanceof FunctionValue) {
                    if (strKey.equals("constructor")) desc.defineProperty(ctx, "type", "name");
                    else desc.defineProperty(ctx, "type", variable ? "function" : "method");
                }
                else desc.defineProperty(ctx, "type", variable ? "variable" : "property");

                switch (Values.type(val)) {
                    case "number":
                    case "boolean":
                        desc.defineProperty(ctx, "detail", Values.toString(ctx, val));
                        break;
                    case "object":
                        if (val == Values.NULL) desc.defineProperty(ctx, "detail", "null");
                        else try {
                            desc.defineProperty(ctx, "detail", Values.getMemberPath(ctx, target, "constructor", "name"));
                        }
                        catch (IllegalArgumentException e) {
                            desc.defineProperty(ctx, "detail", "object");
                        }
                        break;
                    case "function": {
                        var type = "fn(";
                        for (var i = 0; i < ((FunctionValue)val).length; i++) {
                            if (i != 0) type += ",";
                            type += "?";
                        }
                        type += ")";
                        desc.defineProperty(ctx, "detail", type);
                        break;
                    }
                    default:
                        desc.defineProperty(ctx, "type", Values.type(val));
                        break;
                }
            
                res.set(ctx, res.size(), desc);
            }

            tildas += "~";
            variable = true;
        }

        var resObj = new ObjectValue();
        resObj.defineProperty(ctx, "result", res);
        resObj.defineProperty(ctx, "isArray", target instanceof ArrayValue);
        return resObj;
    }

    @Override public synchronized void enable(V8Message msg) throws IOException {
        enabled = true;
        ws.send(msg.respond());

        for (var el : pendingSources) sendSource(el);
        pendingSources.clear();

        updateNotifier.next();
    }
    @Override public synchronized void disable(V8Message msg) throws IOException {
        close();
        ws.send(msg.respond());
    }
    @Override public synchronized void close() {
        enabled = false;
        execptionType = CatchType.NONE;
        state = State.RESUMED;

        // idToBptCand.clear();

        idToBreakpoint.clear();
        // locToBreakpoint.clear();
        // tmpBreakpts.clear();

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

        updateNotifier.next();
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
        var line = (int)msg.params.number("lineNumber") + 1;
        var col = (int)msg.params.number("columnNumber", 0) + 1;
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

        var locs = new JSONList();

        for (var el : mappings.entrySet()) {
            bpt.addFunc(el.getKey(), el.getValue());
        }

        for (var el : bpt.resolvedLocations.values()) {
            for (var loc : el) {
                locs.add(serializeLocation(loc));
            }
        }

        ws.send(msg.respond(new JSONMap()
            .set("breakpointId", bpt.id + "")
            .set("locations", locs)
        ));
    }
    @Override public synchronized void removeBreakpoint(V8Message msg) throws IOException {
        var id = Integer.parseInt(msg.params.string("breakpointId"));

        idToBreakpoint.remove(id);
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

        if (res.error != null) ws.send(msg.respond(new JSONMap().set("exceptionDetails", serializeException(res.ctx, res.error))));
        else ws.send(msg.respond(new JSONMap().set("result", serializeObj(res.ctx, res.result))));
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

        var res = new JSONList();
        var ctx = ref.ctx;

        if (obj != emptyObject && obj != null) {
            for (var key : obj.keys(true)) {
                var propDesc = new JSONMap();

                if (obj.properties.containsKey(key)) {
                    var prop = obj.properties.get(key);

                    propDesc.set("name", Values.toString(ctx, key));
                    if (prop.getter != null) propDesc.set("get", serializeObj(ctx, prop.getter));
                    if (prop.setter != null) propDesc.set("set", serializeObj(ctx, prop.setter));
                    propDesc.set("enumerable", obj.memberEnumerable(key));
                    propDesc.set("configurable", obj.memberConfigurable(key));
                    propDesc.set("isOwn", true);
                    res.add(propDesc);
                }
                else {
                    propDesc.set("name", Values.toString(ctx, key));
                    propDesc.set("value", serializeObj(ctx, Values.getMember(ctx, obj, key)));
                    propDesc.set("writable", obj.memberWritable(key));
                    propDesc.set("enumerable", obj.memberEnumerable(key));
                    propDesc.set("configurable", obj.memberConfigurable(key));
                    propDesc.set("isOwn", true);
                    res.add(propDesc);
                }
            }
    
            var proto = Values.getPrototype(ctx, obj);
    
            var protoDesc = new JSONMap();
            protoDesc.set("name", "__proto__");
            protoDesc.set("value", serializeObj(ctx, proto == null ? Values.NULL : proto));
            protoDesc.set("writable", true);
            protoDesc.set("enumerable", false);
            protoDesc.set("configurable", false);
            protoDesc.set("isOwn", true);
            res.add(protoDesc);
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

        var thisArgRef = idToObject.get(Integer.parseInt(msg.params.string("objectId")));
        var thisArg = thisArgRef.obj;
        var ctx = thisArgRef.ctx;

        while (true) {
            var start = src.lastIndexOf("//# sourceURL=");
            if (start < 0) break;
            var end = src.indexOf("\n", start);
            if (end < 0) src = src.substring(0, start);
            else src = src.substring(0, start) + src.substring(end + 1);
        }

        try {
            Object res = null;
            if (compare(src, VSCODE_EMPTY)) res = emptyObject;
            else if (compare(src, VSCODE_SELF)) res = thisArg;
            else if (compare(src, CHROME_GET_PROP_FUNC)) {
                res = thisArg;
                for (var el : JSON.parse(null, (String)args.get(0)).list()) res = Values.getMember(ctx, res, JSON.toJs(el));
            }
            else if (compare(src, VSCODE_CALL)) {
                var func = (FunctionValue)(args.size() < 1 ? null : args.get(0));
                ws.send(msg.respond(new JSONMap().set("result", serializeObj(ctx, func.call(ctx, thisArg)))));
            }
            else if (compare(src, VSCODE_AUTOCOMPLETE)) {
                var target = args.get(0);
                if (target == null) target = thisArg;
                res = vscodeAutoSuggest(ctx, target, Values.toString(ctx, args.get(1)), Values.toBoolean(args.get(2)));
            }
            else {
                ws.send(new V8Error("Please use well-known functions with callFunctionOn"));
                return;
            }
            ws.send(msg.respond(new JSONMap().set("result", serializeObj(ctx, res, byValue))));
        }
        catch (EngineException e) { ws.send(msg.respond(new JSONMap().set("exceptionDetails", serializeException(ctx, e)))); }
    }

    @Override public synchronized void runtimeEnable(V8Message msg) throws IOException {
        ws.send(msg.respond());
    }

    @Override public void onSourceLoad(Filename filename, String source) {
        int id = nextId();
        var src = new DebugSource(id, filename, source);

        idToSource.put(id, src);
        filenameToId.put(filename, id);

        if (!enabled) pendingSources.add(src);
        else sendSource(src);
    }
    @Override public void onFunctionLoad(FunctionBody body, FunctionMap map) {
        for (var bpt : idToBreakpoint.values()) {
            bpt.addFunc(body, map);
        }
        mappings.put(body, map);
    }
    @Override public boolean onInstruction(Context ctx, Frame cf, Instruction instruction, Object returnVal, EngineException error, boolean caught) {
        if (!enabled) return false;

        boolean isBreakpointable;
        Location loc;
        DebugFrame frame;
        BreakpointType bptType;

        synchronized (this) {
            frame = getFrame(cf);

            var map = DebugContext.get(ctx).getMap(frame.frame.function);

            frame.updateLoc(map.toLocation(frame.frame.codePtr));
            loc = frame.location;
            bptType = map.getBreakpoint(frame.frame.codePtr);
            isBreakpointable = loc != null && (bptType.shouldStepIn());

            if (error != null && (execptionType == CatchType.ALL || execptionType == CatchType.UNCAUGHT && !caught)) {
                pauseException(ctx);
            }
            else if (
                loc != null &&
                (state == State.STEPPING_IN || state == State.STEPPING_OVER) &&
                returnVal != Values.NO_RETURN && stepOutFrame == frame
            ) {
                pauseDebug(ctx, null);
            }
            else if (isBreakpointable && bpLocs.getOrDefault(cf.function.body, new HashMap<>()).containsKey(loc)) {
                var bp = bpLocs.get(cf.function.body).get(loc);
                var ok = bp.condition == null ? true : Values.toBoolean(run(currFrame, bp.condition).result);
                if (ok) pauseDebug(ctx, bp);
            }
            // else if (isBreakpointable && tmpBreakpts.remove(loc)) pauseDebug(ctx, null);
            else if (isBreakpointable && pendingPause) {
                pauseDebug(ctx, null);
                pendingPause = false;
            }
            else if (instruction.type == Type.NOP && instruction.match("debug")) pauseDebug(ctx, null);
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
                            if (returnVal != Values.NO_RETURN || error != null) {
                                state = State.STEPPING_OUT;
                                continue;
                            }
                            else if (stepOutPtr != frame.frame.codePtr) {

                                if (state == State.STEPPING_IN && bptType.shouldStepIn()) {
                                    pauseDebug(ctx, null);
                                    break;
                                }
                                else if (state == State.STEPPING_OVER && bptType.shouldStepOver()) {
                                    pauseDebug(ctx, null);
                                    break;
                                }
                            }
                        }
                        return false;
                }
            }
            updateNotifier.await();
        }

        return false;
    }
    @Override public void onFramePush(Context ctx, Frame frame) {
        var prevFrame = currFrame;
        updateFrames(ctx);

        if (stepOutFrame != null && stepOutFrame.frame == prevFrame.frame && state == State.STEPPING_IN) {
            stepOutFrame = currFrame;
        }
    }
    @Override public void onFramePop(Context ctx, Frame frame) {
        updateFrames(ctx);

        try { idToFrame.remove(codeFrameToFrame.remove(frame).id); }
        catch (NullPointerException e) { }

        if (ctx.stackSize == 0) {
            if (state == State.PAUSED_EXCEPTION || state == State.PAUSED_NORMAL) resume(State.RESUMED);
        }
        else if (stepOutFrame != null && stepOutFrame.frame == frame && state == State.STEPPING_OUT) {
            state = State.STEPPING_IN;
            stepOutFrame = currFrame;
        }
    }

    public SimpleDebugger attach(DebugContext ctx) {
        ctx.attachDebugger(this);
        return this;
    }

    public SimpleDebugger(WebSocket ws) {
        this.ws = ws;
    }
}
