package me.topchetoeu.jscript.compilation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.common.parsing.Filename;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.control.BreakStatement;
import me.topchetoeu.jscript.compilation.control.ContinueStatement;
import me.topchetoeu.jscript.compilation.control.DebugStatement;
import me.topchetoeu.jscript.compilation.control.DeleteStatement;
import me.topchetoeu.jscript.compilation.control.DoWhileStatement;
import me.topchetoeu.jscript.compilation.control.ForInStatement;
import me.topchetoeu.jscript.compilation.control.ForOfStatement;
import me.topchetoeu.jscript.compilation.control.ForStatement;
import me.topchetoeu.jscript.compilation.control.IfStatement;
import me.topchetoeu.jscript.compilation.control.ReturnStatement;
import me.topchetoeu.jscript.compilation.control.SwitchStatement;
import me.topchetoeu.jscript.compilation.control.ThrowStatement;
import me.topchetoeu.jscript.compilation.control.TryStatement;
import me.topchetoeu.jscript.compilation.control.WhileStatement;
import me.topchetoeu.jscript.compilation.scope.LocalScopeRecord;
import me.topchetoeu.jscript.compilation.values.ArrayStatement;
import me.topchetoeu.jscript.compilation.values.FunctionStatement;
import me.topchetoeu.jscript.compilation.values.GlobalThisStatement;
import me.topchetoeu.jscript.compilation.values.ObjectStatement;
import me.topchetoeu.jscript.compilation.values.RegexStatement;
import me.topchetoeu.jscript.compilation.values.VariableStatement;
import me.topchetoeu.jscript.compilation.values.constants.BoolStatement;
import me.topchetoeu.jscript.compilation.values.constants.NullStatement;
import me.topchetoeu.jscript.compilation.values.constants.NumberStatement;
import me.topchetoeu.jscript.compilation.values.constants.StringStatement;
import me.topchetoeu.jscript.compilation.values.operations.CallStatement;
import me.topchetoeu.jscript.compilation.values.operations.ChangeStatement;
import me.topchetoeu.jscript.compilation.values.operations.DiscardStatement;
import me.topchetoeu.jscript.compilation.values.operations.IndexStatement;
import me.topchetoeu.jscript.compilation.values.operations.OperationStatement;
import me.topchetoeu.jscript.compilation.values.operations.TypeofStatement;
import me.topchetoeu.jscript.compilation.values.operations.VariableIndexStatement;
import me.topchetoeu.jscript.runtime.exceptions.SyntaxException;

public class JavaScript {
    static final Set<String> reserved = Set.of(
        "true", "false", "void", "null", "this", "if", "else", "try", "catch",
        "finally", "for", "do", "while", "switch", "case", "default", "new",
        "function", "var", "return", "throw", "typeof", "delete", "break",
        "continue", "debugger", "implements", "interface", "package", "private",
        "protected", "public", "static"
    );

    public static ParseRes<? extends Statement> parseParens(Source src, int i) {
        int n = 0;

        var openParen = Parsing.parseOperator(src, i + n, "(");
        if (!openParen.isSuccess()) return openParen.chainError();
        n += openParen.n;

        var res = JavaScript.parseExpression(src, i + n, 0);
        if (!res.isSuccess()) return res.chainError(src.loc(i + n), "Expected an expression in parens");
        n += res.n;

        var closeParen = Parsing.parseOperator(src, i + n, ")");
        if (!closeParen.isSuccess()) return closeParen.chainError(src.loc(i + n), "Expected a closing paren");
        n += closeParen.n;

        return ParseRes.res(res.result, n);
    }

    public static ParseRes<? extends Statement> parseSimple(Source src, int i, boolean statement) {
        return ParseRes.first(src, i,
            (s, j) -> statement ? ParseRes.failed() : ObjectStatement.parse(s, j),
            (s, j) -> statement ? ParseRes.failed() : FunctionStatement.parseFunction(s, j, false),
            JavaScript::parseLiteral,
            StringStatement::parse,
            RegexStatement::parse,
            NumberStatement::parse,
            ChangeStatement::parsePrefixDecrease,
            ChangeStatement::parsePrefixIncrease,
            OperationStatement::parsePrefix,
            ArrayStatement::parse,
            JavaScript::parseParens,
            CallStatement::parseNew,
            TypeofStatement::parse,
            DiscardStatement::parse,
            DeleteStatement::parse,
            VariableStatement::parse
        );
    }

    public static ParseRes<? extends Statement> parseLiteral(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        var id = Parsing.parseIdentifier(src, i);
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

    public static ParseRes<? extends Statement> parseExpression(Source src, int i, int precedence, boolean statement) {
        var n = Parsing.skipEmpty(src, i);
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
                    (s, j) -> ChangeStatement.parsePostfixIncrease(s, j, _prev, precedence),
                    (s, j) -> ChangeStatement.parsePostfixDecrease(s, j, _prev, precedence),
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

    public static ParseRes<? extends Statement> parseExpression(Source src, int i, int precedence) {
        return parseExpression(src, i, precedence, false);
    }

    public static ParseRes<? extends Statement> parseExpressionStatement(Source src, int i) {
        var res = parseExpression(src, i, 0, true);
        if (!res.isSuccess()) return res.chainError();

        var end = JavaScript.parseStatementEnd(src, i + res.n);
        if (!end.isSuccess()) return ParseRes.error(src.loc(i + res.n), "Expected an end of statement");

        return res.addN(end.n);
    }

    public static ParseRes<? extends Statement> parseStatement(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);

        if (src.is(i + n, ";")) return ParseRes.res(new DiscardStatement(src.loc(i+ n), null), n + 1);
        if (Parsing.isIdentifier(src, i + n, "with")) return ParseRes.error(src.loc(i + n), "'with' statements are not allowed.");

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
            JavaScript::parseExpressionStatement
        );
        return res.addN(n);
    }

    public static ParseRes<Boolean> parseStatementEnd(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        if (i >= src.size()) return ParseRes.res(true, n + 1);

        for (var j = i; j < i + n; j++) {
            if (src.is(j, '\n')) return ParseRes.res(true, n);
        }

        if (src.is(i + n, ';')) return ParseRes.res(true, n + 1);
        if (src.is(i + n, '}')) return ParseRes.res(true, n);

        return ParseRes.failed();
    }

    public static ParseRes<List<String>> parseParamList(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);

        var openParen = Parsing.parseOperator(src, i + n, "(");
        if (!openParen.isSuccess()) return openParen.chainError(src.loc(i + n), "Expected a parameter list.");
        n += openParen.n;

        var args = new ArrayList<String>();

        var closeParen = Parsing.parseOperator(src, i + n, ")");
        n += closeParen.n;

        if (!closeParen.isSuccess()) {
            while (true) {
                var argRes = Parsing.parseIdentifier(src, i + n);
                if (argRes.isSuccess()) {
                    args.add(argRes.result);
                    n += argRes.n;
                    n += Parsing.skipEmpty(src, i);

                    if (src.is(i + n, ",")) {
                        n++;
                        n += Parsing.skipEmpty(src, i + n);
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

    public static Statement[] parse(Environment env, Filename filename, String raw) {
        var src = new Source(env, filename, raw);
        var list = new ArrayList<Statement>();
        int i = 0;

        while (true) {
            if (i >= src.size()) break;

            var res = parseStatement(src, i);

            if (res.isError()) throw new SyntaxException(src.loc(i), res.error);
            else if (res.isFailed()) throw new SyntaxException(src.loc(i), "Unexpected syntax");

            i += res.n;

            list.add(res.result);
        }

        return list.toArray(Statement[]::new);
    }

    public static boolean checkVarName(String name) {
        return !JavaScript.reserved.contains(name);
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

    public static CompileResult compile(Environment env, Filename filename, String raw) {
        return JavaScript.compile(JavaScript.parse(env, filename, raw));
    }
}
