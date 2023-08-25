package me.topchetoeu.jscript.compilation;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.engine.Operation;
import me.topchetoeu.jscript.exceptions.SyntaxException;

public class Instruction {
    public static enum Type {
        RETURN,
        SIGNAL,
        THROW,
        THROW_SYNTAX,
        DELETE,
        TRY,
        NOP,

        CALL,
        CALL_NEW,
        JMP_IF,
        JMP_IFN,
        JMP,

        LOAD_VALUE,

        LOAD_VAR,
        LOAD_MEMBER,
        LOAD_VAL_MEMBER,
        LOAD_GLOB,

        LOAD_FUNC,
        LOAD_ARR,
        LOAD_OBJ,
        STORE_SELF_FUNC,
        LOAD_REGEX,

        DUP,
        MOVE,

        STORE_VAR,
        STORE_MEMBER,
        DISCARD,

        MAKE_VAR,
        DEF_PROP,
        KEYS,

        TYPEOF,
        OPERATION;
        // TYPEOF,
        // INSTANCEOF(true),
        // IN(true),

        // MULTIPLY(true),
        // DIVIDE(true),
        // MODULO(true),
        // ADD(true),
        // SUBTRACT(true),

        // USHIFT_RIGHT(true),
        // SHIFT_RIGHT(true),
        // SHIFT_LEFT(true),

        // GREATER(true),
        // LESS(true),
        // GREATER_EQUALS(true),
        // LESS_EQUALS(true),
        // LOOSE_EQUALS(true),
        // LOOSE_NOT_EQUALS(true),
        // EQUALS(true),
        // NOT_EQUALS(true),

        // AND(true),
        // OR(true),
        // XOR(true),

        // NEG(true),
        // POS(true),
        // NOT(true),
        // INVERSE(true);

        // final boolean isOperation;

        // private Type(boolean isOperation) {
        //     this.isOperation = isOperation;
        // }
        // private Type() {
        //     this(false);
        // }
    }

    public final Type type;
    public final Object[] params;
    public Location location;
    public boolean debugged;

    public Instruction locate(Location loc) {
        this.location = loc;
        return this;
    }
    public Instruction setDebug(boolean debug) {
        debugged = debug;
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(int i) {
        if (i >= params.length || i < 0) return null;
        return (T)params[i];
    }
    @SuppressWarnings("unchecked")
    public <T> T get(int i, T defaultVal) {
        if (i >= params.length || i < 0) return defaultVal;
        return (T)params[i];
    }
    public boolean match(Object ...args) {
        if (args.length != params.length) return false;
        for (int i = 0; i < args.length; i++) {
            var a = params[i];
            var b = args[i];
            if (a == null || b == null) {
                if (!(a == null && b == null)) return false;
            }
            if (!a.equals(b)) return false;
        }
        return true;
    }
    public boolean is(int i, Object arg) {
        if (params.length <= i) return false;
        return params[i].equals(arg);
    }

    private Instruction(Location location, Type type, Object... params) {
        this.location = location;
        this.type = type;
        this.params = params;
    }

    public static Instruction tryInstr(boolean hasCatch, boolean hasFinally) {
        return new Instruction(null, Type.TRY, hasCatch, hasFinally);
    }
    public static Instruction throwInstr() {
        return new Instruction(null, Type.THROW);
    }
    public static Instruction throwSyntax(SyntaxException err) {
        return new Instruction(null, Type.THROW_SYNTAX, err.getMessage());
    }
    public static Instruction delete() {
        return new Instruction(null, Type.DELETE);
    }
    public static Instruction ret() {
        return new Instruction(null, Type.RETURN);
    }
    public static Instruction debug() {
        return new Instruction(null, Type.NOP, "debug");
    }
    public static Instruction debugVarNames(String[] names) {
        var args = new Object[names.length + 1];
        args[0] = "dbg_vars";

        System.arraycopy(names, 0, args, 1, names.length);

        return new Instruction(null, Type.NOP, args);
    }

    /**
     * ATTENTION: Usage outside of try/catch is broken af
     */
    public static Instruction signal(String name) {
        return new Instruction(null, Type.SIGNAL, name);
    }
    public static Instruction nop(Object ...params) {
        for (var param : params) {
            if (param instanceof String) continue;
            if (param instanceof Boolean) continue;
            if (param instanceof Double) continue;
            if (param instanceof Integer) continue;
            if (param == null) continue;

            throw new RuntimeException("NOP params may contain only strings, booleans, doubles, integers and nulls.");
        }
        return new Instruction(null, Type.NOP, params);
    }

    public static Instruction call(int argn) {
        return new Instruction(null, Type.CALL, argn);
    }
    public static Instruction callNew(int argn) {
        return new Instruction(null, Type.CALL_NEW, argn);
    }
    public static Instruction jmp(int offset) {
        return new Instruction(null, Type.JMP, offset);
    }
    public static Instruction jmpIf(int offset) {
        return new Instruction(null, Type.JMP_IF, offset);
    }
    public static Instruction jmpIfNot(int offset) {
        return new Instruction(null, Type.JMP_IFN, offset);
    }

    public static Instruction loadValue(Object val) {
        return new Instruction(null, Type.LOAD_VALUE, val);
    }

    public static Instruction makeVar(String name) {
        return new Instruction(null, Type.MAKE_VAR, name);
    }
    public static Instruction loadVar(Object i) {
        return new Instruction(null, Type.LOAD_VAR, i);
    }
    public static Instruction loadGlob() {
        return new Instruction(null, Type.LOAD_GLOB);
    }
    public static Instruction loadMember() {
        return new Instruction(null, Type.LOAD_MEMBER);
    }
    public static Instruction loadMember(Object key) {
        if (key instanceof Number) key = ((Number)key).doubleValue();
        return new Instruction(null, Type.LOAD_VAL_MEMBER, key);
    }

    public static Instruction loadRegex(String pattern, String flags) {
        return new Instruction(null, Type.LOAD_REGEX, pattern, flags);
    }
    public static Instruction loadFunc(int instrN, int varN, int len, int[] captures) {
        var args = new Object[3 + captures.length];
        args[0] = instrN;
        args[1] = varN;
        args[2] = len;
        for (var i = 0; i < captures.length; i++) args[i + 3] = captures[i];
        return new Instruction(null, Type.LOAD_FUNC, args);
    }
    public static Instruction loadObj() {
        return new Instruction(null, Type.LOAD_OBJ);
    }
    public static Instruction loadArr(int count) {
        return new Instruction(null, Type.LOAD_ARR, count);
    }
    public static Instruction dup() {
        return new Instruction(null, Type.DUP, 0, 1);
    }
    public static Instruction dup(int count, int offset) {
        return new Instruction(null, Type.DUP, offset, count);
    }
    public static Instruction move(int count, int offset) {
        return new Instruction(null, Type.MOVE, offset, count);
    }

    public static Instruction storeSelfFunc(int i) {
        return new Instruction(null, Type.STORE_SELF_FUNC, i);
    }
    public static Instruction storeVar(Object i) {
        return new Instruction(null, Type.STORE_VAR, i, false);
    }
    public static Instruction storeVar(Object i, boolean keep) {
        return new Instruction(null, Type.STORE_VAR, i, keep);
    }
    public static Instruction storeMember() {
        return new Instruction(null, Type.STORE_MEMBER, false);
    }
    public static Instruction storeMember(boolean keep) {
        return new Instruction(null, Type.STORE_MEMBER, keep);
    }
    public static Instruction discard() {
        return new Instruction(null, Type.DISCARD);
    }

    public static Instruction typeof() {
        return new Instruction(null, Type.TYPEOF);
    }
    public static Instruction typeof(Object varName) {
        return new Instruction(null, Type.TYPEOF, varName);
    }

    public static Instruction keys() {
        return new Instruction(null, Type.KEYS);
    }

    public static Instruction defProp() {
        return new Instruction(null, Type.DEF_PROP);
    }

    public static Instruction operation(Operation op) {
        return new Instruction(null, Type.OPERATION, op);
    }

    @Override
    public String toString() {
        var res = type.toString();

        for (int i = 0; i < params.length; i++) {
            res += " " + params[i];
        }

        return res;
    }
}
