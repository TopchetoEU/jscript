package me.topchetoeu.jscript.engine.debug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import me.topchetoeu.jscript.Filename;
import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Instruction.Type;
import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.Engine;
import me.topchetoeu.jscript.engine.StackData;
import me.topchetoeu.jscript.engine.frame.CodeFrame;
import me.topchetoeu.jscript.engine.frame.Runners;
import me.topchetoeu.jscript.engine.scope.GlobalScope;
import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.CodeFunction;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.Symbol;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.events.Notifier;
import me.topchetoeu.jscript.exceptions.EngineException;
import me.topchetoeu.jscript.json.JSON;
import me.topchetoeu.jscript.json.JSONElement;
import me.topchetoeu.jscript.json.JSONList;
import me.topchetoeu.jscript.json.JSONMap;
import me.topchetoeu.jscript.lib.DateLib;
import me.topchetoeu.jscript.lib.MapLib;
import me.topchetoeu.jscript.lib.PromiseLib;
import me.topchetoeu.jscript.lib.RegExpLib;
import me.topchetoeu.jscript.lib.SetLib;
import me.topchetoeu.jscript.lib.GeneratorLib.Generator;

public class SimpleDebugger implements Debugger {
    public static final String CHROME_GET_PROP_FUNC = "function s(e){let t=this;const n=JSON.parse(e);for(let e=0,i=n.length;e<i;++e)t=t[n[e]];return t}";

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
    private static class Source {
        public final int id;
        public final Filename filename;
        public final String source;
        public final TreeSet<Location> breakpoints;

        public Source(int id, Filename filename, String source, TreeSet<Location> breakpoints) {
            this.id = id;
            this.filename = filename;
            this.source = source;
            this.breakpoints = breakpoints;
        }
    }
    private static class Breakpoint {
        public final int id;
        public final Location location;
        public final String condition;

        public Breakpoint(int id, Location location, String condition) {
            this.id = id;
            this.location = location;
            this.condition = condition;
        }
    }
    private static class BreakpointCandidate {
        public final int id;
        public final String condition;
        public final Pattern pattern;
        public final int line, start;
        public final HashSet<Breakpoint> resolvedBreakpoints = new HashSet<>();

        public BreakpointCandidate(int id, Pattern pattern, int line, int start, String condition) {
            this.id = id;
            this.pattern = pattern;
            this.line = line;
            this.start = start;
            if (condition != null && condition.trim().equals("")) condition = null;
            this.condition = condition;
        }
    }
    private class Frame {
        public CodeFrame frame;
        public CodeFunction func;
        public int id;
        public ObjectValue local, capture, global;
        public JSONMap serialized;
        public Location location;
        public boolean debugData = false;

        public void updateLoc(Location loc) {
            serialized.set("location", serializeLocation(loc));
            this.location = loc;
        }

        public Frame(Context ctx, CodeFrame frame, int id) {
            this.frame = frame;
            this.func = frame.function;
            this.id = id;
            this.local = new ObjectValue();
            this.location = frame.function.loc();

            this.global = frame.function.environment.global.obj;
            this.local = frame.getLocalScope(ctx, true);
            this.capture = frame.getCaptureScope(ctx, true);
            this.local.setPrototype(ctx, capture);
            this.capture.setPrototype(ctx, global);

            if (location != null) {
                debugData = true;
                this.serialized = new JSONMap()
                    .set("callFrameId", id + "")
                    .set("functionName", func.name)
                    .set("location", serializeLocation(location))
                    .set("scopeChain", new JSONList()
                        .add(new JSONMap().set("type", "local").set("name", "Local Scope").set("object", serializeObj(ctx, local)))
                        .add(new JSONMap().set("type", "closure").set("name", "Closure").set("object", serializeObj(ctx, capture)))
                        .add(new JSONMap().set("type", "global").set("name", "Global Scope").set("object", serializeObj(ctx, global)))
                    )
                    .setNull("this");
            }
        }
    }

    private class RunResult {
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
    public CatchType execptionType = CatchType.ALL; 
    public State state = State.RESUMED;

    public final WebSocket ws;
    public final Engine target;

    private HashMap<Integer, BreakpointCandidate> idToBptCand = new HashMap<>();

    private HashMap<Integer, Breakpoint> idToBreakpoint = new HashMap<>();
    private HashMap<Location, Breakpoint> locToBreakpoint = new HashMap<>();
    private HashSet<Location> tmpBreakpts = new HashSet<>();

    private HashMap<Filename, Integer> filenameToId = new HashMap<>();
    private HashMap<Integer, Source> idToSource = new HashMap<>();
    private ArrayList<Source> pendingSources = new ArrayList<>();

    private HashMap<Integer, Frame> idToFrame = new HashMap<>();
    private HashMap<CodeFrame, Frame> codeFrameToFrame = new HashMap<>();

    private HashMap<Integer, ObjectValue> idToObject = new HashMap<>();
    private HashMap<ObjectValue, Integer> objectToId = new HashMap<>();
    private HashMap<ObjectValue, Context> objectToCtx = new HashMap<>();
    private HashMap<String, ArrayList<ObjectValue>> objectGroups = new HashMap<>();

    private Notifier updateNotifier = new Notifier(); 

    private int nextId = new Random().nextInt() & 0x7FFFFFFF;
    private Location prevLocation = null;
    private Frame stepOutFrame = null, currFrame = null;

    private int nextId() {
        return nextId++ ^ 1630022591 /* big prime */;
    }

    private void updateFrames(Context ctx) {
        var frame = StackData.peekFrame(ctx);
        if (frame == null) return;

        if (!codeFrameToFrame.containsKey(frame)) {
            var id = nextId();
            var fr = new Frame(ctx, frame, id);

            idToFrame.put(id, fr);
            codeFrameToFrame.put(frame, fr);
        }

        currFrame = codeFrameToFrame.get(frame);
    }
    private JSONList serializeFrames(Context ctx) {
        var res = new JSONList();
        var frames = StackData.frames(ctx);

        for (var i = frames.size() - 1; i >= 0; i--) {
            res.add(codeFrameToFrame.get(frames.get(i)).serialized);
        }

        return res;
    }

    private Location correctLocation(Source source, Location loc) {
        var set = source.breakpoints;

        if (set.contains(loc)) return loc;

        var tail = set.tailSet(loc);
        if (tail.isEmpty()) return null;

        return tail.first();
    }
    private Location deserializeLocation(JSONElement el, boolean correct) {
        if (!el.isMap()) throw new RuntimeException("Expected location to be a map.");
        var id = Integer.parseInt(el.map().string("scriptId"));
        var line = (int)el.map().number("lineNumber") + 1;
        var column = (int)el.map().number("columnNumber") + 1;

        if (!idToSource.containsKey(id)) throw new RuntimeException(String.format("The specified source %s doesn't exist.", id));

        var res = new Location(line, column, idToSource.get(id).filename);
        if (correct) res = correctLocation(idToSource.get(id), res);
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
            objectToId.put(obj, id);
            if (ctx != null) objectToCtx.put(obj, ctx);
            idToObject.put(id, obj);
            return id;
        }
    }
    private JSONMap serializeObj(Context ctx, Object val, boolean recurse) {
        val = Values.normalize(null, val);

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

            if (Values.isWrapper(val, RegExpLib.class)) subtype = "regexp";
            if (Values.isWrapper(val, DateLib.class)) subtype = "date";
            if (Values.isWrapper(val, MapLib.class)) subtype = "map";
            if (Values.isWrapper(val, SetLib.class)) subtype = "set";
            if (Values.isWrapper(val, Generator.class)) subtype = "generator";
            if (Values.isWrapper(val, PromiseLib.class)) subtype = "promise";

            try { className = Values.toString(ctx, Values.getMember(ctx, obj.getMember(ctx, "constructor"), "name")); }
            catch (Exception e) { }

            var res = new JSONMap()
                .set("type", type)
                .set("objectId", id + "");

            if (subtype != null) res.set("subtype", subtype);
            if (className != null) {
                res.set("className", className);
                res.set("description", "{ " + className + " }");
            }

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
            else if (Double.isNaN(num)) res.set("unserializableValue", "NaN");
            else res.set("value", num);

            return res;
        }

        throw new IllegalArgumentException("Unexpected JS object.");
    }
    private JSONMap serializeObj(Context ctx, Object val) {
        return serializeObj(ctx, val, true);
    }
    private void setObjectGroup(String name, Object val) {
        if (val instanceof ObjectValue) {
            var obj = (ObjectValue)val;

            if (objectGroups.containsKey(name)) objectGroups.get(name).add(obj);
            else objectGroups.put(name, new ArrayList<>(List.of(obj)));
        }
    }
    private void releaseGroup(String name) {
        var objs = objectGroups.remove(name);

        if (objs != null) {
            for (var obj : objs) {
                var id = objectToId.remove(obj);
                if (id != null) idToObject.remove(id);
            }
        }
    }
    private Object deserializeArgument(JSONMap val) {
        if (val.isString("objectId")) return idToObject.get(Integer.parseInt(val.string("objectId")));
        else if (val.isString("unserializableValue")) switch (val.string("unserializableValue")) {
            case "NaN": return Double.NaN;
            case "-Infinity": return Double.NEGATIVE_INFINITY;
            case "Infinity": return Double.POSITIVE_INFINITY;
            case "-0": return -0.;
        }
        return JSON.toJs(val.get("value"));
    }

    private void resume(State state) {
        this.state = state;
        ws.send(new V8Event("Debugger.resumed", new JSONMap()));
        updateNotifier.next();
    }
    private void pauseDebug(Context ctx, Breakpoint bp) {
        state = State.PAUSED_NORMAL;
        var map = new JSONMap()
            .set("callFrames", serializeFrames(ctx))
            .set("reason", "debugCommand");

        if (bp != null) map.set("hitBreakpoints", new JSONList().add(bp.id + ""));
        ws.send(new V8Event("Debugger.paused", map));
    }
    private void pauseException(Context ctx) {
        state = State.PAUSED_EXCEPTION;
        var map = new JSONMap()
            .set("callFrames", serializeFrames(ctx))
            .set("reason", "exception");

        ws.send(new V8Event("Debugger.paused", map));
    }

    private void sendSource(Source src) {
        ws.send(new V8Event("Debugger.scriptParsed", new JSONMap()
            .set("scriptId", src.id + "")
            .set("hash", src.source.hashCode())
            .set("url", src.filename + "")
        ));
    }

    private void addBreakpoint(Breakpoint bpt) {
        idToBreakpoint.put(bpt.id, bpt);
        locToBreakpoint.put(bpt.location, bpt);

        ws.send(new V8Event("Debugger.breakpointResolved", new JSONMap()
            .set("breakpointId", bpt.id)
            .set("location", serializeLocation(bpt.location))
        ));
    }

    private RunResult run(Frame codeFrame, String code) {
        var engine = new Engine();
        var env = codeFrame.func.environment.fork();

        ObjectValue global = env.global.obj,
            capture = codeFrame.frame.getCaptureScope(null, enabled),
            local = codeFrame.frame.getLocalScope(null, enabled);

        capture.setPrototype(null, global);
        local.setPrototype(null, capture);
        env.global = new GlobalScope(local);

        var ctx = new Context(engine).pushEnv(env);
        var awaiter = engine.pushMsg(false, ctx, new Filename("temp", "exec"), code, codeFrame.frame.thisArg, codeFrame.frame.args);

        engine.run(true);

        try { return new RunResult(ctx, awaiter.await(), null); }
        catch (EngineException e) { return new RunResult(ctx, null, e); }
    }

    @Override public void enable(V8Message msg) {
        enabled = true;
        ws.send(msg.respond());

        for (var el : pendingSources) sendSource(el);
        pendingSources.clear();

        updateNotifier.next();
    }
    @Override public void disable(V8Message msg) {
        enabled = false;
        ws.send(msg.respond());
        updateNotifier.next();
    }

    @Override public void getScriptSource(V8Message msg) {
        int id = Integer.parseInt(msg.params.string("scriptId"));
        ws.send(msg.respond(new JSONMap().set("scriptSource", idToSource.get(id).source)));
    }
    @Override public void getPossibleBreakpoints(V8Message msg) {
        var src = idToSource.get(Integer.parseInt(msg.params.map("start").string("scriptId")));
        var start = deserializeLocation(msg.params.get("start"), false);
        var end = msg.params.isMap("end") ? deserializeLocation(msg.params.get("end"), false) : null;

        var res = new JSONList();

        for (var loc : src.breakpoints.tailSet(start, true)) {
            if (end != null && loc.compareTo(end) > 0) break;
            res.add(serializeLocation(loc));
        }

        ws.send(msg.respond(new JSONMap().set("locations", res)));
    }

    @Override public void pause(V8Message msg) {
    }

    @Override public void resume(V8Message msg) {
        resume(State.RESUMED);
        ws.send(msg.respond(new JSONMap()));
    }

    @Override public void setBreakpoint(V8Message msg) {
        // int id = nextId();
        // var loc = deserializeLocation(msg.params.get("location"), true);
        // var bpt = new Breakpoint(id, loc, null);
        // breakpoints.put(loc, bpt);
        // idToBrpt.put(id, bpt);
        // ws.send(msg.respond(new JSONMap()
        //     .set("breakpointId", id)
        //     .set("actualLocation", serializeLocation(loc))
        // ));
    }
    @Override public void setBreakpointByUrl(V8Message msg) {
        var line = (int)msg.params.number("lineNumber") + 1;
        var col = (int)msg.params.number("columnNumber", 0) + 1;

        Pattern regex;

        if (msg.params.isString("url")) regex = Pattern.compile(Pattern.quote(msg.params.string("url")));
        else regex = Pattern.compile(msg.params.string("urlRegex"));

        var bpcd = new BreakpointCandidate(nextId(), regex, line, col, null);
        idToBptCand.put(bpcd.id, bpcd);

        var locs = new JSONList();

        for (var src : idToSource.values()) {
            if (regex.matcher(src.filename.toString()).matches()) {
                var loc = correctLocation(src, new Location(line, col, src.filename));
                if (loc == null) continue;
                var bp = new Breakpoint(nextId(), loc, bpcd.condition);

                bpcd.resolvedBreakpoints.add(bp);
                locs.add(serializeLocation(loc));
                addBreakpoint(bp);
            }
        }

        ws.send(msg.respond(new JSONMap()
            .set("breakpointId", bpcd.id + "")
            .set("locations", locs)
        ));
    }
    @Override public void removeBreakpoint(V8Message msg) {
        var id = Integer.parseInt(msg.params.string("breakpointId"));

        if (idToBptCand.containsKey(id)) {
            var bpcd = idToBptCand.get(id);
            for (var bp : bpcd.resolvedBreakpoints) {
                idToBreakpoint.remove(bp.id);
                locToBreakpoint.remove(bp.location);
            }
            idToBptCand.remove(id);
        }
        else if (idToBreakpoint.containsKey(id)) {
            var bp = idToBreakpoint.remove(id);
            locToBreakpoint.remove(bp.location);
        }
        ws.send(msg.respond());
    }
    @Override public void continueToLocation(V8Message msg) {
        var loc = deserializeLocation(msg.params.get("location"), true);

        tmpBreakpts.add(loc);

        resume(State.RESUMED);
        ws.send(msg.respond());
    }

    @Override public void setPauseOnExceptions(V8Message msg) {
        ws.send(new V8Error("i dont wanna to"));
    }

    @Override public void stepInto(V8Message msg) {
        if (state == State.RESUMED) ws.send(new V8Error("Debugger is resumed."));
        else {
            prevLocation = currFrame.location;
            stepOutFrame = currFrame;
            resume(State.STEPPING_IN);
            ws.send(msg.respond());
        }
    }
    @Override public void stepOut(V8Message msg) {
        if (state == State.RESUMED) ws.send(new V8Error("Debugger is resumed."));
        else {
            prevLocation = currFrame.location;
            stepOutFrame = currFrame;
            resume(State.STEPPING_OUT);
            ws.send(msg.respond());
        }
    }
    @Override public void stepOver(V8Message msg) {
        if (state == State.RESUMED) ws.send(new V8Error("Debugger is resumed."));
        else {
            prevLocation = currFrame.location;
            stepOutFrame = currFrame;
            resume(State.STEPPING_OVER);
            ws.send(msg.respond());
        }
    }

    @Override public void evaluateOnCallFrame(V8Message msg) {
        var cfId = Integer.parseInt(msg.params.string("callFrameId"));
        var expr = msg.params.string("expression");
        var group = msg.params.string("objectGroup", null);

        var cf = idToFrame.get(cfId);
        var res = run(cf, expr);

        if (group != null) setObjectGroup(group, res.result);

        ws.send(msg.respond(new JSONMap().set("result", serializeObj(res.ctx, res.result))));
    }

    @Override public void releaseObjectGroup(V8Message msg) {
        var group = msg.params.string("objectGroup");
        releaseGroup(group);
        ws.send(msg.respond());
    }
    @Override public void getProperties(V8Message msg) {
        var obj = idToObject.get(Integer.parseInt(msg.params.string("objectId")));
        var own = msg.params.bool("ownProperties") || true;
        var currOwn = true;

        var res = new JSONList();

        while (obj != null) {
            var ctx = objectToCtx.get(obj);

            for (var key : obj.keys(true)) {
                var propDesc = new JSONMap();

                if (obj.properties.containsKey(key)) {
                    var prop = obj.properties.get(key);

                    propDesc.set("name", Values.toString(ctx, key));
                    if (prop.getter != null) propDesc.set("get", serializeObj(ctx, prop.getter));
                    if (prop.setter != null) propDesc.set("set", serializeObj(ctx, prop.setter));
                    propDesc.set("enumerable", obj.memberEnumerable(key));
                    propDesc.set("configurable", obj.memberConfigurable(key));
                    propDesc.set("isOwn", currOwn);
                    res.add(propDesc);
                }
                else {
                    propDesc.set("name", Values.toString(ctx, key));
                    propDesc.set("value", serializeObj(ctx, obj.getMember(ctx, key)));
                    propDesc.set("writable", obj.memberWritable(key));
                    propDesc.set("enumerable", obj.memberEnumerable(key));
                    propDesc.set("configurable", obj.memberConfigurable(key));
                    propDesc.set("isOwn", currOwn);
                    res.add(propDesc);
                }
            }

            obj = obj.getPrototype(ctx);

            var protoDesc = new JSONMap();
            protoDesc.set("name", "__proto__");
            protoDesc.set("value", serializeObj(ctx, obj == null ? Values.NULL : obj));
            protoDesc.set("writable", true);
            protoDesc.set("enumerable", false);
            protoDesc.set("configurable", false);
            protoDesc.set("isOwn", currOwn);
            res.add(protoDesc);

            currOwn = false;
            if (own) break;
        }

        ws.send(msg.respond(new JSONMap().set("result", res)));
    }
    @Override public void callFunctionOn(V8Message msg) {
        var src = msg.params.string("functionDeclaration");
        var args = msg.params.list("arguments", new JSONList()).stream().map(v -> v.map()).map(this::deserializeArgument).collect(Collectors.toList());
        var thisArg = idToObject.get(Integer.parseInt(msg.params.string("objectId")));
        var ctx = objectToCtx.get(thisArg);

        switch (src) {
            case CHROME_GET_PROP_FUNC: {
                var path = JSON.parse(new Filename("tmp", "json"), (String)args.get(0)).list();
                Object res = thisArg;
                for (var el : path) res = Values.getMember(ctx, res, JSON.toJs(el));
                ws.send(msg.respond(new JSONMap().set("result", serializeObj(ctx, res))));
                return;
            }
            default:
                ws.send(new V8Error("A non well-known function was used with callFunctionOn."));
                break;
        }
    }

    @Override
    public void runtimeEnable(V8Message msg) {
        ws.send(msg.respond());
    }

    @Override public void onSource(Filename filename, String source, TreeSet<Location> locations) {
        int id = nextId();
        var src = new Source(id, filename, source, locations);

        filenameToId.put(filename, id);
        idToSource.put(id, src);

        for (var bpcd : idToBptCand.values()) {
            if (!bpcd.pattern.matcher(filename.toString()).matches()) continue;
            var loc = correctLocation(src, new Location(bpcd.line, bpcd.start, filename));
            var bp = new Breakpoint(nextId(), loc, bpcd.condition);
            if (loc == null) continue;
            bpcd.resolvedBreakpoints.add(bp);
            addBreakpoint(bp);
        }

        if (!enabled) pendingSources.add(src);
        else sendSource(src);
    }
    @Override public boolean onInstruction(Context ctx, CodeFrame cf, Instruction instruction, Object returnVal, EngineException error, boolean caught) {
        if (!enabled) return false;

        updateFrames(ctx);
        var frame = codeFrameToFrame.get(cf);

        if (!frame.debugData) return false;

        if (instruction.location != null) frame.updateLoc(instruction.location);
        var loc = frame.location;
        var isBreakpointable = loc != null && (
            idToSource.get(filenameToId.get(loc.filename())).breakpoints.contains(loc) ||
            returnVal != Runners.NO_RETURN
        );

        if (error != null && !caught && StackData.frames(ctx).size() > 1) error = null;

        if (error != null && (execptionType == CatchType.ALL || execptionType == CatchType.UNCAUGHT && !caught)) {
            pauseException(ctx);
        }
        else if (isBreakpointable && locToBreakpoint.containsKey(loc)) {
            var bp = locToBreakpoint.get(loc);
            var ok = bp.condition == null ? true : Values.toBoolean(run(currFrame, bp.condition));
            if (ok) pauseDebug(ctx, locToBreakpoint.get(loc));
        }
        else if (isBreakpointable && tmpBreakpts.remove(loc)) pauseDebug(ctx, null);
        else if (instruction.type == Type.NOP && instruction.match("debug")) pauseDebug(ctx, null);

        while (enabled) {
            switch (state) {
                case PAUSED_EXCEPTION:
                case PAUSED_NORMAL: break;

                case STEPPING_OUT:
                case RESUMED: return false;
                case STEPPING_IN:
                    if (!prevLocation.equals(loc)) {
                        if (isBreakpointable) pauseDebug(ctx, null);
                        else if (returnVal != Runners.NO_RETURN) pauseDebug(ctx, null);
                        else return false;
                    }
                    else return false;
                    break;
                case STEPPING_OVER:
                    if (stepOutFrame.frame == frame.frame) {
                        if (returnVal != Runners.NO_RETURN) {
                            state = State.STEPPING_OUT;
                            return false;
                        }
                        else if (isBreakpointable && (
                            !loc.filename().equals(prevLocation.filename()) ||
                            loc.line() != prevLocation.line()
                        )) pauseDebug(ctx, null);
                        else return false;
                    }
                    else return false;
                    break;
            }
            updateNotifier.await();
        }

        return false;
    }
    @Override public void onFramePop(Context ctx, CodeFrame frame) {
        updateFrames(ctx);

        try { idToFrame.remove(codeFrameToFrame.remove(frame).id); }
        catch (NullPointerException e) { }

        if (StackData.frames(ctx).size() == 0) resume(State.RESUMED);
        else if (stepOutFrame != null && stepOutFrame.frame == frame &&
            (state == State.STEPPING_OUT || state == State.STEPPING_IN)
        ) {
            pauseDebug(ctx, null);
            updateNotifier.await();
        }
    }

    @Override public void connect() {
        target.data.set(StackData.DEBUGGER, this);
    }
    @Override public void disconnect() {
        target.data.remove(StackData.DEBUGGER);
        enabled = false;
        updateNotifier.next();
    }

    private SimpleDebugger(WebSocket ws, Engine target) {
        this.ws = ws;
        this.target = target;
    }

    public static SimpleDebugger get(WebSocket ws, Engine target) {
        if (target.data.has(StackData.DEBUGGER)) {
            ws.send(new V8Error("A debugger is already attached to this engine."));
            return null;
        }
        else {
            var res = new SimpleDebugger(ws, target);
            return res;
        }
    }
}
