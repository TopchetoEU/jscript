/// <reference no-default-lib="true"/>
/// <reference path="./typing.d.ts"/>
/// <reference path="./iterator.d.ts"/>
/// <reference path="./async.d.ts"/>
/// <reference path="./errors.d.ts"/>
/// <reference path="./values/function.d.ts"/>
/// <reference path="./values/object.d.ts"/>
/// <reference path="./values/array.d.ts"/>
/// <reference path="./values/boolean.d.ts"/>
/// <reference path="./values/symbol.d.ts"/>
/// <reference path="./values/string.d.ts"/>
/// <reference path="./values/number.d.ts"/>
/// <reference path="./values/regexp.d.ts"/>
/// <reference path="./globals/json.d.ts"/>
/// <reference path="./globals/set.d.ts"/>
/// <reference path="./globals/map.d.ts"/>
/// <reference path="./globals/weak-map.d.ts"/>

declare function dofile(name: string): any;
declare function print(...args: any[]): void;
declare function exit(): never;
declare function measure(func: () => void): void;

declare type IArguments = Array<any>;

declare var Error: ErrorConstructor;
declare var TypeError: TypeErrorConstructor;
declare var SyntaxError: SyntaxErrorConstructor;
declare var RangeError: RangeErrorConstructor;

declare var Array: ArrayConstructor;
declare var Boolean: BooleanConstructor;
declare var Function: FunctionConstructor;
declare var Symbol: SymbolConstructor;
declare var Number: NumberConstructor;
declare var Object: ObjectConstructor;
declare var RegExp: RegExpConstructor;
declare var String: StringConstructor;
declare var Promise: PromiseConstructor;

declare var JSON: JSON;

declare var Set: SetConstructor;
declare var Map: MapConstructor;
declare var WeakMap: WeakMapConstructor;
