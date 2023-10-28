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
        try {
            StringBuilder out = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            for(var line = br.readLine(); line != null; line = br.readLine()) {
                out.append(line).append('\n');
            }

            br.close();
            return out.toString();
        }
        catch (Throwable e) { throw new UncheckedException(e); }
    }
    public static String resourceToString(String name) {
        var str = Main.class.getResourceAsStream("/me/topchetoeu/jscript/" + name);
        if (str == null) return null;
        return streamToString(str);
    }
}
