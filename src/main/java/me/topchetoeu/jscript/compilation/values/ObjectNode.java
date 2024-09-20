package me.topchetoeu.jscript.compilation.values;

import java.util.LinkedList;
import java.util.List;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.SyntaxException;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.JavaScript;
import me.topchetoeu.jscript.compilation.Node;
import me.topchetoeu.jscript.compilation.members.AssignShorthandNode;
import me.topchetoeu.jscript.compilation.members.FieldMemberNode;
import me.topchetoeu.jscript.compilation.members.Member;
import me.topchetoeu.jscript.compilation.members.MethodMemberNode;
import me.topchetoeu.jscript.compilation.members.PropertyMemberNode;
import me.topchetoeu.jscript.compilation.patterns.AssignTarget;
import me.topchetoeu.jscript.compilation.patterns.AssignTargetLike;
import me.topchetoeu.jscript.compilation.patterns.ObjectPattern;
import me.topchetoeu.jscript.compilation.values.constants.NumberNode;
import me.topchetoeu.jscript.compilation.values.constants.StringNode;

public class ObjectNode extends Node implements AssignTargetLike {
    public final List<Member> members;

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
        for (var el : members) el.compile(target, true, true);
    }

    @Override public AssignTarget toAssignTarget() {
        var newMembers = new LinkedList<ObjectPattern.Member>();

        for (var el : members) {
            if (el instanceof FieldMemberNode field) {
                if (field.value instanceof AssignTargetLike target) newMembers.add(new ObjectPattern.Member(field.key, target.toAssignTarget()));
                else throw new SyntaxException(field.value.loc(), "Expected an assignable in deconstructor");
            }
            else if (el instanceof AssignShorthandNode shorthand) newMembers.add(new ObjectPattern.Member(shorthand.key, shorthand.target()));
            else throw new SyntaxException(el.loc(), "Unexpected member in deconstructor");
        }

        return new ObjectPattern(loc(), newMembers);
    }

    public ObjectNode(Location loc, List<Member> map) {
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

        var members = new LinkedList<Member>();

        if (src.is(i + n, "}")) {
            n++;
            return ParseRes.res(new ObjectNode(loc, members), n);
        }

        while (true) {
            ParseRes<Member> prop = ParseRes.first(src, i + n,
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
