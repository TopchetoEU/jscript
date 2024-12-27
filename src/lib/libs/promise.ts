import { func, next, object, symbol } from "./primordials.ts";

enum PromiseState {
	Pending = "pend",
	Fulfilled = "ful",
	Rejected = "rej",
}

const pState: unique symbol = symbol.makeSymbol("Promise.state") as any;
const pValue: unique symbol = symbol.makeSymbol("Promise.value") as any;
const pFulHandles: unique symbol = symbol.makeSymbol("Promise.fulfillHandles") as any;
const pRejHandles: unique symbol = symbol.makeSymbol("Promise.rejectHandles") as any;

function makePromise<T>(): Promise<T> {
	return object.setPrototype({
		[pState]: PromiseState.Pending,
		[pFulHandles]: [],
		[pRejHandles]: [],
	}, Promise.prototype) as Promise<T>;
}

function fulfill(self: Promise<any>, val: any) {
	if (self[pState] !== PromiseState.Pending) return;
	if (self === val) throw new Error("A promise may not be fulfilled with itself");

	if (val != null && typeof val.then === "function") {
		val.then(
			(val: any) => fulfill(self, val),
			(err: any) => reject(self, err),
		);
	}
	else {
		self[pValue] = val;
		self[pState] = PromiseState.Fulfilled;

		const handles = self[pFulHandles]!;

		for (let i = 0; i < handles.length; i++) {
			handles[i](val);
		}

		self[pFulHandles] = undefined;
		self[pRejHandles] = undefined;
	}
}
function reject(self: Promise<any>, val: any) {
	if (self[pState] !== PromiseState.Pending) return;
	if (self === val) throw new Error("A promise may not be rejected with itself");

	if (val != null && typeof val.then === "function") {
		val.then(
			(val: any) => reject(self, val),
			(err: any) => reject(self, err),
		);
	}
	else {
		self[pValue] = val;
		self[pState] = PromiseState.Rejected;

		const handles = self[pRejHandles]!;

		for (let i = 0; i < handles.length; i++) {
			handles[i](val);
		}

		self[pFulHandles] = undefined;
		self[pRejHandles] = undefined;
	}
}
function handle<T>(self: Promise<T>, ful?: (val: T) => void, rej?: (err: any) => void) {
	if (self[pState] === PromiseState.Pending) {
		if (ful != null) {
			self[pFulHandles]![self[pFulHandles]!.length] = ful;
		}
		if (rej != null) {
			self[pRejHandles]![self[pRejHandles]!.length] = rej;
		}
	}
	else if (self[pState] === PromiseState.Fulfilled) {
		if (ful != null) ful(self[pValue] as T);
	}
	else if (self[pState] === PromiseState.Rejected) {
		if (rej != null) rej(self[pValue]);
	}
}

export class Promise<T> {
	public [pState]: PromiseState;
	public [pValue]?: T | unknown;
	public [pFulHandles]?: ((val: T) => void)[] = [];
	public [pRejHandles]?: ((val: T) => void)[] = [];

	public then<Res>(ful?: (val: T) => Res, rej?: (err: any) => Res) {
		if (typeof ful !== "function") ful = undefined;
		if (typeof rej !== "function") rej = undefined;

		const promise = makePromise<Res>();

		handle(this,
			val => next(() => {
				if (ful == null) fulfill(promise, val);
				else {
					try { fulfill(promise, ful(val)); }
					catch (e) { reject(promise, e); }
				}
			}),
			err => next(() => {
				if (rej == null) reject(promise, err);
				else {
					try { fulfill(promise, rej(err)); }
					catch (e) { reject(promise, e); }
				}
			}),
		);

		return promise;
	}
	public catch<Res>(rej?: (err: any) => Res) {
		return this.then(undefined, rej);
	}
	public finally(fn?: () => void) {
		if (typeof fn !== "function") return this["then"]();

		return this.then(
			v => {
				fn();
				return v;
			},
			v => {
				fn();
				throw v;
			},
		)
	}

	public constructor(fn: (fulfil: (val: T) => void, reject: (err: unknown) => void) => void) {
		this[pState] = PromiseState.Pending;

		fn(val => fulfill(this, val), err => reject(this, err));
	}

	public static resolve(val: any) {
		const res = makePromise();
		fulfill(res, val);
		return res;
	}
	public static reject(val: any) {
		const res = makePromise();
		reject(res, val);
		return res;
	}
}

func.setCallable(Promise, false);
