declare namespace http {
	interface Socket {
		read(n: number): Promise<Uint8Array | undefined>;
		write(data: Uint8Array): Promise<void>;

		[Symbol.asyncIterator](): AsyncIterator<Uint8Array>;
	}

	class Server implements AsyncIterableIterator<Socket> {
		bind(address: string): Promise<this>;
		next(): Promise<IterationData<Socket, void>>;
		[Symbol.iterator](): this;

		constructor();
	}
}