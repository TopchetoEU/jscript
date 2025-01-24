package me.topchetoeu.j2s.common;

public class Metadata {
	private static String VERSION;
	private static String AUTHOR;
	private static String NAME;

	static {
		var raw = Reading.resourceToString("metadata").split("\n");
		var line = 0;
		var file = "internal://metadata";

		for (var el : raw) {
			line++;

			el = el.trim();
			if (el.startsWith("#")) continue;
			if (el.isEmpty()) continue;

			var i = el.indexOf(":");
			if (i < 0) throw new RuntimeException(String.format("%s:%s: Expected colon on line", file, line));

			var name = el.substring(0, i).trim();
			var value = el.substring(i + 1).trim();

			switch (name) {
				case "version":
					VERSION = value;
					break;
				case "author":
					AUTHOR = value;
					break;
				case "name":
					NAME = value;
					break;
				default:
					throw new RuntimeException(String.format("%s:%s: Unexpected metadata key '%s'", file, line, name));
			}
		}

		if (VERSION == null) throw new RuntimeException(String.format("%s:%s: No version specified", file, line));
		if (AUTHOR == null) throw new RuntimeException(String.format("%s:%s: No author specified", file, line));
		if (NAME == null) throw new RuntimeException(String.format("%s:%s: No name specified", file, line));
	}

	public static String version() {
		if (VERSION.equals("${VERSION}")) return "1337-devel";
		else return VERSION;
	}
	public static String author() {
		if (AUTHOR.equals("${AUTHOR}")) return "anonymous";
		else return AUTHOR;
	}
	public static String name() {
		if (NAME.equals("${NAME}")) return "some-product";
		else return NAME;
	}
}
