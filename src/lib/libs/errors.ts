import { func, object } from "./primordials.ts";
import { String } from "./string.ts";

export class Error {
	public declare name: string;
	public declare message: string;

	public toString() {
		let res = this.name || "Error";
		const msg = this.message;

		if (msg) res += ": " + msg;
		return res;
	}

	public constructor (msg = "") {
		if (func.invokeType(arguments, this) === "call") return new Error(msg);
		this.message = String(msg);
	}
}
object.defineField(Error.prototype, "name", { c: true, e: false, w: true, v: "Error" });
object.defineField(Error.prototype, "message", { c: true, e: false, w: true, v: "" });
func.setCallable(Error, true);
func.setConstructable(Error, true);

export class SyntaxError extends Error { }
object.defineField(SyntaxError.prototype, "name", { c: true, e: false, w: true, v: "SyntaxError" });
func.setCallable(SyntaxError, true);
func.setConstructable(SyntaxError, true);

export class TypeError extends Error { }
object.defineField(TypeError.prototype, "name", { c: true, e: false, w: true, v: "TypeError" });
func.setCallable(TypeError, true);
func.setConstructable(TypeError, true);

export class RangeError extends Error { }
object.defineField(RangeError.prototype, "name", { c: true, e: false, w: true, v: "RangeError" });
func.setCallable(RangeError, true);
func.setConstructable(RangeError, true);
