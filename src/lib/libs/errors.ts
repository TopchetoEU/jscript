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
object.defineField(Error.prototype, "name", true, false, true, "Error");
object.defineField(Error.prototype, "message", true, false, true, "");
func.setCallable(Error, true);
func.setConstructable(Error, true);

export class SyntaxError extends Error { }
object.defineField(SyntaxError.prototype, "name", true, false, true, "SyntaxError");
func.setCallable(SyntaxError, true);
func.setConstructable(SyntaxError, true);

export class TypeError extends Error { }
object.defineField(TypeError.prototype, "name", true, false, true, "TypeError");
func.setCallable(TypeError, true);
func.setConstructable(TypeError, true);

export class RangeError extends Error { }
object.defineField(RangeError.prototype, "name", true, false, true, "RangeError");
func.setCallable(RangeError, true);
func.setConstructable(RangeError, true);
