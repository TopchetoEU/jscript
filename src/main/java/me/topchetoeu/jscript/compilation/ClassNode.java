package me.topchetoeu.jscript.compilation;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.members.FieldMemberNode;
import me.topchetoeu.jscript.compilation.members.MethodMemberNode;
import me.topchetoeu.jscript.compilation.members.PropertyMemberNode;

public abstract class ClassNode extends FunctionNode {
    public static final class ClassBody {
        public final List<Node> staticMembers;
        public final List<FieldMemberNode> protoFields;
        public final List<Node> protoMembers;
        public final Parameters constructorParameters;
        public final CompoundNode constructorBody;

        public ClassBody(
            List<Node> staticMembers, List<FieldMemberNode> protoFields, List<Node> protoMembers,
            Parameters constructorParameters, CompoundNode constructorBody
        ) {
            this.staticMembers = staticMembers;
            this.protoFields = protoFields;
            this.protoMembers = protoMembers;
            this.constructorParameters = constructorParameters;
            this.constructorBody = constructorBody;
        }
    }

    public final ClassBody body;
    public final String name;

    @Override public String name() { return name; }

    public void compileStatic(CompileResult target) {
        for (var member : body.staticMembers) member.compile(target, true);
    }
    public void compilePrototype(CompileResult target) {
        if (body.protoMembers.size() > 0) {
            target.add(Instruction.dup());
            target.add(Instruction.loadMember("prototype"));

            for (var i = 0; i < body.protoMembers.size() - 1; i++) {
                body.protoMembers.get(i).compile(target, true);
            }

            body.protoMembers.get(body.protoMembers.size() - 1).compile(target, false);
        }
    }

    @Override protected void compilePreBody(CompileResult target) {
        for (var member : body.protoFields) {
            target.add(Instruction.loadThis());
            member.compile(target, false);
        }
    }

    @Override public void compile(CompileResult target, boolean pollute, String name, BreakpointType bp) {
        var id = target.addChild(compileBody(target, name, null));
        target.add(_i -> Instruction.loadFunc(id, false, true, false, name, captures(id, target)));
        compileStatic(target);
        compilePrototype(target);
    }

    public ClassNode(Location loc, Location end, String name, ClassBody body) {
        super(loc, end, body.constructorParameters, body.constructorBody);

        this.name = name;
        this.body = body;
    }

    public static ParseRes<Node> parseMember(Source src, int i) {
        return ParseRes.first(src, i,
            PropertyMemberNode::parse,
            FieldMemberNode::parseClass,
            MethodMemberNode::parse
        );
    }

    public static ParseRes<ClassBody> parseBody(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!src.is(i + n, "{")) return ParseRes.failed();
        n++;
        n += Parsing.skipEmpty(src, i + n);

        var fields = new LinkedList<FieldMemberNode>();
        var members = new LinkedList<Node>();
        var statics = new LinkedList<Node>();

        var params = new Parameters(new ArrayList<>());
        var body = new CompoundNode(loc, false);
        var hasConstr = false;

        if (src.is(i + n, "}")) {
            n++;
            return ParseRes.res(new ClassBody(statics, fields, members, params, body), n);
        }

        while (true) {
            ParseRes<Node> prop = parseMember(src, i + n);

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
            else ParseRes.error(src.loc(i + n), "Expected a comma or a closing brace.");
        }

        return ParseRes.res(new ClassBody(statics, fields, members, params, body), n);
    }

    // public FunctionStatementNode(Location loc, Location end, Parameters params, CompoundNode body, String name) {
    //     super(loc, end, params, body);
    //     this.name = name;
    // }
}
