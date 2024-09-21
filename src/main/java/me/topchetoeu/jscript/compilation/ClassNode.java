package me.topchetoeu.jscript.compilation;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.SyntaxException;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.common.environment.Key;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.members.FieldMemberNode;
import me.topchetoeu.jscript.compilation.members.Member;
import me.topchetoeu.jscript.compilation.members.MethodMemberNode;
import me.topchetoeu.jscript.compilation.members.PropertyMemberNode;
import me.topchetoeu.jscript.compilation.scope.FunctionScope;

public abstract class ClassNode extends FunctionNode {
    public static final class ClassBody {
        public final List<Member> staticMembers;
        public final List<FieldMemberNode> protoFields;
        public final List<Member> protoMembers;
        public final Parameters constructorParameters;
        public final CompoundNode constructorBody;
        public final Node superExpr;
        public final boolean hasConstr;

        public ClassBody(
            List<Member> staticMembers, List<FieldMemberNode> protoFields, List<Member> protoMembers,
            Parameters constructorParameters, CompoundNode constructorBody,
            Node superExpr, boolean hasConstr
        ) {
            this.staticMembers = staticMembers;
            this.protoFields = protoFields;
            this.protoMembers = protoMembers;
            this.constructorParameters = constructorParameters;
            this.constructorBody = constructorBody;
            this.superExpr = superExpr;
            this.hasConstr = hasConstr;
        }
    }

    public static final Key<Environment> CLASS_ROOT = Key.of();
    public static final Key<Consumer<CompileResult>> SUPER = Key.of();
    public static final Key<Consumer<CompileResult>> SUPER_PROTO = Key.of();
    public static final Key<Consumer<CompileResult>> SUPER_CONSTR = Key.of();
    public static final Key<Consumer<CompileResult>> ON_SUPER_CALL = Key.of();

    public final ClassBody body;
    public final String name;

    @Override public String name() { return name; }

    public void compileStatic(CompileResult target) {
        for (var member : body.staticMembers) {
            member.compile(target, true, false);
        }
    }
    public void compilePrototype(CompileResult target) {
        if (body.protoMembers.size() > 0) {
            target.add(Instruction.dup());
            target.add(Instruction.loadMember("prototype"));

            for (var i = 0; i < body.protoMembers.size() - 1; i++) {
                body.protoMembers.get(i).compile(target, true, false);
            }

            body.protoMembers.get(body.protoMembers.size() - 1).compile(target, false, false);
        }
    }

    private void compileFieldInits(CompileResult target) {
        for (var member : body.protoFields) {
            target.add(Instruction.loadThis());
            member.compile(target, false, true);
        }
    }

    @Override protected void compilePreBody(CompileResult target) {
        if (target.env.hasNotNull(SUPER_PROTO)) {
            if (!body.hasConstr) {
                throw new SyntaxException(loc(), "Default constructors in derived classes not supported");
                // compileFieldInits(target);
            }
        }
        else compileFieldInits(target);
    }

    @Override public void compile(CompileResult target, boolean pollute, String name, BreakpointType bp) {
        if (body.superExpr == null) {
            var id = target.addChild(compileBody(target, name, null));
            target.add(_i -> Instruction.loadFunc(id, false, true, false, false, name, captures(id, target)));
            compileStatic(target);
            compilePrototype(target);
        }
        else {
            var subtarget = target.subtarget().rootEnvironment(JavaScript.COMPILE_ROOT);
            subtarget.scope.singleEntry = true;
            subtarget.beginScope();
            var protoVar = target.scope.defineTemp();
            var constrVar = target.scope.defineTemp();

            subtarget.env.add(SUPER_PROTO, t -> {
                var i = t.scope.get(protoVar, false);
                t.add(_i -> i.index().toGet());
            });
            subtarget.env.add(SUPER_CONSTR, t -> {
                var i = t.scope.get(constrVar, false);
                t.add(_i -> i.index().toGet());
            });

            var staticTarget = subtarget.subEnvironment();
            staticTarget.env.add(SUPER, subtarget.env.get(SUPER_CONSTR));
            staticTarget.env.add(CLASS_ROOT, staticTarget.env);

            var protoTarget = subtarget.subEnvironment();
            protoTarget.env.add(SUPER, subtarget.env.get(SUPER_PROTO));
            protoTarget.env.add(CLASS_ROOT, protoTarget.env);

            var constrEnv = subtarget.env.child();
            constrEnv.add(SUPER, subtarget.env.get(SUPER_PROTO));
            constrEnv.add(ON_SUPER_CALL, this::compileFieldInits);
            constrEnv.add(CLASS_ROOT, constrEnv);

            var id = target.addChild(compileBody(constrEnv, new FunctionScope(subtarget.scope), false, name, null));
            target.add(_i -> Instruction.loadFunc(id, false, true, false, true, name, captures(id, target)));

            body.superExpr.compile(target, true);

            target.add(Instruction.extend());
            target.add(Instruction.dup(1, 0));
            target.add(Instruction.loadMember("prototype"));
            target.add(_i -> protoVar.index().toInit());
            target.add(_i -> constrVar.index().toInit());

            compileStatic(staticTarget);
            compilePrototype(protoTarget);
            subtarget.endScope();
        }
    }

    public ClassNode(Location loc, Location end, String name, ClassBody body) {
        super(loc, end, body.constructorParameters, body.constructorBody);

        this.name = name;
        this.body = body;
    }

    public static ParseRes<Member> parseMember(Source src, int i) {
        return ParseRes.first(src, i,
            PropertyMemberNode::parse,
            FieldMemberNode::parseClass,
            MethodMemberNode::parse
        );
    }

    public static ParseRes<ClassBody> parseBody(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        ParseRes<Node> superExpr = ParseRes.failed();

        if (Parsing.isIdentifier(src, i + n, "extends")) {
            n += 7;

            superExpr = JavaScript.parseExpression(src, i + n, 14);
            if (!superExpr.isSuccess()) return superExpr.chainError(src.loc(i + n), "Expected an expression after 'extends'");
            n += superExpr.n;
            n += Parsing.skipEmpty(src, i + n);
        }

        if (!src.is(i + n, "{")) return ParseRes.error(src.loc(i + n), "Expected a class body");
        n++;
        n += Parsing.skipEmpty(src, i + n);

        var fields = new LinkedList<FieldMemberNode>();
        var members = new LinkedList<Member>();
        var statics = new LinkedList<Member>();

        var params = new Parameters(new ArrayList<>());
        var body = new CompoundNode(loc, false);
        var hasConstr = false;

        if (src.is(i + n, "}")) {
            n++;
            return ParseRes.res(new ClassBody(statics, fields, members, params, body, superExpr.result, false), n);
        }

        while (true) {
            ParseRes<Member> prop = parseMember(src, i + n);

            if (prop.isSuccess()) {
                n += prop.n;

                if (prop.result instanceof FieldMemberNode field) fields.add(field);
                else if (prop.result instanceof MethodMemberNode method && method.name().equals("constructor")) {
                    if (hasConstr) return ParseRes.error(loc, "A class may only have one constructor");

                    params = method.params;
                    body = method.body;
                    hasConstr = true;
                }
                else members.add(prop.result);
            }
            else if (Parsing.isIdentifier(src, i + n, "static")) {
                n += 6;

                var staticProp = parseMember(src, i + n);
                if (!staticProp.isSuccess()) {
                    if (prop.isError()) return prop.chainError();
                    else return staticProp.chainError(src.loc(i + n), "Expected a member after 'static' keyword");
                }
                n += staticProp.n;

                statics.add(staticProp.result);
            }
            else {
                var end = JavaScript.parseStatementEnd(src, i + n);
                if (end.isSuccess()) n += end.n;
                else return ParseRes.error(src.loc(i + n), "Expected a member, end of statement or a closing colon");
            }

            n += Parsing.skipEmpty(src, i + n);

            if (src.is(i + n, "}")) {
                n++;
                break;
            }
            // else return ParseRes.error(src.loc(i + n), "Expected a comma or a closing brace.");
        }

        return ParseRes.res(new ClassBody(statics, fields, members, params, body, superExpr.result, hasConstr), n);
    }

    // public FunctionStatementNode(Location loc, Location end, Parameters params, CompoundNode body, String name) {
    //     super(loc, end, params, body);
    //     this.name = name;
    // }
}
