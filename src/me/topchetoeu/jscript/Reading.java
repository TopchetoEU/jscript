package me.topchetoeu.jscript;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import me.topchetoeu.jscript.exceptions.UncheckedException;

public class Reading {
    private static final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    public static synchronized String read() throws IOException {
        return reader.readLine();
    }

    public static String streamToString(InputStream in) {
        try { return new String(in.readAllBytes()); }
        catch (Throwable e) { throw new UncheckedException(e); }
    }
    public static InputStream resourceToStream(String name) {
        return Reading.class.getResourceAsStream("/assets/" + name);
    }
    public static String resourceToString(String name) {
        return streamToString(resourceToStream(name));
    }
}
