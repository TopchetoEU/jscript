import { parse } from "acorn";
import {} from "acorn";
import { traverse } from "estraverse";
import { Declaration, Identifier, Node, VariableDeclaration } from "estree";

enum VariableType {
	Var,
	Let,
	Const,
}

class Variable {
	public constructor(
		public readonly scope: Scope,
		public readonly name: string,
		public readonly type: VariableType,
		public readonly readers = new Set<Node>(),
		public readonly writers = new Set<Node>(),
	) { }

	public child(scope: Scope) {
		return new Variable(scope, this.name, this.type, this.readers, this.writers);
	}

	public get writable() {
		return this.type !== VariableType.Const;
	}
}

class Scope {
	private _locals = new Set<Variable>();
	private _capturables = new Set<Variable>();
	private _captures = new Set<Variable>();

	private _localNames = new Map<string, Variable>();
	private _captureNames = new Map<string, Variable>();
	private _parentToChild = new Map<Variable, Variable>();
	private _childToParent = new Map<Variable, Variable>();

	public get locals() {
		return Iterator.from(this._locals.values());
	}

	private _addCapture(v?: Variable) {
		if (v != null && this._locals.delete(v)) {
			this._capturables.add(v);
		}

		return v;
	}

	public capture(name: string) {
		if (this._localNames.has(name)) return this._addCapture(this._localNames.get(name));
		if (this._captureNames.has(name)) return this._addCapture(this._captureNames.get(name));

		const parent = this.parent?.capture(name);
		if (parent == null) return undefined;

		const child = parent.child(this);

		this._parentToChild.set(parent, child);
		this._childToParent.set(child, parent);
		this._captures.add(child);
		this._captureNames.set(child.name, child);
	}

	public add(name: string, type: VariableType) {
		let res = this.get(name, false);
		if (res != null) return res;

		res = new Variable(this, name, type);
		this._locals.add(res);
		this._localNames.set(name, res);
		return res;
	}
	public get(name: string, capture = true): Variable | undefined {
		if (this._localNames.has(name)) return this._localNames.get(name);
		if (this._captureNames.has(name)) return this._captureNames.get(name);

		if (capture) this.parent?.capture(name);
		else return undefined;
	}

	public constructor(
		public readonly major: boolean,
		public readonly node: Node,
		public readonly parent?: Scope,
	) { }
}

class BiMap<A, B> implements Iterable<[A, B]> {
	private _first = new Map<A, B>();
	private _second = new Map<B, A>();

	public get(val: A): B;
	public get(val: B): A;
	public get(val: any) {
		if (this._first.has(val)) return this._first.get(val);
		if (this._second.has(val)) return this._second.get(val);
		if (this._same.has(val)) return val;
		return undefined;
	}

	public set(a: A, b: B) {
		this._first.set(a, b);
		this._second.set(b, a);

		return this;
	}

	public has(val: A | B) {
		return this._first.has(val as any) || this._second.has(val as any);
	}

	public delete(val: A | B) {
		if (this._first.has(val as any)) {
			const second = this._first.get(val as any)!;
			this._first.delete(val as any);
			this._second.delete(second);

			return true;
		}
		else if (this._second.has(val as any)) {
			const first = this._second.get(val as any)!;
			this._second.delete(val as any);
			this._first.delete(first);

			return true;
		}
		else return false;
	}

	public *[Symbol.iterator]() {
		yield *this._first;
	}
	public *keys() {
		yield *this._first.keys();
	}
	public *values() {
		yield *this._second.keys();
	}
	public *entries() {
		yield *this._first.entries();
	}
}

class ResolutionContext {
	public readonly variableRefs = new Map<Identifier, Scope>();
	public readonly declarations = new BiMap<Variable, Declaration>();

	public resolveVariables() {
		for (const el of this.variableRefs) {
		}
	}
}

class NodeContext {
	public node: Node = undefined!;
	public path: Node[] = [];

	public atPath(i: number) {
		return this.path[this.path.length - 1 - i];
	}

	public scope: Scope = undefined!;
	public declType?: VariableType;
}

interface Collector {
	enter(ctx: NodeContext, root: ResolutionContext): void;
	leave(ctx: NodeContext, root: ResolutionContext): void;
}

function collect(node: Node, root: ResolutionContext, ...collectors: Collector[]) {
	const nodeCtx = new NodeContext();
	const path: Node[] = [];

	traverse(node, {
		enter(node) {
			nodeCtx.node = node;
			nodeCtx.path.push(node);
			for (let i = 0; i < collectors.length; i++) {
				collectors[i].enter(nodeCtx, root);
			}
		},
		leave(node) {
			nodeCtx.node = node;
			nodeCtx.path.pop();
			for (let i = 0; i < collectors.length; i++) {
				collectors[i].leave(nodeCtx, root);
			}
		},
	});
}

function assertDefined(val: unknown): asserts val is {} {
	if (val == null) throw new Error("Undefined or null expression");
}

const scopeCollector: Collector = {
	enter(ctx, root) {
		if (ctx.node.type === "BlockStatement") {
			ctx.scope = new Scope(false, ctx.node, ctx.scope.parent);
		}
		else if (ctx.node.type === "VariableDeclaration") {
			switch (ctx.node.kind) {
				case "var": ctx.declType = VariableType.Var; break;
				case "let": ctx.declType = VariableType.Let; break;
				case "const": ctx.declType = VariableType.Const; break;
				default: throw new Error(`Unknown variable type '${(ctx.node as any).kind}'`);
			}
		}
		else if (ctx.node.type === "VariableDeclarator") {
			ctx.scope.
		}
		else if (ctx.node.type === "ClassDeclaration") {

		}
		else if (ctx.node.type === "Identifier") {
		}
	},
	leave(ctx, root) {
		if (ctx.scope.node === ctx.node) {
			assertDefined(ctx.scope.parent);
			ctx.scope = ctx.scope.parent;
		}
		else if (ctx.node.type === "VariableDeclaration") {
			ctx.declType = undefined;
		}
	},
};


const program = parse("const a = 10;", { ecmaVersion: "latest" }) as Node;
collect(program, domain, ...stage1);