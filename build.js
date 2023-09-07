const { spawn } = require('child_process');
const fs = require('fs/promises');
const pt = require('path');
const conf = require('./meta');
const { argv } = require('process');

async function* find(src, dst, wildcard) {
    const stat = await fs.stat(src);

    if (stat.isDirectory()) {
        for (const el of await fs.readdir(src)) {
            for await (const res of find(pt.join(src, el), dst ? pt.join(dst, el) : undefined, wildcard)) yield res;
        }
    }
    else if (stat.isFile() && wildcard(src)) yield dst ? { src, dst } : src;
}
async function copy(src, dst, wildcard) {
    const promises = [];

    for await (const el of find(src, dst, wildcard)) {
        promises.push((async () => {
            await fs.mkdir(pt.dirname(el.dst), { recursive: true });
            await fs.copyFile(el.src, el.dst);
        })());
    }

    await Promise.all(promises);
}

function run(cmd, ...args) {
    return new Promise((res, rej) => {
        const proc = spawn(cmd, args, { stdio: 'inherit' });
        proc.once('exit', code => {
            if (code === 0) res(code);
            else rej(new Error(`Process ${cmd} exited with code ${code}.`));
        });
    })
}

async function compileJava() {
    await fs.writeFile('Metadata.java', (await fs.readFile('src/me/topchetoeu/jscript/Metadata.java')).toString()
        .replace('${VERSION}', conf.version)
        .replace('${NAME}', conf.name)
        .replace('${AUTHOR}', conf.author)
    );

    const args = ['-d', 'dst/classes', 'Metadata.java'];
    for await (const path of find('src', undefined, v => v.endsWith('.java') && !v.endsWith('Metadata.java'))) args.push(path);
    await run(conf.javahome + '/javac', ...args);
    await fs.rm('Metadata.java');
}

(async () => {
    try {
        fs.rm('dst', { recursive: true });
        await copy('src', 'dst/classes', v => !v.endsWith('.java'));
        await run('tsc', '-p', 'lib/tsconfig.json', '--outFile', 'dst/classes/me/topchetoeu/jscript/js/core.js'),
        await compileJava();
        await run('jar', '-c', '-f', 'dst/jscript.jar', '-e', 'me.topchetoeu.jscript.Main', '-C', 'dst/classes', '.');
    }
    catch (e) {
        if (argv.includes('debug')) throw e;
        else console.log(e.toString());
    }
})();
