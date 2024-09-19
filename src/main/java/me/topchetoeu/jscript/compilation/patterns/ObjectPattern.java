package me.topchetoeu.jscript.compilation.patterns;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.SyntaxException;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Node;
import me.topchetoeu.jscript.compilation.JavaScript.DeclarationType;
import me.topchetoeu.jscript.compilation.values.ObjectNode;
import me.topchetoeu.jscript.compilation.values.VariableNode;
import me.topchetoeu.jscript.compilation.values.constants.StringNode;
import me.topchetoeu.jscript.compilation.values.operations.IndexNode;

public class ObjectPattern extends Node implements Pattern {
    public static final class Member {
        public final Node key;
        public final AssignTarget consumable;

        public Member(Node key, AssignTarget consumer) {
            this.key = key;
            this.consumable = consumer;
        }
    }

    public final List<Member> members;

    public void compile(CompileResult target, Consumer<AssignTarget> consumer, boolean pollute) {
        for (var el : members) {
            target.add(Instruction.dup());
            IndexNode.indexLoad(target, el.key, true);
            consumer.accept(el.consumable);
        }

        if (!pollute) target.add(Instruction.discard());
    }

    @Override public void destructDeclResolve(CompileResult target) {
        for (var t : members) {
            if (t.consumable instanceof Pattern p) p.destructDeclResolve(target);
            else throw new SyntaxException(t.consumable.loc(), "Unexpected non-pattern in destruct context");
        }
    }

    @Override public void destruct(CompileResult target, DeclarationType decl, boolean shouldDeclare) {
        compile(target, t -> {
            if (t instanceof Pattern p) p.destruct(target, decl, shouldDeclare);
            else throw new SyntaxException(t.loc(), "Unexpected non-pattern in destruct context");
        }, false);
    }

    @Override public void afterAssign(CompileResult target, boolean pollute) {
        compile(target, t -> t.assign(target, false), pollute);
    }

    @Override public void declare(CompileResult target, DeclarationType decl, boolean lateInitializer) {
        if (lateInitializer) {
            for (var t : members) {
                if (t.consumable instanceof Pattern p) p.declare(target, decl, lateInitializer);
                else throw new SyntaxException(t.consumable.loc(), "Unexpected non-pattern in destruct context");
            }
        }
        else throw new SyntaxException(loc(), "Object pattern must be initialized");
    }

    public ObjectPattern(Location loc, List<Member> members) {
        super(loc);
        this.members = members;
    }

    private static ParseRes<Member> parseShorthand(Source src, int i) {
        ParseRes<Pattern> res = ParseRes.first(src, i,
            AssignPattern::parse,
            VariableNode::parse
        );

        if (res.isSuccess()) {
            if (res.result instanceof AssignPattern assign) {
                if (assign.assignable instanceof VariableNode var) {
                    return ParseRes.res(new Member(new StringNode(var.loc(), var.name), res.result), res.n);
                }
            }
            else if (res.result instanceof VariableNode var) {
                return ParseRes.res(new Member(new StringNode(var.loc(), var.name), res.result), res.n);
            }
        }

        return res.chainError();
    }
    private static ParseRes<Member> parseKeyed(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);

        var key = ObjectNode.parsePropName(src, i + n);
        if (!key.isSuccess()) return key.chainError();
        n += key.n;
        n += Parsing.skipEmpty(src, i + n);

        if (!src.is(i + n , ":")) return ParseRes.failed();
        n++;

        ParseRes<Pattern> res = Pattern.parse(src, i + n, true);
        if (!res.isSuccess()) return ParseRes.error(src.loc(i + n), "Expected a pattern after colon");
        n += res.n;

        return ParseRes.res(new Member(key.result, res.result), n);
    }

    public static ParseRes<ObjectPattern> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!src.is(i + n, "{")) return ParseRes.failed();
        n++;
        n += Parsing.skipEmpty(src, i + n);

        var members = new LinkedList<Member>();

        if (src.is(i + n, "}")) {
            n++;
            return ParseRes.res(new ObjectPattern(loc, members), n);
        }

        while (true) {
            ParseRes<Member> prop = ParseRes.first(src, i + n,
                ObjectPattern::parseKeyed,
                ObjectPattern::parseShorthand
            );

            if (!prop.isSuccess()) return prop.chainError(src.loc(i + n), "Expected a member in object pattern");
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
    
        return ParseRes.res(new ObjectPattern(loc, members), n);
    }
}
