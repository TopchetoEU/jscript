import { buffer, type InternalBuffer, map, symbol } from "../primordials.ts";

export const abs = new map(true);
export const abKey: unique symbol = symbol.getSymbol("ArrayBuffer.impl") as any;

export class ArrayBuffer {
	public [abKey]!: InternalBuffer;

	public get byteLength() {
		return this[abKey].length;
	}
	public get byteOffset() {
		return 0;
	}

	public constructor(val: unknown) {
		if (buffer.isBuff(val)) this[abKey] = val;
		else this[abKey] = buffer.buff(Number(val));
	}
}

export function getAB(buff: InternalBuffer): ArrayBuffer {
	let res = abs.get(buff);
	if (res == null) {
		res = new ArrayBuffer(buff);
		abs.set(buff, res);
	}

	return res;
}
