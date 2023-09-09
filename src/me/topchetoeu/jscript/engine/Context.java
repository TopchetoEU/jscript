package me.topchetoeu.jscript.engine;

public class Context {
    public final FunctionContext function;
    public final MessageContext message;

    public Context(FunctionContext funcCtx, MessageContext msgCtx) {
        this.function = funcCtx;
        this.message = msgCtx;
    }
}
