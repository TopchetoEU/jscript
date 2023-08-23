package me.topchetoeu.jscript.parsing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.*;
import me.topchetoeu.jscript.compilation.Instruction.Type;
import me.topchetoeu.jscript.compilation.VariableDeclareStatement.Pair;
import me.topchetoeu.jscript.compilation.control.*;
import me.topchetoeu.jscript.compilation.control.SwitchStatement.SwitchCase;
import me.topchetoeu.jscript.compilation.values.*;
import me.topchetoeu.jscript.engine.scope.GlobalScope;
import me.topchetoeu.jscript.engine.scope.ValueVariable;
import me.topchetoeu.jscript.engine.values.CodeFunction;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.exceptions.SyntaxException;
import me.topchetoeu.jscript.parsing.ParseRes.State;

// TODO: this has to be rewritten
public class Parsing {
    public static interface Parser<T> {
        ParseRes<T> parse(String filename, List<Token> tokens, int i);
    }

    private static record ObjProp(Object name, String access, FunctionStatement func) {}
    private static final HashSet<String> reserved = new HashSet<String>();
    static {
        reserved.add("true");
        reserved.add("false");
        reserved.add("void");
        reserved.add("null");
        reserved.add("this");
        reserved.add("NaN");
        reserved.add("Infinity");
        reserved.add("if");
        reserved.add("else");
        reserved.add("try");
        reserved.add("catch");
        reserved.add("finally");
        reserved.add("for");
        reserved.add("do");
        reserved.add("while");
        reserved.add("switch");
        reserved.add("case");
        reserved.add("default");
        reserved.add("new");
        reserved.add("function");
        reserved.add("var");
        reserved.add("return");
        reserved.add("throw");
        reserved.add("typeof");
        reserved.add("delete");
        reserved.add("break");
        reserved.add("continue");
        reserved.add("debug");
        reserved.add("let");
        reserved.add("implements");
        reserved.add("interface");
        reserved.add("package");
        reserved.add("private");
        reserved.add("protected");
        reserved.add("public");
        reserved.add("static");
        reserved.add("yield");
        // Although the standards allow it, these are keywords in newer ES, so we won't allow them
        reserved.add("const");
        // reserved.add("await");
        reserved.add("async");
        // These are allowed too, however our parser considers them keywords
        reserved.add("undefined");
        reserved.add("arguments");
        reserved.add("globalThis");
        reserved.add("window");
        reserved.add("self");
    }


    public static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
    public static boolean isWhitespace(char c) {
        return isAny(c, " \t\r\n");
    }
    public static boolean isLetter(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }
    public static boolean isAlphanumeric(char c) {
        return isLetter(c) || isDigit(c);
    }
    public static boolean isAny(char c, String alphabet) {
        return alphabet.contains(Character.toString(c));
    }

    private static final int CURR_NONE = 0;
    private static final int CURR_NUMBER = 1;
    private static final int CURR_FLOAT = 11;
    private static final int CURR_SCIENTIFIC_NOT = 12;
    private static final int CURR_NEG_SCIENTIFIC_NOT = 13;
    private static final int CURR_HEX = 14;
    private static final int CURR_STRING = 2;
    private static final int CURR_LITERAL = 3;
    private static final int CURR_OPERATOR = 4;
    private static final int CURR_REGEX = 6;
    private static final int CURR_REGEX_FLAGS = 7;
    private static final int CURR_MULTI_COMMENT = 8;
    private static final int CURR_SINGLE_COMMENT = 9;

    private static void addToken(StringBuilder currToken, int currStage, int line, int lastStart, String filename, List<RawToken> tokens) {
        var res = currToken.toString();

        switch (currStage) {
            case CURR_STRING: tokens.add(new RawToken(res, TokenType.STRING, line, lastStart)); break;
            case CURR_REGEX_FLAGS: tokens.add(new RawToken(res, TokenType.REGEX, line, lastStart)); break;
            case CURR_NUMBER:
            case CURR_HEX:
            case CURR_NEG_SCIENTIFIC_NOT:
            case CURR_SCIENTIFIC_NOT:
            case CURR_FLOAT:
                tokens.add(new RawToken(res, TokenType.NUMBER, line, lastStart)); break;
            case CURR_LITERAL: tokens.add(new RawToken(res, TokenType.LITERAL, line, lastStart)); break;
            case CURR_OPERATOR: tokens.add(new RawToken(res, TokenType.OPERATOR, line, lastStart)); break;
        }

        currToken.delete(0, currToken.length());
    }

    // This method is so long because we're tokenizing the string using an iterative approach
    // instead of a recursive descent parser. This is mainly done for performance reasons.
    private static ArrayList<RawToken> splitTokens(String filename, String raw) {
        var tokens = new ArrayList<RawToken>();
        var currToken = new StringBuilder(64);

        // Those are state variables, and will be reset every time a token has ended parsing
        boolean lastEscape = false, inBrackets = false;

        int line = 1, start = 1, lastStart = 1, parenI = 0;
        var loc = new Location(line, lastStart, filename);
        int currStage = CURR_NONE;

        // when we want to continue parsing a token, we will execute continue;, which will skip
        // the token end logic
        loop: for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);

            start++;

            switch (currStage) {
                case CURR_STRING:
                    currToken.append(c);

                    if (!lastEscape) {
                        if (c == '\n') throw new SyntaxException(loc, "Can't have a multiline string.");
                        else if (c == '\\') {
                            lastEscape = true;
                            continue;
                        }
                        else if (c != currToken.charAt(0)) continue;
                    }
                    else {
                        lastEscape = false;
                        continue;
                    }
                    break;
                case CURR_REGEX:
                    currToken.append(c);
                    if (!lastEscape) {
                        if (c == '\\') lastEscape = true;
                        if (c == '/' & parenI == 0 & !inBrackets) {
                            currStage = CURR_REGEX_FLAGS;
                            continue;
                        }
                        if (c == '[') inBrackets = true;
                        if (c == ']') inBrackets = false;
                        if (c == '(' && !inBrackets) parenI++;
                        if (c == ')' && !inBrackets) parenI--;
                    }
                    else lastEscape = false;
                    continue;
                case CURR_REGEX_FLAGS:
                    if (isAny(c, "dgimsuy")) {
                        currToken.append(c);
                        continue;
                    }
                    i--; start--;
                    break;
                case CURR_NUMBER:
                    if (c == '.') currStage = CURR_FLOAT;
                    else if (c == 'e' || c == 'E') currStage = CURR_SCIENTIFIC_NOT;
                    else if ((c == 'x' || c == 'X') && currToken.toString().equals("0")) currStage = CURR_HEX;
                    else if (!isDigit(c)) {
                        i--; start--;
                        break;
                    }
                    currToken.append(c);
                    continue;
                case CURR_FLOAT:
                    if (c == 'e' || c == 'E') currStage = CURR_SCIENTIFIC_NOT;
                    else if (!isDigit(c)) {
                        i--; start--;
                        break;
                    }
                    currToken.append(c);
                    continue;
                case CURR_SCIENTIFIC_NOT:
                    if (c == '-') currStage = CURR_NEG_SCIENTIFIC_NOT;
                    else if (!isDigit(c)) {
                        i--; start--;
                        break;
                    }
                    currToken.append(c);
                    continue;
                case CURR_NEG_SCIENTIFIC_NOT:
                    if (isDigit(c)) currToken.append(c);
                    else {
                        i--; start--;
                        break;
                    }
                    continue;
                case CURR_HEX:
                    if (isDigit(c) || isAny(c, "ABCDEFabcdef")) currToken.append(c);
                    else {
                        i--; start--;
                        break;
                    }
                    continue;
                case CURR_SINGLE_COMMENT:
                    currToken.delete(0, currToken.length());
                    if (c != '\n') continue;
                    else {
                        line++;
                        start = 1;
                    }
                    break;
                case CURR_MULTI_COMMENT:
                    if (c == '\n') line++;
                    if (!(currToken.charAt(0) == '*' && c == '/')) {
                        currToken.delete(0, currToken.length());
                        currToken.append(c);
                        continue;
                    }
                    break;
                case CURR_LITERAL:
                    if (isAlphanumeric(c) || c == '_') {
                        currToken.append(c);
                        continue;
                    }
                    else { i--; start--; }
                    break;
                case CURR_OPERATOR: {
                    // here we do several things:
                    // - detect a comment
                    // - detect a regular expression
                    // - detect a float number (.xxxx)
                    // - read an operator greedily

                    // this variable keeps track of whether we're still reading an operator
                    boolean ok = false;
                    if (currToken.length() == 1) {
                        // double operators
                        if (currToken.charAt(0) == c && isAny(c, "&|=+-<>")) ok = true;
                        // assignments
                        else if (c == '=' && isAny(currToken.charAt(0), "&|^+-/*%!<>")) ok = true;
                        // detect float numbers
                        else if (isDigit(c) && currToken.charAt(0) == '.') {
                            currStage = CURR_FLOAT;
                            currToken.append(c);
                            continue;
                        }
                        else if (currToken.charAt(0) == '/') {
                            // single line comments
                            if (c == '/') {
                                currStage = CURR_SINGLE_COMMENT;
                                continue;
                            }
                            // multiline comments
                            else if (c == '*') {
                                currStage = CURR_MULTI_COMMENT;
                                continue;
                            }
                            // regular expressions
                            else {
                                // regular expressions must be in the start of a file, or be followed by a
                                // newline, or an operator
                                // this is because of expressions like 1 / 2 / 3 (/ 2 /) will get recognized as regex
                                // still, the closing paren must be ignored, because in an expression, we can't have a value, following a paren
                                var prevToken = tokens.size() == 0 ? null : tokens.get(tokens.size() - 1);
                                if (tokens.size() == 0 || (
                                    prevToken.line < line ||
                                    prevToken.type == TokenType.OPERATOR && !prevToken.value.equals(")") ||
                                    prevToken.value.equals("return") ||
                                    prevToken.value.equals("throe")
                                )) {
                                    // we look for a second / on the same line
                                    // if we don't find one, we determine the current operator
                                    // to be a division
                                    for (int j = i; j < raw.length(); j++) {
                                        if (raw.charAt(j) == '/') {
                                            i--; start--;
                                            currStage = CURR_REGEX;
                                            continue loop;
                                        }
                                        if (raw.charAt(j) == '\n') break;
                                    }
                                }
                            }
                        }
                    }
                    if (currToken.length() == 2) {
                        var a = currToken.charAt(0);
                        var b = currToken.charAt(1);
                        if ((
                            a == '=' && b == '=' ||
                            a == '!' && b == '=' ||
                            a == '<' && b == '<' ||
                            a == '>' && b == '>' ||
                            a == '>' && b == '>'
                        ) && c == '=') ok = true;
                        if (a == '>' && b == '>' && c == '>') ok = true;
                    }
                    if (
                        currToken.length() == 3 &&
                        currToken.charAt(0) == '>' &&
                        currToken.charAt(1) == '>' &&
                        currToken.charAt(2) == '>' &&
                        c == '='
                    ) ok = true;

                    if (ok) {
                        currToken.append(c);
                        continue;
                    }
                    else { i--; start--; }
                    break;
                }
                default:
                    // here we detect what type of token we're reading
                    if (isAny(c, " \t\n\r")) {
                        if (c == '\n') {
                            line++;
                            start = 1;
                        }
                    }
                    else if (isDigit(c)) {
                        currToken.append(c);
                        currStage = CURR_NUMBER;
                        continue;
                    }
                    else if (isAlphanumeric(c) || c == '_' || c == '$') {
                        currToken.append(c);
                        currStage = CURR_LITERAL;
                        continue;
                    }
                    else if (isAny(c, "+-/*%=!&|^(){}[];.,<>!:~?")) {
                        currToken.append(c);
                        currStage = CURR_OPERATOR;
                        continue;
                    }
                    else if (c == '"' || c == '\'') {
                        currToken.append(c);
                        currStage = CURR_STRING;
                        continue;
                    }
                    else throw new SyntaxException(new Location(line, start, filename), "Unrecognized character %s.".formatted(c));
            }

            // if we got here, we know that we have encountered the end of a token
            addToken(currToken, currStage, line, lastStart, filename, tokens);
            lastEscape = inBrackets = false;
            currStage = CURR_NONE;
            lastStart = start;
        }

        // here, we save a leftover token (if any)
        switch (currStage) {
            case CURR_STRING: throw new SyntaxException(new Location(line, start, filename), "Unterminated string literal.");
            case CURR_REGEX: throw new SyntaxException(new Location(line, start, filename), "Incomplete regex.");
        }
        addToken(currToken, currStage, line, lastStart, filename, tokens);

        return tokens;
    }

    private static int fromHex(char c) {
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        if (c >= '0' && c <= '9') return c - '0';
        return -1;
    }

    private static String parseString(Location loc, String literal) {
        var res = new StringBuilder();

        for (var i = 1; i < literal.length() - 1; i++) {
            if (literal.charAt(i) == '\\') {
                char c = literal.charAt(++i);
                if (c == 'b') res.append('\b');
                else if (c == 't') res.append('\t');
                else if (c == 'n') res.append('\n');
                else if (c == 'f') res.append('\f');
                else if (c == 'r') res.append('\r');
                else if (c == '0') {
                    if (i + 1 >= literal.length()) res.append((char)0);
                    c = literal.charAt(i + 1);
                    if (c >= '0' && c <= '9') throw new SyntaxException(loc.add(i), "Octal escape sequences not allowed.");
                    res.append((char)0);
                }
                else if (c >= '1' && c <= '9') {
                    throw new SyntaxException(loc.add(i), "Octal escape sequences not allowed.");
                }
                else if (c == 'x') {
                    var newC = 0;
                    i++;
                    for (var j = 0; j < 2; j++) {
                        if (i >= literal.length()) throw new SyntaxException(loc.add(i), "Incomplete unicode escape sequence.");
                        int val = fromHex(literal.charAt(i++));
                        if (val == -1) throw new SyntaxException(loc.add(i + 1), "Invalid character in unicode escape sequence.");
                        newC = (newC << 4) | val;
                    }
                    i--;

                    res.append((char)newC);
                }
                else if (c == 'u') {
                    var newC = 0;
                    i++;
                    for (var j = 0; j < 4; j++) {
                        if (i >= literal.length()) throw new SyntaxException(loc.add(i), "Incomplete unicode escape sequence.");
                        int val = fromHex(literal.charAt(i++));
                        if (val == -1) throw new SyntaxException(loc.add(i + 1), "Invalid character in unicode escape sequence.");
                        newC = (newC << 4) | val;
                    }
                    i--;

                    res.append((char)newC);
                }
                else res.append(c);
            }
            else res.append(literal.charAt(i));
        }

        return res.toString();
    }
    private static String parseRegex(Location loc, String literal) {
        var res = new StringBuilder();

        int end = literal.lastIndexOf('/');

        for (var i = 1; i < end; i++) {
            if (literal.charAt(i) == '\\') {
                char c = literal.charAt(++i);
                if (c == 'b') res.append('\b');
                else if (c == 't') res.append('\t');
                else if (c == 'n') res.append('\n');
                else if (c == 'f') res.append('\f');
                else if (c == 'r') res.append('\r');
                else if (c == '0') {
                    if (i + 1 >= literal.length()) res.append((char)0);
                    c = literal.charAt(i + 1);
                    if (c >= '0' && c <= '9') throw new SyntaxException(loc.add(i), "Octal escape sequences not allowed.");
                    res.append((char)0);
                }
                else if (c >= '1' && c <= '9') {
                    res.append((char)(c - '0'));
                    i++;
                }
                else if (c == 'x') {
                    var newC = 0;
                    i++;
                    for (var j = 0; j < 2; j++) {
                        if (i >= literal.length()) throw new SyntaxException(loc.add(i), "Incomplete unicode escape sequence.");
                        int val = fromHex(literal.charAt(i++));
                        if (val == -1) throw new SyntaxException(loc.add(i + 1), "Invalid character in unicode escape sequence.");
                        newC = (newC << 4) | val;
                    }
                    i--;

                    res.append((char)newC);
                }
                else if (c == 'u') {
                    var newC = 0;
                    i++;
                    for (var j = 0; j < 4; j++) {
                        if (i >= literal.length()) throw new SyntaxException(loc.add(i), "Incomplete unicode escape sequence.");
                        int val = fromHex(literal.charAt(i++));
                        if (val == -1) throw new SyntaxException(loc.add(i + 1), "Invalid character in unicode escape sequence.");
                        newC = (newC << 4) | val;
                    }
                    i--;

                    res.append((char)newC);
                }
                else res.append("\\" + c);
            }
            else res.append(literal.charAt(i));
        }

        return '/' + res.toString() + literal.substring(end);
    }

    private static double parseHex(String literal) {
        double res = 0;

        for (int i = 2; i < literal.length(); i++) {
            res *= 16;
            res += fromHex(literal.charAt(i));
        }

        return res;
    }

    private static List<Token> parseTokens(String filename, Collection<RawToken> tokens) {
        var res = new ArrayList<Token>();

        for (var el : tokens) {
            var loc = new Location(el.line, el.start, filename);
            switch (el.type) {
                case LITERAL: res.add(Token.identifier(el.line, el.start, el.value)); break;
                case NUMBER:
                    if (el.value.startsWith("0x") || el.value.startsWith("0X")) {
                        if (el.value.endsWith("x") || el.value.endsWith("X")) {
                            throw new SyntaxException(loc, "Invalid number format.");
                        }
                        res.add(Token.number(el.line, el.start, parseHex(el.value))); break;
                    }
                    if (
                        el.value.endsWith("e") || el.value.endsWith("E") || el.value.endsWith("-")
                    ) throw new SyntaxException(loc, "Invalid number format.");
                    else res.add(Token.number(el.line, el.start, Double.parseDouble(el.value))); break;
                case OPERATOR:
                    Operator op = Operator.parse(el.value);
                    if (op == null) throw new SyntaxException(loc, "Unrecognized operator '%s'.".formatted(el.value));
                    res.add(Token.operator(el.line, el.start, op));
                    break;
                case STRING:
                    res.add(Token.string(el.line, el.start, parseString(loc, el.value)));
                    break;
                case REGEX:
                    res.add(Token.regex(el.line, el.start, parseRegex(loc, el.value)));
                    break;
            }
        }

        return res;
    }

    public static List<Token> tokenize(String filename, String raw) {
        return parseTokens(filename, splitTokens(filename, raw));
    }

    public static Location getLoc(String filename, List<Token> tokens, int i) {
        if (tokens.size() == 0 || tokens.size() == 0) return new Location(1, 1, filename);
        if (i >= tokens.size()) i = tokens.size() - 1;
        return new Location(tokens.get(i).line, tokens.get(i).start, filename);
    }
    public static int getLines(List<Token> tokens) {
        if (tokens.size() == 0) return 1;
        return tokens.get(tokens.size() - 1).line;
    }

    public static ParseRes<String> parseIdentifier(List<Token> tokens, int i) {
        try {
            if (tokens.get(i).isIdentifier()) {
                return ParseRes.res(tokens.get(i).identifier(), 1);
            }
            else return ParseRes.failed();
        }
        catch (IndexOutOfBoundsException e) {
            return ParseRes.failed();
        }
    }
    public static ParseRes<Operator> parseOperator(List<Token> tokens, int i) {
        try {
            if (tokens.get(i).isOperator()) {
                return ParseRes.res(tokens.get(i).operator(), 1);
            }
            else return ParseRes.failed();
        }
        catch (IndexOutOfBoundsException e) {
            return ParseRes.failed();
        }
    }

    public static boolean isIdentifier(List<Token> tokens, int i, String lit) {
        try {
            if (tokens.get(i).isIdentifier(lit)) {
                return true;
            }
            else return false;
        }
        catch (IndexOutOfBoundsException e) {
            return false;
        }
    }
    public static boolean isOperator(List<Token> tokens, int i, Operator op) {
        try {
            if (tokens.get(i).isOperator(op)) {
                return true;
            }
            else return false;
        }
        catch (IndexOutOfBoundsException e) {
            return false;
        }
    }
    public static boolean isStatementEnd(List<Token> tokens, int i) {
        if (isOperator(tokens, i, Operator.SEMICOLON)) return true;
        if (isOperator(tokens, i, Operator.BRACE_CLOSE)) return true;
        if (i < 0) return false;
        if (i >= tokens.size()) return true;
        return getLoc(null, tokens, i).line() > getLoc(null, tokens, i - 1).line();
    }
    public static boolean checkVarName(String name) {
        return !reserved.contains(name);
    }

    public static ParseRes<ConstantStatement> parseString(String filename, List<Token> tokens, int i) {
        var loc = getLoc(filename, tokens, i);
        try {
            if (tokens.get(i).isString()) {
                return ParseRes.res(new ConstantStatement(loc, tokens.get(i).string()), 1);
            }
            else return ParseRes.failed();
        }
        catch (IndexOutOfBoundsException e) {
            return ParseRes.failed();
        }
    }
    public static ParseRes<ConstantStatement> parseNumber(String filename, List<Token> tokens, int i) {
        var loc = getLoc(filename, tokens, i);
        try {
            if (tokens.get(i).isNumber()) {
                return ParseRes.res(new ConstantStatement(loc, tokens.get(i).number()), 1);
            }
            else return ParseRes.failed();
        }
        catch (IndexOutOfBoundsException e) {
            return ParseRes.failed();
        }

    }
    public static ParseRes<NewStatement> parseRegex(String filename, List<Token> tokens, int i) {
        var loc = getLoc(filename, tokens, i);
        try {
            if (tokens.get(i).isRegex()) {
                var val = tokens.get(i).regex();
                var index = val.lastIndexOf('/');
                var first = val.substring(1, index);
                var second = val.substring(index + 1);
                return ParseRes.res(new NewStatement(loc,
                    new VariableStatement(null, "RegExp"),
                    new ConstantStatement(loc, first),
                    new ConstantStatement(loc, second)
                ), 1);
            }
            else return ParseRes.failed();
        }
        catch (IndexOutOfBoundsException e) {
            return ParseRes.failed();
        }
    }

    public static ParseRes<ArrayStatement> parseArray(String filename, List<Token> tokens, int i) {
        var loc = getLoc(filename, tokens, i);
        int n = 0;
        if (!isOperator(tokens, i + n++, Operator.BRACKET_OPEN)) return ParseRes.failed();

        var values = new ArrayList<Statement>();

        // Java allows labels, so me uses labels
        loop: while (true) {
            if (isOperator(tokens, i + n, Operator.BRACKET_CLOSE)) {
                n++;
                break;
            }

            while (isOperator(tokens, i + n, Operator.COMMA)) {
                n++;
                values.add(null);
                if (isOperator(tokens, i + n, Operator.BRACKET_CLOSE)) {
                    n++;
                    break loop;
                }
            }

            var res = parseValue(filename, tokens, i + n, 2);
            if (!res.isSuccess()) return ParseRes.error(loc, "Expected an array element.", res);
            else n += res.n;

            values.add(res.result);

            if (isOperator(tokens, i + n, Operator.COMMA)) n++;
            else if (isOperator(tokens, i + n, Operator.BRACKET_CLOSE)) {
                n++;
                break;
            }
        }

        return ParseRes.res(new ArrayStatement(loc, values.toArray(Statement[]::new)), n);
    }

    public static ParseRes<List<String>> parseParamList(String filename, List<Token> tokens, int i) {
        var loc = getLoc(filename, tokens, i);
        int n = 0;

        if (!isOperator(tokens, i + n++, Operator.PAREN_OPEN)) return ParseRes.error(loc, "Expected a parameter list.");

        var args = new ArrayList<String>();

        if (isOperator(tokens, i + n, Operator.PAREN_CLOSE)) {
            n++;
        }
        else {
            while (true) {
                var argRes = parseIdentifier(tokens, i + n);
                if (argRes.isSuccess()) {
                    args.add(argRes.result);
                    n++;
                    if (isOperator(tokens, i + n, Operator.COMMA)) {
                        n++;
                    }
                    if (isOperator(tokens, i + n, Operator.PAREN_CLOSE)) {
                        n++;
                        break;
                    }
                }
                else return ParseRes.error(loc, "Expected an argument, comma or a closing brace.");
            }
        }

        return ParseRes.res(args, n);
    }

    public static ParseRes<? extends Object> parsePropName(String filename, List<Token> tokens, int i) {
        var idRes = parseIdentifier(tokens, i);
        if (idRes.isSuccess()) return ParseRes.res(idRes.result, 1);
        var strRes = parseString(null, tokens, i);
        if (strRes.isSuccess()) return ParseRes.res(strRes.result.value, 1);
        var numRes = parseNumber(null, tokens, i);
        if (numRes.isSuccess()) return ParseRes.res(numRes.result.value, 1);

        return ParseRes.failed();
    }
    public static ParseRes<ObjProp> parseObjectProp(String filename, List<Token> tokens, int i) {
        var loc = getLoc(filename, tokens, i);
        int n = 0;

        var accessRes = parseIdentifier(tokens, i + n++);
        if (!accessRes.isSuccess()) return ParseRes.failed();
        var access = accessRes.result;
        if (!access.equals("get") && !access.equals("set")) return ParseRes.failed();

        var nameRes = parsePropName(filename, tokens, i + n);
        if (!nameRes.isSuccess()) return ParseRes.error(loc, "Expected a property name after '" + access + "'.");
        var name = nameRes.result;
        n += nameRes.n;

        var argsRes = parseParamList(filename, tokens, i + n);
        if (!argsRes.isSuccess()) return ParseRes.error(loc, "Expected an argument list.", argsRes);
        n += argsRes.n;

        var res = parseCompound(filename, tokens, i + n);
        if (!res.isSuccess()) return ParseRes.error(loc, "Expected a compound statement for property accessor.", res);
        n += res.n;

        return ParseRes.res(new ObjProp(
            name, access,
            new FunctionStatement(loc, access + " " + name.toString(), argsRes.result.toArray(String[]::new), res.result)
        ), n);
    }
    public static ParseRes<ObjectStatement> parseObject(String filename, List<Token> tokens, int i) {
        var loc = getLoc(filename, tokens, i);
        int n = 0;
        if (!isOperator(tokens, i + n++, Operator.BRACE_OPEN)) return ParseRes.failed();

        var values = new LinkedHashMap<Object, Statement>();
        var getters = new LinkedHashMap<Object, FunctionStatement>();
        var setters = new LinkedHashMap<Object, FunctionStatement>();

        if (isOperator(tokens, i + n, Operator.BRACE_CLOSE)) {
            n++;
            return ParseRes.res(new ObjectStatement(loc, values, getters, setters), n);
        }

        while (true) {
            var propRes = parseObjectProp(filename, tokens, i + n);

            if (propRes.isSuccess()) {
                n += propRes.n;
                if (propRes.result.access.equals("set")) {
                    setters.put(propRes.result.name, propRes.result.func);
                }
                else {
                    getters.put(propRes.result.name, propRes.result.func);
                }
            }
            else {
                var nameRes = parsePropName(filename, tokens, i + n);
                if (!nameRes.isSuccess()) return ParseRes.error(loc, "Expected a field name.", propRes);
                n += nameRes.n;

                if (!isOperator(tokens, i + n++, Operator.COLON)) return ParseRes.error(loc, "Expected a colon.");

                var valRes = parseValue(filename, tokens, i + n, 2);
                if (!valRes.isSuccess()) return ParseRes.error(loc, "Expected a value in array list.", valRes);
                n += valRes.n;

                values.put(nameRes.result, valRes.result);
            }

            if (isOperator(tokens, i + n, Operator.COMMA)) {
                n++;
                if (isOperator(tokens, i + n, Operator.BRACE_CLOSE)) {
                    n++;
                    break;
                }
                continue;
            }
            else if (isOperator(tokens, i + n, Operator.BRACE_CLOSE)) {
                n++;
                break;
            }
            else ParseRes.error(loc, "Expected a comma or a closing brace.");
        }

        return ParseRes.res(new ObjectStatement(loc, values, getters, setters), n);
    }
    public static ParseRes<NewStatement> parseNew(String filename, List<Token> tokens, int i) {
        var loc = getLoc(filename, tokens, i);
        var n = 0;
        if (!isIdentifier(tokens, i + n++, "new")) return ParseRes.failed();

        var valRes = parseValue(filename, tokens, i + n, 18);
        n += valRes.n;
        if (!valRes.isSuccess()) return ParseRes.error(loc, "Expected a value after 'new' keyword.", valRes);
        var callRes = parseCall(filename, tokens, i + n, valRes.result, 0);
        n += callRes.n;
        if (callRes.isError()) return callRes.transform();
        else if (callRes.isFailed()) return ParseRes.res(new NewStatement(loc, valRes.result), n);
        var call = (CallStatement)callRes.result;

        return ParseRes.res(new NewStatement(loc, call.func, call.args), n);
    }
    public static ParseRes<TypeofStatement> parseTypeof(String filename, List<Token> tokens, int i) {
        var loc = getLoc(filename, tokens, i);
        var n = 0;
        if (!isIdentifier(tokens, i + n++, "typeof")) return ParseRes.failed();

        var valRes = parseValue(filename, tokens, i + n, 15);
        if (!valRes.isSuccess()) return ParseRes.error(loc, "Expected a value after 'typeof' keyword.", valRes);
        n += valRes.n;

        return ParseRes.res(new TypeofStatement(loc, valRes.result), n);
    }
    public static ParseRes<VoidStatement> parseVoid(String filename, List<Token> tokens, int i) {
        var loc = getLoc(filename, tokens, i);
        var n = 0;
        if (!isIdentifier(tokens, i + n++, "void")) return ParseRes.failed();

        var valRes = parseValue(filename, tokens, i + n, 14);
        if (!valRes.isSuccess()) return ParseRes.error(loc, "Expected a value after 'void' keyword.", valRes);
        n += valRes.n;

        return ParseRes.res(new VoidStatement(loc, valRes.result), n);
    }
    public static ParseRes<? extends Statement> parseDelete(String filename, List<Token> tokens, int i) {
        var loc = getLoc(filename, tokens, i);
        int n = 0;
        if (!isIdentifier(tokens, i + n++, "delete")) return ParseRes.failed();

        var valRes = parseValue(filename, tokens, i + n, 15);
        if (!valRes.isSuccess()) return ParseRes.error(loc, "Expected a value after 'delete'.", valRes);
        n += valRes.n;

        if (valRes.result instanceof IndexStatement) {
            var index = (IndexStatement)valRes.result;
            return ParseRes.res(new DeleteStatement(loc, index.index, index.object), n);
        }
        else if (valRes.result instanceof VariableStatement) {
            return ParseRes.error(loc, "A variable may not be deleted.");
        }
        else {
            return ParseRes.res(new ConstantStatement(loc, true), n);
        }
    }

    public static ParseRes<FunctionStatement> parseFunction(String filename, List<Token> tokens, int i, boolean statement) {
        var loc = getLoc(filename, tokens, i);
        int n = 0;

        if (!isIdentifier(tokens, i + n++, "function")) return ParseRes.failed();

        var nameRes = parseIdentifier(tokens, i + n);
        if (!nameRes.isSuccess() && statement) return ParseRes.error(loc, "A statement function requires a name, one is not present.");
        var name = nameRes.result;
        n += nameRes.n;

        if (!isOperator(tokens, i + n++, Operator.PAREN_OPEN)) return ParseRes.error(loc, "Expected a parameter list.");

        var args = new ArrayList<String>();

        if (isOperator(tokens, i + n, Operator.PAREN_CLOSE)) {
            n++;
        }
        else {
            while (true) {
                var argRes = parseIdentifier(tokens, i + n);
                if (argRes.isSuccess()) {
                    args.add(argRes.result);
                    n++;
                    if (isOperator(tokens, i + n, Operator.COMMA)) {
                        n++;
                    }
                    if (isOperator(tokens, i + n, Operator.PAREN_CLOSE)) {
                        n++;
                        break;
                    }
                }
                else return ParseRes.error(loc, "Expected an argument, comma or a closing brace.");
            }
        }

        var res = parseCompound(filename, tokens, i + n);
        n += res.n;

        if (res.isSuccess()) return ParseRes.res(new FunctionStatement(loc, name, args.toArray(String[]::new), res.result), n);
        else return ParseRes.error(loc, "Expected a compound statement for function.", res);
    }

    public static ParseRes<OperationStatement> parseUnary(String filename, List<Token> tokens, int i) {
        var loc = getLoc(filename, tokens, i);
        int n = 0;

        var opState = parseOperator(tokens, i + n++);
        if (!opState.isSuccess()) return ParseRes.failed();
        var op = opState.result;

        Type operation = null;

        if (op == Operator.ADD) operation = Type.POS;
        else if (op == Operator.SUBTRACT) operation = Type.NEG;
        else if (op == Operator.INVERSE) operation = Type.INVERSE;
        else if (op == Operator.NOT) operation = Type.NOT;
        else return ParseRes.failed();

        var res = parseValue(filename, tokens, n + i, 14);

        if (res.isSuccess()) return ParseRes.res(new OperationStatement(loc, operation, res.result), n + res.n);
        else return ParseRes.error(loc, "Expected a value after the unary operator '%s'.".formatted(op.value), res);
    }
    public static ParseRes<ChangeStatement> parsePrefixChange(String filename, List<Token> tokens, int i) {
        var loc = getLoc(filename, tokens, i);
        int n = 0;

        var opState = parseOperator(tokens, i + n++);
        if (!opState.isSuccess()) return ParseRes.failed();

        int change = 0;

        if (opState.result == Operator.INCREASE) change = 1;
        else if (opState.result == Operator.DECREASE) change = -1;
        else return ParseRes.failed();

        var res = parseValue(filename, tokens, i + n, 15);
        if (!(res.result instanceof AssignableStatement)) return ParseRes.error(loc, "Expected assignable value after prefix operator.");
        return ParseRes.res(new ChangeStatement(loc, (AssignableStatement)res.result, change, false), n + res.n);
    }
    public static ParseRes<? extends Statement> parseParens(String filename, List<Token> tokens, int i) {
        int n = 0;
        if (!isOperator(tokens, i + n++, Operator.PAREN_OPEN)) return ParseRes.failed();

        var res = parseValue(filename, tokens, i + n, 0);
        if (!res.isSuccess()) return res;
        n += res.n;

        if (!isOperator(tokens, i + n++, Operator.PAREN_CLOSE)) return ParseRes.failed();

        return ParseRes.res(res.result, n);
    }
    @SuppressWarnings("all")
    public static ParseRes<? extends Statement> parseSimple(String filename, List<Token> tokens, int i, boolean statement) {
        var res = new ArrayList<>(List.of(
            parseVariable(filename, tokens, i),
            parseLiteral(filename, tokens, i),
            parseString(filename, tokens, i),
            parseRegex(filename, tokens, i),
            parseNumber(filename, tokens, i),
            parseUnary(filename, tokens, i),
            parseArray(filename, tokens, i),
            parsePrefixChange(filename, tokens, i),
            parseParens(filename, tokens, i),
            parseNew(filename, tokens, i),
            parseTypeof(filename, tokens, i),
            parseVoid(filename, tokens, i),
            parseDelete(filename, tokens, i)
        ));

        if (!statement) {
            res.add(parseObject(filename, tokens, i));
            res.add(parseFunction(filename, tokens, i, false));
        }

        return ParseRes.any(res.toArray(ParseRes[]::new));
    }

    public static ParseRes<VariableStatement> parseVariable(String filename, List<Token> tokens, int i) {
        var loc = getLoc(filename, tokens, i);
        var literal = parseIdentifier(tokens, i);

        if (!literal.isSuccess()) return ParseRes.failed();

        if (!checkVarName(literal.result)) {
            if (literal.result.equals("await")) return ParseRes.error(loc, "'await' expressions are not supported.");
            if (literal.result.equals("async")) return ParseRes.error(loc, "'async' is not supported.");
            if (literal.result.equals("const")) return ParseRes.error(loc, "'const' declarations are not supported.");
            if (literal.result.equals("let")) return ParseRes.error(loc, "'let' declarations are not supported.");
            return ParseRes.error(loc, "Unexpected identifier '%s'.".formatted(literal.result));
        }

        return ParseRes.res(new VariableStatement(loc, literal.result), 1);
    }
    public static ParseRes<? extends Statement> parseLiteral(String filename, List<Token> tokens, int i) {
        var loc = getLoc(filename, tokens, i);
        var id = parseIdentifier(tokens, i);
        if (!id.isSuccess()) return id.transform();

        if (id.result.equals("true")) {
            return ParseRes.res(new ConstantStatement(loc, true), 1);
        }
        if (id.result.equals("false")) {
            return ParseRes.res(new ConstantStatement(loc, false), 1);
        }
        if (id.result.equals("undefined")) {
            return ParseRes.res(new ConstantStatement(loc, null), 1);
        }
        if (id.result.equals("null")) {
            return ParseRes.res(new ConstantStatement(loc, Values.NULL), 1);
        }
        if (id.result.equals("NaN")) {
            return ParseRes.res(new ConstantStatement(loc, Double.NaN), 1);
        }
        if (id.result.equals("Infinity")) {
            return ParseRes.res(new ConstantStatement(loc, Double.POSITIVE_INFINITY), 1);
        }
        if (id.result.equals("this")) {
            return ParseRes.res(new VariableIndexStatement(loc, 0), 1);
        }
        if (id.result.equals("arguments")) {
            return ParseRes.res(new VariableIndexStatement(loc, 1), 1);
        }
        if (id.result.equals("globalThis") || id.result.equals("window") || id.result.equals("self")) {
            return ParseRes.res(new GlobalThisStatement(loc), 1);
        }
        return ParseRes.failed();
    }
    public static ParseRes<IndexStatement> parseMember(String filename, List<Token> tokens, int i, Statement prev, int precedence) {
        var loc = getLoc(filename, tokens, i);
        var n = 0;

        if (precedence > 18) return ParseRes.failed();

        if (!isOperator(tokens, i + n++, Operator.DOT)) return ParseRes.failed();

        var literal = parseIdentifier(tokens, i + n++);
        if (!literal.isSuccess()) return ParseRes.error(loc, "Expected an identifier after member access.");

        return ParseRes.res(new IndexStatement(loc, prev, new ConstantStatement(loc, literal.result)), n);
    }
    public static ParseRes<IndexStatement> parseIndex(String filename, List<Token> tokens, int i, Statement prev, int precedence) {
        var loc = getLoc(filename, tokens, i);
        var n = 0;

        if (precedence > 18) return ParseRes.failed();

        if (!isOperator(tokens, i + n++, Operator.BRACKET_OPEN)) return ParseRes.failed();

        var valRes = parseValue(filename, tokens, i + n, 0);
        if (!valRes.isSuccess()) return ParseRes.error(loc, "Expected a value in index expression.", valRes);
        n += valRes.n;

        if (!isOperator(tokens, i + n++, Operator.BRACKET_CLOSE)) return ParseRes.error(loc, "Expected a closing bracket.");

        return ParseRes.res(new IndexStatement(loc, prev, valRes.result), n);
    }
    public static ParseRes<? extends Statement> parseAssign(String filename, List<Token> tokens, int i, Statement prev, int precedence) {
        var loc = getLoc(filename, tokens, i);
        int n = 0 ;

        if (precedence > 2) return ParseRes.failed();

        var opRes = parseOperator(tokens, i + n++);
        if (opRes.state != State.SUCCESS) return ParseRes.failed();

        var op = opRes.result;
        if (!op.isAssign()) return ParseRes.failed();

        if (!(prev instanceof AssignableStatement)) return ParseRes.error(loc, "Invalid expression on left hand side of assign operator.");

        var res = parseValue(filename, tokens, i + n, 2);
        if (!res.isSuccess()) return ParseRes.error(loc, "Expected value after assignment operator '%s'.".formatted(op.value), res);
        n += res.n;

        Type operation = null;

        if (op == Operator.ASSIGN_ADD) operation = Type.ADD;
        if (op == Operator.ASSIGN_SUBTRACT) operation = Type.SUBTRACT;
        if (op == Operator.ASSIGN_MULTIPLY) operation = Type.MULTIPLY;
        if (op == Operator.ASSIGN_DIVIDE) operation = Type.DIVIDE;
        if (op == Operator.ASSIGN_MODULO) operation = Type.MODULO;
        if (op == Operator.ASSIGN_OR) operation = Type.OR;
        if (op == Operator.ASSIGN_XOR) operation = Type.XOR;
        if (op == Operator.ASSIGN_AND) operation = Type.AND;
        if (op == Operator.ASSIGN_SHIFT_LEFT) operation = Type.SHIFT_LEFT;
        if (op == Operator.ASSIGN_SHIFT_RIGHT) operation = Type.SHIFT_RIGHT;
        if (op == Operator.ASSIGN_USHIFT_RIGHT) operation = Type.USHIFT_RIGHT;

        return ParseRes.res(((AssignableStatement)prev).toAssign(res.result, operation), n);
    }
    public static ParseRes<CallStatement> parseCall(String filename, List<Token> tokens, int i, Statement prev, int precedence) {
        var loc = getLoc(filename, tokens, i);
        var n = 0;

        if (precedence > 17) return ParseRes.failed();
        if (!isOperator(tokens, i + n++, Operator.PAREN_OPEN)) return ParseRes.failed();

        var args = new ArrayList<Statement>();
        boolean prevArg = false;

        while (true) {
            var argRes = parseValue(filename, tokens, i + n, 2);
            if (argRes.isSuccess()) {
                args.add(argRes.result);
                n += argRes.n;
                prevArg = true;
            }
            else if (argRes.isError()) return argRes.transform();
            else if (isOperator(tokens, i + n, Operator.COMMA)) {
                if (!prevArg) args.add(null);
                prevArg = false;
                n++;
            }
            else if (isOperator(tokens, i + n, Operator.PAREN_CLOSE)) {
                n++;
                break;
            }
            else return ParseRes.failed();
        }

        return ParseRes.res(new CallStatement(loc, prev, args.toArray(Statement[]::new)), n);
    }
    public static ParseRes<ChangeStatement> parsePostfixChange(String filename, List<Token> tokens, int i, Statement prev, int precedence) {
        var loc = getLoc(filename, tokens, i);
        int n = 0;

        if (precedence > 15) return ParseRes.failed();

        var opState = parseOperator(tokens, i + n++);
        if (!opState.isSuccess()) return ParseRes.failed();

        int change = 0;

        if (opState.result == Operator.INCREASE) change = 1;
        else if (opState.result == Operator.DECREASE) change = -1;
        else return ParseRes.failed();

        if (!(prev instanceof AssignableStatement)) return ParseRes.error(loc, "Expected assignable value before suffix operator.");
        return ParseRes.res(new ChangeStatement(loc, (AssignableStatement)prev, change, true), n);
    }
    public static ParseRes<OperationStatement> parseInstanceof(String filename, List<Token> tokens, int i, Statement prev, int precedence) {
        var loc = getLoc(filename, tokens, i);
        int n = 0;

        if (precedence > 9) return ParseRes.failed();
        if (!isIdentifier(tokens, i + n++, "instanceof")) return ParseRes.failed();

        var valRes = parseValue(filename, tokens, i + n, 10);
        if (!valRes.isSuccess()) return ParseRes.error(loc, "Expected a value after 'instanceof'.", valRes);
        n += valRes.n;

        return ParseRes.res(new OperationStatement(loc, Type.INSTANCEOF, prev, valRes.result), n);
    }
    public static ParseRes<OperationStatement> parseIn(String filename, List<Token> tokens, int i, Statement prev, int precedence) {
        var loc = getLoc(filename, tokens, i);
        int n = 0;

        if (precedence > 9) return ParseRes.failed();
        if (!isIdentifier(tokens, i + n++, "in")) return ParseRes.failed();

        var valRes = parseValue(filename, tokens, i + n, 10);
        if (!valRes.isSuccess()) return ParseRes.error(loc, "Expected a value after 'in'.", valRes);
        n += valRes.n;

        return ParseRes.res(new OperationStatement(loc, Type.IN, prev, valRes.result), n);
    }
    public static ParseRes<CommaStatement> parseComma(String filename, List<Token> tokens, int i, Statement prev, int precedence) {
        var loc = getLoc(filename, tokens, i);
        var n = 0;

        if (precedence > 1) return ParseRes.failed();
        if (!isOperator(tokens, i + n++, Operator.COMMA)) return ParseRes.failed();
        
        var res = parseValue(filename, tokens, i + n, 2);
        if (!res.isSuccess()) return ParseRes.error(loc, "Expected a value after the comma.", res);
        n += res.n;

        return ParseRes.res(new CommaStatement(loc, prev, res.result), n);
    }
    public static ParseRes<TernaryStatement> parseTernary(String filename, List<Token> tokens, int i, Statement prev, int precedence) {
        var loc = getLoc(filename, tokens, i);
        var n = 0;

        if (precedence > 2) return ParseRes.failed();
        if (!isOperator(tokens, i + n++, Operator.QUESTION)) return ParseRes.failed();

        var a = parseValue(filename, tokens, i + n, 2);
        if (!a.isSuccess()) return ParseRes.error(loc, "Expected a value after the ternary operator.", a);
        n += a.n;

        if (!isOperator(tokens, i + n++, Operator.COLON)) return ParseRes.failed();

        var b = parseValue(filename, tokens, i + n, 2);
        if (!b.isSuccess()) return ParseRes.error(loc, "Expected a second value after the ternary operator.", b);
        n += b.n;

        return ParseRes.res(new TernaryStatement(loc, prev, a.result, b.result), n);
    }
    public static ParseRes<? extends Statement> parseOperator(String filename, List<Token> tokens, int i, Statement prev, int precedence) {
        var loc = getLoc(filename, tokens, i);
        var n = 0;

        var opRes = parseOperator(tokens, i + n++);
        if (!opRes.isSuccess()) return ParseRes.failed();
        var op = opRes.result;

        if (op.precedence < precedence) return ParseRes.failed();
        if (op.isAssign()) return parseAssign(filename, tokens, i + n - 1, prev, precedence);

        var res = parseValue(filename, tokens, i + n, op.precedence + (op.reverse ? 0 : 1));
        if (!res.isSuccess()) return ParseRes.error(loc, "Expected a value after the '%s' operator.".formatted(op.value), res);
        n += res.n;

        if (op == Operator.LAZY_AND) {
            return ParseRes.res(new LazyAndStatement(loc, prev, res.result), n);
        }
        if (op == Operator.LAZY_OR) {
            return ParseRes.res(new LazyOrStatement(loc, prev, res.result), n);
        }

        return ParseRes.res(new OperationStatement(loc, op.operation, prev, res.result), n);
    }

    public static ParseRes<? extends Statement> parseValue(String filename, List<Token> tokens, int i, int precedence, boolean statement) {
        Statement prev = null;
        int n = 0;

        while (true) {
            if (prev == null) {
                var res = parseSimple(filename, tokens, i + n, statement);
                if (res.isSuccess()) {
                    n += res.n;
                    prev = res.result;
                }
                else if (res.isError()) return res.transform();
                else break;
            }
            else {
                var res = ParseRes.any(
                    parseOperator(filename, tokens, i + n, prev, precedence),
                    parseMember(filename, tokens, i + n, prev, precedence),
                    parseIndex(filename, tokens, i + n, prev, precedence),
                    parseCall(filename, tokens, i + n, prev, precedence),
                    parsePostfixChange(filename, tokens, i + n, prev, precedence),
                    parseInstanceof(filename, tokens, i + n, prev, precedence),
                    parseIn(filename, tokens, i + n, prev, precedence),
                    parseComma(filename, tokens, i + n, prev, precedence),
                    parseTernary(filename, tokens, i + n, prev, precedence)
                );

                if (res.isSuccess()) {
                    n += res.n;
                    prev = res.result;
                    continue;
                }
                else if (res.isError()) return res.transform();

                break;
            }
        }
    
        if (prev == null) return ParseRes.failed();
        else return ParseRes.res(prev, n);
    }
    public static ParseRes<? extends Statement> parseValue(String filename, List<Token> tokens, int i, int precedence) {
        return parseValue(filename, tokens, i, precedence, false);
    }

    public static ParseRes<? extends Statement> parseValueStatement(String filename, List<Token> tokens, int i) {
        var loc = getLoc(filename, tokens, i);
        var valRes = parseValue(filename, tokens, i, 0, true);
        if (!valRes.isSuccess()) return valRes.transform();

        valRes.result.setLoc(loc);
        var res = ParseRes.res(valRes.result, valRes.n);

        if (isStatementEnd(tokens, i + res.n)) {
            if (isOperator(tokens, i + res.n, Operator.SEMICOLON)) return res.addN(1);
            else return res;
        }
        else if (isIdentifier(tokens, i, "const") || isIdentifier(tokens, i, "let")) {
            return ParseRes.error(getLoc(filename, tokens, i), "Detected the usage of 'const'/'let'. Please, use 'var' instead.");
        }
        else return ParseRes.error(getLoc(filename, tokens, i), "Expected an end of statement.", res);
    }
    public static ParseRes<VariableDeclareStatement> parseVariableDeclare(String filename, List<Token> tokens, int i) {
        var loc = getLoc(filename, tokens, i);
        int n = 0;
        if (!isIdentifier(tokens, i + n++, "var")) return ParseRes.failed();

        var res = new ArrayList<Pair>();

        if (isStatementEnd(tokens, i + n)) {
            if (isOperator(tokens, i + n, Operator.SEMICOLON)) return ParseRes.res(new VariableDeclareStatement(loc, res), 2);
            else return ParseRes.res(new VariableDeclareStatement(loc, res), 1);
        }

        while (true) {
            var nameRes = parseIdentifier(tokens, i + n++);
            if (!nameRes.isSuccess()) return ParseRes.error(loc, "Expected a variable name.");

            if (!checkVarName(nameRes.result)) {
                return ParseRes.error(loc, "Unexpected identifier '%s'.".formatted(nameRes.result));
            }

            Statement val = null;

            if (isOperator(tokens, i + n, Operator.ASSIGN)) {
                n++;
                var valRes = parseValue(filename, tokens, i + n, 2);
                if (!valRes.isSuccess()) return ParseRes.error(loc, "Expected a value after '='.", valRes);
                n += valRes.n;
                val = valRes.result;
            }

            res.add(new Pair(nameRes.result, val));
            
            if (isOperator(tokens, i + n, Operator.COMMA)) {
                n++;
                continue;
            }
            else if (isStatementEnd(tokens, i + n)) {
                if (isOperator(tokens, i + n, Operator.SEMICOLON)) return ParseRes.res(new VariableDeclareStatement(loc, res), n + 1);
                else return ParseRes.res(new VariableDeclareStatement(loc, res), n);
            }
            else return ParseRes.error(loc, "Expected a comma or end of statement.");
        }
    }

    public static ParseRes<ReturnStatement> parseReturn(String filename, List<Token> tokens, int i) {
        var loc = getLoc(filename, tokens, i);
        int n = 0;
        if (!isIdentifier(tokens, i + n++, "return")) return ParseRes.failed();

        if (isStatementEnd(tokens, i + n)) {
            if (isOperator(tokens, i + n, Operator.SEMICOLON)) return ParseRes.res(new ReturnStatement(loc, null), 2);
            else return ParseRes.res(new ReturnStatement(loc, null), 1);
        }

        var valRes = parseValue(filename, tokens, i + n, 0);
        n += valRes.n;
        if (valRes.isError())
            return ParseRes.error(loc, "Expected a return value.", valRes);

        var res = ParseRes.res(new ReturnStatement(loc, valRes.result), n);

        if (isStatementEnd(tokens, i + n)) {
            if (isOperator(tokens, i + n, Operator.SEMICOLON)) return res.addN(1);
            else return res;
        }
        else
            return ParseRes.error(getLoc(filename, tokens, i), "Expected an end of statement.", valRes);
    }
    public static ParseRes<ThrowStatement> parseThrow(String filename, List<Token> tokens, int i) {
        var loc = getLoc(filename, tokens, i);
        int n = 0;
        if (!isIdentifier(tokens, i + n++, "throw")) return ParseRes.failed();

        var valRes = parseValue(filename, tokens, i + n, 0);
        n += valRes.n;
        if (valRes.isError()) return ParseRes.error(loc, "Expected a throw value.", valRes);

        var res = ParseRes.res(new ThrowStatement(loc, valRes.result), n);

        if (isStatementEnd(tokens, i + n)) {
            if (isOperator(tokens, i + n, Operator.SEMICOLON)) return res.addN(1);
            else return res;
        }
        else return ParseRes.error(getLoc(filename, tokens, i), "Expected an end of statement.", valRes);
    }

    public static ParseRes<BreakStatement> parseBreak(String filename, List<Token> tokens, int i) {
        if (!isIdentifier(tokens, i, "break")) return ParseRes.failed();

        if (isStatementEnd(tokens, i + 1)) {
            if (isOperator(tokens, i + 1, Operator.SEMICOLON)) return ParseRes.res(new BreakStatement(getLoc(filename, tokens, i), null), 2);
            else return ParseRes.res(new BreakStatement(getLoc(filename, tokens, i), null), 1);
        }
        
        var labelRes = parseIdentifier(tokens, i + 1);
        if (labelRes.isFailed()) return ParseRes.error(getLoc(filename, tokens, i), "Expected a label name or an end of statement.");
        var label = labelRes.result;

        if (isStatementEnd(tokens, i + 2)) {
            if (isOperator(tokens, i + 2, Operator.SEMICOLON)) return ParseRes.res(new BreakStatement(getLoc(filename, tokens, i), label), 3);
            else return ParseRes.res(new BreakStatement(getLoc(filename, tokens, i), label), 2);
        }
        else return ParseRes.error(getLoc(filename, tokens, i), "Expected an end of statement.");
    }
    public static ParseRes<ContinueStatement> parseContinue(String filename, List<Token> tokens, int i) {
        if (!isIdentifier(tokens, i, "continue")) return ParseRes.failed();

        if (isStatementEnd(tokens, i + 1)) {
            if (isOperator(tokens, i + 1, Operator.SEMICOLON)) return ParseRes.res(new ContinueStatement(getLoc(filename, tokens, i), null), 2);
            else return ParseRes.res(new ContinueStatement(getLoc(filename, tokens, i), null), 1);
        }
        
        var labelRes = parseIdentifier(tokens, i + 1);
        if (labelRes.isFailed()) return ParseRes.error(getLoc(filename, tokens, i), "Expected a label name or an end of statement.");
        var label = labelRes.result;

        if (isStatementEnd(tokens, i + 2)) {
            if (isOperator(tokens, i + 2, Operator.SEMICOLON)) return ParseRes.res(new ContinueStatement(getLoc(filename, tokens, i), label), 3);
            else return ParseRes.res(new ContinueStatement(getLoc(filename, tokens, i), label), 2);
        }
        else return ParseRes.error(getLoc(filename, tokens, i), "Expected an end of statement.");
    }
    public static ParseRes<DebugStatement> parseDebug(String filename, List<Token> tokens, int i) {
        if (!isIdentifier(tokens, i, "debug")) return ParseRes.failed();

        if (isStatementEnd(tokens, i + 1)) {
            if (isOperator(tokens, i + 1, Operator.SEMICOLON)) return ParseRes.res(new DebugStatement(getLoc(filename, tokens, i)), 2);
            else return ParseRes.res(new DebugStatement(getLoc(filename, tokens, i)), 1);
        }
        else return ParseRes.error(getLoc(filename, tokens, i), "Expected an end of statement.");
    }

    public static ParseRes<CompoundStatement> parseCompound(String filename, List<Token> tokens, int i) {
        var loc = getLoc(filename, tokens, i);
        int n = 0;
        if (!isOperator(tokens, i + n++, Operator.BRACE_OPEN)) return ParseRes.failed();

        var statements = new ArrayList<Statement>();

        while (true) {
            if (isOperator(tokens, i + n, Operator.BRACE_CLOSE)) {
                n++;
                break;
            }
            if (isOperator(tokens, i + n, Operator.SEMICOLON)) {
                n++;
                continue;
            }

            var res = parseStatement(filename, tokens, i + n);
            if (!res.isSuccess()) {
                return ParseRes.error(getLoc(filename, tokens, i), "Expected a statement.", res);
            }
            n += res.n;

            statements.add(res.result);
        }

        return ParseRes.res(new CompoundStatement(loc, statements.toArray(Statement[]::new)), n);
    }
    public static ParseRes<String> parseLabel(List<Token> tokens, int i) {
        int n = 0;

        var nameRes = parseIdentifier(tokens, i + n++);
        if (!isOperator(tokens, i + n++, Operator.COLON)) return ParseRes.failed();

        return ParseRes.res(nameRes.result, n);
    }
    public static ParseRes<IfStatement> parseIf(String filename, List<Token> tokens, int i) {
        var loc = getLoc(filename, tokens, i);
        int n = 0;

        if (!isIdentifier(tokens, i + n++, "if")) return ParseRes.failed();
        if (!isOperator(tokens, i + n++, Operator.PAREN_OPEN)) return ParseRes.error(loc, "Expected a open paren after 'if'.");

        var condRes = parseValue(filename, tokens, i + n, 0);
        if (!condRes.isSuccess()) return ParseRes.error(loc, "Expected an if condition.", condRes);
        n += condRes.n;

        if (!isOperator(tokens, i + n++, Operator.PAREN_CLOSE))
            return ParseRes.error(loc, "Expected a closing paren after if condition.");

        var res = parseStatement(filename, tokens, i + n);
        if (!res.isSuccess()) return ParseRes.error(loc, "Expected an if body.", res);
        n += res.n;

        if (!isIdentifier(tokens, i + n, "else")) return ParseRes.res(new IfStatement(loc, condRes.result, res.result, null), n);
        n++;

        var elseRes = parseStatement(filename, tokens, i + n);
        if (!elseRes.isSuccess()) return ParseRes.error(loc, "Expected an else body.", elseRes);
        n += elseRes.n;
    
        return ParseRes.res(new IfStatement(loc, condRes.result, res.result, elseRes.result), n);
    }
    public static ParseRes<WhileStatement> parseWhile(String filename, List<Token> tokens, int i) {
        var loc = getLoc(filename, tokens, i);
        int n = 0;

        var labelRes = parseLabel(tokens, i + n);
        n += labelRes.n;

        if (!isIdentifier(tokens, i + n++, "while")) return ParseRes.failed();
        if (!isOperator(tokens, i + n++, Operator.PAREN_OPEN)) return ParseRes.error(loc, "Expected a open paren after 'while'.");

        var condRes = parseValue(filename, tokens, i + n, 0);
        if (!condRes.isSuccess()) return ParseRes.error(loc, "Expected a while condition.", condRes);
        n += condRes.n;
    
        if (!isOperator(tokens, i + n++, Operator.PAREN_CLOSE)) return ParseRes.error(loc, "Expected a closing paren after while condition.");

        var res = parseStatement(filename, tokens, i + n);
        if (!res.isSuccess()) return ParseRes.error(loc, "Expected a while body.", res);
        n += res.n;

        return ParseRes.res(new WhileStatement(loc, labelRes.result, condRes.result, res.result), n);
    }
    public static ParseRes<Statement> parseSwitchCase(String filename, List<Token> tokens, int i) {
        var loc = getLoc(filename, tokens, i);
        int n = 0;

        if (!isIdentifier(tokens, i + n++, "case")) return ParseRes.failed();

        var valRes = parseValue(filename, tokens, i + n, 0);
        if (!valRes.isSuccess()) return ParseRes.error(loc, "Expected a value after 'case'.", valRes);
        n += valRes.n;

        if (!isOperator(tokens, i + n++, Operator.COLON)) return ParseRes.error(loc, "Expected colons after 'case' value.");

        return ParseRes.res(valRes.result, n);
    }
    public static ParseRes<Statement> parseDefaultCase(List<Token> tokens, int i) {
        if (!isIdentifier(tokens, i, "default")) return ParseRes.failed();
        if (!isOperator(tokens, i + 1, Operator.COLON)) return ParseRes.error(getLoc(null, tokens, i), "Expected colons after 'default'.");

        return ParseRes.res(null, 2);
    }
    public static ParseRes<SwitchStatement> parseSwitch(String filename, List<Token> tokens, int i) {
        var loc = getLoc(filename, tokens, i);
        int n = 0;

        if (!isIdentifier(tokens, i + n++, "switch")) return ParseRes.failed();
        if (!isOperator(tokens, i + n++, Operator.PAREN_OPEN)) return ParseRes.error(loc, "Expected a open paren after 'switch'.");

        var valRes = parseValue(filename, tokens, i + n, 0);
        if (!valRes.isSuccess()) return ParseRes.error(loc, "Expected a switch value.", valRes);
        n += valRes.n;
    
        if (!isOperator(tokens, i + n++, Operator.PAREN_CLOSE)) return ParseRes.error(loc, "Expected a closing paren after switch value.");
        if (!isOperator(tokens, i + n++, Operator.BRACE_OPEN)) return ParseRes.error(loc, "Expected an opening brace after switch value.");

        var statements = new ArrayList<Statement>();
        var cases = new ArrayList<SwitchCase>();
        var defaultI = -1;

        while (true) {
            if (isOperator(tokens, i + n, Operator.BRACE_CLOSE)) {
                n++;
                break;
            }
            if (isOperator(tokens, i + n, Operator.SEMICOLON)) {
                n++;
                continue;
            }

            var defaultRes = parseDefaultCase(tokens, i + n);
            var caseRes = parseSwitchCase(filename, tokens, i + n);

            if (defaultRes.isSuccess()) {
                defaultI = statements.size();
                n += defaultRes.n;
            }
            else if (caseRes.isSuccess()) {
                cases.add(new SwitchCase(caseRes.result, statements.size()));
                n += caseRes.n;
            }
            else if (defaultRes.isError()) return defaultRes.transform();
            else if (caseRes.isError()) return defaultRes.transform();
            else {
                var res = ParseRes.any(
                    parseStatement(filename, tokens, i + n),
                    parseCompound(filename, tokens, i + n)
                );
                if (!res.isSuccess()) {
                    return ParseRes.error(getLoc(filename, tokens, i), "Expected a statement.", res);
                }
                n += res.n;
                statements.add(res.result);
            }
        }

        return ParseRes.res(new SwitchStatement(
            loc, valRes.result, defaultI,
            cases.toArray(SwitchCase[]::new),
            statements.toArray(Statement[]::new)
        ), n);
    }
    public static ParseRes<DoWhileStatement> parseDoWhile(String filename, List<Token> tokens, int i) {
        var loc = getLoc(filename, tokens, i);
        int n = 0;

        var labelRes = parseLabel(tokens, i + n);
        n += labelRes.n;

        if (!isIdentifier(tokens, i + n++, "do")) return ParseRes.failed();
        var bodyRes = parseStatement(filename, tokens, i + n);
        if (!bodyRes.isSuccess()) return ParseRes.error(loc, "Expected a do-while body.", bodyRes);
        n += bodyRes.n;

        if (!isIdentifier(tokens, i + n++, "while")) return ParseRes.error(loc, "Expected 'while' keyword.");
        if (!isOperator(tokens, i + n++, Operator.PAREN_OPEN)) return ParseRes.error(loc, "Expected a open paren after 'while'.");

        var condRes = parseValue(filename, tokens, i + n, 0);
        if (!condRes.isSuccess()) return ParseRes.error(loc, "Expected a while condition.", condRes);
        n += condRes.n;

        if (!isOperator(tokens, i + n++, Operator.PAREN_CLOSE)) return ParseRes.error(loc, "Expected a closing paren after while condition.");

        var res = ParseRes.res(new DoWhileStatement(loc, labelRes.result, condRes.result, bodyRes.result), n);

        if (isStatementEnd(tokens, i + n)) {
            if (isOperator(tokens, i + n, Operator.SEMICOLON)) return res.addN(1);
            else return res;
        }
        else return ParseRes.error(getLoc(filename, tokens, i), "Expected a semicolon.");
    }
    public static ParseRes<Statement> parseFor(String filename, List<Token> tokens, int i) {
        var loc = getLoc(filename, tokens, i);
        int n = 0;

        var labelRes = parseLabel(tokens, i + n);
        n += labelRes.n;

        if (!isIdentifier(tokens, i + n++, "for")) return ParseRes.failed();
        if (!isOperator(tokens, i + n++, Operator.PAREN_OPEN)) return ParseRes.error(loc, "Expected a open paren after 'for'.");

        Statement decl, cond, inc;

        if (isOperator(tokens, i + n, Operator.SEMICOLON)) {
            n++;
            decl = new CompoundStatement(loc);
        }
        else {
            var declRes = ParseRes.any(
                parseVariableDeclare(filename, tokens, i + n),
                parseValueStatement(filename, tokens, i + n)
            );
            if (!declRes.isSuccess())
                return ParseRes.error(loc, "Expected a declaration or an expression.", declRes);
            n += declRes.n;
            decl = declRes.result;
        }

        if (isOperator(tokens, i + n, Operator.SEMICOLON)) {
            n++;
            cond = new ConstantStatement(loc, 1);
        }
        else {
            var condRes = parseValue(filename, tokens, i + n, 0);
            if (!condRes.isSuccess()) return ParseRes.error(loc, "Expected a condition.", condRes);
            n += condRes.n;
            if (!isOperator(tokens, i + n++, Operator.SEMICOLON)) return ParseRes.error(loc, "Expected a semicolon.", condRes);
            cond = condRes.result;
        }

        if (isOperator(tokens, i + n, Operator.PAREN_CLOSE)) {
            n++;
            inc = new CompoundStatement(loc);
        }
        else {
            var incRes = parseValue(filename, tokens, i + n, 0);
            if (!incRes.isSuccess()) return ParseRes.error(loc, "Expected a condition.", incRes);
            n += incRes.n;
            inc = incRes.result;
            if (!isOperator(tokens, i + n++, Operator.PAREN_CLOSE)) return ParseRes.error(loc, "Expected a closing paren after for.");
        }


        var res = parseStatement(filename, tokens, i + n);
        if (!res.isSuccess()) return ParseRes.error(loc, "Expected a for body.", res);
        n += res.n;

        return ParseRes.res(new ForStatement(loc, labelRes.result, decl, cond, inc, res.result), n);
    }
    public static ParseRes<ForInStatement> parseForIn(String filename, List<Token> tokens, int i) {
        var loc = getLoc(filename, tokens, i);
        int n = 0;

        var labelRes = parseLabel(tokens, i + n);
        var isDecl = false;
        n += labelRes.n;

        if (!isIdentifier(tokens, i + n++, "for")) return ParseRes.failed();
        if (!isOperator(tokens, i + n++, Operator.PAREN_OPEN)) return ParseRes.error(loc, "Expected a open paren after 'for'.");

        if (isIdentifier(tokens, i + n, "var")) {
            isDecl = true;
            n++;
        }

        var nameRes = parseIdentifier(tokens, i + n);
        if (!nameRes.isSuccess()) return ParseRes.error(loc, "Expected a variable name for 'for' loop.");
        n += nameRes.n;

        Statement varVal = null;

        if (isOperator(tokens, i + n, Operator.ASSIGN)) {
            n++;

            var valRes = parseValue(filename, tokens, i + n, 2);
            if (!valRes.isSuccess()) return ParseRes.error(loc, "Expected a value after '='.", valRes);
            n += nameRes.n;

            varVal = valRes.result;
        }

        if (!isIdentifier(tokens, i + n++, "in")) {
            if (varVal == null) {
                if (nameRes.result.equals("const")) return ParseRes.error(loc, "'const' declarations are not supported.");
                else if (nameRes.result.equals("let")) return ParseRes.error(loc, "'let' declarations are not supported.");
            }
            return ParseRes.error(loc, "Expected 'in' keyword after variable declaration.");
        }

        var objRes = parseValue(filename, tokens, i + n, 0);
        if (!objRes.isSuccess()) return ParseRes.error(loc, "Expected a value.", objRes);
        n += objRes.n;

        if (!isOperator(tokens, i + n++, Operator.PAREN_CLOSE)) return ParseRes.error(loc, "Expected a closing paren after for.");


        var bodyRes = parseStatement(filename, tokens, i + n);
        if (!bodyRes.isSuccess()) return ParseRes.error(loc, "Expected a for body.", bodyRes);
        n += bodyRes.n;

        return ParseRes.res(new ForInStatement(loc, labelRes.result, isDecl, nameRes.result, varVal, objRes.result, bodyRes.result), n);
    }
    public static ParseRes<TryStatement> parseCatch(String filename, List<Token> tokens, int i) {
        var loc = getLoc(filename, tokens, i);
        int n = 0;

        if (!isIdentifier(tokens, i + n++, "try")) return ParseRes.failed();

        var res = parseStatement(filename, tokens, i + n);
        if (!res.isSuccess()) return ParseRes.error(loc, "Expected an if body.", res);
        n += res.n;

        String name = null;
        Statement catchBody = null, finallyBody = null;


        if (isIdentifier(tokens, i + n, "catch")) {
            n++;
            if (isOperator(tokens, i + n, Operator.PAREN_OPEN)) {
                n++;
                var nameRes = parseIdentifier(tokens, i + n++);
                if (!nameRes.isSuccess()) return ParseRes.error(loc, "Expected a catch variable name.");
                name = nameRes.result;
                if (!isOperator(tokens, i + n++, Operator.PAREN_CLOSE)) return ParseRes.error(loc, "Expected a closing paren after catch variable name.");
            }
    
            var catchRes = parseStatement(filename, tokens, i + n);
            if (!catchRes.isSuccess()) return ParseRes.error(loc, "Expected a catch body.", catchRes);
            n += catchRes.n;
            catchBody = catchRes.result;
        }

        if (isIdentifier(tokens, i + n, "finally")) {
            n++;
            var finallyRes = parseStatement(filename, tokens, i + n);
            if (!finallyRes.isSuccess()) return ParseRes.error(loc, "Expected a finally body.", finallyRes);
            n += finallyRes.n;
            finallyBody = finallyRes.result;
        }

        return ParseRes.res(new TryStatement(loc, res.result, catchBody, finallyBody, name), n);
    }

    public static ParseRes<? extends Statement> parseStatement(String filename, List<Token> tokens, int i) {
        if (isOperator(tokens, i, Operator.SEMICOLON)) return ParseRes.res(new CompoundStatement(getLoc(filename, tokens, i)), 1);
        if (isIdentifier(tokens, i, "with")) return ParseRes.error(getLoc(filename, tokens, i), "'with' statements are not allowed.");
        return ParseRes.any(
            parseVariableDeclare(filename, tokens, i),
            parseReturn(filename, tokens, i),
            parseThrow(filename, tokens, i),
            parseContinue(filename, tokens, i),
            parseBreak(filename, tokens, i),
            parseDebug(filename, tokens, i),
            parseValueStatement(filename, tokens, i),
            parseIf(filename, tokens, i),
            parseWhile(filename, tokens, i),
            parseSwitch(filename, tokens, i),
            parseFor(filename, tokens, i),
            parseForIn(filename, tokens, i),
            parseDoWhile(filename, tokens, i),
            parseCatch(filename, tokens, i),
            parseCompound(filename, tokens, i),
            parseFunction(filename, tokens, i, true)
        );
    }

    public static Statement[] parse(String filename, String raw) {
        var tokens = tokenize(filename, raw);
        var list = new ArrayList<Statement>();
        int i = 0;

        while (true) {
            if (i >= tokens.size()) break;

            var res = Parsing.parseStatement(filename, tokens, i);

            if (res.isError()) throw new SyntaxException(getLoc(filename, tokens, i), res.error);
            else if (res.isFailed()) throw new SyntaxException(getLoc(filename, tokens, i), "Unexpected syntax.");

            i += res.n;

            list.add(res.result);
        }

        return list.toArray(Statement[]::new);
    }

    public static CodeFunction compile(GlobalScope scope, Statement... statements) {
        var target = scope.globalChild();
        var subscope = target.child();
        var res = new ArrayList<Instruction>();
        var body = new CompoundStatement(null, statements);
        // var optimized = body.optimize();
        if (body instanceof CompoundStatement) body = (CompoundStatement)body;
        else body = new CompoundStatement(null, new Statement[] { body });

        subscope.define("this");
        subscope.define("arguments");

        body.declare(target);

        try {
            body.compile(res, subscope);
            FunctionStatement.checkBreakAndCont(res, 0);
        }
        catch (SyntaxException e) {
            res.clear();
            res.add(Instruction.throwSyntax(e));
        }

        if (res.size() != 0 && res.get(res.size() - 1).type == Type.DISCARD) {
            res.set(res.size() - 1, Instruction.ret());
        }
        else res.add(Instruction.ret());

        return new CodeFunction("", subscope.localsCount(), 0, scope, new ValueVariable[0], res.toArray(Instruction[]::new));
    }
    public static CodeFunction compile(GlobalScope scope, String filename, String raw) {
        try {
            return compile(scope, parse(filename, raw));
        }
        catch (SyntaxException e) {
            return new CodeFunction(null, 2, 0, scope, new ValueVariable[0], new Instruction[] { Instruction.throwSyntax(e) });
        }
    }
}
