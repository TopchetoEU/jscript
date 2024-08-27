package me.topchetoeu.jscript.compilation.parsing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.compilation.*;
import me.topchetoeu.jscript.compilation.control.*;
import me.topchetoeu.jscript.compilation.scope.LocalScopeRecord;
import me.topchetoeu.jscript.compilation.values.*;
import me.topchetoeu.jscript.compilation.values.constants.BoolStatement;
import me.topchetoeu.jscript.compilation.values.constants.NullStatement;
import me.topchetoeu.jscript.compilation.values.constants.NumberStatement;
import me.topchetoeu.jscript.compilation.values.constants.StringStatement;
import me.topchetoeu.jscript.runtime.exceptions.SyntaxException;

// TODO: this has to be rewritten
// @SourceFile
public class Parsing {
    public static final HashMap<Long, ArrayList<Instruction>> functions = new HashMap<>();

    private static final Set<String> reserved = Set.of(
        "true", "false", "void", "null", "this", "if", "else", "try", "catch",
        "finally", "for", "do", "while", "switch", "case", "default", "new",
        "function", "var", "return", "throw", "typeof", "delete", "break",
        "continue", "debugger", "implements", "interface", "package", "private",
        "protected", "public", "static",

        // Although ES5 allow these, we will comply to ES6 here
        "const", "let",

        // These are allowed too, however our parser considers them keywords
        // We allow yield and await, because they're part of the custom async and generator functions
        "undefined", "arguments", "globalThis", "window", "self"
    );

    public static boolean isDigit(Character c) {
        return c != null && c >= '0' && c <= '9';
    }
    public static boolean isAny(char c, String alphabet) {
        return alphabet.contains(Character.toString(c));
    }

    public static int fromHex(char c) {
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        if (c >= '0' && c <= '9') return c - '0';
        return -1;
    }

    public static int skipEmpty(Source src, int i) {
        int n = 0;

        while (n < src.size() && src.is(i + n, Character::isWhitespace)) n++;

        return n;
    }

    public static ParseRes<Character> parseChar(Source src, int i) {
        int n = 0;

        if (src.is(i + n, '\\')) {
            n++;
            char c = src.at(i + n++);

            if (c == 'b') return ParseRes.res('\b', n);
            else if (c == 't') return ParseRes.res('\t', n);
            else if (c == 'n') return ParseRes.res('\n', n);
            else if (c == 'f') return ParseRes.res('\f', n);
            else if (c == 'r') return ParseRes.res('\r', n);
            else if (c == '0') {
                if (src.is(i + n, Parsing::isDigit)) return ParseRes.error(src.loc(i), "Octal escape sequences are not allowed");
                else return ParseRes.res('\0', n);
            }
            else if (c >= '1' && c <= '9') return ParseRes.error(src.loc(i), "Octal escape sequences are not allowed");
            else if (c == 'x') {
                var newC = 0;

                for (var j = 0; j < 2; j++) {
                    if (i + n >= src.size()) return ParseRes.error(src.loc(i), "Invalid hexadecimal escape sequence.");

                    int val = fromHex(src.at(i + n));
                    if (val == -1) throw new SyntaxException(src.loc(i + n), "Invalid hexadecimal escape sequence.");
                    n++;

                    newC = (newC << 4) | val;
                }

                return ParseRes.res((char)newC, n);
            }
            else if (c == 'u') {
                var newC = 0;

                for (var j = 0; j < 4; j++) {
                    if (i + n >= src.size()) return ParseRes.error(src.loc(i), "Invalid Unicode escape sequence");

                    int val = fromHex(src.at(i + n));
                    if (val == -1) throw new SyntaxException(src.loc(i + n), "Invalid Unicode escape sequence");
                    n++;

                    newC = (newC << 4) | val;
                }

                return ParseRes.res((char)newC, n);
            }
            else if (c == '\n') return ParseRes.res(null, n);
        }

        return ParseRes.res(src.at(i + n), n + 1);
    }

    public static ParseRes<String> parseIdentifier(Source src, int i) {
        var n = skipEmpty(src, i);
        var res = new StringBuilder();
        var first = false;

        while (true) {
            if (i + n > src.size()) break;
            char c = src.at(i + n, '\0');

            if (first && !Character.isLetterOrDigit(c) && c != '_' && c != '$') break;
            if (!first && !Character.isLetter(c) && c != '_' && c != '$') break;
            res.append(c);
            n++;
        }

        if (res.length() <= 0) return ParseRes.failed();
        else return ParseRes.res(res.toString(), n);
    }
    public static ParseRes<String> parseIdentifier(Source src, int i, String test) {
        var n = skipEmpty(src, i);
        var res = new StringBuilder();
        var first = true;

        while (true) {
            if (i + n > src.size()) break;
            char c = src.at(i + n, '\0');

            if (first && !Character.isLetterOrDigit(c) && c != '_' && c != '$') break;
            if (!first && !Character.isLetter(c) && c != '_' && c != '$') break;
            first = false;
            res.append(c);
            n++;
        }

        if (res.length() <= 0) return ParseRes.failed();
        else if (test == null || res.toString().equals(test)) return ParseRes.res(res.toString(), n);
        else return ParseRes.failed();
    }
    public static boolean isIdentifier(Source src, int i, String test) {
        return parseIdentifier(src, i, test).isSuccess();
    }

    public static ParseRes<String> parseOperator(Source src, int i, String op) {
        var n = skipEmpty(src, i);

        if (src.is(i + n, op)) return ParseRes.res(op, n + op.length());
        else return ParseRes.failed();
    }

    public static ParseRes<Boolean> parseStatementEnd(Source src, int i) {
        var n = skipEmpty(src, i);
        if (i >= src.size()) return ParseRes.res(true, n + 1);

        for (var j = i; j < i + n; j++) {
            if (src.is(j, '\n')) return ParseRes.res(true, n);
        }

        if (src.is(i + n, ';')) return ParseRes.res(true, n + 1);
        if (src.is(i + n, '}')) return ParseRes.res(true, n);

        return ParseRes.failed();
    }
    public static boolean checkVarName(String name) {
        return !reserved.contains(name);
    }

    public static ParseRes<List<String>> parseParamList(Source src, int i) {
        var n = skipEmpty(src, i);

        var openParen = parseOperator(src, i + n, "(");
        if (!openParen.isSuccess()) return openParen.chainError(src.loc(i + n), "Expected a parameter list.");
        n += openParen.n;

        var args = new ArrayList<String>();

        var closeParen = parseOperator(src, i + n, ")");
        n += closeParen.n;

        if (!closeParen.isSuccess()) {
            while (true) {
                var argRes = parseIdentifier(src, i + n);
                if (argRes.isSuccess()) {
                    args.add(argRes.result);
                    n++;

                    n += skipEmpty(src, i);

                    if (src.is(i + n, ",")) {
                        n++;
                        n += skipEmpty(src, i + n);
                    }
                    if (src.is(i + n, ")")) {
                        n++;
                        break;
                    }
                }
                else return ParseRes.error(src.loc(i + n), "Expected an argument, or a closing brace.");
            }
        }

        return ParseRes.res(args, n);
    }

    public static ParseRes<? extends Statement> parseParens(Source src, int i) {
        int n = 0;

        var openParen = parseOperator(src, i + n, "(");
        if (!openParen.isSuccess()) return openParen.chainError();
        n += openParen.n;

        var res = parseValue(src, i + n, 0);
        if (!res.isSuccess()) return res.chainError(src.loc(i + n), "Expected an expression in parens");
        n += res.n;

        var closeParen = parseOperator(src, i + n, ")");
        if (!closeParen.isSuccess()) return closeParen.chainError(src.loc(i + n), "Expected a closing paren");
        n += closeParen.n;

        return ParseRes.res(res.result, n);
    }
    public static ParseRes<? extends Statement> parseSimple(Source src, int i, boolean statement) {
        return ParseRes.first(src, i,
            (a, b) -> statement ? ParseRes.failed() : ObjectStatement.parse(a, b),
            (a, b) -> statement ? ParseRes.failed() : FunctionStatement.parseFunction(a, b, false),
            VariableStatement::parse,
            Parsing::parseLiteral,
            StringStatement::parse,
            RegexStatement::parse,
            NumberStatement::parse,
            ChangeStatement::parsePrefixDecrease,
            ChangeStatement::parsePrefixIncrease,
            OperationStatement::parsePrefix,
            ArrayStatement::parse,
            Parsing::parseParens,
            CallStatement::parseNew,
            TypeofStatement::parse,
            DiscardStatement::parse,
            DeleteStatement::parse
        );
    }

    public static ParseRes<? extends Statement> parseLiteral(Source src, int i) {
        var n = skipEmpty(src, i);
        var loc = src.loc(i + n);

        var id = parseIdentifier(src, i);
        if (!id.isSuccess()) return id.chainError();
        n += id.n;

        if (id.result.equals("true")) return ParseRes.res(new BoolStatement(loc, true), n);
        if (id.result.equals("false")) return ParseRes.res(new BoolStatement(loc, false), n);
        if (id.result.equals("undefined")) return ParseRes.res(new DiscardStatement(loc, null), n);
        if (id.result.equals("null")) return ParseRes.res(new NullStatement(loc), n);
        if (id.result.equals("this")) return ParseRes.res(new VariableIndexStatement(loc, 0), n);
        if (id.result.equals("arguments")) return ParseRes.res(new VariableIndexStatement(loc, 1), n);
        if (id.result.equals("globalThis")) return ParseRes.res(new GlobalThisStatement(loc), n);

        return ParseRes.failed();
    }
    public static ParseRes<? extends Statement> parseValue(Source src, int i, int precedence, boolean statement) {
        var n = skipEmpty(src, i);
        Statement prev = null;

        while (true) {
            if (prev == null) {
                var res = parseSimple(src, i + n, statement);
                if (res.isSuccess()) {
                    n += res.n;
                    prev = res.result;
                }
                else if (res.isError()) return res.chainError();
                else break;
            }
            else {
                var _prev = prev;
                ParseRes<Statement> res = ParseRes.first(src, i + n,
                    (s, j) -> OperationStatement.parseInstanceof(s, j, _prev, precedence),
                    (s, j) -> OperationStatement.parseIn(s, j, _prev, precedence),
                    (s, j) -> LazyOrStatement.parse(s, j, _prev, precedence),
                    (s, j) -> LazyAndStatement.parse(s, j, _prev, precedence),
                    (s, j) -> ChangeStatement.parsePostfixIncrease(s, j, _prev, precedence),
                    (s, j) -> ChangeStatement.parsePostfixDecrease(s, j, _prev, precedence),
                    (s, j) -> AssignableStatement.parse(s, j, _prev, precedence),
                    (s, j) -> OperationStatement.parseOperator(s, j, _prev, precedence),
                    (s, j) -> IfStatement.parseTernary(s, j, _prev, precedence),
                    (s, j) -> IndexStatement.parseMember(s, j, _prev, precedence),
                    (s, j) -> IndexStatement.parseIndex(s, j, _prev, precedence),
                    (s, j) -> CallStatement.parseCall(s, j, _prev, precedence),
                    (s, j) -> CompoundStatement.parseComma(s, j, _prev, precedence)
                );

                if (res.isSuccess()) {
                    n += res.n;
                    prev = res.result;
                    continue;
                }
                else if (res.isError()) return res.chainError();

                break;
            }
        }
    
        if (prev == null) return ParseRes.failed();
        else return ParseRes.res(prev, n);
    }
    public static ParseRes<? extends Statement> parseValue(Source src, int i, int precedence) {
        return parseValue(src, i, precedence, false);
    }

    public static ParseRes<? extends Statement> parseValueStatement(Source src, int i) {
        var res = parseValue(src, i, 0, true);
        if (!res.isSuccess()) return res.chainError();

        var end = parseStatementEnd(src, i + res.n);
        if (!end.isSuccess()) return ParseRes.error(src.loc(i + res.n), "Expected an end of statement");

        return res.addN(end.n);
    }
    public static ParseRes<? extends Statement> parseStatement(Source src, int i) {
        var n = skipEmpty(src, i);

        if (src.is(i + n, ";")) return ParseRes.res(new DiscardStatement(src.loc(i+ n), null), n + 1);
        if (isIdentifier(src, i + n, "with")) return ParseRes.error(src.loc(i + n), "'with' statements are not allowed.");

        ParseRes<? extends Statement> res = ParseRes.first(src, i + n,
            VariableDeclareStatement::parse,
            ReturnStatement::parse,
            ThrowStatement::parse,
            ContinueStatement::parse,
            BreakStatement::parse,
            DebugStatement::parse,
            IfStatement::parse,
            WhileStatement::parse,
            SwitchStatement::parse,
            ForStatement::parse,
            ForInStatement::parse,
            ForOfStatement::parse,
            DoWhileStatement::parse,
            TryStatement::parse,
            CompoundStatement::parse,
            (s, j) -> FunctionStatement.parseFunction(s, j, true),
            Parsing::parseValueStatement
        );
        return res.addN(n);
    }

    public static Statement[] parse(Filename filename, String raw) {
        var src = new Source(filename, raw);
        var list = new ArrayList<Statement>();
        int i = 0;

        while (true) {
            if (i >= src.size()) break;

            var res = Parsing.parseStatement(src, i);

            if (res.isError()) throw new SyntaxException(src.loc(i), res.error);
            else if (res.isFailed()) throw new SyntaxException(src.loc(i), "Unexpected syntax");

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

    private static ParseRes<Double> parseHex(Source src, int i) {
        int n = 0;
        double res = 0;

        while (true) {
            int digit = Parsing.fromHex(src.at(i + n, '\0'));
            if (digit < 0) {
                if (n <= 0) return ParseRes.failed();
                else return ParseRes.res(res, n);
            }
            n++;

            res *= 16;
            res += digit;
        }
    }
    private static ParseRes<Double> parseOct(Source src, int i) {
        int n = 0;
        double res = 0;

        while (true) {
            int digit = src.at(i + n, '\0') - '0';
            if (digit < 0 || digit > 9) break;
            if (digit > 7) return ParseRes.error(src.loc(i + n), "Digits in octal literals must be from 0 to 7, encountered " + digit);

            if (digit < 0) {
                if (n <= 0) return ParseRes.failed();
                else return ParseRes.res(res, n);
            }
            n++;

            res *= 8;
            res += digit;
        }

        return ParseRes.res(res, n);
    }

    public static ParseRes<String> parseString(Source src, int i) {
        var n = skipEmpty(src, i);
    
        char quote;
    
        if (src.is(i + n, '\'')) quote = '\'';
        else if (src.is(i + n, '"')) quote = '"';
        else return ParseRes.failed();
        n++;
    
        var res = new StringBuilder();
    
        while (true) {
            if (i + n >= src.size()) return ParseRes.error(src.loc(i + n), "Unterminated string literal");
            if (src.is(i + n, quote)) {
                n++;
                return ParseRes.res(res.toString(), n);
            }

            var charRes = parseChar(src, i + n);
            if (!charRes.isSuccess()) return charRes.chainError(src.loc(i + n), "Invalid character");
            n += charRes.n;

            if (charRes.result != null) res.append(charRes.result);
        }
    }
    public static ParseRes<Double> parseNumber(Source src, int i) {
        var n = skipEmpty(src, i);

        double whole = 0;
        double fract = 0;
        long exponent = 0;
        boolean parsedAny = false;

        if (src.is(i + n, "0x") || src.is(i + n, "0X")) {
            n += 2;

            var res = parseHex(src, i);
            if (!res.isSuccess()) return res.chainError(src.loc(i + n), "Incomplete hexadecimal literal");
            else return res.addN(2);
        }
        else if (src.is(i + n, "0o") || src.is(i + n, "0O")) {
            n += 2;

            var res = parseOct(src, i);
            if (!res.isSuccess()) return res.chainError(src.loc(i + n), "Incomplete octal literal");
            else return res.addN(2);
        }
        else if (src.is(i + n, '0')) {
            n++;
            parsedAny = true;
            if (src.is(i + n, Parsing::isDigit)) return ParseRes.error(src.loc(i + n), "Decimals with leading zeroes are not allowed");
        }

        while (src.is(i + n, Parsing::isDigit)) {
            parsedAny = true;
            whole *= 10;
            whole += src.at(i + n++) - '0';
        }

        if (src.is(i + n, '.')) {
            parsedAny = true;
            n++;

            while (src.is(i + n, Parsing::isDigit)) {
                fract += src.at(i + n++) - '0';
                fract /= 10;
            }
        }

        if (src.is(i + n, 'e') || src.is(i + n, 'E')) {
            n++;
            parsedAny = true;
            boolean negative = src.is(i + n, '-');
            boolean parsedE = false;
            if (negative) n++;

            while (src.is(i + n, Parsing::isDigit)) {
                parsedE = true;
                exponent *= 10;

                if (negative) exponent -= src.at(i + n++) - '0';
                else exponent += src.at(i + n++) - '0';
            }

            if (!parsedE) return ParseRes.error(src.loc(i + n), "Incomplete number exponent");
        }

        if (!parsedAny) return ParseRes.failed();
        else return ParseRes.res((whole + fract) * NumberStatement.power(10, exponent), n);
    }
}
