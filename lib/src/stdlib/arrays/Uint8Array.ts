import { buffer } from "../primordials.ts";
import { token, TypedArray, typedArrayFuncs } from "./TypedArray.ts";

const factory = buffer.uint8;
const funcs = typedArrayFuncs(1, factory);

export class Uint8Array extends TypedArray {
	public override get byteOffset() {
		return funcs.byteOffset(this as any);
	}
	public override get byteLength() {
		return funcs.byteLength(this as any);
	}

	public override subarray(this: number[], start?: number, end?: number) {
		return funcs.subarray(this, start, end);
	}
	public override slice(this: any[], start?: number, end?: number) {
		return funcs.slice(this, start, end);
	}
	public override map(this: any[], cb: (val: number, i: number, self: any) => number, self?: any) {
		return funcs.map(this, cb, self);
	}
	public override filter(this: any[], cb: (val: number, i: number, self: any) => boolean, self?: any) {
		return funcs.filter(this, cb, self);
	}

	public constructor(obj: any, start?: number, end?: number) {
		super(token);
		return funcs.construct(obj, start, end) as any;
	}
}