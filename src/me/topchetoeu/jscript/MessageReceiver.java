package me.topchetoeu.jscript;

public interface MessageReceiver {
    void sendMessage(String msg);
    void sendError(String msg);
}