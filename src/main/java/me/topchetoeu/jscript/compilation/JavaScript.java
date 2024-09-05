package me.topchetoeu.jscript.compilation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.common.parsing.Filename;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.control.BreakNode;
import me.topchetoeu.jscript.compilation.control.ContinueNode;
import me.topchetoeu.jscript.compilation.control.DebugNode;
import me.topchetoeu.jscript.compilation.control.DeleteNode;
import me.topchetoeu.jscript.compilation.control.DoWhileNode;
import me.topchetoeu.jscript.compilation.control.ForInNode;
import me.topchetoeu.jscript.compilation.control.ForNode;
import me.topchetoeu.jscript.compilation.control.ForOfNode;
import me.topchetoeu.jscript.compilation.control.IfNode;
import me.topchetoeu.jscript.compilation.control.ReturnNode;
import me.topchetoeu.jscript.compilation.control.SwitchNode;
import me.topchetoeu.jscript.compilation.control.ThrowNode;
import me.topchetoeu.jscript.compilation.control.TryNode;
import me.topchetoeu.jscript.compilation.control.WhileNode;
import me.topchetoeu.jscript.compilation.scope.FunctionScope;
import me.topchetoeu.jscript.compilation.values.ArgumentsNode;
import me.topchetoeu.jscript.compilation.values.ArrayNode;
import me.topchetoeu.jscript.compilation.values.ObjectNode;
import me.topchetoeu.jscript.compilation.values.RegexNode;
import me.topchetoeu.jscript.compilation.values.ThisNode;
import me.topchetoeu.jscript.compilation.values.VariableNode;
import me.topchetoeu.jscript.compilation.values.constants.BoolNode;
import me.topchetoeu.jscript.compilation.values.constants.NullNode;
import me.topchetoeu.jscript.compilation.values.constants.NumberNode;
import me.topchetoeu.jscript.compilation.values.constants.StringNode;
import me.topchetoeu.jscript.compilation.values.operations.CallNode;
import me.topchetoeu.jscript.compilation.values.operations.ChangeNode;
import me.topchetoeu.jscript.compilation.values.operations.DiscardNode;
import me.topchetoeu.jscript.compilation.values.operations.IndexNode;
import me.topchetoeu.jscript.compilation.values.operations.OperationNode;
import me.topchetoeu.jscript.compilation.values.operations.TypeofNode;
import me.topchetoeu.jscript.runtime.exceptions.SyntaxException;

public final class JavaScript {
    public static enum DeclarationType {
        VAR(false, false),
        CONST(true, true),
        LET(true, false);

        public final boolean strict, readonly;

        private DeclarationType(boolean strict, boolean readonly) {
            this.strict = strict;
            this.readonly = readonly;
        }
    }

    static final Set<String> reserved = Set.of(
        "true", "false", "void", "null", "this", "if", "else", "try", "catch",
        "finally", "for", "do", "while", "switch", "case", "default", "new",
        "function", "var", "return", "throw", "typeof", "delete", "break",
        "continue", "debugger", "implements", "interface", "package", "private",
        "protected", "public", "static", "arguments"
    );

    public static ParseRes<? extends Node> parseParens(Source src, int i) {
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

    public static ParseRes<? extends Node> parseSimple(Source src, int i, boolean statement) {
        return ParseRes.first(src, i,
            (s, j) -> statement ? ParseRes.failed() : ObjectNode.parse(s, j),
            (s, j) -> statement ? ParseRes.failed() : FunctionNode.parseFunction(s, j, false),
            JavaScript::parseLiteral,
            StringNode::parse,
            RegexNode::parse,
            NumberNode::parse,
            ChangeNode::parsePrefixDecrease,
            ChangeNode::parsePrefixIncrease,
            OperationNode::parsePrefix,
            ArrayNode::parse,
            FunctionArrowNode::parse,
            JavaScript::parseParens,
            CallNode::parseNew,
            TypeofNode::parse,
            DiscardNode::parse,
            DeleteNode::parse,
            VariableNode::parse
        );
    }

    public static ParseRes<? extends Node> parseLiteral(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        var id = Parsing.parseIdentifier(src, i);
        if (!id.isSuccess()) return id.chainError();
        n += id.n;

        if (id.result.equals("true")) return ParseRes.res(new BoolNode(loc, true), n);
        if (id.result.equals("false")) return ParseRes.res(new BoolNode(loc, false), n);
        if (id.result.equals("null")) return ParseRes.res(new NullNode(loc), n);
        if (id.result.equals("this")) return ParseRes.res(new ThisNode(loc), n);
        if (id.result.equals("arguments")) return ParseRes.res(new ArgumentsNode(loc), n);

        return ParseRes.failed();
    }

    public static ParseRes<? extends Node> parseExpression(Source src, int i, int precedence, boolean statement) {
        var n = Parsing.skipEmpty(src, i);
        Node prev = null;

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
                ParseRes<Node> res = ParseRes.first(src, i + n,
                    (s, j) -> OperationNode.parseInstanceof(s, j, _prev, precedence),
                    (s, j) -> OperationNode.parseIn(s, j, _prev, precedence),
                    (s, j) -> ChangeNode.parsePostfixIncrease(s, j, _prev, precedence),
                    (s, j) -> ChangeNode.parsePostfixDecrease(s, j, _prev, precedence),
                    (s, j) -> OperationNode.parseOperator(s, j, _prev, precedence),
                    (s, j) -> IfNode.parseTernary(s, j, _prev, precedence),
                    (s, j) -> IndexNode.parseMember(s, j, _prev, precedence),
                    (s, j) -> IndexNode.parseIndex(s, j, _prev, precedence),
                    (s, j) -> CallNode.parseCall(s, j, _prev, precedence),
                    (s, j) -> CompoundNode.parseComma(s, j, _prev, precedence)
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

    public static ParseRes<? extends Node> parseExpression(Source src, int i, int precedence) {
        return parseExpression(src, i, precedence, false);
    }

    public static ParseRes<? extends Node> parseExpressionStatement(Source src, int i) {
        var res = parseExpression(src, i, 0, true);
        if (!res.isSuccess()) return res.chainError();

        var end = JavaScript.parseStatementEnd(src, i + res.n);
        if (!end.isSuccess()) return ParseRes.error(src.loc(i + res.n), "Expected an end of statement");

        return res.addN(end.n);
    }

    public static ParseRes<? extends Node> parseStatement(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);

        if (src.is(i + n, ";")) return ParseRes.res(new DiscardNode(src.loc(i+ n), null), n + 1);
        if (Parsing.isIdentifier(src, i + n, "with")) return ParseRes.error(src.loc(i + n), "'with' statements are not allowed.");

        ParseRes<? extends Node> res = ParseRes.first(src, i + n,
            VariableDeclareNode::parse,
            ReturnNode::parse,
            ThrowNode::parse,
            ContinueNode::parse,
            BreakNode::parse,
            DebugNode::parse,
            IfNode::parse,
            WhileNode::parse,
            SwitchNode::parse,
            ForNode::parse,
            ForInNode::parse,
            ForOfNode::parse,
            DoWhileNode::parse,
            TryNode::parse,
            CompoundNode::parse,
            (s, j) -> FunctionNode.parseFunction(s, j, true),
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

    public static ParseRes<Parameters> parseParameters(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);

        var openParen = Parsing.parseOperator(src, i + n, "(");
        if (!openParen.isSuccess()) return openParen.chainError(src.loc(i + n), "Expected a parameter list");
        n += openParen.n;

        var params = new ArrayList<Parameter>();

        var closeParen = Parsing.parseOperator(src, i + n, ")");
        n += closeParen.n;

        if (!closeParen.isSuccess()) {
            while (true) {
                n += Parsing.skipEmpty(src, i + n);

                if (src.is(i + n, "...")) {
                    n += 3;
                    var restLoc = src.loc(i);

                    var restName = Parsing.parseIdentifier(src, i + n);
                    if (!restName.isSuccess()) return ParseRes.error(src.loc(i + n), "Expected a rest parameter");
                    n += restName.n;
                    n += Parsing.skipEmpty(src, i + n);

                    if (!src.is(i + n, ")")) return ParseRes.error(src.loc(i + n),  "Expected an end of parameters list after rest parameter");
                    n++;

                    return ParseRes.res(new Parameters(params, restName.result, restLoc), n);
                }

                var paramLoc = src.loc(i);

                var name = Parsing.parseIdentifier(src, i + n);
                if (!name.isSuccess()) return ParseRes.error(src.loc(i + n), "Expected a parameter or a closing brace");
                n += name.n;
                n += Parsing.skipEmpty(src, i + n);

                if (src.is(i + n, "=")) {
                    n++;

                    var val = parseExpression(src, i + n, 2);
                    if (!val.isSuccess()) return openParen.chainError(src.loc(i + n), "Expected a parameter default value");
                    n += val.n;
                    n += Parsing.skipEmpty(src, i + n);

                    params.add(new Parameter(paramLoc, name.result, val.result));
                }
                else params.add(new Parameter(paramLoc, name.result, null));

                if (src.is(i + n, ",")) {
                    n++;
                    n += Parsing.skipEmpty(src, i + n);
                }

                if (src.is(i + n, ")")) {
                    n++;
                    break;
                }
            }
        }

        return ParseRes.res(new Parameters(params), n);
    }

    public static ParseRes<DeclarationType> parseDeclarationType(Source src, int i) {
        var res = Parsing.parseIdentifier(src, i);
        if (!res.isSuccess()) return res.chainError();

        if (res.result.equals("var")) return ParseRes.res(DeclarationType.VAR, res.n);
        if (res.result.equals("let")) return ParseRes.res(DeclarationType.LET, res.n);
        if (res.result.equals("const")) return ParseRes.res(DeclarationType.CONST, res.n);

        return ParseRes.failed();
    }

    public static Node[] parse(Environment env, Filename filename, String raw) {
        var src = new Source(env, filename, raw);
        var list = new ArrayList<Node>();
        int i = 0;

        while (true) {
            if (i >= src.size()) break;

            var res = parseStatement(src, i);

            if (res.isError()) throw new SyntaxException(res.errorLocation, res.error);
            else if (res.isFailed()) throw new SyntaxException(src.loc(i), "Unexpected syntax");

            i += res.n;
            i += Parsing.skipEmpty(src, i);

            list.add(res.result);
        }

        return list.toArray(Node[]::new);
    }

    public static boolean checkVarName(String name) {
        return !JavaScript.reserved.contains(name);
    }

    public static CompileResult compile(Environment env, Node ...statements) {
        var func = new FunctionValueNode(null, null, new Parameters(List.of()), new CompoundNode(null, true, statements), null);
        var res = func.compileBody(env, new FunctionScope(true), true, null, null);
        res.buildTask.run();
        return res;
    }

    public static CompileResult compile(Environment env, Filename filename, String raw) {
        return JavaScript.compile(env, JavaScript.parse(env, filename, raw));
    }
    public static CompileResult compile(Filename filename, String raw) {
        var env = new Environment();
        return JavaScript.compile(env, JavaScript.parse(env, filename, raw));
    }

    public static ParseRes<String> parseLabel(Source src, int i) {
        int n = Parsing.skipEmpty(src, i);
    
        var nameRes = Parsing.parseIdentifier(src, i + n);
        if (!nameRes.isSuccess()) return nameRes.chainError();
        n += nameRes.n;
        n += Parsing.skipEmpty(src, i + n);
    
        if (!src.is(i + n, ":")) return ParseRes.failed();
        n++;
    
        return ParseRes.res(nameRes.result, n);
    }
}
