import { compile, func, string } from "./primordials.ts";
import { String } from "./string.ts";

export const Function = (() => {
	class Function {
		declare public readonly name: string;
		declare public readonly length: number;

		public toString(this: Function) {
			if (this.name !== "") return "function " + this.name + "(...) { ... }";
			else return "function (...) { ... }";
		}
		public valueOf() {
			return this;
		}

		public apply(this: (...args: any) => any, self: any, args: any[]) {
			return func.invoke(this, self, args);
		}
		public call(this: (...args: any) => any, self: any) {
			const args: any[] = [];
			for (let i = arguments.length - 1; i >= 1; i--) args[i - 1] = arguments[i];
			return func.invoke(this, self, args);
		}
		public bind(this: (...args: any) => any, self: any) {
			const cb = this;
			if (arguments.length === 0) return function (this: any) { return func.invoke(cb, this, arguments as any) };
			if (arguments.length <= 1) return function () { return func.invoke(cb, self, arguments as any); }

			const base: any[] = [];
			const offset = arguments.length - 1;
			base.length = offset;

			for (let i = 0; i < offset; i++) base[i] = arguments[i + 1];

			return function () {
				for (let i = 0; i < arguments.length; i++) {
					base[offset + i] = arguments[i];
				}

				return func.invoke(cb, self, base);
			};
		}

		public constructor () {
			const parts = ["(function anonymous("];
			for (let i = 0; i < arguments.length - 1; i++) {
				if (i > 0) parts[parts.length] = ",";
				parts[parts.length] = arguments[i];
			}
			parts[parts.length] = "){\n";
			parts[parts.length] = String(arguments[arguments.length - 1]);
			parts[parts.length] = "\n})";
			var res = compile(string.stringBuild(parts))();
			return res;
		}

		public static compile(src = "", { globals = [], wrap = false }: { globals?: string[], wrap?: boolean } = {}) {
			const parts = [];

			if (wrap) parts[parts.length] = "return (function() {\n";
			if (globals.length > 0) {
				parts[parts.length] = "let {";
				for (let i = 0; i < globals.length; i++) {
					if (i > 0) parts[parts.length] = ",";
					parts[parts.length] = globals[i];
				}
				parts[parts.length] = "} = arguments[0];";
			}
			parts[parts.length] = src;
			if (wrap) parts[parts.length] = "\n})(arguments[0])";

			const res = compile(string.stringBuild(parts));
			return res;
		}
	}

	func.setCallable(Function, true);
	func.setConstructable(Function, true);

	return Function as any as typeof Function & ((value?: unknown) => (...args: any[]) => any);
})();
