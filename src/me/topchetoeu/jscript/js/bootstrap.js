// TODO: load this in java
var ts = require('./ts__');
log("Loaded typescript!");

var src = '', lib = libs.concat([ 'declare const exit: never;' ]).join(''), decls = '', version = 0;
var libSnapshot = ts.ScriptSnapshot.fromString(lib);

var settings = {
    outDir: "/out",
    declarationDir: "/out",
    target: ts.ScriptTarget.ES5,
    lib: [ ],
    module: ts.ModuleKind.None,
    declaration: true,
    stripInternal: true,
    downlevelIteration: true,
    forceConsistentCasingInFileNames: true,
    experimentalDecorators: true,
    strict: true,
};

var reg = ts.createDocumentRegistry();
var service = ts.createLanguageService({
    getCurrentDirectory: function() { return "/"; },
    getDefaultLibFileName: function() { return "/lib_.d.ts"; },
    getScriptFileNames: function() { return [ "/src.ts", "/lib.d.ts", "/glob.d.ts" ]; },
    getCompilationSettings: function () { return settings; },
    fileExists: function(filename) { return filename === "/lib.d.ts" || filename === "/src.ts" || filename === "/glob.d.ts"; },

    getScriptSnapshot: function(filename) {
        if (filename === "/lib.d.ts") return libSnapshot;
        if (filename === "/src.ts") return ts.ScriptSnapshot.fromString(src);
        if (filename === "/glob.d.ts") return ts.ScriptSnapshot.fromString(decls);
        throw new Error("File '" + filename + "' doesn't exist.");
    },
    getScriptVersion: function (filename) {
        if (filename === "/lib.d.ts") return 0;
        else return version;
    },
}, reg);

service.getEmitOutput('/lib.d.ts');
log('Loaded libraries!');

function compile(filename, code) {
    src = code, version++;

    var emit = service.getEmitOutput("/src.ts");

    var diagnostics = []
        .concat(service.getCompilerOptionsDiagnostics())
        .concat(service.getSyntacticDiagnostics("/src.ts"))
        .concat(service.getSemanticDiagnostics("/src.ts"))
        .map(function (diagnostic) {
            var message = ts.flattenDiagnosticMessageText(diagnostic.messageText, "\n");
            if (diagnostic.file) {
                var pos = diagnostic.file.getLineAndCharacterOfPosition(diagnostic.start);
                var file = diagnostic.file.fileName.substring(1);
                if (file === "src.ts") file = filename;
                return file + ":" + (pos.line + 1) + ":" + (pos.character + 1) + ": " + message;
            }
            else return "Error: " + message;
        });

    if (diagnostics.length > 0) {
        throw new SyntaxError(diagnostics.join('\n'));
    }

    return {
        result: emit.outputFiles[0].text,
        declaration: emit.outputFiles[1].text
    };
}

init(function (filename, code) {
    var res = compile(filename, code);

    return [
        res.result,
        function(func, th, args) {
            var val = func.apply(th, args);
            decls += res.declaration;
            return val;
        }
    ];
});
