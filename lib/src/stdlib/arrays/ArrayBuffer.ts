import { buffer, type InternalBuffer, map, symbol } from "../primordials.ts";

export const abs = new map(true);

export class ArrayBuffer {
	#internal!: InternalBuffer;

	public get byteLength() {
		return this.#internal.length;
	}
	public get byteOffset() {
		return 0;
	}

	public constructor(val: unknown) {
		if (buffer.isBuff(val)) this.#internal = val;
		else this.#internal = buffer.buff(Number(val));
	}

	public static unwrap(instance: ArrayBuffer) {
		return instance.#internal;
	}
}

function wrapAB(buff: InternalBuffer): ArrayBuffer {
	let res = abs.get(buff);
	if (res == null) {
		res = new ArrayBuffer(buff);
		abs.set(buff, res);
	}

	return res;
}
const unwrapAB = ArrayBuffer.unwrap;

delete (ArrayBuffer as any).unwrap;



export { wrapAB, unwrapAB };

