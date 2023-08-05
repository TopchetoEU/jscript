// TODO: load this in java
var ts = require('./ts__');

var src = '', lib = libs.join(''), decls = [], version = 0;
var libSnapshot = ts.ScriptSnapshot.fromString(lib);

var settings = {
    outDir: "/out",
    declarationDir: "/out",
    target: ts.ScriptTarget.ES5,
    lib: [ ],
    module: ts.ModuleKind.CommonJS,
    declaration: true,
    stripInternal: true,
    downlevelIteration: true,
    forceConsistentCasingInFileNames: true,
    experimentalDecorators: true,
    strict: true,
};

var reg = ts.createDocumentRegistry();
var service = ts.createLanguageService({
    getCanonicalFileName: function (fileName) { return fileName; },
    useCaseSensitiveFileNames: function () { return true; },
    getNewLine: function () { return "\n"; },
    getEnvironmentVariable: function () { return ""; },

    log: function() {
        log.apply(undefined, arguments);
    },
    fileExists: function (fileName) {
        return (
            fileName === "/src.ts" ||
            fileName === "/lib.d.ts" ||
            fileName === "/glob.d.ts"
        );
    },
    readFile: function (fileName) {
        if (fileName === "/src.ts") return src;
        if (fileName === "/lib.d.ts") return lib;
        if (fileName === "/glob.d.ts") return decls.join('\n');
        throw new Error("File '" + fileName + "' doesn't exist.");
    },
    writeFile: function (fileName, data) {
        if (fileName.endsWith(".js")) res = data;
        else if (fileName.endsWith(".d.ts")) decls.push(data);
        else throw new Error("File '" + fileName + "' isn't writable.");
    },
    getCompilationSettings: function () {
        return settings;
    },
    getCurrentDirectory: function() { return "/"; },
    getDefaultLibFileName: function() { return "/lib_.d.ts"; },
    getScriptFileNames: function() { return [ "/src.ts", "/lib.d.ts", "/glob.d.ts" ]; },
    getScriptSnapshot: function(filename) {
        if (filename === "/lib.d.ts") return libSnapshot;
        else return ts.ScriptSnapshot.fromString(this.readFile(filename));
    },
    getScriptVersion: function (filename) {
        if (filename === "/lib.d.ts") return 0;
        else return version;
    },
}, reg);

service.getEmitOutput('/lib.d.ts');
log('Loaded libraries!');


function compile(code) {
    src = code;
    version++;

    var emit = service.getEmitOutput("/src.ts");

    var res = emit.outputFiles[0].text;
    var decl = emit.outputFiles[1].text;

    var diagnostics = []
        .concat(service.getCompilerOptionsDiagnostics())
        .concat(service.getSyntacticDiagnostics("/src.ts"))
        .concat(service.getSemanticDiagnostics("/src.ts"))
        .map(function (diagnostic) {
            var message = ts.flattenDiagnosticMessageText(diagnostic.messageText, "\n");
            if (diagnostic.file) {
                var pos = diagnostic.file.getLineAndCharacterOfPosition(diagnostic.start);
                return diagnostic.file.fileName.substring(1) + ":" + (pos.line + 1) + ":" + (pos.character + 1) + ": " + message;
            }
            else return "Error: " + message;
        });

    if (diagnostics.length > 0) {
        throw new SyntaxError(diagnostics.join('\n'));
    }

    decls.push(decl);

    return {
        result: res,
        diagnostics: diagnostics
    };
}

log("Loaded typescript!");
init(function (code) {
    var res = compile(code);
    return res.result;
});

