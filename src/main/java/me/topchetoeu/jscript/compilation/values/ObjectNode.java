package me.topchetoeu.jscript.compilation.values;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.SyntaxException;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.CompoundNode;
import me.topchetoeu.jscript.compilation.FunctionNode;
import me.topchetoeu.jscript.compilation.JavaScript;
import me.topchetoeu.jscript.compilation.Node;
import me.topchetoeu.jscript.compilation.Parameters;
import me.topchetoeu.jscript.compilation.patterns.AssignTarget;
import me.topchetoeu.jscript.compilation.patterns.AssignTargetLike;
import me.topchetoeu.jscript.compilation.patterns.ObjectAssignable;
import me.topchetoeu.jscript.compilation.patterns.Pattern;
import me.topchetoeu.jscript.compilation.patterns.ObjectDestructor.Member;
import me.topchetoeu.jscript.compilation.values.constants.NumberNode;
import me.topchetoeu.jscript.compilation.values.constants.StringNode;
import me.topchetoeu.jscript.compilation.values.operations.AssignNode;

public class ObjectNode extends Node implements AssignTargetLike {
    public static class PropertyMemberNode extends FunctionNode {
        public final Node key;
        public final Pattern argument;

        @Override public String name() {
            if (key instanceof StringNode str) {
                if (isGetter()) return "get " + str.value;
                else return "set " + str.value;
            }
            else return null;
        }

        public boolean isGetter() { return argument == null; }
        public boolean isSetter() { return argument != null; }

        @Override public void compile(CompileResult target, boolean pollute, String name, BreakpointType bp) {
            key.compile(target, true);

            var id = target.addChild(compileBody(target, name, null));
            target.add(_i -> Instruction.loadFunc(id, true, false, false, name, captures(id, target)));

            target.add(Instruction.defProp(isSetter()));
        }

        public PropertyMemberNode(Location loc, Location end, Node key, Pattern argument, CompoundNode body) {
            super(loc, end, argument == null ? new Parameters(Arrays.asList()) : new Parameters(Arrays.asList(argument)), body);
            this.key = key;
            this.argument = argument;
        }

        public static ParseRes<PropertyMemberNode> parse(Source src, int i) {
            var n = Parsing.skipEmpty(src, i);
            var loc = src.loc(i + n);

            var access = Parsing.parseIdentifier(src, i + n);
            if (!access.isSuccess()) return ParseRes.failed();
            if (!access.result.equals("get") && !access.result.equals("set")) return ParseRes.failed();
            n += access.n;

            var name = parsePropName(src, i + n);
            if (!name.isSuccess()) return name.chainError(src.loc(i + n), "Expected a property name after '" + access + "'");
            n += name.n;

            var params = Parameters.parseParameters(src, i + n);
            if (!params.isSuccess()) return params.chainError(src.loc(i + n), "Expected an argument list");
            if (access.result.equals("get") && params.result.params.size() != 0) return ParseRes.error(src.loc(i + n), "Getter must not have any parameters");
            if (access.result.equals("set") && params.result.params.size() != 1) return ParseRes.error(src.loc(i + n), "Setter must have exactly one parameter");
            if (params.result.rest != null) return ParseRes.error(params.result.rest.loc(), "Property members may not have rest arguments");
            n += params.n;

            var body = CompoundNode.parse(src, i + n);
            if (!body.isSuccess()) return body.chainError(src.loc(i + n), "Expected a compound statement for property accessor.");
            n += body.n;

            var end = src.loc(i + n - 1);

            return ParseRes.res(new PropertyMemberNode(
                loc, end, name.result, access.result.equals("get") ? null : params.result.params.get(0), body.result
            ), n);
        }
    }
    public static class MethodMemberNode extends FunctionNode {
        public final Node key;

        @Override public String name() {
            if (key instanceof StringNode str) return str.value;
            else return null;
        }

        @Override public void compile(CompileResult target, boolean pollute, String name, BreakpointType bp) {
            key.compile(target, true);

            var id = target.addChild(compileBody(target, name, null));
            target.add(_i -> Instruction.loadFunc(id, true, false, false, name, captures(id, target)));

            target.add(Instruction.defField());
        }

        public MethodMemberNode(Location loc, Location end, Node key, Parameters params, CompoundNode body) {
            super(loc, end, params, body);
            this.key = key;
        }

        public static ParseRes<MethodMemberNode> parse(Source src, int i) {
            var n = Parsing.skipEmpty(src, i);
            var loc = src.loc(i + n);

            var name = parsePropName(src, i + n);
            if (!name.isSuccess()) return name.chainError();
            n += name.n;

            var params = Parameters.parseParameters(src, i + n);
            if (!params.isSuccess()) return params.chainError(src.loc(i + n), "Expected an argument list");
            n += params.n;

            var body = CompoundNode.parse(src, i + n);
            if (!body.isSuccess()) return body.chainError(src.loc(i + n), "Expected a compound statement for property accessor.");
            n += body.n;

            var end = src.loc(i + n - 1);

            return ParseRes.res(new MethodMemberNode(
                loc, end, name.result, params.result, body.result
            ), n);
        }
    }
    public static class FieldMemberNode extends Node {
        public final Node key;
        public final Node value;

        @Override public void compile(CompileResult target, boolean pollute) {
            key.compile(target, true);

            if (value == null) target.add(Instruction.pushUndefined());
            else value.compile(target, true);

            target.add(Instruction.defField());
        }

        public FieldMemberNode(Location loc, Node key, Node value) {
            super(loc);
            this.key = key;
            this.value = value;
        }

        public static ParseRes<FieldMemberNode> parseObject(Source src, int i) {
            var n = Parsing.skipEmpty(src, i);
            var loc = src.loc(i + n);

            var name = parsePropName(src, i + n);
            if (!name.isSuccess()) return name.chainError();
            n += name.n;
            n += Parsing.skipEmpty(src, i + n);

            if (!src.is(i + n, ":")) return ParseRes.failed();
            n++;

            var value = JavaScript.parseExpression(src, i + n, 2);
            if (!value.isSuccess()) return value.chainError(src.loc(i + n), "Expected a value");
            n += value.n;

            return ParseRes.res(new FieldMemberNode(loc, name.result, value.result), n);
        }

        public static ParseRes<FieldMemberNode> parseShorthand(Source src, int i) {
            var n = Parsing.skipEmpty(src, i);
            var loc = src.loc(i + n);

            var var = VariableNode.parse(src, i + n);
            if (!var.isSuccess()) return var.chainError();
            n += var.n;

            return ParseRes.res(new FieldMemberNode(loc, new StringNode(loc, var.result.name), var.result), n);
        }

        public static ParseRes<FieldMemberNode> parseClass(Source src, int i) {
            var n = Parsing.skipEmpty(src, i);
            var loc = src.loc(i + n);

            var name = parsePropName(src, i + n);
            if (!name.isSuccess()) return name.chainError();
            n += name.n;
            n += Parsing.skipEmpty(src, i + n);

            if (!src.is(i + n, "=")) {
                var end = JavaScript.parseStatement(src, i + n);
                if (!end.isSuccess()) return ParseRes.error(src.loc(i + n), "Expected an end of statement or a field initializer");
                n += end.n;

                return ParseRes.res(new FieldMemberNode(loc, name.result, null), n);
            }
            n++;

            var value = JavaScript.parseExpression(src, i + n, 2);
            if (!value.isSuccess()) return value.chainError(src.loc(i + n), "Expected a value");
            n += value.n;

            return ParseRes.res(new FieldMemberNode(loc, name.result, value.result), n);
        }
    }
    public static class AssignShorthandNode extends Node {
        public final Node key;
        public final AssignTarget target;
        public final Node value;

        @Override public void compile(CompileResult target, boolean pollute) {
            throw new SyntaxException(loc(), "Unexpected assign shorthand in non-destructor context");
        }

        public AssignShorthandNode(Location loc, Node key, AssignTarget target, Node value) {
            super(loc);
            this.key = key;
            this.target = target;
            this.value = value;
        }

        public AssignTarget target() {
            return new AssignNode(loc(), target, value);
        }

        public static ParseRes<AssignShorthandNode> parse(Source src, int i) {
            var n = Parsing.skipEmpty(src, i);
            var loc = src.loc(i + n);

            var var = VariableNode.parse(src, i + n);
            if (!var.isSuccess()) return var.chainError();
            n += var.n;
            n += Parsing.skipEmpty(src, i + n);

            if (!src.is(i + n, "=")) return ParseRes.failed();
            n++;

            var value = JavaScript.parseExpression(src, i + n, 2);
            if (!value.isSuccess()) return value.chainError(src.loc(i + n), "Expected a shorthand initializer");
            n += value.n;

            return ParseRes.res(new AssignShorthandNode(loc, new StringNode(loc, var.result.name), var.result, value.result), n);
        }
    }

    public final List<Node> members;

    // TODO: Implement spreading into object

    // private void compileRestObjBuilder(CompileResult target, int srcDupN) {
    //     var subtarget = target.subtarget();
    //     var src = subtarget.scope.defineTemp();
    //     var dst = subtarget.scope.defineTemp();

    //     target.add(Instruction.loadObj());
    //     target.add(_i -> src.index().toSet(true));
    //     target.add(_i -> dst.index().toSet(destructors.size() > 0));

    //     target.add(Instruction.keys(true, true));
    //     var start = target.size();

    //     target.add(Instruction.dup());
    //     var mid = target.temp();

    //     target.add(_i -> src.index().toGet());
    //     target.add(Instruction.dup(1, 1));
    //     target.add(Instruction.loadMember());

    //     target.add(_i -> dst.index().toGet());
    //     target.add(Instruction.dup(1, 1));
    //     target.add(Instruction.storeMember());

    //     target.add(Instruction.discard());
    //     var end = target.size();
    //     target.add(Instruction.jmp(start - end));
    //     target.set(mid, Instruction.jmpIfNot(end - mid + 1));

    //     target.add(Instruction.discard());

    //     target.add(Instruction.dup(srcDupN, 1));

    //     target.scope.end();
    // }

    @Override public void compile(CompileResult target, boolean pollute) {
        target.add(Instruction.loadObj());

        for (var el : members) {
            target.add(Instruction.dup());
            el.compile(target, false);
        }
    }

    @Override public AssignTarget toAssignTarget() {
        var newMembers = new LinkedList<Member<AssignTarget>>();

        for (var el : members) {
            if (el instanceof FieldMemberNode field) {
                if (field.value instanceof AssignTargetLike target) newMembers.add(new Member<>(field.key, target.toAssignTarget()));
                else throw new SyntaxException(field.value.loc(), "Expected an assignable in deconstructor");
            }
            else if (el instanceof AssignShorthandNode shorthand) newMembers.add(new Member<>(shorthand.key, shorthand.target()));
            else throw new SyntaxException(el.loc(), "Unexpected member in deconstructor");
        }

        return new ObjectAssignable(loc(), newMembers);
    }

    public ObjectNode(Location loc, List<Node> map) {
        super(loc);
        this.members = map;
    }

    private static ParseRes<Node> parseComputePropName(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        if (!src.is(i + n, "[")) return ParseRes.failed();
        n++;

        var val = JavaScript.parseExpression(src, i, 0);
        if (!val.isSuccess()) return val.chainError(src.loc(i + n), "Expected an expression in compute property");
        n += val.n;
        n += Parsing.skipEmpty(src, i + n);

        if (!src.is(i + n, "]")) return ParseRes.error(src.loc(i + n), "Expected a closing bracket after compute property");
        n++;

        return ParseRes.res(val.result, n);
    }
    public static ParseRes<Node> parsePropName(Source src, int i) {
        return ParseRes.first(src, i,
            (s, j) -> {
                var m = Parsing.skipEmpty(s, j);
                var l = s.loc(j + m);

                var r = Parsing.parseIdentifier(s, j + m);
                if (r.isSuccess()) return ParseRes.res(new StringNode(l, r.result), r.n);
                else return r.chainError();
            },
            StringNode::parse,
            NumberNode::parse,
            ObjectNode::parseComputePropName
        );
    }

    public static ParseRes<ObjectNode> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!src.is(i + n, "{")) return ParseRes.failed();
        n++;
        n += Parsing.skipEmpty(src, i + n);

        var members = new LinkedList<Node>();

        if (src.is(i + n, "}")) {
            n++;
            return ParseRes.res(new ObjectNode(loc, members), n);
        }

        while (true) {
            ParseRes<Node> prop = ParseRes.first(src, i + n,
                MethodMemberNode::parse,
                PropertyMemberNode::parse,
                FieldMemberNode::parseObject,
                AssignShorthandNode::parse,
                FieldMemberNode::parseShorthand
            );
            if (!prop.isSuccess()) return prop.chainError(src.loc(i + n), "Expected a member in object literal");
            n += prop.n;

            members.add(prop.result);

            n += Parsing.skipEmpty(src, i + n);
            if (src.is(i + n, ",")) {
                n++;
                n += Parsing.skipEmpty(src, i + n);

                if (src.is(i + n, "}")) {
                    n++;
                    break;
                }

                continue;
            }
            else if (src.is(i + n, "}")) {
                n++;
                break;
            }
            else ParseRes.error(src.loc(i + n), "Expected a comma or a closing brace.");
        }
    
        return ParseRes.res(new ObjectNode(loc, members), n);
    }
}
