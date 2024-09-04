package me.topchetoeu.jscript.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;

public class Reading {
    private static final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    public static synchronized String readline() throws IOException {
        return reader.readLine();
    }

    public static String streamToString(InputStream in) {
        try {
            return new String(in.readAllBytes());
        }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }
    public static InputStream resourceToStream(String name) {
        return Reading.class.getResourceAsStream("/" + name);
    }
    public static String resourceToString(String name) {
        return streamToString(resourceToStream(name));
    }
}
