package me.topchetoeu.jscript.common.events;

import me.topchetoeu.jscript.core.exceptions.InterruptException;

public class Notifier {
    private boolean ok = false;

    public synchronized void next() {
        ok = true;
        notifyAll();
    }
    public synchronized void await() {
        try {
            while (!ok) wait();
            ok = false;
        }
        catch (InterruptedException e) { throw new InterruptException(e); }
    }
}
