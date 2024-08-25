package me.topchetoeu.jscript.compilation.parsing;

import java.lang.invoke.CallSite;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.common.ParseRes;
import me.topchetoeu.jscript.compilation.*;
import me.topchetoeu.jscript.compilation.control.*;
import me.topchetoeu.jscript.compilation.scope.LocalScopeRecord;
import me.topchetoeu.jscript.compilation.values.*;
import me.topchetoeu.jscript.runtime.exceptions.SyntaxException;

// TODO: this has to be rewritten
// @SourceFile
public class Parsing {
    public static interface Parser<T> {
        ParseRes<T> parse(Filename filename, List<Token> tokens, int i);
    }

    public static class ObjProp {
        public final String name;
        public final String access;
        public final FunctionStatement func;

        public ObjProp(String name, String access, FunctionStatement func) {
            this.name = name;
            this.access = access;
            this.func = func;
        }
    }

    public static final HashMap<Long, ArrayList<Instruction>> functions = new HashMap<>();

    private static final HashSet<String> reserved = new HashSet<String>();
    static {
        reserved.add("true");
        reserved.add("false");
        reserved.add("void");
        reserved.add("null");
        reserved.add("this");
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
        reserved.add("debugger");
        reserved.add("implements");
        reserved.add("interface");
        reserved.add("package");
        reserved.add("private");
        reserved.add("protected");
        reserved.add("public");
        reserved.add("static");
        // Although ES5 allow these, we will comply to ES6 here
        reserved.add("const");
        reserved.add("let");
        // These are allowed too, however our parser considers them keywords
        reserved.add("undefined");
        reserved.add("arguments");
        reserved.add("globalThis");
        reserved.add("window");
        reserved.add("self");
        // We allow yield and await, because they're part of the custom async and generator functions
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

    private static void addToken(StringBuilder currToken, int currStage, int line, int lastStart, Filename filename, List<RawToken> tokens) {
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
    // preformance reasons my ass
    private static ArrayList<RawToken> splitTokens(Filename filename, String raw) {
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
                    if (c == '-') {
                        if (currToken.toString().endsWith("e")) currStage = CURR_NEG_SCIENTIFIC_NOT;
                    }
                    if (currStage == CURR_SCIENTIFIC_NOT && !isDigit(c)) {
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
                    if (isAlphanumeric(c) || c == '_' || c == '$') {
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
                    else throw new SyntaxException(new Location(line, start, filename), String.format("Unrecognized character %s.", c));
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

    public static int fromHex(char c) {
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        if (c >= '0' && c <= '9') return c - '0';
        return -1;
    }

    public static boolean inBounds(List<Token> tokens, int i) {
        return i >= 0 && i < tokens.size();
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
            int dig = fromHex(literal.charAt(i));
            res += dig;
        }

        return res;
    }

    public static Double parseNumber(boolean octals, String value) {
        if (value.startsWith("0x") || value.startsWith("0X")) {
            if (value.length() == 2) return null;
            return parseHex(value);
        }
        if (value.endsWith("e") || value.endsWith("E") || value.endsWith("-")) return null;

        int i = 0;
        double res = 0, dotDivisor = 1;
        boolean e = false, dot = false;
        int exponent = 0;

        for (; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '.') { dot = true; break; }
            if (c == 'e') { e = true; break; }
            if (!isDigit(c)) break;

            res = res * 10 + c - '0';
        }

        if (dot) for (i++; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == 'e') { e = true; break; }
            if (!isDigit(c)) break;

            res += (c - '0') / (dotDivisor *= 10);
        }

        if (e) for (i++; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!isDigit(c)) break;
            exponent = 10 * exponent + c - '0';
        }

        if (exponent < 0) for (int j = 0; j < -exponent; j++) res /= 10;
        else for (int j = 0; j < exponent; j++) res *= 10;

        return res;
    }
    private static double parseNumber(Location loc, String value) {
        var res = parseNumber(false, value);
        if (res == null)
            throw new SyntaxException(loc, "Invalid number format.");
        else return res;
    }

    private static List<Token> parseTokens(Filename filename, Collection<RawToken> tokens) {
        var res = new ArrayList<Token>();

        for (var el : tokens) {
            var loc = new Location(el.line, el.start, filename);
            switch (el.type) {
                case LITERAL: res.add(Token.identifier(el.line, el.start, el.value)); break;
                case NUMBER: res.add(Token.number(el.line, el.start, parseNumber(loc, el.value), el.value)); break;
                case STRING: res.add(Token.string(el.line, el.start, parseString(loc, el.value), el.value)); break;
                case REGEX: res.add(Token.regex(el.line, el.start, parseRegex(loc, el.value), el.value)); break;
                case OPERATOR:
                    Operator op = Operator.parse(el.value);
                    if (op == null) throw new SyntaxException(loc, String.format("Unrecognized operator '%s'.", el.value));
                    res.add(Token.operator(el.line, el.start, op));
                    break;
            }
        }

        return res;
    }

    public static List<Token> tokenize(Filename filename, String raw) {
        return parseTokens(filename, splitTokens(filename, raw));
    }

    public static Location getLoc(Filename filename, List<Token> tokens, int i) {
        if (tokens.size() == 0 || tokens.size() == 0) return new Location(1, 1, filename);
        if (i >= tokens.size()) i = tokens.size() - 1;
        return new Location(tokens.get(i).line, tokens.get(i).start, filename);
    }
    public static int getLines(List<Token> tokens) {
        if (tokens.size() == 0) return 1;
        return tokens.get(tokens.size() - 1).line;
    }

    public static ParseRes<String> parseIdentifier(List<Token> tokens, int i) {
        if (inBounds(tokens, i)) {
            if (tokens.get(i).isIdentifier()) {
                return ParseRes.res(tokens.get(i).identifier(), 1);
            }
            else return ParseRes.failed();
        }
        else return ParseRes.failed();
    }
    public static ParseRes<Operator> parseOperator(List<Token> tokens, int i) {
        if (inBounds(tokens, i)) {
            if (tokens.get(i).isOperator()) {
                return ParseRes.res(tokens.get(i).operator(), 1);
            }
            else return ParseRes.failed();
        }
        else return ParseRes.failed();
    }

    public static boolean isIdentifier(List<Token> tokens, int i, String lit) {
        if (inBounds(tokens, i)) {
            if (tokens.get(i).isIdentifier(lit)) {
                return true;
            }
            else return false;
        }
        else return false;
    }
    public static boolean isOperator(List<Token> tokens, int i, Operator op) {
        if (inBounds(tokens, i)) {
            if (tokens.get(i).isOperator(op)) {
                return true;
            }
            else return false;
        }
        else return false;
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

    public static ParseRes<List<String>> parseParamList(Filename filename, List<Token> tokens, int i) {
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

    public static ParseRes<? extends Statement> parseParens(Filename filename, List<Token> tokens, int i) {
        int n = 0;
        if (!isOperator(tokens, i + n++, Operator.PAREN_OPEN)) return ParseRes.failed();

        var res = parseValue(filename, tokens, i + n, 0);
        if (!res.isSuccess()) return res;
        n += res.n;

        if (!isOperator(tokens, i + n++, Operator.PAREN_CLOSE)) return ParseRes.failed();

        return ParseRes.res(res.result, n);
    }
    public static ParseRes<? extends Statement> parseSimple(Filename filename, List<Token> tokens, int i, boolean statement) {
        var res = new ArrayList<ParseRes<? extends Statement>>();

        if (!statement) {
            res.add(ObjectStatement.parse(filename, tokens, i));
            res.add(FunctionStatement.parseFunction(filename, tokens, i, false));
        }

        res.addAll(List.of(
            VariableStatement.parseVariable(filename, tokens, i),
            parseLiteral(filename, tokens, i),
            ConstantStatement.parseString(filename, tokens, i),
            RegexStatement.parse(filename, tokens, i),
            ConstantStatement.parseNumber(filename, tokens, i),
            OperationStatement.parseUnary(filename, tokens, i),
            ArrayStatement.parse(filename, tokens, i),
            ChangeStatement.parsePrefix(filename, tokens, i),
            parseParens(filename, tokens, i),
            CallStatement.parseNew(filename, tokens, i),
            TypeofStatement.parse(filename, tokens, i),
            DiscardStatement.parse(filename, tokens, i),
            DeleteStatement.parseDelete(filename, tokens, i)
        ));

        return ParseRes.any(res);
    }

    public static ParseRes<? extends Statement> parseLiteral(Filename filename, List<Token> tokens, int i) {
        var loc = getLoc(filename, tokens, i);
        var id = parseIdentifier(tokens, i);
        if (!id.isSuccess()) return id.transform();

        if (id.result.equals("true")) return ParseRes.res(new ConstantStatement(loc, true), 1);
        if (id.result.equals("false")) return ParseRes.res(new ConstantStatement(loc, false), 1);
        if (id.result.equals("undefined")) return ParseRes.res(ConstantStatement.ofUndefined(loc), 1);
        if (id.result.equals("null")) return ParseRes.res(ConstantStatement.ofNull(loc), 1);
        if (id.result.equals("this")) return ParseRes.res(new VariableIndexStatement(loc, 0), 1);
        if (id.result.equals("arguments")) return ParseRes.res(new VariableIndexStatement(loc, 1), 1);
        if (id.result.equals("globalThis")) return ParseRes.res(new GlobalThisStatement(loc), 1);

        return ParseRes.failed();
    }
    public static ParseRes<? extends Statement> parseValue(Filename filename, List<Token> tokens, int i, int precedence, boolean statement) {
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
                    OperationStatement.parseOperator(filename, tokens, i + n, prev, precedence),
                    IndexStatement.parseMember(filename, tokens, i + n, prev, precedence),
                    IndexStatement.parseIndex(filename, tokens, i + n, prev, precedence),
                    CallStatement.parseCall(filename, tokens, i + n, prev, precedence),
                    ChangeStatement.parsePostfix(filename, tokens, i + n, prev, precedence),
                    OperationStatement.parseInstanceof(filename, tokens, i + n, prev, precedence),
                    OperationStatement.parseIn(filename, tokens, i + n, prev, precedence),
                    CompoundStatement.parseComma(filename, tokens, i + n, prev, precedence),
                    IfStatement.parseTernary(filename, tokens, i + n, prev, precedence)
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
    public static ParseRes<? extends Statement> parseValue(Filename filename, List<Token> tokens, int i, int precedence) {
        return parseValue(filename, tokens, i, precedence, false);
    }

    public static ParseRes<? extends Statement> parseValueStatement(Filename filename, List<Token> tokens, int i) {
        var valRes = parseValue(filename, tokens, i, 0, true);
        if (!valRes.isSuccess()) return valRes.transform();

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
    public static ParseRes<? extends Statement> parseStatement(Filename filename, List<Token> tokens, int i) {
        if (isOperator(tokens, i, Operator.SEMICOLON)) return ParseRes.res(new CompoundStatement(getLoc(filename, tokens, i), false), 1);
        if (isIdentifier(tokens, i, "with")) return ParseRes.error(getLoc(filename, tokens, i), "'with' statements are not allowed.");
        return ParseRes.any(
            VariableDeclareStatement.parse(filename, tokens, i),
            ReturnStatement.parseReturn(filename, tokens, i),
            ThrowStatement.parseThrow(filename, tokens, i),
            ContinueStatement.parseContinue(filename, tokens, i),
            BreakStatement.parseBreak(filename, tokens, i),
            DebugStatement.parse(filename, tokens, i),
            IfStatement.parse(filename, tokens, i),
            WhileStatement.parseWhile(filename, tokens, i),
            SwitchStatement.parse(filename, tokens, i),
            ForStatement.parse(filename, tokens, i),
            ForInStatement.parse(filename, tokens, i),
            ForOfStatement.parse(filename, tokens, i),
            DoWhileStatement.parseDoWhile(filename, tokens, i),
            TryStatement.parse(filename, tokens, i),
            CompoundStatement.parse(filename, tokens, i),
            FunctionStatement.parseFunction(filename, tokens, i, true),
            parseValueStatement(filename, tokens, i)
        );
    }

    public static Statement[] parse(Filename filename, String raw) {
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

    public static CompileResult compile(Statement ...statements) {
        var target = new CompileResult(new LocalScopeRecord());
        var stm = new CompoundStatement(null, true, statements);

        target.scope.define("this");
        target.scope.define("arguments");

        try {
            stm.compile(target, true);
            FunctionStatement.checkBreakAndCont(target, 0);
        }
        catch (SyntaxException e) {
            target = new CompileResult(new LocalScopeRecord());

            target.scope.define("this");
            target.scope.define("arguments");

            target.add(Instruction.throwSyntax(e)).setLocation(stm.loc());
        }

        target.add(Instruction.ret()).setLocation(stm.loc());

        return target;
    }
    public static CompileResult compile(Filename filename, String raw) {
        return compile(parse(filename, raw));
    }
}
