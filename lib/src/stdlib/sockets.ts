import { Promise as Promise } from "./classes/promise";
import { func, InternalServer, InternalSocket, symbol } from "./primordials";
import { Error } from "./values/errors";

const socketToken = symbol.makeSymbol("ServerSocket.token");

export class ServerSocket {
	#internal: InternalSocket;

	public read() {
		return new Promise((ful, rej) => {
			this.#internal.read(ful, rej, ful as any);
		});
	}
	public write(data: Uint8Array) {
		return new Promise<void>((ful, rej) => {
			this.#internal.write(data, ful, rej);
		});
	}

	public next() {
		return new Promise((ful, rej) => {
			this.#internal.read(
				data => ful({ value: data, done: false }),
				rej,
				() => ful({ value: undefined, done: true })
			);
		});
	}
	public [Symbol.iterator](): this {
		return this;
	}

	public constructor(token: typeof socketToken, socket: InternalSocket) {
		if (token !== socketToken) throw new Error("Invalid token for creation");
		this.#internal = socket;
	}
}

export class Server {
	#internal: InternalServer;

	public bind(address: string) {
		return new Promise<void>((res, rej) => this.#internal.bind(address, res, rej));
	}

	public next() {
		return new Promise((ful, rej) => {
			this.#internal.next(
				data => ful({ value: new ServerSocket(socketToken, data), done: false }),
				rej,
				() => ful({ value: undefined, done: true })
			);
		});
	}
	public [Symbol.iterator](): this {
		return this;
	}

	public constructor(server: InternalServer) {
		this.#internal = server;
	}
}
