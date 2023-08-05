package me.topchetoeu.jscript.engine.debug;

import java.util.Map;

public record HttpRequest(String method, String path, Map<String, String> headers) {}

