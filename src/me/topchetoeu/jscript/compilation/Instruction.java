package me.topchetoeu.jscript.compilation;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.engine.Operation;
import me.topchetoeu.jscript.exceptions.SyntaxException;

public class Instruction {
    public static enum Type {
        RETURN,
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

    public Instruction locate(Location loc) {
        this.location = loc;
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

    private Instruction(Location location, Type type, Object ...params) {
        this.location = location;
        this.type = type;
        this.params = params;
    }

    public static Instruction tryInstr(Location loc, int n, int catchN, int finallyN) {
        return new Instruction(loc, Type.TRY, n, catchN, finallyN);
    }
    public static Instruction throwInstr(Location loc) {
        return new Instruction(loc, Type.THROW);
    }
    public static Instruction throwSyntax(Location loc, SyntaxException err) {
        return new Instruction(loc, Type.THROW_SYNTAX, err.getMessage());
    }
    public static Instruction throwSyntax(Location loc, String err) {
        return new Instruction(loc, Type.THROW_SYNTAX, err);
    }
    public static Instruction delete(Location loc) {
        return new Instruction(loc, Type.DELETE);
    }
    public static Instruction ret(Location loc) {
        return new Instruction(loc, Type.RETURN);
    }
    public static Instruction debug(Location loc) {
        return new Instruction(loc, Type.NOP, "debug");
    }

    public static Instruction nop(Location loc, Object ...params) {
        for (var param : params) {
            if (param instanceof String) continue;
            if (param instanceof Boolean) continue;
            if (param instanceof Double) continue;
            if (param instanceof Integer) continue;
            if (param == null) continue;

            throw new RuntimeException("NOP params may contain only strings, booleans, doubles, integers and nulls.");
        }
        return new Instruction(loc, Type.NOP, params);
    }

    public static Instruction call(Location loc, int argn) {
        return new Instruction(loc, Type.CALL, argn);
    }
    public static Instruction callNew(Location loc, int argn) {
        return new Instruction(loc, Type.CALL_NEW, argn);
    }
    public static Instruction jmp(Location loc, int offset) {
        return new Instruction(loc, Type.JMP, offset);
    }
    public static Instruction jmpIf(Location loc, int offset) {
        return new Instruction(loc, Type.JMP_IF, offset);
    }
    public static Instruction jmpIfNot(Location loc, int offset) {
        return new Instruction(loc, Type.JMP_IFN, offset);
    }

    public static Instruction loadValue(Location loc, Object val) {
        return new Instruction(loc, Type.LOAD_VALUE, val);
    }

    public static Instruction makeVar(Location loc, String name) {
        return new Instruction(loc, Type.MAKE_VAR, name);
    }
    public static Instruction loadVar(Location loc, Object i) {
        return new Instruction(loc, Type.LOAD_VAR, i);
    }
    public static Instruction loadGlob(Location loc) {
        return new Instruction(loc, Type.LOAD_GLOB);
    }
    public static Instruction loadMember(Location loc) {
        return new Instruction(loc, Type.LOAD_MEMBER);
    }
    public static Instruction loadMember(Location loc, Object key) {
        if (key instanceof Number) key = ((Number)key).doubleValue();
        return new Instruction(loc, Type.LOAD_VAL_MEMBER, key);
    }

    public static Instruction loadRegex(Location loc, String pattern, String flags) {
        return new Instruction(loc, Type.LOAD_REGEX, pattern, flags);
    }
    public static Instruction loadFunc(Location loc, long id, int[] captures) {
        var args = new Object[1 + captures.length];
        args[0] = id;
        for (var i = 0; i < captures.length; i++) args[i + 1] = captures[i];
        return new Instruction(loc, Type.LOAD_FUNC, args);
    }
    public static Instruction loadObj(Location loc) {
        return new Instruction(loc, Type.LOAD_OBJ);
    }
    public static Instruction loadArr(Location loc, int count) {
        return new Instruction(loc, Type.LOAD_ARR, count);
    }
    public static Instruction dup(Location loc) {
        return new Instruction(loc, Type.DUP, 0, 1);
    }
    public static Instruction dup(Location loc, int count, int offset) {
        return new Instruction(loc, Type.DUP, offset, count);
    }
    public static Instruction move(Location loc, int count, int offset) {
        return new Instruction(loc, Type.MOVE, offset, count);
    }

    public static Instruction storeSelfFunc(Location loc, int i) {
        return new Instruction(loc, Type.STORE_SELF_FUNC, i);
    }
    public static Instruction storeVar(Location loc, Object i) {
        return new Instruction(loc, Type.STORE_VAR, i, false);
    }
    public static Instruction storeVar(Location loc, Object i, boolean keep) {
        return new Instruction(loc, Type.STORE_VAR, i, keep);
    }
    public static Instruction storeMember(Location loc) {
        return new Instruction(loc, Type.STORE_MEMBER, false);
    }
    public static Instruction storeMember(Location loc, boolean keep) {
        return new Instruction(loc, Type.STORE_MEMBER, keep);
    }
    public static Instruction discard(Location loc) {
        return new Instruction(loc, Type.DISCARD);
    }

    public static Instruction typeof(Location loc) {
        return new Instruction(loc, Type.TYPEOF);
    }
    public static Instruction typeof(Location loc, Object varName) {
        return new Instruction(loc, Type.TYPEOF, varName);
    }

    public static Instruction keys(Location loc, boolean forInFormat) {
        return new Instruction(loc, Type.KEYS, forInFormat);
    }

    public static Instruction defProp(Location loc) {
        return new Instruction(loc, Type.DEF_PROP);
    }

    public static Instruction operation(Location loc, Operation op) {
        return new Instruction(loc, Type.OPERATION, op);
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
