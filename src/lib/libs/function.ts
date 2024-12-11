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
		public call(this: (...args: any) => any, self: any, ...args: any[]) {
			return func.invoke(this, self, args);
		}

		public constructor (...args: string[]) {
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
