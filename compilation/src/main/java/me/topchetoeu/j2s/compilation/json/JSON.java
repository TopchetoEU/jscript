package me.topchetoeu.j2s.compilation.json;

import java.util.HashMap;
import java.util.stream.Collectors;

import me.topchetoeu.j2s.common.Filename;
import me.topchetoeu.j2s.common.Location;
import me.topchetoeu.j2s.common.Metadata;
import me.topchetoeu.j2s.common.StringifyUtils;
import me.topchetoeu.j2s.common.SyntaxException;
import me.topchetoeu.j2s.compilation.parsing.ParseRes;
import me.topchetoeu.j2s.compilation.parsing.Parsing;
import me.topchetoeu.j2s.compilation.parsing.Source;

public class JSON {
	public static ParseRes<JSONElement> parseString(Source src, int i) {
		var res = Parsing.parseString(src, i);
		if (!res.isSuccess()) return res.chainError();
		return ParseRes.res(JSONElement.string(res.result), res.n);
	}
	public static ParseRes<JSONElement> parseNumber(Source src, int i) {
		var res = Parsing.parseNumber(src, i, true);
		if (!res.isSuccess()) return res.chainError();
		else return ParseRes.res(JSONElement.number(res.result), res.n);
	}
	public static ParseRes<JSONElement> parseLiteral(Source src, int i) {
		var id = Parsing.parseIdentifier(src, i);

		if (!id.isSuccess()) return ParseRes.failed();
		else if (id.result.equals("true")) return ParseRes.res(JSONElement.bool(true), id.n);
		else if (id.result.equals("false")) return ParseRes.res(JSONElement.bool(false), id.n);
		else if (id.result.equals("null")) return ParseRes.res(JSONElement.NULL, id.n);
		else return ParseRes.failed();
	}

	public static ParseRes<JSONElement> parseValue(Source src, int i) {
		return ParseRes.first(src, i,
			JSON::parseString,
			JSON::parseNumber,
			JSON::parseLiteral,
			JSON::parseMap,
			JSON::parseList
		);
	}

	public static ParseRes<JSONElement> parseMap(Source src, int i) {
		var n = Parsing.skipEmpty(src, i);

		if (!src.is(i + n, "{")) return ParseRes.failed();
		n++;

		var values = new JSONMap();

		if (src.is(i + n, "}")) return ParseRes.res(JSONElement.map(new JSONMap(new HashMap<>())), n + 1);
		while (true) {
			var name = parseString(src, i + n);
			if (!name.isSuccess()) return name.chainError(src.loc(i + n), "Expected an index");
			n += name.n;
			n += Parsing.skipEmpty(src, i + n);

			if (!src.is(i + n, ":")) return name.chainError(src.loc(i + n), "Expected a colon");
			n++;

			var res = parseValue(src, i + n);
			if (!res.isSuccess()) return res.chainError(src.loc(i + n), "Expected a list element");
			values.put(name.result.toString(), res.result);
			n += res.n;
			n += Parsing.skipEmpty(src, i + n);

			if (src.is(i + n, ",")) n++;
			else if (src.is(i + n, "}")) {
				n++;
				break;
			}
		}

		return ParseRes.res(JSONElement.map(values), n);
	}
	public static ParseRes<JSONElement> parseList(Source src, int i) {
		var n = Parsing.skipEmpty(src, i);

		if (!src.is(i + n++, "[")) return ParseRes.failed();

		var values = new JSONList();

		if (src.is(i + n, "]")) return ParseRes.res(JSONElement.list(new JSONList()), n + 1);
		while (true) {
			var res = parseValue(src, i + n);
			if (!res.isSuccess()) return res.chainError(src.loc(i + n), "Expected a list element");
			values.add(res.result);
			n += res.n;
			n += Parsing.skipEmpty(src, i + n);

			if (src.is(i + n, ",")) n++;
			else if (src.is(i + n, "]")) {
				n++;
				break;
			}
		}

		return ParseRes.res(JSONElement.list(values), n);
	}
	public static JSONElement parse(Filename filename, String raw) {
		if (filename == null) filename = new Filename(Metadata.name(), "json");

		var res = parseValue(new Source(null, filename, raw), 0);
		if (res.isFailed()) throw new SyntaxException(Location.of(filename, 0, 0), "Invalid JSON given");
		else if (res.isError()) throw new SyntaxException(res.errorLocation, res.error);
		else return JSONElement.of(res.result);
	}

	public static String stringify(JSONElement el) {
		if (el.isNumber()) return StringifyUtils.quoteNumber(el.number());
		if (el.isString()) return StringifyUtils.quoteString(el.string());
		if (el.isBoolean()) return el.bool() ? "true" : "false";
		if (el.isNull()) return "null";
		if (el.isList()) {
			var res = new StringBuilder().append("[");
			for (int i = 0; i < el.list().size(); i++) {
				if (i != 0) res.append(",");
				res.append(stringify(el.list().get(i)));
			}
			res.append("]");
			return res.toString();
		}
		if (el.isMap()) {
			var res = new StringBuilder().append("{");
			var entries = el.map().entrySet().stream().collect(Collectors.toList());

			for (int i = 0; i < entries.size(); i++) {
				if (i != 0) res.append(",");
				res.append(StringifyUtils.quoteString(entries.get(i).getKey()));
				res.append(":");
				res.append(stringify(entries.get(i).getValue()));
			}
			res.append("}");
			return res.toString();
		}
		return null;
	}
	public static String stringify(JSONMap map) {
		return stringify(JSONElement.of(map));
	}
	public static String stringify(JSONList list) {
		return stringify(JSONElement.of(list));
	}
}
