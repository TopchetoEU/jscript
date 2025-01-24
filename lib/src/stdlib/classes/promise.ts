import { next } from "../primordials.ts";

enum PromiseState {
	Pending = "pend",
	Fulfilled = "ful",
	Rejected = "rej",
}

export class Promise<T> {
	static #InternalPromise = function <T>(this: Promise<T>) {
		this.#state = PromiseState.Pending;
		this.#fulHandles = [];
		this.#rejHandles = [];
	} as any as new <T>() => Promise<T>;

	static {
		this.#InternalPromise.prototype = this.prototype;
	}

	#state: PromiseState;
	#value?: T | unknown;
	#fulHandles?: ((val: T) => void)[] = [];
	#rejHandles?: ((val: T) => void)[] = [];

	#fulfill(val: any) {
		if (this.#state !== PromiseState.Pending) return;
		if (this === val) throw new Error("A promise may not be fulfilled with itself");

		if (val != null && typeof val.then === "function") {
			val.then(
				(val: any) => this.#fulfill(val),
				(err: any) => this.#reject(err),
			);
		}
		else {
			this.#value = val;
			this.#state = PromiseState.Fulfilled;

			const handles = this.#fulHandles!;

			for (let i = 0; i < handles.length; i++) {
				handles[i](val);
			}

			this.#fulHandles = undefined;
			this.#rejHandles = undefined;
		}
	}
	#reject(val: any) {
		if (this.#state !== PromiseState.Pending) return;
		if (this === val) throw new Error("A promise may not be rejected with itself");

		if (val != null && typeof val.then === "function") {
			val.then(
				(val: any) => this.#reject(val),
				(err: any) => this.#reject(err),
			);
		}
		else {
			this.#value = val;
			this.#state = PromiseState.Rejected;

			const handles = this.#rejHandles!;

			for (let i = 0; i < handles.length; i++) {
				handles[i](val);
			}

			this.#fulHandles = undefined;
			this.#rejHandles = undefined;
		}
	}
	#handle(ful?: (val: T) => void, rej?: (err: any) => void) {
		if (this.#state === PromiseState.Pending) {
			if (ful != null) {
				this.#fulHandles![this.#fulHandles!.length] = ful;
			}
			if (rej != null) {
				this.#rejHandles![this.#rejHandles!.length] = rej;
			}
		}
		else if (this.#state === PromiseState.Fulfilled) {
			if (ful != null) ful(this.#value as T);
		}
		else if (this.#state === PromiseState.Rejected) {
			if (rej != null) rej(this.#value);
		}
	}

	public then<Res>(ful?: (val: T) => Res, rej?: (err: any) => Res) {
		if (typeof ful !== "function") ful = undefined;
		if (typeof rej !== "function") rej = undefined;

		const promise = new Promise.#InternalPromise<Res>();

		this.#handle(
			val => next(() => {
				if (ful == null) promise.#fulfill(val);
				else {
					try { promise.#fulfill(ful(val)); }
					catch (e) { promise.#reject(e); }
				}
			}),
			err => next(() => {
				if (rej == null) promise.#reject(err);
				else {
					try { promise.#fulfill(rej(err)); }
					catch (e) { promise.#reject(e); }
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
		this.#state = PromiseState.Pending;

		fn(val => this.#fulfill(val), err => this.#reject(err));
	}

	public static resolve(val: any) {
		const res = new this.#InternalPromise();
		res.#fulfill(val);
		return res;
	}
	public static reject(val: any) {
		const res = new this.#InternalPromise();
		res.#reject(val);
		return res;
	}
}
