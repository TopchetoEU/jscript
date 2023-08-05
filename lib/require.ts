type RequireFunc = (path: string) => any;

interface Module {
    exports: any;
    name: string;
}

declare var require: RequireFunc;
declare var exports: any;
declare var module: Module;

gt.require = function(path: string) {
    if (typeof path !== 'string') path = path + '';
    return internals.require(path);
};
