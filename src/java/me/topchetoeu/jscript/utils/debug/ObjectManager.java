package me.topchetoeu.jscript.utils.debug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;

import me.topchetoeu.jscript.common.json.JSON;
import me.topchetoeu.jscript.common.json.JSONMap;
import me.topchetoeu.jscript.runtime.environment.Environment;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.values.ArrayValue;
import me.topchetoeu.jscript.runtime.values.FunctionValue;
import me.topchetoeu.jscript.runtime.values.ObjectValue;
import me.topchetoeu.jscript.runtime.values.Symbol;
import me.topchetoeu.jscript.runtime.values.Values;

class ObjectManager {
    public static class ObjRef {
        public final ObjectValue obj;
        public final Environment ext;
        public final HashSet<String> heldGroups = new HashSet<>();
        public boolean held = true;

        public boolean shouldRelease() {
            return !held && heldGroups.size() == 0;
        }

        public ObjRef(Environment ext, ObjectValue obj) {
            this.ext = ext;
            this.obj = obj;
        }
    }

    private Supplier<Integer> idSupplier;
    private HashMap<Integer, ObjRef> idToObject = new HashMap<>();
    private HashMap<ObjectValue, Integer> objectToId = new HashMap<>();
    private HashMap<String, ArrayList<ObjRef>> objectGroups = new HashMap<>();

    public JSONMap serialize(Environment env, Object val, boolean byValue) {
        val = Values.normalize(null, val);
        env = SimpleDebugger.sanitizeEnvironment(env);

        if (val == Values.NULL) {
            return new JSONMap()
                .set("type", "object")
                .set("subtype", "null")
                .setNull("value")
                .set("description", "null");
        }

        if (val instanceof ObjectValue) {
            var obj = (ObjectValue)val;
            int id;

            if (objectToId.containsKey(obj)) id = objectToId.get(obj);
            else {
                id = idSupplier.get();
                var ref = new ObjRef(env, obj);
                objectToId.put(obj, id);
                idToObject.put(id, ref);
            }

            var type = "object";
            String subtype = null;
            String className = null;

            if (obj instanceof FunctionValue) type = "function";
            if (obj instanceof ArrayValue) subtype = "array";

            try { className = Values.toString(env, Values.getMemberPath(env, obj, "constructor", "name")); }
            catch (Exception e) { }

            var res = new JSONMap()
                .set("type", type)
                .set("objectId", id + "");

            if (subtype != null) res.set("subtype", subtype);
            if (className != null) {
                res.set("className", className);
                res.set("description", className);
            }

            if (obj instanceof ArrayValue) res.set("description", "Array(" + ((ArrayValue)obj).size() + ")");
            else if (obj instanceof FunctionValue) res.set("description", obj.toString());
            else {
                var defaultToString = false;

                try {
                    defaultToString =
                        Values.getMember(env, obj, "toString") ==
                        Values.getMember(env, env.get(Environment.OBJECT_PROTO), "toString");
                }
                catch (Exception e) { }

                try { res.set("description", className + (defaultToString ? "" : " { " + Values.toString(env, obj) + " }")); }
                catch (Exception e) { }
            }


            if (byValue) try { res.put("value", JSON.fromJs(env, obj)); }
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
    public JSONMap serialize(Environment ext, Object val) {
        return serialize(ext, val, false);
    }

    public void addToGroup(String name, Object val) {
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
    public void removeGroup(String name) {
        var objs = objectGroups.remove(name);

        if (objs != null) {
            for (var obj : objs) {
                if (obj.heldGroups.remove(name) && obj.shouldRelease()) {
                    var id = objectToId.remove(obj.obj);
                    if (id != null) idToObject.remove(id);
                }
            }
        }
    }

    public ObjRef get(int id) {
        return idToObject.get(id);
    }
    public void release(int id) {
        var ref = idToObject.get(id);
        ref.held = false;

        if (ref.shouldRelease()) {
            objectToId.remove(ref.obj);
            idToObject.remove(id);
        }
    }

    public Object deserializeArgument(JSONMap val) {
        if (val.isString("objectId")) return get(Integer.parseInt(val.string("objectId"))).obj;
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

    public JSONMap serializeException(Environment ext, EngineException err) {
        String text = null;

        try {
            text = Values.toString(ext, err.value);
        }
        catch (EngineException e) {
            text = "[error while stringifying]";
        }

        return new JSONMap()
            .set("exceptionId", idSupplier.get())
            .set("exception", serialize(ext, err.value))
            .set("text", text);
    }

    public void clear() {
        this.idToObject.clear();
        this.objectToId.clear();
        this.objectGroups.clear();
    }

    public ObjectManager(Supplier<Integer> idSupplier) {
        this.idSupplier = idSupplier;
    }
}