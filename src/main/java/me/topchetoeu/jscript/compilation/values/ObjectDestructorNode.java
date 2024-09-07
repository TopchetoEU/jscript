package me.topchetoeu.jscript.compilation.values;

import java.util.LinkedHashMap;
import java.util.Map;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Node;
import me.topchetoeu.jscript.compilation.JavaScript.DeclarationType;
import me.topchetoeu.jscript.compilation.destructing.Destructor;


public class ObjectDestructorNode extends Node implements Destructor {
    public final LinkedHashMap<String, Destructor> destructors;
    public final VariableNode restDestructor;

    private void compileRestObjBuilder(CompileResult target, int srcDupN) {
        var subtarget = target.subtarget();
        var src = subtarget.scope.defineTemp();
        var dst = subtarget.scope.defineTemp();

        target.add(Instruction.loadObj());
        target.add(_i -> src.index().toSet(true));
        target.add(_i -> dst.index().toSet(destructors.size() > 0));

        target.add(Instruction.keys(true, true));
        var start = target.size();

        target.add(Instruction.dup());
        var mid = target.temp();

        target.add(_i -> src.index().toGet());
        target.add(Instruction.dup(1, 1));
        target.add(Instruction.loadMember());

        target.add(_i -> dst.index().toGet());
        target.add(Instruction.dup(1, 1));
        target.add(Instruction.storeMember());

        target.add(Instruction.discard());
        var end = target.size();
        target.add(Instruction.jmp(start - end));
        target.set(mid, Instruction.jmpIfNot(end - mid + 1));

        target.add(Instruction.discard());

        target.add(Instruction.dup(srcDupN, 1));

        target.scope.end();
    }

    @Override public void destructDeclResolve(CompileResult target) {
        for (var el : destructors.values()) {
            el.destructDeclResolve(target);
        }

        if (restDestructor != null) restDestructor.destructDeclResolve(target);
    }
    @Override public void destructArg(CompileResult target) {
        if (restDestructor != null) compileRestObjBuilder(target, destructors.size() * 2);
        else if (destructors.size() > 0) target.add(Instruction.dup(destructors.size(), 0));

        for (var el : destructors.entrySet()) {
            if (restDestructor != null) {
                target.add(Instruction.pushValue(el.getKey()));
                target.add(Instruction.delete());
            }

            target.add(Instruction.loadMember(el.getKey()));
            el.getValue().destructArg(target);
        }

        if (restDestructor != null) restDestructor.destructArg(target);

        target.add(Instruction.discard());
    }
    @Override public void afterAssign(CompileResult target, DeclarationType decl) {
        if (restDestructor != null) compileRestObjBuilder(target, destructors.size() * 2);
        else if (destructors.size() > 0) target.add(Instruction.dup(destructors.size(), 0));

        for (var el : destructors.entrySet()) {
            if (restDestructor != null) {
                target.add(Instruction.pushValue(el.getKey()));
                target.add(Instruction.delete());
            }

            target.add(Instruction.loadMember(el.getKey()));
            el.getValue().afterAssign(target, decl);
        }

        if (restDestructor != null) restDestructor.afterAssign(target, decl);

        target.add(Instruction.discard());
    }
    @Override public void destructAssign(CompileResult target, boolean pollute) {
        if (restDestructor != null) compileRestObjBuilder(target, destructors.size() * 2);
        else if (destructors.size() > 0) target.add(Instruction.dup(destructors.size(), 0));

        for (var el : destructors.entrySet()) {
            if (restDestructor != null) {
                target.add(Instruction.pushValue(el.getKey()));
                target.add(Instruction.delete());
            }

            target.add(Instruction.loadMember(el.getKey()));
            el.getValue().destructAssign(target, pollute);
        }

        if (restDestructor != null) restDestructor.destructAssign(target, pollute);

        if (!pollute) target.add(Instruction.discard());
    }

    public ObjectDestructorNode(Location loc, Map<String, Destructor> map, VariableNode rest) {
        super(loc);
        this.destructors = new LinkedHashMap<>(map);
        this.restDestructor = rest;
    }
    public ObjectDestructorNode(Location loc, Map<String, Destructor> map) {
        super(loc);
        this.destructors = new LinkedHashMap<>(map);
        this.restDestructor = null;
    }

    private static ParseRes<String> parsePropName(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);

        var res = ParseRes.first(src, i + n,
            Parsing::parseIdentifier,
            Parsing::parseString,
            (s, j) -> Parsing.parseNumber(s, j, false)
        );
        if (!res.isSuccess()) return res.chainError();
        n += res.n;

        if (!src.is(i + n, ":")) return ParseRes.error(src.loc(i + n), "Expected a colon");
        n++;

        return ParseRes.res(res.result.toString(), n);
    }

    public static ParseRes<ObjectDestructorNode> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!src.is(i + n, "{")) return ParseRes.failed();
        n++;
        n += Parsing.skipEmpty(src, i + n);

        var destructors = new LinkedHashMap<String, Destructor>();

        if (src.is(i + n, "}")) {
            n++;
            return ParseRes.res(new ObjectDestructorNode(loc, destructors), n);
        }

        while (true) {
            n += Parsing.skipEmpty(src, i + n);

            // if (src.is(i, null))

            var name = parsePropName(src, i + n);
            if (!name.isSuccess()) return ParseRes.error(src.loc(i + n), "Expected a field name");
            n += name.n;
            n += Parsing.skipEmpty(src, i + n);

            var destructor = Destructor.parse(src, i + n);
            if (!destructor.isSuccess()) return destructor.chainError(src.loc(i + n), "Expected a value in array list");
            n += destructor.n;

            destructors.put(name.result, destructor.result);

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
    
        return ParseRes.res(new ObjectDestructorNode(loc, destructors), n);
    }

}
