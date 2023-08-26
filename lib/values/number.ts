interface Number {
    toString(): string;
    valueOf(): number;
    constructor: NumberConstructor;
}
interface NumberConstructor {
    (val: any): number;
    new (val: any): Number;
    prototype: Number;
    parseInt(val: unknown): number;
    parseFloat(val: unknown): number;
}

declare var Number: NumberConstructor;
declare var parseInt: typeof Number.parseInt;
declare var parseFloat: typeof Number.parseFloat;
declare var NaN: number;
declare var Infinity: number;

gt.Number = function(this: Number | undefined, arg: any) {
    var val;
    if (arguments.length === 0) val = 0;
    else val = arg - 0;
    if (this === undefined || this === null) return val;
    else (this as any).value = val;
} as NumberConstructor;

Number.prototype = (0 as any).__proto__ as Number;
setConstr(Number.prototype, Number);

setProps(Number.prototype, {
    valueOf() {
        if (typeof this === 'number') return this;
        else return (this as any).value;
    },
    toString() {
        if (typeof this === 'number') return this + '';
        else return (this as any).value + '';
    }
});

setProps(Number, {
    parseInt(val) { return Math.trunc(Number.parseFloat(val)); },
    parseFloat(val) { return internals.parseFloat(val); },
});

Object.defineProperty(gt, 'parseInt', { value: Number.parseInt, writable: false });
Object.defineProperty(gt, 'parseFloat', { value: Number.parseFloat, writable: false });
Object.defineProperty(gt, 'NaN', { value: 0 / 0, writable: false });
Object.defineProperty(gt, 'Infinity', { value: 1 / 0, writable: false });
