package me.topchetoeu.jscript.compilation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.topchetoeu.jscript.common.SyntaxException;
import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.common.environment.Key;
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
import me.topchetoeu.jscript.compilation.control.IfNode;
import me.topchetoeu.jscript.compilation.control.ReturnNode;
import me.topchetoeu.jscript.compilation.control.SwitchNode;
import me.topchetoeu.jscript.compilation.control.ThrowNode;
import me.topchetoeu.jscript.compilation.control.TryNode;
import me.topchetoeu.jscript.compilation.control.WhileNode;
import me.topchetoeu.jscript.compilation.scope.FunctionScope;
import me.topchetoeu.jscript.compilation.values.ArgumentsNode;
import me.topchetoeu.jscript.compilation.values.ArrayNode;
import me.topchetoeu.jscript.compilation.values.GlobalThisNode;
import me.topchetoeu.jscript.compilation.values.ObjectNode;
import me.topchetoeu.jscript.compilation.values.RegexNode;
import me.topchetoeu.jscript.compilation.values.SuperNode;
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
import me.topchetoeu.jscript.compilation.values.operations.PostfixNode;
import me.topchetoeu.jscript.compilation.values.operations.TypeofNode;

public final class JavaScript {
    public static enum DeclarationType {
		@Deprecated
        VAR;
    }

    public static final Key<Environment> COMPILE_ROOT = Key.of();

    static final Set<String> reserved = new HashSet<>(Arrays.asList(
        "true", "false", "void", "null", "this", "if", "else", "try", "catch",
        "finally", "for", "do", "while", "switch", "case", "default", "new",
        "function", "var", "return", "throw", "typeof", "delete", "break",
        "continue", "debugger", "implements", "interface", "package", "private",
        "protected", "public", "static", "arguments", "class", "extends"
    ));

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
        if (id.result.equals("super")) return ParseRes.res(new SuperNode(loc), n);
        if (id.result.equals("arguments")) return ParseRes.res(new ArgumentsNode(loc), n);
        if (id.result.equals("globalThis")) return ParseRes.res(new GlobalThisNode(loc), n);

        return ParseRes.failed();
    }

    public static ParseRes<Node> parseExpression(Source src, int i, int precedence, boolean statement) {
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
                    (s, j) -> PostfixNode.parsePostfixIncrease(s, j, _prev, precedence),
                    (s, j) -> PostfixNode.parsePostfixDecrease(s, j, _prev, precedence),
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

    public static ParseRes<Node> parseExpression(Source src, int i, int precedence) {
        return parseExpression(src, i, precedence, false);
    }

    public static ParseRes<Node> parseExpressionStatement(Source src, int i) {
        var res = parseExpression(src, i, 0, true);
        if (!res.isSuccess()) return res.chainError();

        var end = JavaScript.parseStatementEnd(src, i + res.n);
        if (!end.isSuccess()) return ParseRes.error(src.loc(i + res.n), "Expected an end of statement");

        return res.addN(end.n);
    }

    public static ParseRes<Node> parseStatement(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);

        if (src.is(i + n, ";")) return ParseRes.res(new DiscardNode(src.loc(i+ n), null), n + 1);
        if (Parsing.isIdentifier(src, i + n, "with")) return ParseRes.error(src.loc(i + n), "'with' statements are not allowed.");

        ParseRes<Node> res = ParseRes.first(src, i + n,
            VariableDeclareNode::parse,
            ReturnNode::parse,
            ThrowNode::parse,
            ContinueNode::parse,
            BreakNode::parse,
            DebugNode::parse,
            IfNode::parse,
            WhileNode::parse,
            SwitchNode::parse,
            ForInNode::parse,
            ForNode::parse,
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
        if (i + n >= src.size()) return ParseRes.res(true, n);

        for (var j = i; j < i + n; j++) {
            if (src.is(j, '\n')) return ParseRes.res(true, n);
        }

        if (src.is(i + n, ';')) return ParseRes.res(true, n + 1);
        if (src.is(i + n, '}')) return ParseRes.res(true, n);

        return ParseRes.failed();
    }

    public static ParseRes<Boolean> parseDeclarationType(Source src, int i) {
        var res = Parsing.parseIdentifier(src, i);
        if (!res.isSuccess()) return res.chainError();

        if (res.result.equals("var")) return ParseRes.res(true, res.n);

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

        return list.toArray(new Node[0]);
    }

    public static boolean checkVarName(String name) {
        return !JavaScript.reserved.contains(name);
    }

    public static CompileResult compile(Environment env, Node ...statements) {
        env = env.child();
        env.add(COMPILE_ROOT, env);

        var func = new FunctionValueNode(null, null, Arrays.asList(), new CompoundNode(null, statements), null);
        var res = func.compileBody(env, new FunctionScope(true), true, null, null);
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

	public static ParseRes<List<VariableNode>> parseParameters(Source src, int i) {
	    var n = Parsing.skipEmpty(src, i);
	
	    var openParen = Parsing.parseOperator(src, i + n, "(");
	    if (!openParen.isSuccess()) return openParen.chainError(src.loc(i + n), "Expected a parameter list");
	    n += openParen.n;
	
	    var params = new ArrayList<VariableNode>();
	
	    var closeParen = Parsing.parseOperator(src, i + n, ")");
	    n += closeParen.n;
	
	    if (!closeParen.isSuccess()) {
	        while (true) {
	            n += Parsing.skipEmpty(src, i + n);
	
	            var param = VariableNode.parse(src, i + n);
	            if (!param.isSuccess()) return ParseRes.error(src.loc(i + n), "Expected a parameter or a closing brace");
	            n += param.n;
	            n += Parsing.skipEmpty(src, i + n);
	
	            params.add(param.result);
	
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
	
	    return ParseRes.res(params, n);
	}
}
