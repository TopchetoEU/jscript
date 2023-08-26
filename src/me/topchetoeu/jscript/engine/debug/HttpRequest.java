package me.topchetoeu.jscript.engine.debug;

import java.util.Map;

public class HttpRequest {
    public final String method;
    public final String path;
    public final Map<String, String> headers;

    public HttpRequest(String method, String path, Map<String, String> headers) {
        this.method = method;
        this.path = path;
        this.headers = headers;
    }
}

