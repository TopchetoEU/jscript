var { define, run } = (() => {
    const modules: Record<string, Function> = {};

    function define(name: string, func: Function) {
        modules[name] = func;
    }
    function run(name: string) {
        if (typeof modules[name] === 'function') return modules[name]();
        else throw "The module '" + name + "' doesn't exist.";
    }

    return { define, run };
})();
