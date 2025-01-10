declare interface Boolean {
	valueOf(): boolean;
	toString(): string;
}
declare interface BooleanConstructor {
	new (val?: unknown): Boolean;
	(val?: unknown): boolean;
}
