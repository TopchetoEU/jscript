package me.topchetoeu.j2s.common;

import java.io.File;

/**
 * The class that represents all filenames in J2S
 */
public class Filename {
	/**
	 * The protocol of the filename (file://, http://, ftp://, etc...)
	 */
	public final String protocol;
	/**
	 * The path to the file (/home/me/test.js, example.org/test.js, etc...)
	 */
	public final String path;

	@Override public String toString() {
		return protocol + "://" + path;
	}
	@Override public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + protocol.hashCode();
		result = prime * result + path.hashCode();
		return result;
	}
	@Override public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;

		var other = (Filename) obj;

		if (protocol == null) {
			if (other.protocol != null) return false;
		}
		else if (!protocol.equals(other.protocol)) return false;

		if (path == null) {
			if (other.path != null) return false;
		}
		else if (!path.equals(other.path)) return false;
		return true;
	}

	public Filename(String protocol, String path) {
		path = path.trim();
		protocol = protocol.trim();
		this.protocol = protocol;
		this.path = path;
	}

	/**
	 * Parses the given string to a filename.
	 * If a :// is not found, the protocol will default to "file"
	 */
	public static Filename parse(String val) {
		var i = val.indexOf("://");
		if (i >= 0) return new Filename(val.substring(0, i).trim(), val.substring(i + 3).trim());
		else return new Filename("file", val.trim());
	}
	/**
	 * Will convert the File instance to a filename, with the protocol set to "file"
	 */
	public static Filename fromFile(File file) {
		return new Filename("file", file.getAbsolutePath());
	}
}
