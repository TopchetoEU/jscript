package me.topchetoeu.jscript.events;

public class Notifier {
    private boolean ok = false;

    public synchronized void next() {
        ok = true;
        notifyAll();
    }
    public synchronized void await() throws InterruptedException {
        while (!ok) wait();
        ok = false;
    }
}
