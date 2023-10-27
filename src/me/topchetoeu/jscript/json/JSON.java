package me.topchetoeu.jscript.json;

import java.util.List;
import java.util.stream.Collectors;

import me.topchetoeu.jscript.Filename;
import me.topchetoeu.jscript.exceptions.SyntaxException;
import me.topchetoeu.jscript.parsing.Operator;
import me.topchetoeu.jscript.parsing.ParseRes;
import me.topchetoeu.jscript.parsing.Parsing;
import me.topchetoeu.jscript.parsing.Token;

public class JSON {
    public static ParseRes<String> parseIdentifier(List<Token> tokens, int i) {
        return Parsing.parseIdentifier(tokens, i);
    }
    public static ParseRes<String> parseString(Filename filename, List<Token> tokens, int i) {
        var res = Parsing.parseString(filename, tokens, i);
        if (res.isSuccess()) return ParseRes.res((String)res.result.value, res.n);
        else return res.transform();
    }
    public static ParseRes<Double> parseNumber(Filename filename, List<Token> tokens, int i) {
        var res = Parsing.parseNumber(filename, tokens, i);
        if (res.isSuccess()) return ParseRes.res((Double)res.result.value, res.n);
        else return res.transform();
    }
    public static ParseRes<Boolean> parseBool(Filename filename, List<Token> tokens, int i) {
        var id = parseIdentifier(tokens, i);

        if (!id.isSuccess()) return ParseRes.failed();
        else if (id.result.equals("true")) return ParseRes.res(true, 1);
        else if (id.result.equals("false")) return ParseRes.res(false, 1);
        else return ParseRes.failed();
    }

    public static ParseRes<?> parseValue(Filename filename, List<Token> tokens, int i) {
        return ParseRes.any(
            parseString(filename, tokens, i),
            parseNumber(filename, tokens, i),
            parseBool(filename, tokens, i),
            parseMap(filename, tokens, i),
            parseList(filename, tokens, i)
        );
    }

    public static ParseRes<JSONMap> parseMap(Filename filename, List<Token> tokens, int i) {
        int n = 0;
        if (!Parsing.isOperator(tokens, i + n++, Operator.BRACE_OPEN)) return ParseRes.failed();

        var values = new JSONMap();

        while (true) {
            if (Parsing.isOperator(tokens, i + n, Operator.BRACE_CLOSE)) {
                n++;
                break;
            }

            var name = ParseRes.any(
                parseIdentifier(tokens, i + n),
                parseString(filename, tokens, i + n),
                parseNumber(filename, tokens, i + n)
            );
            if (!name.isSuccess()) return ParseRes.error(Parsing.getLoc(filename, tokens, i + n), "Expected an index.", name);
            else n += name.n;

            if (!Parsing.isOperator(tokens, i + n, Operator.COLON)) {
                return ParseRes.error(Parsing.getLoc(filename, tokens, i + n), "Expected a colon.", name);
            }
            n++;

            var res = parseValue(filename, tokens, i + n);
            if (!res.isSuccess()) return ParseRes.error(Parsing.getLoc(filename, tokens, i + n), "Expected a list element.", res);
            else n += res.n;

            values.put(name.result.toString(), JSONElement.of(res.result));

            if (Parsing.isOperator(tokens, i + n, Operator.COMMA)) n++;
            else if (Parsing.isOperator(tokens, i + n, Operator.BRACE_CLOSE)) {
                n++;
                break;
            }
        }

        return ParseRes.res(values, n);
    }
    public static ParseRes<JSONList> parseList(Filename filename, List<Token> tokens, int i) {
        int n = 0;
        if (!Parsing.isOperator(tokens, i + n++, Operator.BRACKET_OPEN)) return ParseRes.failed();

        var values = new JSONList();

        while (true) {
            if (Parsing.isOperator(tokens, i + n, Operator.BRACKET_CLOSE)) {
                n++;
                break;
            }

            var res = parseValue(filename, tokens, i + n);
            if (!res.isSuccess()) return ParseRes.error(Parsing.getLoc(filename, tokens, i + n), "Expected a list element.", res);
            else n += res.n;

            values.add(JSONElement.of(res.result));

            if (Parsing.isOperator(tokens, i + n, Operator.COMMA)) n++;
            else if (Parsing.isOperator(tokens, i + n, Operator.BRACKET_CLOSE)) {
                n++;
                break;
            }
        }

        return ParseRes.res(values, n);
    }
    public static JSONElement parse(Filename filename, String raw) {
        var res = parseValue(filename, Parsing.tokenize(filename, raw), 0);
        if (res.isFailed()) throw new SyntaxException(null, "Invalid JSON given.");
        else if (res.isError()) throw new SyntaxException(null, res.error);
        else return JSONElement.of(res.result);
    }

    public static String stringify(JSONElement el) {
        if (el.isNumber()) return Double.toString(el.number());
        if (el.isBoolean()) return el.bool() ? "true" : "false";
        if (el.isNull()) return "null";
        if (el.isString()) return "\"" + el.string()
            .replace("\\", "\\\\")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\"", "\\\"")
        + "\"";
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
                res.append(stringify(JSONElement.string(entries.get(i).getKey())));
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
