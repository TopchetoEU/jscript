import { func, json, object } from "../primordials.ts";

export const console = {};

function method(name: string, func: Function) {
	object.defineField(console, name, { c: true, e: false, w: true, v: func });
}

method("log", function log() {
	func.invoke(print, null, arguments as any);
});
