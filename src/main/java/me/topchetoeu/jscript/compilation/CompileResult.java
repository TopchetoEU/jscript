package me.topchetoeu.jscript.compilation;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import me.topchetoeu.jscript.common.FunctionBody;
import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.common.mapping.FunctionMap;
import me.topchetoeu.jscript.common.mapping.FunctionMap.FunctionMapBuilder;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.compilation.scope.Scope;

public final class CompileResult {
    public static final class ChildData {
        public final int id;
        public final CompileResult result;

        public ChildData(int id, CompileResult result) {
            this.result = result;
            this.id = id;
        }
    }

    public final List<IntFunction<Instruction>> instructions;
    public final List<CompileResult> children;
    public final FunctionMapBuilder map;
    public final Environment env;
    public int length;
    public Runnable buildTask = () -> {
        throw new IllegalStateException("Compile result is not ready to be built");
    };
    public final Scope scope;

    public int temp() {
        instructions.add(null);
        return instructions.size() - 1;
    }

    public CompileResult add(Instruction instr) {
        instructions.add(i -> instr);
        return this;
    }
    public CompileResult add(IntFunction<Instruction> instr) {
        instructions.add(instr);
        return this;
    }
    public CompileResult set(int i, Instruction instr) {
        instructions.set(i, _i -> instr);
        return this;
    }
    public CompileResult set(int i, IntFunction<Instruction>instr) {
        instructions.set(i, instr);
        return this;
    }
    public int size() { return instructions.size(); }

    public void setDebug(Location loc, BreakpointType type) {
        map.setDebug(loc, type);
    }
    public void setLocation(int i, Location loc) {
        map.setLocation(i, loc);
    }
    public void setLocationAndDebug(int i, Location loc, BreakpointType type) {
        map.setLocationAndDebug(i, loc, type);
    }
    public void setDebug(BreakpointType type) {
        setDebug(map.last(), type);
    }
    public void setLocation(Location type) {
        setLocation(instructions.size() - 1, type);
    }
    public void setLocationAndDebug(Location loc, BreakpointType type) {
        setLocationAndDebug(instructions.size() - 1, loc, type);
    }

    public void beginScope() {
        // for (var cap : scope.capturables()) {
        //     add(_i -> Instruction.capInit(cap.index().index));
        // }
    }
    public void reallocScope() {
        for (var cap : scope.capturables()) {
            add(_i -> cap.index().toGet());
            add(_i -> Instruction.capFree(cap.index().index));
            add(_i -> cap.index().toInit());
        }

        scope.end();
    }
    public void endScope() {
        for (var cap : scope.capturables()) {
            add(_i -> Instruction.capFree(cap.index().index));
        }
        for (var var : scope.locals()) {
            add(_i -> Instruction.varFree(var.index().index));
        }

        scope.end();
    }

    public int addChild(CompileResult res) {
        this.children.add(res);
        return this.children.size() - 1;
    }

    public Instruction[] instructions() {
        var res = new Instruction[instructions.size()];
        var i = 0;
        for (var suppl : instructions) {
            res[i] = suppl.apply(i);
            i++;
        }
        return res;
    }

    public FunctionMap map() {
        return map.build(scope);
    }
    public FunctionBody body() {
        var builtChildren = new FunctionBody[children.size()];

        for (var i = 0; i < children.size(); i++) builtChildren[i] = children.get(i).body();

        var instrRes = new Instruction[instructions.size()];
        var i = 0;

        for (var suppl : instructions) {
            instrRes[i] = suppl.apply(i);
            // System.out.println(instrRes[i]);
            i++;
        }

        return new FunctionBody(
            scope.localsCount(), scope.capturablesCount(), scope.capturesCount(),
            length, instrRes, builtChildren
        );
    }

    public CompileResult subtarget() {
        return new CompileResult(new Scope(scope), this);
    }

    public CompileResult(Environment env, Scope scope, int length, Consumer<CompileResult> task) {
        this.scope = scope;
        this.instructions = new ArrayList<>();
        this.children = new LinkedList<>();
        this.map = FunctionMap.builder();
        this.env = env;
        this.length = length;
        this.buildTask = () -> task.accept(this);
    }
    private CompileResult(Scope scope, CompileResult parent) {
        this.scope = scope;
        this.instructions = parent.instructions;
        this.children = parent.children;
        this.map = parent.map;
        this.env = parent.env;
    }
}
