package me.topchetoeu.jscript.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;

import me.topchetoeu.jscript.engine.CallContext.DataKey;
import me.topchetoeu.jscript.engine.debug.DebugState;
import me.topchetoeu.jscript.engine.modules.ModuleManager;
import me.topchetoeu.jscript.engine.scope.GlobalScope;
import me.topchetoeu.jscript.engine.values.CodeFunction;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.ObjectValue.PlaceholderProto;
import me.topchetoeu.jscript.events.Awaitable;
import me.topchetoeu.jscript.events.DataNotifier;
import me.topchetoeu.jscript.exceptions.EngineException;
import me.topchetoeu.jscript.interop.NativeTypeRegister;
import me.topchetoeu.jscript.parsing.Parsing;

public class Engine {
    private static record RawFunction(GlobalScope scope, String filename, String raw) { }
    private static class Task {
        public final Object func;
        public final Object thisArg;
        public final Object[] args;
        public final Map<DataKey<?>, Object> data;
        public final DataNotifier<Object> notifier = new DataNotifier<>();

        public Task(Object func, Map<DataKey<?>, Object> data, Object thisArg, Object[] args) {
            this.func = func;
            this.data = data;
            this.thisArg = thisArg;
            this.args = args;
        }
    }

    public static final DataKey<DebugState> DEBUG_STATE_KEY = new DataKey<>();
    private static int nextId = 0;

    private Map<DataKey<?>, Object> callCtxVals = new HashMap<>();
    private GlobalScope global = new GlobalScope();
    private ObjectValue arrayProto = new ObjectValue();
    private ObjectValue boolProto = new ObjectValue();
    private ObjectValue funcProto = new ObjectValue();
    private ObjectValue numProto = new ObjectValue();
    private ObjectValue objProto = new ObjectValue(PlaceholderProto.NONE);
    private ObjectValue strProto = new ObjectValue();
    private ObjectValue symProto = new ObjectValue();
    private ObjectValue errProto = new ObjectValue();
    private ObjectValue syntaxErrProto = new ObjectValue(PlaceholderProto.ERROR);
    private ObjectValue typeErrProto = new ObjectValue(PlaceholderProto.ERROR);
    private ObjectValue rangeErrProto = new ObjectValue(PlaceholderProto.ERROR);
    private NativeTypeRegister typeRegister;
    private Thread thread;

    private LinkedBlockingDeque<Task> macroTasks = new LinkedBlockingDeque<>();
    private LinkedBlockingDeque<Task> microTasks = new LinkedBlockingDeque<>();

    public final int id = ++nextId;
    public final DebugState debugState = new DebugState();

    public ObjectValue arrayProto() { return arrayProto; }
    public ObjectValue booleanProto() { return boolProto; }
    public ObjectValue functionProto() { return funcProto; }
    public ObjectValue numberProto() { return numProto; }
    public ObjectValue objectProto() { return objProto; }
    public ObjectValue stringProto() { return strProto; }
    public ObjectValue symbolProto() { return symProto; }
    public ObjectValue errorProto() { return errProto; }
    public ObjectValue syntaxErrorProto() { return syntaxErrProto; }
    public ObjectValue typeErrorProto() { return typeErrProto; }
    public ObjectValue rangeErrorProto() { return rangeErrProto; }

    public GlobalScope global() { return global; }
    public NativeTypeRegister typeRegister() { return typeRegister; }

    public void copyFrom(Engine other) {
        global = other.global;
        typeRegister = other.typeRegister;
        arrayProto = other.arrayProto;
        boolProto = other.boolProto;
        funcProto = other.funcProto;
        numProto = other.numProto;
        objProto = other.objProto;
        strProto = other.strProto;
        symProto = other.symProto;
        errProto = other.errProto;
        syntaxErrProto = other.syntaxErrProto;
        typeErrProto = other.typeErrProto;
        rangeErrProto = other.rangeErrProto;
    }

    private void runTask(Task task) throws InterruptedException {
        try {
            FunctionValue func;
            if (task.func instanceof FunctionValue) func = (FunctionValue)task.func;
            else {
                var raw = (RawFunction)task.func;
                func = compile(raw.scope, raw.filename, raw.raw);
            }

            task.notifier.next(func.call(context().mergeData(task.data), task.thisArg, task.args));
        }
        catch (InterruptedException e) {
            task.notifier.error(new RuntimeException(e));
            throw e;
        }
        catch (EngineException e) {
            task.notifier.error(e);
        }
        catch (RuntimeException e) {
            task.notifier.error(e);
            e.printStackTrace();
        }
    }
    private void run() {
        while (true) {
            try {
                runTask(macroTasks.take());

                while (!microTasks.isEmpty()) {
                    runTask(microTasks.take());
                }
            }
            catch (InterruptedException e) {
                for (var msg : macroTasks) {
                    msg.notifier.error(new RuntimeException(e));
                }
                break;
            }
        }
    }

    public void exposeClass(String name, Class<?> clazz) {
        global.define(name, true, typeRegister.getConstr(clazz));
    }
    public void exposeNamespace(String name, Class<?> clazz) {
        global.define(name, true, NativeTypeRegister.makeNamespace(clazz));
    }

    public Thread start() {
        if (this.thread == null) {
            this.thread = new Thread(this::run, "JavaScript Runner #" + id);
            this.thread.start();
        }
        return this.thread;
    }
    public void stop() {
        thread.interrupt();
        thread = null;
    }
    public boolean inExecThread() {
        return Thread.currentThread() == thread;
    }
    public boolean isRunning() {
        return this.thread != null;
    }

    public Object makeRegex(String pattern, String flags) {
        throw EngineException.ofError("Regular expressions not supported.");
    }
    public ModuleManager modules() {
        return null;
    }
    public ObjectValue getPrototype(Class<?> clazz) {
        return typeRegister.getProto(clazz);
    }
    public CallContext context() { return new CallContext(this).mergeData(callCtxVals); }

    public Awaitable<Object> pushMsg(boolean micro, FunctionValue func, Map<DataKey<?>, Object> data, Object thisArg, Object... args) {
        var msg = new Task(func, data, thisArg, args);
        if (micro) microTasks.addLast(msg);
        else macroTasks.addLast(msg);
        return msg.notifier;
    }
    public Awaitable<Object> pushMsg(boolean micro, GlobalScope scope, Map<DataKey<?>, Object> data, String filename, String raw, Object thisArg, Object... args) {
        var msg = new Task(new RawFunction(scope, filename, raw), data, thisArg, args);
        if (micro) microTasks.addLast(msg);
        else macroTasks.addLast(msg);
        return msg.notifier;
    }

    public CodeFunction compile(GlobalScope scope, String filename, String raw) throws InterruptedException {
        return Parsing.compile(scope, filename, raw);
    }

    public Engine(NativeTypeRegister register) {
        this.typeRegister = register;
        this.callCtxVals.put(DEBUG_STATE_KEY, debugState);
    }
    public Engine() {
        this(new NativeTypeRegister());
    }
}
