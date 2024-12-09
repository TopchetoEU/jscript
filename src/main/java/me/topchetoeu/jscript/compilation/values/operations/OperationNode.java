package me.topchetoeu.jscript.compilation.values.operations;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Operation;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.JavaScript;
import me.topchetoeu.jscript.compilation.Node;
import me.topchetoeu.jscript.compilation.patterns.AssignTargetLike;
import me.topchetoeu.jscript.compilation.patterns.ChangeTarget;

public class OperationNode extends Node {
    private static interface OperatorFactory {
        String token();
        int precedence();
        ParseRes<Node> construct(Source src, int i, Node prev);
    }

    private static class NormalOperatorFactory implements OperatorFactory {
        public final String token;
        public final int precedence;
        public final Operation operation;

        @Override public int precedence() { return precedence; }
        @Override public String token() { return token; }
        @Override public ParseRes<Node> construct(Source src, int i, Node prev) {
            var loc = src.loc(i);

            var other = JavaScript.parseExpression(src, i, precedence + 1);
            if (!other.isSuccess()) return other.chainError(src.loc(i + other.n), String.format("Expected a value after '%s'", token));
            return ParseRes.res(new OperationNode(loc, operation, prev, (Node)other.result), other.n);
        }

        public NormalOperatorFactory(String token, int precedence, Operation operation) {
            this.token = token;
            this.precedence = precedence;
            this.operation = operation;
        }
    }
    private static class AssignmentOperatorFactory implements OperatorFactory {
        public final String token;
        public final int precedence;
        public final Operation operation;

        @Override public int precedence() { return precedence; }
        @Override public String token() { return token; }
        @Override public ParseRes<Node> construct(Source src, int i, Node prev) {
            var loc = src.loc(i);

            if (operation == null) {
                if (!(prev instanceof AssignTargetLike target)) return ParseRes.error(loc, String.format("Expected an assignable expression before '%s'", token));

                var other = JavaScript.parseExpression(src, i, precedence);
                if (!other.isSuccess()) return other.chainError(src.loc(i + other.n), String.format("Expected a value after '%s'", token));

                return ParseRes.res(new AssignNode(loc, target.toAssignTarget(), other.result), other.n);
            }
            else {
                if (!(prev instanceof ChangeTarget target)) return ParseRes.error(loc, String.format("Expected a changeable expression before '%s'", token));

                var other = JavaScript.parseExpression(src, i, precedence);
                if (!other.isSuccess()) return other.chainError(src.loc(i + other.n), String.format("Expected a value after '%s'", token));

                return ParseRes.res(new ChangeNode(loc, target, other.result, operation), other.n);
            }
        }

        public AssignmentOperatorFactory(String token, int precedence, Operation operation) {
            this.token = token;
            this.precedence = precedence;
            this.operation = operation;
        }
    }
    private static class LazyAndFactory implements OperatorFactory {
        @Override public int precedence() { return 4; }
        @Override public String token() { return "&&"; }
        @Override public ParseRes<Node> construct(Source src, int i, Node prev) {
            var loc = src.loc(i);

            var other = JavaScript.parseExpression(src, i, 5);
            if (!other.isSuccess()) return other.chainError(src.loc(i + other.n), "Expected a value after '&&'");
            return ParseRes.res(new LazyAndNode(loc, prev, (Node)other.result), other.n);
        }
    }
    private static class LazyOrFactory implements OperatorFactory {
        @Override public int precedence() { return 5; }
        @Override public String token() { return "||"; }
        @Override public ParseRes<Node> construct(Source src, int i, Node prev) {
            var loc = src.loc(i);

            var other = JavaScript.parseExpression(src, i, 6);
            if (!other.isSuccess()) return other.chainError(src.loc(i + other.n), "Expected a value after '||'");
            return ParseRes.res(new LazyOrNode(loc, prev, (Node)other.result), other.n);
        }
    }

    public final Node[] args;
    public final Operation operation;

	@Override public void compileFunctions(CompileResult target) {
		for (var arg : args) arg.compileFunctions(target);
	}

    @Override public void compile(CompileResult target, boolean pollute) {
        for (var arg : args) {
            arg.compile(target, true);
        }

        target.add(Instruction.operation(operation));
        if (!pollute) target.add(Instruction.discard());
    }

    public OperationNode(Location loc, Operation operation, Node ...args) {
        super(loc);
        this.operation = operation;
        this.args = args;
    }

    private static final Map<String, OperatorFactory> factories = Arrays.asList(
        new NormalOperatorFactory("*", 13, Operation.MULTIPLY),
        new NormalOperatorFactory("/", 12, Operation.DIVIDE),
        new NormalOperatorFactory("%", 12, Operation.MODULO),
        new NormalOperatorFactory("-", 11, Operation.SUBTRACT),
        new NormalOperatorFactory("+", 11, Operation.ADD),
        new NormalOperatorFactory(">>", 10, Operation.SHIFT_RIGHT),
        new NormalOperatorFactory("<<", 10, Operation.SHIFT_LEFT),
        new NormalOperatorFactory(">>>", 10, Operation.USHIFT_RIGHT),
        new NormalOperatorFactory(">", 9, Operation.GREATER),
        new NormalOperatorFactory("<", 9, Operation.LESS),
        new NormalOperatorFactory(">=", 9, Operation.GREATER_EQUALS),
        new NormalOperatorFactory("<=", 9, Operation.LESS_EQUALS),
        new NormalOperatorFactory("!=", 8, Operation.LOOSE_NOT_EQUALS),
        new NormalOperatorFactory("!==", 8, Operation.NOT_EQUALS),
        new NormalOperatorFactory("==", 8, Operation.LOOSE_EQUALS),
        new NormalOperatorFactory("===", 8, Operation.EQUALS),
        new NormalOperatorFactory("&", 7, Operation.AND),
        new NormalOperatorFactory("^", 6, Operation.XOR),
        new NormalOperatorFactory("|", 5, Operation.OR),

        new AssignmentOperatorFactory("=", 2, null),
        new AssignmentOperatorFactory("*=", 2, Operation.MULTIPLY),
        new AssignmentOperatorFactory("/=", 2, Operation.DIVIDE),
        new AssignmentOperatorFactory("%=", 2, Operation.MODULO),
        new AssignmentOperatorFactory("-=", 2, Operation.SUBTRACT),
        new AssignmentOperatorFactory("+=", 2, Operation.ADD),
        new AssignmentOperatorFactory(">>=", 2, Operation.SHIFT_RIGHT),
        new AssignmentOperatorFactory("<<=", 2, Operation.SHIFT_LEFT),
        new AssignmentOperatorFactory(">>>=", 2, Operation.USHIFT_RIGHT),
        new AssignmentOperatorFactory("&=", 2, Operation.AND),
        new AssignmentOperatorFactory("^=", 2, Operation.XOR),
        new AssignmentOperatorFactory("|=", 2, Operation.OR),

        new LazyAndFactory(),
        new LazyOrFactory()
    ).stream().collect(Collectors.toMap(v -> v.token(), v -> v));

    private static final List<String> operatorsByLength = factories.keySet().stream().sorted((a, b) -> -a.compareTo(b)).collect(Collectors.toList());

    public static ParseRes<OperationNode> parsePrefix(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        Operation operation = null;
        String op;

        if (src.is(i + n, op = "+")) operation = Operation.POS;
        else if (src.is(i + n, op = "-")) operation = Operation.NEG;
        else if (src.is(i + n, op = "~")) operation = Operation.INVERSE;
        else if (src.is(i + n, op = "!")) operation = Operation.NOT;
        else return ParseRes.failed();

        n++;

        var res = JavaScript.parseExpression(src, i + n, 14);

        if (res.isSuccess()) return ParseRes.res(new OperationNode(loc, operation, res.result), n + res.n);
        else return res.chainError(src.loc(i + n), String.format("Expected a value after the unary operator '%s'.", op));
    }
    public static ParseRes<OperationNode> parseInstanceof(Source src, int i, Node prev, int precedence) {
        if (precedence > 9) return ParseRes.failed();

        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        var kw = Parsing.parseIdentifier(src, i + n, "instanceof");
        if (!kw.isSuccess()) return kw.chainError();
        n += kw.n;

        var valRes = JavaScript.parseExpression(src, i + n, 10);
        if (!valRes.isSuccess()) return valRes.chainError(src.loc(i + n), "Expected a value after 'instanceof'.");
        n += valRes.n;

        return ParseRes.res(new OperationNode(loc, Operation.INSTANCEOF, prev, valRes.result), n);
    }
    public static ParseRes<OperationNode> parseIn(Source src, int i, Node prev, int precedence) {
        if (precedence > 9) return ParseRes.failed();

        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        var kw = Parsing.parseIdentifier(src, i + n, "in");
        if (!kw.isSuccess()) return kw.chainError();
        n += kw.n;

        var valRes = JavaScript.parseExpression(src, i + n, 10);
        if (!valRes.isSuccess()) return valRes.chainError(src.loc(i + n), "Expected a value after 'in'.");
        n += valRes.n;

        return ParseRes.res(new OperationNode(loc, Operation.IN, valRes.result, prev), n);
    }
    public static ParseRes<? extends Node> parseOperator(Source src, int i, Node prev, int precedence) {
        var n = Parsing.skipEmpty(src, i);

        for (var token : operatorsByLength) {
            var factory = factories.get(token);

            if (!src.is(i + n, token)) continue;
            if (factory.precedence() < precedence) return ParseRes.failed();

            n += token.length();
            n += Parsing.skipEmpty(src, i + n);

            var res = factory.construct(src, i + n, prev);
            return res.addN(n);
        }

        return ParseRes.failed();
    }
}
