import { json, object } from "./primordials";

export const JSON = {};

function method(name: string, func: Function) {
	object.defineField(JSON, name, { c: true, e: false, w: true, v: func });
}

method("parse", function parse(val: string) {
	return json.parse(val);
});

method("stringify", function stringify(val: string) {
	return json.stringify(val);
});
